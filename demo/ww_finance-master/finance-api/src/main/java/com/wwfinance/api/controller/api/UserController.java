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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户注册")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserAccountService userAccountService;

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



    @ApiOperation("用户注册")
    @PostMapping("/register")
    public PccAjaxResult register(@RequestBody UserDto userDTO){
//判断手机是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getMobile, userDTO.getMobile());
        User user2 = userService.getOne(queryWrapper);
        if(user2 != null){
            return new PccAjaxResult(500, "手机号已经存在11");
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
