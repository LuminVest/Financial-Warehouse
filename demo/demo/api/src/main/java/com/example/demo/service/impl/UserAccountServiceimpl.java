package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.UserAccountMapper;
import com.example.demo.service.UserAccountService;
import com.example.demo.utils.LendNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class UserAccountServiceimpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Override
    public UserAccount getUserById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 充值（demo 简化版）：真实项目会调用第三方支付平台生成支付表单；
     * 这里先生成交易单号返回，便于把充值流程走通。
     */
    @Override
    public String commitCharge(BigDecimal chargeAmt, Long userId) {
        String orderNo = LendNoUtils.getTransNo();
        log.info("生成充值订单, userId={}, amount={}, orderNo={}", userId, chargeAmt, orderNo);
        return orderNo;
    }

    /**
     * 查询账户余额
     */
    @Override
    public BigDecimal getAccount(Long userId) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);
        return account == null ? BigDecimal.ZERO : account.getAmount();
    }

    /**
     * 充值异步回调：同步账户数据（demo 简化版）
     */
    @Override
    public String notify(Map<String, Object> paramMap) {
        log.info("充值回调同步账户, paramMap={}", paramMap);
        return "success";
    }
}
