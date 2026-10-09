package com.wwfinance.api.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 还款入参 DTO：借款人选择某标的的某一期发起还款
 */
@Getter
@Setter
public class RepaymentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标的id
     */
    private Long lendId;

    /**
     * 还款期数（第几期）
     */
    private Integer currentPeriod;

}
