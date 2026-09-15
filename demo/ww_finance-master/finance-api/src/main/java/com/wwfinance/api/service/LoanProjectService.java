package com.wwfinance.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 管理后台-标的管理服务
 */
public interface LoanProjectService {

    /**
     * 管理后台：标的列表（关键词+状态筛选）
     *
     * @return {list: [LoanProjectAdminVO], total}
     */
    Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer status);

    /**
     * 管理后台：发布标的（测试用）
     */
    void publishForAdmin(String title, Long borrowerId, BigDecimal amount,
                         BigDecimal rate, Integer term, String purpose, Integer riskLevel);

    /**
     * 管理后台：下架标的
     */
    void offlineByAdmin(Long id);

    /**
     * 管理后台：某标的的投资列表
     */
    List<Object> listInvestments(Long projectId);

    /**
     * 管理后台：放款（满标后手动触发放款，幂等）
     */
    void loanByAdmin(Long id);
}
