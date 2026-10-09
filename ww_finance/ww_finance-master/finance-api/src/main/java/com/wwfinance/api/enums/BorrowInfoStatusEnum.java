package com.wwfinance.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 借款信息状态枚举（对应 borrow_info.status）
 * 0：未提交，1：审核中，2：审核通过，-1：审核不通过
 */
@Getter
@AllArgsConstructor
public enum BorrowInfoStatusEnum {

    NO_AUTH(0, "未提交"),
    CHECK_RUN(1, "审核中"),
    CHECK_OK(2, "审核通过"),
    CHECK_FAIL(-1, "审核不通过");

    private Integer status;
    private String msg;
}
