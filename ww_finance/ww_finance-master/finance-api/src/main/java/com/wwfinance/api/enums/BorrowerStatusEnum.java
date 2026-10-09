package com.wwfinance.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 借款人认证状态枚举（对应 borrower.status）
 * 0：未认证，1：认证中，2：认证通过，-1：认证失败
 */
@Getter
@AllArgsConstructor
public enum BorrowerStatusEnum {

    NO_AUTH(0, "未认证"),
    AUTH_RUNNING(1, "认证中"),
    AUTH_OK(2, "认证通过"),
    AUTH_FAIL(-1, "认证失败");

    private Integer status;
    private String msg;
}
