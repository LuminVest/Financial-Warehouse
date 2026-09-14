package com.wwfinance.api.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendItem;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.entity.dto.InvestDTO;
import com.wwfinance.api.mapper.LendItemMapper;
import com.wwfinance.api.service.LendItemReturnService;
import com.wwfinance.api.service.LendItemService;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.service.LendService;
import com.wwfinance.api.service.UserBindService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.Amount1Helper;
import com.wwfinance.api.utils.Amount2Helper;
import com.wwfinance.api.utils.Amount3Helper;
import com.wwfinance.api.utils.Amount4Helper;
import com.wwfinance.api.utils.FormHelper;
import com.wwfinance.api.utils.HfbConst;
import com.wwfinance.api.utils.LendNoUtils;
import com.wwfinance.api.utils.RequestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LendItemServiceImpl extends ServiceImpl<LendItemMapper, LendItem> implements LendItemService {

    /** 银行回调成功编码（与充值/绑定回调约定一致） */
    private static final String RESULT_OK = "0001";

    /** 标的可投状态：募集中 */
    private static final int LEND_STATUS_INVESTING = 1;

    /** 标的满标状态 */
    private static final int LEND_STATUS_FULL = 2;

    /** 投资记录状态：已支付 */
    private static final int ITEM_STATUS_PAID = 1;

    @Autowired
    private LendService lendService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserBindService userBindService;

    @Autowired
    private LendReturnService lendReturnService;

    @Autowired
    private LendItemReturnService lendItemReturnService;

    @Override
    public List<LendItem> getListByLendId(Long lendId) {
        return this.list(new LambdaQueryWrapper<LendItem>()
                .eq(LendItem::getLendId, lendId)
                .eq(LendItem::getDeleted, false)
                .orderByDesc(LendItem::getInvestTime));
    }

    /**
     * 提交投资：
     * 1. 校验：已绑定托管账户、标的存在且募集中、金额在最低投额与剩余额度之间
     * 2. 组装旺旺银行投标表单（voteBindCode / agentBillNo / agentProjectCode / voteAmt ...）
     * 3. 返回自动提交表单（前端跳转托管平台完成投标）
     */
    @Override
    public String commitInvest(InvestDTO investDTO, Long userId) {
        // 0. 覆盖投资人信息（以当前登录用户为准，防止伪造）
        User user = userService.getById(userId);
        if (user != null) {
            investDTO.setInvestUserId(user.getId());
            investDTO.setInvestName(user.getName());
        }
        // 1. 校验绑定：必须有托管协议号
        UserBind userBind = userBindService.getBindByUserId(userId);
        if (userBind == null || userBind.getBindCode() == null || userBind.getBindCode().isEmpty()) {
            throw new RuntimeException("用户未绑定托管账户，请先完成实名绑定");
        }

        // 2. 校验标的
        Lend lend = lendService.getById(investDTO.getLendId());
        if (lend == null) {
            throw new RuntimeException("标的不存在");
        }
        if (lend.getStatus() == null || lend.getStatus() != LEND_STATUS_INVESTING) {
            throw new RuntimeException("标的当前不可投资（非募集中状态）");
        }
        BigDecimal amount = lend.getAmount() == null ? BigDecimal.ZERO : lend.getAmount();
        BigDecimal invested = lend.getInvestAmount() == null ? BigDecimal.ZERO : lend.getInvestAmount();
        BigDecimal remain = amount.subtract(invested);

        // 3. 校验金额：不低于最低投额、不超过剩余可投金额
        BigDecimal investAmount = investDTO.getInvestAmount();
        if (investAmount == null || investAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("投资金额必须大于 0");
        }
        BigDecimal lowest = lend.getLowestAmount() == null ? BigDecimal.ZERO : lend.getLowestAmount();
        if (investAmount.compareTo(lowest) < 0) {
            throw new RuntimeException("投资金额低于最低投资金额：" + lowest);
        }
        if (investAmount.compareTo(remain) > 0) {
            throw new RuntimeException("投资金额超出剩余可投金额：" + remain);
        }

        // 4. 组装旺旺银行投标参数（参数 key 对齐银行 ww_bank UserInvestController.invest）
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentUserId", userId);
        paramMap.put("voteBindCode", userBind.getBindCode());
        paramMap.put("agentBillNo", LendNoUtils.getLendItemNo());
        paramMap.put("agentProjectCode", lend.getLendNo());
        paramMap.put("agentProjectName", lend.getTitle());   // 银行确认页模板必填（EL1008E）
        paramMap.put("projectAmt", lend.getAmount());        // 银行 invest 超投校验必填（UserInvest.projectAmt）
        paramMap.put("projectType", "1");
        paramMap.put("voteAmt", investAmount);
        paramMap.put("votePrizeAmt", "0");
        paramMap.put("voteFeeAmt", "0");
        paramMap.put("returnUrl", HfbConst.INVEST_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.INVEST_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        String formStr = FormHelper.buildForm(HfbConst.INVEST_URL, paramMap);
        log.info("构建投资托管表单, userId={}, lendId={}, voteAmt={}", userId, investDTO.getLendId(), investAmount);
        return formStr;
    }

    /**
     * 投资异步回调：
     * 1. 验签
     * 2. 按 agentBillNo（=投资编号）幂等：已处理直接 success
     * 3. 写 lend_item 投资记录
     * 4. 更新 lend：累加已投金额/人数，满标置 status=2
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String notify(Map<String, Object> paramMap) {
        log.info("用户投资异步回调, paramMap={}", JSON.toJSONString(paramMap));
        if (!RequestHelper.isSignEquals(paramMap)) {
            log.error("用户投资异步回调签名错误：" + JSON.toJSONString(paramMap));
            return "fail";
        }
        if (!RESULT_OK.equals(String.valueOf(paramMap.get("resultCode")))) {
            // 业务失败（如余额不足），不写投资记录
            return "success";
        }

        String lendItemNo = String.valueOf(paramMap.get("agentBillNo"));
        BigDecimal voteAmt = new BigDecimal(String.valueOf(paramMap.get("voteAmt")));

        // 幂等：该投资编号已处理过，直接返回 success（避免银行重试重复入账）
        LendItem exist = this.getOne(new LambdaQueryWrapper<LendItem>()
                .eq(LendItem::getLendItemNo, lendItemNo));
        if (exist != null) {
            log.warn("投资回调重复，已忽略, lendItemNo={}", lendItemNo);
            return "success";
        }

        // 反查标的（agentProjectCode = 标的编号 lend_no）
        String lendNo = String.valueOf(paramMap.get("agentProjectCode"));
        Lend lend = lendService.getOne(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getLendNo, lendNo));
        if (lend == null) {
            log.error("投资回调找不到标的, lendNo={}", lendNo);
            return "fail";
        }

        // 投资用户：回调无用户ID，通过 voteBindCode（托管协议号）反查绑定记录
        String bindCode = String.valueOf(paramMap.get("voteBindCode"));
        UserBind userBind = userBindService.getOne(new LambdaQueryWrapper<UserBind>()
                .eq(UserBind::getBindCode, bindCode));
        Long investUserId = userBind == null ? null : userBind.getUserId();
        User user = investUserId == null ? null : userService.getById(investUserId);

        // 写投资记录
        LendItem lendItem = new LendItem();
        lendItem.setLendItemNo(lendItemNo);
        lendItem.setLendId(lend.getId());
        lendItem.setInvestUserId(investUserId);
        lendItem.setInvestName(user == null ? null : user.getName());
        lendItem.setInvestAmount(voteAmt);
        lendItem.setLendYearRate(lend.getLendYearRate());
        lendItem.setInvestTime(LocalDateTime.now());
        lendItem.setLendStartDate(lend.getLendStartDate());
        lendItem.setLendEndDate(lend.getLendEndDate());
        // 预期收益：按标的还款方式计算
        BigDecimal expectAmount = calcInterest(voteAmt, lend.getLendYearRate(),
                lend.getPeriod() == null ? 0 : lend.getPeriod(),
                lend.getReturnMethod() == null ? 1 : lend.getReturnMethod());
        lendItem.setExpectAmount(expectAmount);
        lendItem.setStatus(ITEM_STATUS_PAID);
        lendItem.setDeleted(false);
        this.save(lendItem);

        // 更新标的：累加已投金额/人数，满标置状态
        BigDecimal invested = lend.getInvestAmount() == null ? BigDecimal.ZERO : lend.getInvestAmount();
        BigDecimal newInvested = invested.add(voteAmt);
        Integer num = lend.getInvestNum() == null ? 0 : lend.getInvestNum();
        lend.setInvestAmount(newInvested);
        lend.setInvestNum(num + 1);
        if (newInvested.compareTo(lend.getAmount()) >= 0) {
            lend.setStatus(LEND_STATUS_FULL);
            log.info("标的满标, lendId={}, lendNo={}", lend.getId(), lendNo);
            // 满标 → 自动生成还款计划（按标的还款方式拆期；幂等，重复回调不会重复生成）
            lendReturnService.generateReturnPlan(lend);
            // 满标 → 自动生成回款明细（还款计划按投资人份额拆分；幂等）
            lendItemReturnService.generateReturnDetail(lend);
        }
        lendService.updateById(lend);
        return "success";
    }

    private BigDecimal calcInterest(BigDecimal invest, BigDecimal yearRate, int totalmonth, int returnMethod) {
        if (invest == null || yearRate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal interest;
        switch (returnMethod) {
            case 1: interest = Amount1Helper.getInterestCount(invest, yearRate, totalmonth); break;
            case 2: interest = Amount2Helper.getInterestCount(invest, yearRate, totalmonth); break;
            case 3: interest = Amount3Helper.getInterestCount(invest, yearRate, totalmonth); break;
            case 4: interest = Amount4Helper.getInterestCount(invest, yearRate, totalmonth); break;
            default: interest = BigDecimal.ZERO;
        }
        return interest == null ? BigDecimal.ZERO : interest;
    }
}
