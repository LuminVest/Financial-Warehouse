package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.UserAccount;

import java.math.BigDecimal;
import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {

    UserAccount getUserById(Long id);

    /**
     * 充值（生成支付表单/订单号）
     */
    String commitCharge(BigDecimal chargeAmt, Long userId);

    /**
     * 查询账户余额
     */
    BigDecimal getAccount(Long userId);

    /**
     * 充值异步回调：同步账户数据
     */
    String notify(Map<String, Object> paramMap);

}
