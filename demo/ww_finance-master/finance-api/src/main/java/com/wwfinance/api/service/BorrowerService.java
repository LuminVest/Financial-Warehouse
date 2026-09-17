package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.dto.BorrowerDTO;

import java.util.Map;

/**
 * 借款人认证服务
 */
public interface BorrowerService extends IService<Borrower> {

    /**
     * 保存借款人认证信息（borrower + borrower_attach + 更新 user 表）
     *
     * @param borrowerDTO 借款人认证信息
     * @param userId      当前登录用户 id
     */
    void saveBorrowerVOByUserId(BorrowerDTO borrowerDTO, Long userId);

    /**
     * 根据用户 id 查询认证状态（未认证返回 null）
     */
    Integer getStatusByUserId(Long userId);

    /**
     * 管理后台：借款人分页列表（keyword 模糊匹配姓名/身份证/手机号）
     *
     * @return {list: [BorrowerAdminVO], total}
     */
    Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer auditStatus);

    /**
     * 管理后台：审核借款人（auditStatus：1通过 2拒绝）
     *
     * @return 审批通过时本次回写的积分（拒绝或无需加分返回 0）
     */
    int auditByAdmin(Long id, Integer auditStatus, String remark, Integer idCardOk, Integer carOk, Integer houseOk);
}
