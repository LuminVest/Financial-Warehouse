package com.wwfinance.api.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 投资入参 DTO（对齐接口文档 7.1）
 */
@Getter
@Setter
public class InvestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标的id
     */
    private Long lendId;

    /**
     * 投资金额
     */
    private BigDecimal investAmount;

    /**
     * 投资用户id（服务端会以登录用户覆盖，前端可不传）
     */
    private Long investUserId;

    /**
     * 投资人名称（服务端会以登录用户覆盖，前端可不传）
     */
    private String investName;

}
