package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.mapper.UserAccountMapper;
import com.wwfinance.api.mapper.UserBindMapper;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.service.TransFlowService;
import com.wwfinance.api.service.UserIntegralService;
import com.wwfinance.api.utils.FormHelper;
import com.wwfinance.api.utils.HfbConst;
import com.wwfinance.api.utils.LendNoUtils;
import com.wwfinance.api.utils.RequestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class UserAccountServiceimpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Resource
    private UserBindMapper userBindMapper;

    @Resource
    private TransFlowService transFlowService;

    @Resource
    private UserIntegralService userIntegralService;

    @Override
    public UserAccount getUserById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 充值（对接旺旺银行托管平台）：
     * 1. 查当前用户托管账户号 bindCode（需先完成实名绑定）
     * 2. 生成商户订单号 agentBillNo（幂等键）
     * 3. 组装签名参数，构建自动提交表单 POST 到银行端 AgreeBankCharge（渲染充值确认页）
     */
    @Override
    public String commitCharge(String chargeAmt, Long userId) {
        // 查绑定信息（托管账户号）
        UserBind userBind = userBindMapper.getBindInfoByUserId(userId);
        if (userBind == null || StringUtils.isEmpty(userBind.getBindCode())) {
            throw new RuntimeException("用户未绑定托管账户，请先完成实名绑定");
        }
        // 生成商户订单号
        String agentBillNo = LendNoUtils.getChargeNo();
        // 组装参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("bindCode", userBind.getBindCode());
        paramMap.put("chargeAmt", chargeAmt);
        paramMap.put("agentBillNo", agentBillNo);
        paramMap.put("returnUrl", HfbConst.RECHARGE_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.RECHARGE_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));
        log.info("构建充值托管表单, userId={}, agentBillNo={}, chargeAmt={}", userId, agentBillNo, chargeAmt);
        return FormHelper.buildForm(HfbConst.RECHARGE_URL, paramMap);
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
     * 充值异步回调：同步账户数据
     * 模拟银行通知字段：bindCode(托管账户号) / chargeAmt(充值金额) / agentBillNo(商户订单号)
     * 按 bindCode 反查用户，累加到 user_account.amount
     */
    @Override
    public String notify(Map<String, Object> paramMap) {
        log.info("充值回调同步账户, paramMap={}", paramMap);
        String bindCode = String.valueOf(paramMap.get("bindCode"));
        BigDecimal amount = new BigDecimal(String.valueOf(paramMap.get("chargeAmt")));

        // 按 bindCode 反查绑定记录，定位用户
        UserBind userBind = userBindMapper.getByBindCode(bindCode);
        if (userBind == null) {
            log.error("充值回调失败：bindCode 未找到绑定记录, bindCode={}", bindCode);
            return "fail";
        }
        Long userId = userBind.getUserId();

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

        // 埋点：充值流水 + 积分（1元=1分，幂等键=商户订单号）
        String billNo = String.valueOf(paramMap.get("agentBillNo"));
        transFlowService.addFlow(userId, 1, billNo, amount, "充值到账");
        userIntegralService.addIntegral(userId, amount.intValue(), "充值" + billNo);
        return "success";
    }

    /**
     * 提现（对接旺旺银行托管平台）：
     * 1. 查当前用户托管账户号 bindCode（需先完成实名绑定）
     * 2. 校验本地余额充足
     * 3. 生成商户订单号 agentBillNo（幂等键）
     * 4. 组装签名参数，构建自动提交表单 POST 到银行端 CashBankManager（渲染提现确认页）
     */
    @Override
    public String commitWithdraw(String withdrawAmt, Long userId) {
        // 查绑定信息（托管账户号）
        UserBind userBind = userBindMapper.getBindInfoByUserId(userId);
        if (userBind == null || StringUtils.isEmpty(userBind.getBindCode())) {
            throw new RuntimeException("用户未绑定托管账户，请先完成实名绑定");
        }
        // 本地余额校验（先校验再走银行）
        BigDecimal fetchAmt = new BigDecimal(withdrawAmt);
        BigDecimal balance = getAccount(userId);
        if (balance.compareTo(fetchAmt) < 0) {
            throw new RuntimeException("账户余额不足，当前余额=" + balance + "，提现金额=" + fetchAmt);
        }
        // 生成商户订单号
        String agentBillNo = String.valueOf(LendNoUtils.getWithdrawNo());
        // 组装参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("bindCode", userBind.getBindCode());
        paramMap.put("fetchAmt", withdrawAmt);
        paramMap.put("agentBillNo", agentBillNo);
        paramMap.put("returnUrl", HfbConst.WITHDRAW_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.WITHDRAW_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));
        log.info("构建提现托管表单, userId={}, agentBillNo={}, withdrawAmt={}", userId, agentBillNo, withdrawAmt);
        return FormHelper.buildForm(HfbConst.WITHDRAW_URL, paramMap);
    }

    /**
     * 提现异步回调：银行扣款成功后同步本地账户（余额扣减 + 提现流水）
     */
    @Override
    public String notifyWithdraw(Map<String, Object> paramMap) {
        log.info("提现回调同步账户, paramMap={}", paramMap);
        String bindCode = String.valueOf(paramMap.get("bindCode"));
        BigDecimal amount = new BigDecimal(String.valueOf(paramMap.get("fetchAmt")));

        // 按 bindCode 反查绑定记录，定位用户
        UserBind userBind = userBindMapper.getByBindCode(bindCode);
        if (userBind == null) {
            log.error("提现回调失败：bindCode 未找到绑定记录, bindCode={}", bindCode);
            return "fail";
        }
        Long userId = userBind.getUserId();

        // 按 userId 查账户
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);
        if (account == null) {
            log.error("提现回调失败：账户不存在, userId={}", userId);
            return "fail";
        }
        // 余额扣减（银行侧已扣，本地同步；防重复扣减由 addFlow 幂等键保证）
        BigDecimal old = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
        account.setAmount(old.subtract(amount));
        account.setUpdateTime(LocalDateTime.now());
        this.updateById(account);
        log.info("提现到账成功, userId={}, 提现={}, 当前余额={}", userId, amount, account.getAmount());

        // 埋点：提现流水（transType=2 提现，幂等键=商户订单号）
        String billNo = String.valueOf(paramMap.get("agentBillNo"));
        transFlowService.addFlow(userId, 2, billNo, amount, "提现成功");
        return "success";
    }

}
