package com.wwfinance.api.controller.api;

import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.dto.UserDto;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "用户注册")
@RestController
@RequestMapping("/api/core/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/hello")
    public String index(){
        return "hello";
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUserDto(id);
    }

    /**
     * 校验手机号是否可用（被 sms 服务通过 Feign 调用，注册前检查是否已注册）
     * 注意：公开接口，无需登录；返回 boolean 供 CoreUserInfoClient.checkMobile 反序列化
     */
    @ApiOperation("校验手机号是否可用")
    @GetMapping("/checkMobile/{mobile}")
    public boolean checkMobile(@ApiParam(value = "手机号", required = true) @PathVariable String mobile) {
        return userService.checkMobile(mobile);
    }

    @ApiOperation("发送注册验证码")
    @PostMapping("/sendCode")
    public PccAjaxResult sendCode(
            @ApiParam(value = "手机号", required = true)
            @RequestParam String mobile){
        return new PccAjaxResult(200, "验证码发送成功", userService.sendCode(mobile));
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public PccAjaxResult register(@RequestBody UserDto userDTO){
        userService.register(userDTO);
        return new PccAjaxResult(200, "注册成功");
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public PccAjaxResult login(@RequestBody UserDto userDTO){
        return new PccAjaxResult(200, "登录成功", userService.login(userDTO));
    }

    @ApiOperation("退出登录")
    @GetMapping("/logout")
    public PccAjaxResult logout(){
        // JWT 无状态：前端清除本地 token 即可；如需服务端强制失效，可引入 Redis 黑名单
        return new PccAjaxResult(200, "退出成功");
    }

    @ApiOperation("获取当前登录用户个人信息")
    @GetMapping("/userInfo")
    public PccAjaxResult getInfo(){
        Integer userId = LoginUserContext.getUserid();
        if (userId == null) {
            return new PccAjaxResult(401, "未登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return new PccAjaxResult(500, "用户不存在");
        }
        return new PccAjaxResult(200, "获取成功", user);
    }
}
