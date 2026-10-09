package com.wwfinance.api.controller.admin;

import com.wwfinance.api.entity.dto.AdminLoginDTO;
import com.wwfinance.api.service.AdminUserService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台-登录
 * 与前端 ww_finance_admin 对齐：
 *  - POST /admin/core/login    管理员登录（响应 data = token 字符串）
 *  - GET  /admin/core/logout   退出登录
 *  - GET  /admin/core/userInfo 获取当前管理员信息
 */
@Api(tags = "管理后台-登录")
@RestController
@RequestMapping("/admin/core")
@Slf4j
public class AdminLoginController {

    @Autowired
    private AdminUserService adminUserService;

    @ApiOperation("管理员登录")
    @PostMapping("/login")
    public PccAjaxResult login(@RequestBody AdminLoginDTO loginDTO) {
        String token = adminUserService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return new PccAjaxResult(200, "登录成功", token);
    }

    @ApiOperation("退出登录")
    @GetMapping("/logout")
    public PccAjaxResult logout() {
        // JWT 无状态：前端清除本地 cookie 即可
        return new PccAjaxResult(200, "退出成功");
    }

    @ApiOperation("获取当前管理员信息")
    @GetMapping("/userInfo")
    public PccAjaxResult getUserInfo() {
        Integer adminId = LoginUserContext.getUserid();
        if (adminId == null) {
            return new PccAjaxResult(401, "未登录");
        }
        Map<String, Object> data = adminUserService.getUserInfo(adminId.longValue());
        return new PccAjaxResult(200, "获取成功", data);
    }
}
