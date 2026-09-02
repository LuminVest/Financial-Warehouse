package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.mapper.UserAccountMapper;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.utils.LendNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.time.LocalDateTime;


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

//    /**
//     * 充值异步回调：同步账户数据（demo 简化版）
//     */
//    @Override
//    public String notify(Map<String, Object> paramMap) {
//        log.info("充值回调同步账户, paramMap={}", paramMap);
//        return "success";
//    }
    /**
     * 充值异步回调：同步账户数据（真正更新余额）
     */
    @Override
    public String notify(Map<String, Object> paramMap) {
        log.info("充值回调同步账户, paramMap={}", paramMap);
        // 解析参数：userId=充值用户ID，amount=充值金额
        Long userId = Long.valueOf(String.valueOf(paramMap.get("userId")));
        BigDecimal amount = new BigDecimal(String.valueOf(paramMap.get("amount")));

        // 按 userId 查账户
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);

        if (account == null) {
            // 首次充值：新建账户记录
            account = new UserAccount()
                    .setUserId(userId)
                    .setAmount(amount)
                    .setFreezeAmount(BigDecimal.ZERO)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now())
                    .setDeleted(false)
                    .setVersion(1);
        } else {
            // 已有账户：累加余额
            BigDecimal old = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
            account.setAmount(old.add(amount));
            account.setUpdateTime(LocalDateTime.now());
        }
        this.saveOrUpdate(account);
        log.info("充值到账成功, userId={}, 到账={}, 当前余额={}", userId, amount, account.getAmount());
        return "success";
    }

}
