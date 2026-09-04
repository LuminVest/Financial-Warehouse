package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.entity.BorrowerAttach;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.dto.BorrowerDTO;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.api.mapper.BorrowerAttachMapper;
import com.wwfinance.api.mapper.BorrowerMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.BorrowerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BorrowerServiceImpl extends ServiceImpl<BorrowerMapper, Borrower> implements BorrowerService {

    @Autowired
    private BorrowerAttachMapper borrowerAttachMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 借款人认证提交：
     * 1. 添加 borrower（user_id 唯一索引：已存在则更新，否则新增）
     * 2. 添加 borrower_attach（附件）
     * 3. 更新 user 表（borrow_auth_status -> 认证中）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBorrowerVOByUserId(BorrowerDTO borrowerDTO, User user) {
        // 1. 添加 borrower：姓名/身份证/手机号取 user 表，其余认证信息来自 DTO
        Borrower borrower = new Borrower();
        BeanUtils.copyProperties(borrowerDTO, borrower);
        borrower.setUserId(user.getId());
        borrower.setName(user.getName());
        borrower.setIdCard(user.getIdCard());
        borrower.setMobile(user.getMobile());
        borrower.setStatus(BorrowerStatusEnum.AUTH_RUNNING.getStatus()); // 1 认证中
        // user_id 唯一：先查已存在记录
        Borrower exist = this.getOne(new LambdaQueryWrapper<Borrower>().eq(Borrower::getUserId, user.getId()));
        if (exist != null) {
            borrower.setId(exist.getId());
            this.updateById(borrower);
        } else {
            this.save(borrower);
        }

        // 2. 添加 borrower_attach（附件：身份证正反面、房产证、车等）
        List<BorrowerAttach> attachList = borrowerDTO.getBorrowerAttachList();
        if (attachList != null && !attachList.isEmpty()) {
            for (BorrowerAttach attach : attachList) {
                attach.setBorrowerId(borrower.getId());
                borrowerAttachMapper.insert(attach);
            }
        }

        // 3. 更新 user 表：借款人认证状态置为「认证中」
        user.setBorrowAuthStatus(BorrowerStatusEnum.AUTH_RUNNING.getStatus());
        userMapper.updateById(user);
    }
}
