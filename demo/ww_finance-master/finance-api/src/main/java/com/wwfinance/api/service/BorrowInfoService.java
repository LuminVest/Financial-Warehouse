package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.BorrowInfo;

import java.math.BigDecimal;

/**
 * 借款信息服务
 */
public interface BorrowInfoService extends IService<BorrowInfo> {

    /**
     * 获取当前用户借款申请审批状态
     */
    Integer getStatusByUserId(Long userId);

    /**
     * 获取当前用户可借额度
     */
    BigDecimal getBorrowAmount(Long userId);

    /**
     * 提交借款申请
     */
    void saveBorrowInfo(BorrowInfo borrowInfo, Long userId);
}
