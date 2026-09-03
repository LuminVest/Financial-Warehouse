package com.wwfinance.api.enums;

import lombok.Getter;

/**
 * 用户绑定状态枚举（对齐老师 UserBindEnum）
 */
@Getter
public enum UserBindEnum {

    NO_BIND(0, "未绑定"),
    BIND_OK(1, "绑定成功"),
    BIND_FAIL(-1, "绑定失败");

    private final Integer status;
    private final String msg;

    UserBindEnum(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }
}
