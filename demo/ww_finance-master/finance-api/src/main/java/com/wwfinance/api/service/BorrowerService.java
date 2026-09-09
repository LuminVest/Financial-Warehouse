package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.dto.BorrowerDTO;

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
}
