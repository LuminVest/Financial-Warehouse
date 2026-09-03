package com.wwfinance.api.controller.api;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.entity.dto.UserDto;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.api.utils.TokenUtil;
import com.wwfinance.common.result.PccAjaxResult;
import com.wwfinance.common.utils.MD5;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Api(tags = "用户注册")
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RedisTemplate redisTemplate;

    /** 注册验证码 Redis key 前缀（与短信服务约定保持一致） */
    private static final String SMS_CODE_PREFIX = "xx:code:";

    /** 验证码有效期：5 分钟 */
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;

    @GetMapping("/hello")
    public String index(){
        return "hello";
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
       User  user =   userService.getById(id);
       UserDto userDto = new UserDto();
        userDto.setMobile(user.getMobile()) ;
        userDto.setUserType(user.getUserType()) ;
        return userDto;
    }

    /**
     * 调用 UserMapper.xml 中的 selectByCondition 按条件查询
     * 示例：GET /api/user/condition?name=张&mobile=13800000000
     */
//    @GetMapping("/condition")
//    public List<User> selectByCondition(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String mobile) {
//        return userService.selectByCondition(name, mobile);
//    }


    @ApiOperation("发送注册验证码")
    @PostMapping("/sendCode")
    public PccAjaxResult sendCode(
            @ApiParam(value = "手机号", required = true)
            @RequestParam String mobile){
        // 1. 手机号非空 + 11 位手机号格式校验
        if (mobile == null || !mobile.matches("^1\\d{10}$")) {
            return new PccAjaxResult(500, "手机号格式不正确");
        }
        // 2. 已注册的手机号不允许再发送注册验证码
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getMobile, mobile);
        if (userService.getOne(queryWrapper) != null) {
            return new PccAjaxResult(500, "手机号已被注册");
        }
        // 3. 生成 4 位随机验证码并存入 Redis，有效期 5 分钟
        String code = String.valueOf(new Random().nextInt(9000) + 1000);
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + mobile, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("注册验证码已生成，mobile={}, code={}", mobile, code);
        // demo 阶段：直接返回验证码方便前后端联调；生产环境应通过短信网关下发，禁止返回
        return new PccAjaxResult(200, "验证码发送成功", code);
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public PccAjaxResult register(@RequestBody UserDto userDTO){
        // 0. 校验验证码：注册前必须通过 /api/user/sendCode 获取验证码
        Object cacheCode = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + userDTO.getMobile());
        if (userDTO.getCode() == null || cacheCode == null || !userDTO.getCode().equals(cacheCode.toString())) {
            return new PccAjaxResult(500, "验证码错误或已过期，请重新获取");
        }
//判断手机是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getMobile, userDTO.getMobile());
        User user2 = userService.getOne(queryWrapper);
        if(user2 != null){
            return new PccAjaxResult(500, "手机号已被注册");
        }

        //判断两次密码是否一致
        if(!(userDTO.getPassword().equals(userDTO.getPasswordto()))) {
            return new PccAjaxResult(500, "两次输入的密码不正确");
        }
        User user = new User();
        user.setMobile(userDTO.getMobile());
        user.setUserType(userDTO.getUserType());
        user.setName(userDTO.getMobile());
        user.setNickName(userDTO.getMobile());
        user.setPassword(MD5.encrypt(userDTO.getPassword()));
        user.setStatus(1);
        userService.save(user);

        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(user.getId());
        userAccountService.save(userAccount);

        // 注册成功，删除已使用的验证码，防止被重复使用
        redisTemplate.delete(SMS_CODE_PREFIX + userDTO.getMobile());

        return new PccAjaxResult(200, "注册成功");
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public PccAjaxResult login(@RequestBody UserDto userDTO){
        // 1. 按手机号查用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getMobile, userDTO.getMobile());
        User user = userService.getOne(queryWrapper);
        if(user == null){
            return new PccAjaxResult(500, "手机号未注册");
        }
        // 2. 校验密码（数据库存的是 MD5 加密后的值）
        if(!MD5.encrypt(userDTO.getPassword()).equals(user.getPassword())){
            return new PccAjaxResult(500, "密码错误");
        }
        // 3. 校验账号状态（1 正常 / 0 禁用）
        if(user.getStatus() != null && user.getStatus() == 0){
            return new PccAjaxResult(500, "账号已被禁用，请联系管理员");
        }
        // 4. 生成 token（claims 中存 userId，避免手机号超出 int 范围）
        String token = TokenUtil.generateMerchantToken(String.valueOf(user.getId()));
        // 5. 组装返回：token + 用户信息（password 字段已被 @JsonIgnore，不会返回）
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return new PccAjaxResult(200, "登录成功", data);
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public PccAjaxResult logout(){
        // JWT 无状态：前端清除本地 token 即可；如需服务端强制失效，可引入 Redis 黑名单
        return new PccAjaxResult(200, "退出成功");
    }

    @ApiOperation("获取当前登录用户个人信息")
    @GetMapping("/info")
    public PccAjaxResult getInfo(){
        Integer userId = LoginUserContext.getUserid();
        if(userId == null){
            return new PccAjaxResult(401, "未登录");
        }
        User user = userService.getById(userId);
        if(user == null){
            return new PccAjaxResult(500, "用户不存在");
        }
        return new PccAjaxResult(200, "获取成功", user);
    }
}
