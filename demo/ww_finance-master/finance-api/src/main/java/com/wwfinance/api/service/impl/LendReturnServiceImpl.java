package com.wwfinance.api.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendItem;
import com.wwfinance.api.entity.LendItemReturn;
import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.mapper.LendReturnMapper;
import com.wwfinance.api.service.LendItemReturnService;
import com.wwfinance.api.service.LendItemService;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.service.LendService;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.service.UserBindService;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LendReturnServiceImpl extends ServiceImpl<LendReturnMapper, LendReturn> implements LendReturnService {

    /** 等额本息 */
    private static final int METHOD_EQUAL_INSTALLMENT = 1;
    /** 等额本金 */
    private static final int METHOD_EQUAL_PRINCIPAL = 2;
    /** 每月还息一次还本 */
    private static final int METHOD_MONTH_INTEREST = 3;
    /** 一次还本 */
    private static final int METHOD_ONE_TIME = 4;

    /** 未归还 */
    private static final int STATUS_UNPAID = 0;

    /** 已归还 */
    private static final int STATUS_PAID = 1;

    /** 银行回调成功编码（与充值/绑定/投资回调约定一致） */
    private static final String RESULT_OK = "0001";

    @Autowired
    private LendService lendService;

    @Autowired
    private UserBindService userBindService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private LendItemReturnService lendItemReturnService;

    @Autowired
    private LendItemService lendItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateReturnPlan(Lend lend) {
        if (lend == null || lend.getId() == null) {
            log.warn("生成还款计划失败: lend 为空");
            return;
        }
        Integer period = lend.getPeriod();
        if (period == null || period <= 0) {
            log.warn("生成还款计划失败: 标的期数非法 lendId={}, period={}", lend.getId(), period);
            return;
        }
        // 幂等：同一标的已生成过还款计划则跳过（避免满标回调重试重复生成）
        long exist = this.count(new LambdaQueryWrapper<LendReturn>()
                .eq(LendReturn::getLendId, lend.getId())
                .apply("is_deleted = 0"));
        if (exist > 0) {
            log.info("还款计划已生成, 跳过: lendId={}", lend.getId());
            return;
        }

        BigDecimal amount = lend.getAmount() == null ? BigDecimal.ZERO : lend.getAmount();
        BigDecimal yearRate = lend.getLendYearRate() == null ? BigDecimal.ZERO : lend.getLendYearRate();
        Integer returnMethod = lend.getReturnMethod() == null ? METHOD_EQUAL_INSTALLMENT : lend.getReturnMethod();
        // 满标日为起息基准，第 i 期还款日 = 满标日 + i 个月
        LocalDate baseDate = LocalDate.now();

        List<LendReturn> planList = new ArrayList<>();
        BigDecimal remaining = amount; // 剩余本金（用于计息本金额与末期兜底）

        if (returnMethod == METHOD_ONE_TIME) {
            // 一次还本付息：只有 1 期（到期一次还清），期数记为最后一期
            LendReturn one = buildReturn(lend, 1, period, amount, yearRate, returnMethod, baseDate);
            BigDecimal interest = Amount4Helper.getInterestCount(amount, yearRate, period);
            one.setBaseAmount(amount);
            one.setPrincipal(amount);
            one.setInterest(interest == null ? BigDecimal.ZERO : interest.setScale(2, RoundingMode.HALF_UP));
            one.setTotal(one.getPrincipal().add(one.getInterest()));
            one.setIsLast(true);
            planList.add(one);
        } else {
            // 等额本息 / 等额本金 / 每月还息一次还本：按工具类逐期拆分
            Map<Integer, BigDecimal> principalMap = getPrincipalMap(amount, yearRate, period, returnMethod);
            Map<Integer, BigDecimal> interestMap = getInterestMap(amount, yearRate, period, returnMethod);

            BigDecimal accumulatedPrincipal = BigDecimal.ZERO; // 已累计本金（末期兜底）
            for (int i = 1; i <= period; i++) {
                BigDecimal principal = principalMap.get(i);
                BigDecimal interest = interestMap.get(i);
                if (principal == null) {
                    principal = BigDecimal.ZERO;
                }
                if (interest == null) {
                    interest = BigDecimal.ZERO;
                }
                principal = principal.setScale(2, RoundingMode.HALF_UP);
                interest = interest.setScale(2, RoundingMode.HALF_UP);

                // 最后一期兜底：本金取剩余本金，保证各期本金之和 = 借款金额（消除四舍五入误差）
                boolean isLast = (i == period);
                if (isLast) {
                    principal = remaining;
                }
                BigDecimal total = principal.add(interest);

                LendReturn one = buildReturn(lend, i, period, amount, yearRate, returnMethod, baseDate);
                one.setBaseAmount(remaining);
                one.setPrincipal(principal);
                one.setInterest(interest);
                one.setTotal(total);
                one.setIsLast(isLast);
                planList.add(one);

                accumulatedPrincipal = accumulatedPrincipal.add(principal);
                remaining = amount.subtract(accumulatedPrincipal);
                if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    remaining = BigDecimal.ZERO;
                }
            }
        }

        this.saveBatch(planList);
        log.info("生成还款计划完成: lendId={}, returnMethod={}, period={}, 共 {} 期",
                lend.getId(), returnMethod, period, planList.size());
    }

    /** 各还款方式的本金 Map（一次还本除外，单独处理） */
    private Map<Integer, BigDecimal> getPrincipalMap(BigDecimal amount, BigDecimal yearRate, int period, int method) {
        switch (method) {
            case METHOD_EQUAL_INSTALLMENT:
                return Amount1Helper.getPerMonthPrincipal(amount, yearRate, period);
            case METHOD_EQUAL_PRINCIPAL:
                return Amount2Helper.getPerMonthPrincipal(amount, yearRate, period);
            case METHOD_MONTH_INTEREST:
                return Amount3Helper.getPerMonthPrincipal(amount, yearRate, period);
            default:
                throw new IllegalArgumentException("不支持的还款方式：" + method);
        }
    }

    /** 各还款方式的利息 Map（一次还本除外，单独处理） */
    private Map<Integer, BigDecimal> getInterestMap(BigDecimal amount, BigDecimal yearRate, int period, int method) {
        switch (method) {
            case METHOD_EQUAL_INSTALLMENT:
                return Amount1Helper.getPerMonthInterest(amount, yearRate, period);
            case METHOD_EQUAL_PRINCIPAL:
                return Amount2Helper.getPerMonthInterest(amount, yearRate, period);
            case METHOD_MONTH_INTEREST:
                return Amount3Helper.getPerMonthInterest(amount, yearRate, period);
            default:
                throw new IllegalArgumentException("不支持的还款方式：" + method);
        }
    }

    /** 组装还款计划基础字段 */
    private LendReturn buildReturn(Lend lend, int currentPeriod, int totalPeriod,
                                   BigDecimal amount, BigDecimal yearRate, Integer returnMethod,
                                   LocalDate baseDate) {
        LendReturn ret = new LendReturn();
        ret.setLendId(lend.getId());
        ret.setBorrowInfoId(lend.getBorrowInfoId());
        ret.setReturnNo(LendNoUtils.getReturnNo());
        ret.setUserId(lend.getUserId());
        ret.setAmount(amount);
        ret.setCurrentPeriod(currentPeriod);
        ret.setLendYearRate(yearRate);
        ret.setReturnMethod(returnMethod);
        ret.setFee(BigDecimal.ZERO);
        ret.setReturnDate(baseDate.plusMonths(currentPeriod));
        ret.setStatus(STATUS_UNPAID);
        ret.setIsOverdue(false);
        ret.setOverdueTotal(BigDecimal.ZERO);
        ret.setIsLast(currentPeriod == totalPeriod);
        ret.setDeleted(false);
        return ret;
    }

    @Override
    public List<LendReturn> listByLendId(Long lendId) {
        return this.list(new LambdaQueryWrapper<LendReturn>()
                .eq(LendReturn::getLendId, lendId)
                .apply("is_deleted = 0")
                .orderByAsc(LendReturn::getCurrentPeriod));
    }

    /**
     * 借款人发起还款（生成银行还款确认表单）
     */
    @Override
    public String commitRepayment(Long lendId, Integer currentPeriod, Long userId) {
        // 1. 校验标的与借款人身份
        Lend lend = lendId == null ? null : lendService.getById(lendId);
        if (lend == null) {
            throw new RuntimeException("标的不存在");
        }
        if (lend.getUserId() == null || !lend.getUserId().equals(userId)) {
            throw new RuntimeException("只有借款人本人可以发起还款");
        }

        // 2. 校验绑定托管账户（还款从银行托管账户扣款）
        UserBind userBind = userBindService.getBindByUserId(userId);
        if (userBind == null || StringUtils.isEmpty(userBind.getBindCode())) {
            throw new RuntimeException("用户未绑定托管账户，请先完成实名绑定");
        }

        // 3. 定位该期还款计划（未归还）
        if (currentPeriod == null || currentPeriod <= 0) {
            throw new RuntimeException("还款期数非法");
        }
        LendReturn plan = this.getOne(new LambdaQueryWrapper<LendReturn>()
                .eq(LendReturn::getLendId, lendId)
                .eq(LendReturn::getCurrentPeriod, currentPeriod)
                .apply("is_deleted = 0"));
        if (plan == null) {
            throw new RuntimeException("该期还款计划不存在");
        }
        if (plan.getStatus() != null && plan.getStatus() == STATUS_PAID) {
            throw new RuntimeException("该期已归还，请勿重复还款");
        }

        // 4. 组装该期回款明细（银行 returnCommit 按 data 给每位投资人转账）
        //    银行校验：明细 transitAmt 合计 + voteFeeAmt == totalAmt
        List<LendItemReturn> details = lendItemReturnService.list(new LambdaQueryWrapper<LendItemReturn>()
                .eq(LendItemReturn::getLendId, lendId)
                .eq(LendItemReturn::getCurrentPeriod, currentPeriod)
                .apply("is_deleted = 0"));
        if (details.isEmpty()) {
            throw new RuntimeException("该期回款明细不存在，无法发起还款");
        }
        JSONArray dataArr = new JSONArray();
        for (LendItemReturn detail : details) {
            JSONObject obj = new JSONObject();
            // 投资流水号（银行回款明细唯一标识）
            LendItem item = detail.getLendItemId() == null ? null : lendItemService.getById(detail.getLendItemId());
            obj.put("voteBillNo", item == null ? null : item.getLendItemNo());
            // 投资人托管协议号（银行据此给投资人入账）
            UserBind investBind = detail.getInvestUserId() == null ? null
                    : userBindService.getBindByUserId(detail.getInvestUserId());
            obj.put("toBindCode", investBind == null ? null : investBind.getBindCode());
            obj.put("transitAmt", detail.getTotal());
            obj.put("baseAmt", detail.getPrincipal());
            obj.put("benifitAmt", detail.getInterest());
            dataArr.add(obj);
        }

        // 5. 组装旺旺银行还款表单（参数 key 对齐银行 ww_bank LendReturnController）
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentUserId", userId);
        paramMap.put("fromBindCode", userBind.getBindCode());   // 还款人托管协议号
        paramMap.put("agentBatchNo", plan.getReturnNo());       // 还款批次号（=该期还款编号，回调定位键）
        paramMap.put("agentGoodsName", lend.getTitle());        // 银行确认页模板必填
        paramMap.put("totalAmt", plan.getTotal());              // 本期应还本息
        paramMap.put("baseAmt", plan.getPrincipal());           // 本金
        paramMap.put("benifitAmt", plan.getInterest());         // 利息
        paramMap.put("transitAmt", BigDecimal.ZERO);
        paramMap.put("voteFeeAmt", "0");
        paramMap.put("data", dataArr.toJSONString());           // 该期回款明细（银行按明细分发给投资人）
        paramMap.put("returnUrl", HfbConst.BORROW_RETURN_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.BORROW_RETURN_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        String formStr = FormHelper.buildForm(HfbConst.BORROW_RETURN_URL, paramMap);
        log.info("构建还款托管表单, userId={}, lendId={}, currentPeriod={}, totalAmt={}",
                userId, lendId, currentPeriod, plan.getTotal());
        return formStr;
    }

    /**
     * 还款异步回调：更新还款计划 + 同步回款明细 + 投资人入账
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String notifyRepayment(Map<String, Object> paramMap) {
        log.info("还款异步回调, paramMap={}", JSON.toJSONString(paramMap));
        // 1. 验签
        if (!RequestHelper.isSignEquals(paramMap)) {
            log.error("还款异步回调签名错误：" + JSON.toJSONString(paramMap));
            return "fail";
        }
        // 2. 业务结果判断
        if (!RESULT_OK.equals(String.valueOf(paramMap.get("resultCode")))) {
            // 还款失败（如余额不足），不更新业务状态
            return "success";
        }

        // 3. 按批次号（=该期还款编号）定位还款计划
        String returnNo = String.valueOf(paramMap.get("agentBatchNo"));
        LendReturn plan = this.getOne(new LambdaQueryWrapper<LendReturn>()
                .eq(LendReturn::getReturnNo, returnNo)
                .apply("is_deleted = 0"));
        if (plan == null) {
            log.error("还款回调找不到还款计划, returnNo={}", returnNo);
            return "fail";
        }

        // 4. 幂等：已归还直接跳过（避免银行重试重复入账）
        if (plan.getStatus() != null && plan.getStatus() == STATUS_PAID) {
            log.warn("还款回调重复，已忽略, returnNo={}", returnNo);
            return "success";
        }

        // 5. 金额校验（防篡改）
        BigDecimal totalAmt = paramMap.get("totalAmt") == null
                ? null : new BigDecimal(String.valueOf(paramMap.get("totalAmt")));
        if (totalAmt == null || totalAmt.compareTo(plan.getTotal()) != 0) {
            log.error("还款金额与还款计划不符, returnNo={}, 回调金额={}, 计划金额={}",
                    returnNo, totalAmt, plan.getTotal());
            return "fail";
        }

        // 6. 逾期标记：实际还款日晚于计划还款日
        LocalDateTime now = LocalDateTime.now();
        boolean overdue = plan.getReturnDate() != null && plan.getReturnDate().isBefore(now.toLocalDate());

        // 7. 更新还款计划：已归还
        plan.setStatus(STATUS_PAID);
        plan.setRealReturnTime(now);
        if (overdue) {
            plan.setIsOverdue(true);
            plan.setOverdueTotal(plan.getTotal() == null ? BigDecimal.ZERO : plan.getTotal());
        }
        this.updateById(plan);
        log.info("还款到账, returnNo={}, lendId={}, currentPeriod={}, total={}, 逾期={}",
                returnNo, plan.getLendId(), plan.getCurrentPeriod(), plan.getTotal(), overdue);

        // 8. 借款人本地账户扣减（与银行托管账户扣款同步）
        debitBorrower(plan.getUserId(), plan.getTotal() == null ? BigDecimal.ZERO : plan.getTotal());

        // 9. 同步该期回款明细 + 投资人入账
        List<LendItemReturn> details = lendItemReturnService.list(new LambdaQueryWrapper<LendItemReturn>()
                .eq(LendItemReturn::getLendId, plan.getLendId())
                .eq(LendItemReturn::getCurrentPeriod, plan.getCurrentPeriod())
                .apply("is_deleted = 0"));
        for (LendItemReturn detail : details) {
            if (detail.getStatus() != null && detail.getStatus() == STATUS_PAID) {
                continue;
            }
            detail.setStatus(STATUS_PAID);
            detail.setRealReturnTime(now);
            if (overdue) {
                detail.setIsOverdue(true);
                detail.setOverdueTotal(detail.getTotal() == null ? BigDecimal.ZERO : detail.getTotal());
            }
            lendItemReturnService.updateById(detail);

            // 投资人回款入账：user_account.amount += 该期回款
            if (detail.getInvestUserId() != null) {
                creditInvestor(detail.getInvestUserId(), detail.getTotal() == null ? BigDecimal.ZERO : detail.getTotal());
            }
        }
        log.info("还款同步回款明细完成: returnNo={}, 投资人={}人", returnNo, details.size());
        return "success";
    }

    /** 投资人回款入账（无账户则新建） */
    private void creditInvestor(Long userId, BigDecimal amount) {
        UserAccount account = userAccountService.getOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId));
        if (account == null) {
            account = new UserAccount()
                    .setUserId(userId)
                    .setAmount(amount)
                    .setFreezeAmount(BigDecimal.ZERO)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now())
                    .setDeleted(false)
                    .setVersion(1);
        } else {
            BigDecimal old = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
            account.setAmount(old.add(amount));
            account.setUpdateTime(LocalDateTime.now());
        }
        userAccountService.saveOrUpdate(account);
        log.info("投资人回款到账, userId={}, 到账={}, 当前余额={}", userId, amount, account.getAmount());
    }

    /** 借款人还款扣减本地账户（银行托管账户已扣款，本地账本同步） */
    private void debitBorrower(Long userId, BigDecimal amount) {
        if (userId == null) {
            log.warn("借款人账户扣减跳过: userId 为空");
            return;
        }
        UserAccount account = userAccountService.getOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId));
        if (account == null) {
            log.warn("借款人账户扣减跳过: 无账户记录 userId={}", userId);
            return;
        }
        BigDecimal old = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
        if (old.compareTo(amount) < 0) {
            throw new RuntimeException("借款人本地账户余额不足，无法完成扣减");
        }
        account.setAmount(old.subtract(amount));
        account.setUpdateTime(LocalDateTime.now());
        userAccountService.saveOrUpdate(account);
        log.info("借款人还款扣款, userId={}, 扣减={}, 当前余额={}", userId, amount, account.getAmount());
    }
}
