package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.mapper.LendReturnMapper;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.utils.Amount1Helper;
import com.wwfinance.api.utils.Amount2Helper;
import com.wwfinance.api.utils.Amount3Helper;
import com.wwfinance.api.utils.Amount4Helper;
import com.wwfinance.api.utils.LendNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
}
