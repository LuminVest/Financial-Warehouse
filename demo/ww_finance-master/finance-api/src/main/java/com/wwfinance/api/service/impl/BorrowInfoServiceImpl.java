package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.BorrowInfo;
import com.wwfinance.api.entity.Borrower;
import com.wwfinance.api.enums.BorrowInfoStatusEnum;
import com.wwfinance.api.enums.BorrowerStatusEnum;
import com.wwfinance.api.mapper.BorrowInfoMapper;
import com.wwfinance.api.mapper.BorrowerMapper;
import com.wwfinance.api.service.BorrowInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class BorrowInfoServiceImpl extends ServiceImpl<BorrowInfoMapper, BorrowInfo> implements BorrowInfoService {

    @Autowired
    private BorrowerMapper borrowerMapper;

    /**
     * 获取借款申请审批状态：查该用户最近一条借款信息的 status
     */
    @Override
    public Integer getStatusByUserId(Long userId) {
        BorrowInfo borrowInfo = this.getOne(new LambdaQueryWrapper<BorrowInfo>()
                .eq(BorrowInfo::getUserId, userId)
                .orderByDesc(BorrowInfo::getId)
                .last("limit 1"));
        if (borrowInfo == null) {
            return BorrowInfoStatusEnum.NO_AUTH.getStatus(); // 0 未提交
        }
        return borrowInfo.getStatus();
    }

    /**
     * 获取可借额度：
     * 借款人认证通过后，根据月收入档位（income）映射可借额度。
     * 说明：额度档位为演示口径（1→5千，2→1万，3→3万，4→5万），可按老师要求调整。
     */
    @Override
    public BigDecimal getBorrowAmount(Long userId) {
        Borrower borrower = borrowerMapper.selectOne(new LambdaQueryWrapper<Borrower>()
                .eq(Borrower::getUserId, userId));
        if (borrower == null || borrower.getStatus() == null
                || borrower.getStatus() != BorrowerStatusEnum.AUTH_OK.getStatus()) {
            // 未认证或未认证通过：额度为 0
            return new BigDecimal(0);
        }
        BigDecimal amount = new BigDecimal(0);
        Integer income = borrower.getIncome() == null ? 0 : borrower.getIncome();
        switch (income) {
            case 1: amount = new BigDecimal(5000); break;
            case 2: amount = new BigDecimal(10000); break;
            case 3: amount = new BigDecimal(30000); break;
            case 4: amount = new BigDecimal(50000); break;
            default: amount = new BigDecimal(0);
        }
        return amount;
    }

    /**
     * 提交借款申请：补充 userId，状态置为「审核中」，保存
     */
    @Override
    public void saveBorrowInfo(BorrowInfo borrowInfo, Long userId) {
        borrowInfo.setUserId(userId);
        borrowInfo.setStatus(BorrowInfoStatusEnum.CHECK_RUN.getStatus()); // 1 审核中
        this.save(borrowInfo);
    }
}
