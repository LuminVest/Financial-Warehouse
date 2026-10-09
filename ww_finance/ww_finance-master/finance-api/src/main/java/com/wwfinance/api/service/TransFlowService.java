package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.TransFlow;

import java.math.BigDecimal;

/**
 * 交易流水 Service
 */
public interface TransFlowService extends IService<TransFlow> {

    /**
     * 记录一笔交易流水（trans_no 唯一，重复写入自动幂等跳过）
     *
     * @param transType 1充值 2提现 3投标 4投资回款 5放款 6还款
     */
    void addFlow(Long userId, Integer transType, String transNo, BigDecimal amount, String memo);
}
