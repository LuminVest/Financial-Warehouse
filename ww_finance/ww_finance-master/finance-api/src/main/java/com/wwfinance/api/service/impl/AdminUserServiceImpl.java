package com.wwfinance.api.service.impl;

import com.wwfinance.api.service.AdminUserService;
import com.wwfinance.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证服务
 * demo 简化：管理员账号密码配置在 application.yml，不建表
 */
@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public String login(String username, String password) {
        // 1. 校验账号密码（与配置比对）
        if (!adminUsername.equals(username)) {
            throw new BusinessException("账号不存在");
        }
        if (!adminPassword.equals(password)) {
            throw new BusinessException("密码错误");
        }
        // 2. 生成 token（claims 存管理员标识 1，与前台用户 token 同构）
        return com.wwfinance.api.utils.TokenUtil.generateMerchantToken("1");
    }

    @Override
    public Map<String, Object> getUserInfo(Long adminId) {
        // demo：管理员信息固定返回（id 统一为 1）
        Map<String, Object> data = new HashMap<>();
        data.put("id", adminId == null ? 1 : adminId);
        data.put("username", adminUsername);
        data.put("nickname", "系统管理员");
        data.put("avatar", "");
        return data;
    }
}
