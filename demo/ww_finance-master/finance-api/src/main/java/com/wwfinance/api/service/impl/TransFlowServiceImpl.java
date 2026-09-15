package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.TransFlow;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.mapper.TransFlowMapper;
import com.wwfinance.api.service.TransFlowService;
import com.wwfinance.api.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水实现：统一在各业务回调（充值/投标/放款/还款/回款）处调用，
 * 依托 trans_no 唯一索引保证幂等，重复回调不会重复记流水。
 */
@Slf4j
@Service
public class TransFlowServiceImpl extends ServiceImpl<TransFlowMapper, TransFlow> implements TransFlowService {

    @Autowired
    private UserService userService;

    @Override
    public void addFlow(Long userId, Integer transType, String transNo, BigDecimal amount, String memo) {
        try {
            User user = userId == null ? null : userService.getById(userId);
            TransFlow flow = new TransFlow()
                    .setUserId(userId)
                    .setUserName(user == null ? null : user.getName())
                    .setTransNo(transNo)
                    .setTransType(transType)
                    .setTransTypeName(typeName(transType))
                    .setTransAmount(amount)
                    .setMemo(memo)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now())
                    .setDeleted(false);
            this.save(flow);
            log.info("记录交易流水, userId={}, type={}, transNo={}, amount={}", userId, transType, transNo, amount);
        } catch (DuplicateKeyException e) {
            // trans_no 唯一：重复回调（重试/幂等）直接跳过
            log.info("交易流水已存在, 跳过: transNo={}", transNo);
        }
    }

    private String typeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case 1: return "充值";
            case 2: return "提现";
            case 3: return "投标";
            case 4: return "投资回款";
            case 5: return "放款";
            case 6: return "还款";
            default: return "其他";
        }
    }
}
