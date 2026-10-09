package com.wwfinance.api.service;

import java.util.Map;

/**
 * 管理员认证服务（demo：账号密码配置在 application.yml，不建表）
 */
public interface AdminUserService {

    /**
     * 管理员登录
     *
     * @param username 登录账号
     * @param password 登录密码
     * @return token（含 5grcs 前缀，前端原样带回请求头 token）
     */
    String login(String username, String password);

    /**
     * 获取当前登录管理员信息
     *
     * @param adminId 管理员 id
     * @return {id, username, nickname, avatar}
     */
    Map<String, Object> getUserInfo(Long adminId);
}
