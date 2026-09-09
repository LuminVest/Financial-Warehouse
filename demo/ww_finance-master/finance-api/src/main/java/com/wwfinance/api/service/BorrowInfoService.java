package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.BorrowInfo;

import java.math.BigDecimal;
import java.util.Map;

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

    /**
     * 管理后台：借款记录分页列表（keyword 按借款人姓名模糊匹配）
     *
     * @return {list: [BorrowRecordAdminVO], total}
     */
    Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer status);

    /**
     * 管理后台：审核借款申请（status：1通过 4拒绝）
     */
    void auditByAdmin(Long id, Integer status, String rejectReason);
}
