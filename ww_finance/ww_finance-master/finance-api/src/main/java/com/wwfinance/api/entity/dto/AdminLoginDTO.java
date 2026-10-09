package com.wwfinance.api.entity.dto;

import lombok.Data;

/**
 * 管理员登录参数
 */
@Data
public class AdminLoginDTO {

    /**
     * 登录账号
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;
}
