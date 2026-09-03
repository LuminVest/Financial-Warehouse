package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.mapper.UserAccountMapper;
import com.wwfinance.api.mapper.UserBindMapper;
import com.wwfinance.api.service.UserAccountService;
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
    public String commitCharge(BigDecimal chargeAmt, Long userId) {
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
        paramMap.put("chargeAmt", chargeAmt.toString());
        paramMap.put("agentBillNo", agentBillNo);
        paramMap.put("returnUrl", HfbConst.USERCHARGE_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.USERCHARGE_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));
        log.info("构建充值托管表单, userId={}, agentBillNo={}, chargeAmt={}", userId, agentBillNo, chargeAmt);
        return FormHelper.buildForm(HfbConst.USERCHARGE_URL, paramMap);
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
        return "success";
    }

}
