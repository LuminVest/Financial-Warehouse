package com.wwfinance.api.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 还款方式金额计算工具类单元测试（等额本息/等额本金/按月付息到期还本/一次还本付息）
 *
 * 校验策略：用「财务性质断言」而非写死浮点精确值——
 * ① 各期本金之和 ≈ 借款金额（本金守恒，允许 1 分内精度差）
 * ② 利息逐期递减（本金偿还越多，剩余计息本金越少）
 * ③ 总利息 = 各期利息之和（口径一致，允许 1 分内精度差）
 */
class AmountHelperTest {

    /** 借款金额：12万 */
    private static final BigDecimal INVEST = new BigDecimal("120000");
    /** 年利率：8% */
    private static final BigDecimal YEAR_RATE = new BigDecimal("0.08");
    /** 期限：12 期 */
    private static final int MONTH = 12;

    /** 允许的精度差上限（分） */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    /** 等额本息为 double 浮点算法，多期累计误差可到角级（业务层 generateReturnPlan 末期兜底修正），单独放宽 */
    private static final BigDecimal TOLERANCE_DEB = new BigDecimal("0.10");

    /** 断言 a ≈ b（|a-b| ≤ tolerance） */
    private void assertApprox(BigDecimal actual, BigDecimal expected, String msg) {
        assertApprox(actual, expected, TOLERANCE, msg);
    }

    /** 断言 a ≈ b（指定公差） */
    private void assertApprox(BigDecimal actual, BigDecimal expected, BigDecimal tolerance, String msg) {
        BigDecimal diff = actual.subtract(expected).abs();
        assertTrue(diff.compareTo(tolerance) <= 0,
                msg + "：实际=" + actual + "，期望=" + expected + "，差=" + diff);
    }

    // ==================== 等额本息 Amount1Helper ====================

    @Test
    void 等额本息_各期本金之和应约等于借款金额() {
        Map<Integer, BigDecimal> principal = Amount1Helper.getPerMonthPrincipal(INVEST, YEAR_RATE, MONTH);
        assertApprox(sum(principal), INVEST, TOLERANCE_DEB, "等额本息各期本金之和应约等于借款金额（double浮点累计误差放宽到角级）");
    }

    @Test
    void 等额本息_每月还款额应恒定() {
        Map<Integer, BigDecimal> principal = Amount1Helper.getPerMonthPrincipal(INVEST, YEAR_RATE, MONTH);
        Map<Integer, BigDecimal> interest = Amount1Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        BigDecimal first = principal.get(1).add(interest.get(1));
        for (int i = 2; i <= MONTH; i++) {
            BigDecimal cur = principal.get(i).add(interest.get(i));
            assertApprox(cur, first, "等额本息每月还款额应恒定，第" + i + "期");
        }
    }

    @Test
    void 等额本息_利息应逐期递减() {
        Map<Integer, BigDecimal> interest = Amount1Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        for (int i = 1; i < MONTH; i++) {
            assertTrue(interest.get(i).compareTo(interest.get(i + 1)) > 0,
                    "等额本息利息应逐期递减，第" + i + "期=" + interest.get(i) + " 应大于 第" + (i + 1) + "期=" + interest.get(i + 1));
        }
    }

    @Test
    void 等额本息_总利息应约等于各期利息之和() {
        Map<Integer, BigDecimal> interest = Amount1Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        assertApprox(Amount1Helper.getInterestCount(INVEST, YEAR_RATE, MONTH), sum(interest),
                "等额本息总利息应约等于各期利息之和");
    }

    // ==================== 等额本金 Amount2Helper ====================

    @Test
    void 等额本金_各期本金应相等且之和约等于借款金额() {
        Map<Integer, BigDecimal> principal = Amount2Helper.getPerMonthPrincipal(INVEST, YEAR_RATE, MONTH);
        BigDecimal first = principal.get(1);
        for (int i = 2; i <= MONTH; i++) {
            assertEquals(0, first.compareTo(principal.get(i)), "等额本金每期本金应相等");
        }
        assertApprox(sum(principal), INVEST, "等额本金各期本金之和应约等于借款金额");
    }

    @Test
    void 等额本金_利息应逐期递减() {
        Map<Integer, BigDecimal> interest = Amount2Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        for (int i = 1; i < MONTH; i++) {
            assertTrue(interest.get(i).compareTo(interest.get(i + 1)) > 0,
                    "等额本金利息应逐期递减，第" + i + "期=" + interest.get(i) + " 应大于 第" + (i + 1) + "期=" + interest.get(i + 1));
        }
    }

    @Test
    void 等额本金_总利息应约等于各期利息之和() {
        Map<Integer, BigDecimal> interest = Amount2Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        assertApprox(Amount2Helper.getInterestCount(INVEST, YEAR_RATE, MONTH), sum(interest),
                "等额本金总利息应约等于各期利息之和");
    }

    // ==================== 按月付息到期还本 Amount3Helper ====================

    @Test
    void 按月付息_末期还本金其余期本金为零() {
        Map<Integer, BigDecimal> principal = Amount3Helper.getPerMonthPrincipal(INVEST, YEAR_RATE, MONTH);
        for (int i = 1; i < MONTH; i++) {
            assertEquals(0, principal.get(i).compareTo(BigDecimal.ZERO), "前 " + (MONTH - 1) + " 期本金应为 0");
        }
        assertEquals(0, principal.get(MONTH).compareTo(INVEST), "末期应偿还全部本金");
    }

    @Test
    void 按月付息_每期利息应相等() {
        Map<Integer, BigDecimal> interest = Amount3Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        BigDecimal first = interest.get(1);
        for (int i = 2; i <= MONTH; i++) {
            assertEquals(0, first.compareTo(interest.get(i)),
                    "按月付息每期利息应相等，第1期=" + first + "，第" + i + "期=" + interest.get(i));
        }
    }

    @Test
    void 按月付息_总利息应约等于每期利息乘期数() {
        Map<Integer, BigDecimal> interest = Amount3Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        BigDecimal perMonth = interest.get(1);
        assertApprox(Amount3Helper.getInterestCount(INVEST, YEAR_RATE, MONTH),
                perMonth.multiply(new BigDecimal(MONTH)),
                "按月付息总利息应约等于每期利息×期数");
    }

    // ==================== 一次还本付息 Amount4Helper ====================

    @Test
    void 一次还本_应只有一期且本金等于借款金额() {
        Map<Integer, BigDecimal> principal = Amount4Helper.getPerMonthPrincipal(INVEST, YEAR_RATE, MONTH);
        assertEquals(1, principal.size(), "一次还本付息应只有 1 期");
        assertEquals(0, principal.get(1).compareTo(INVEST));
    }

    @Test
    void 一次还本_利息应为本金乘月利率乘期数() {
        BigDecimal monthRate = YEAR_RATE.divide(new BigDecimal("12"), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal expect = INVEST.multiply(monthRate).multiply(new BigDecimal(MONTH));
        assertApprox(Amount4Helper.getInterestCount(INVEST, YEAR_RATE, MONTH), expect,
                "一次还本总利息应约等于本金×月利率×期数");
        Map<Integer, BigDecimal> interest = Amount4Helper.getPerMonthInterest(INVEST, YEAR_RATE, MONTH);
        assertApprox(interest.get(1), expect, "一次还本单期利息");
    }

    // ==================== 通用断言辅助 ====================

    /** 求和 */
    private BigDecimal sum(Map<Integer, BigDecimal> map) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : map.values()) {
            total = total.add(v);
        }
        return total;
    }
}
