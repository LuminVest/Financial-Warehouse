package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendItem;
import com.wwfinance.api.entity.LendItemReturn;
import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.mapper.LendItemReturnMapper;
import com.wwfinance.api.service.LendItemReturnService;
import com.wwfinance.api.service.LendItemService;
import com.wwfinance.api.service.LendReturnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LendItemReturnServiceImpl extends ServiceImpl<LendItemReturnMapper, LendItemReturn>
        implements LendItemReturnService {

    /** 未归还 */
    private static final int STATUS_UNPAID = 0;

    @Autowired
    private LendReturnService lendReturnService;

    @Autowired
    private LendItemService lendItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateReturnDetail(Lend lend) {
        if (lend == null || lend.getId() == null) {
            log.warn("生成回款明细失败: lend 为空");
            return;
        }
        // 幂等：同一标的已生成过回款明细则跳过（避免满标回调重试重复生成）
        long exist = this.count(new LambdaQueryWrapper<LendItemReturn>()
                .eq(LendItemReturn::getLendId, lend.getId())
                .apply("is_deleted = 0"));
        if (exist > 0) {
            log.info("回款明细已生成, 跳过: lendId={}", lend.getId());
            return;
        }

        // 该标的的还款计划（每期）与投资记录（每个投资人）
        List<LendReturn> planList = lendReturnService.listByLendId(lend.getId());
        List<LendItem> items = lendItemService.getListByLendId(lend.getId());
        if (planList.isEmpty() || items.isEmpty()) {
            log.warn("生成回款明细失败: 还款计划或投资记录为空 lendId={}, planSize={}, itemSize={}",
                    lend.getId(), planList.size(), items.size());
            return;
        }

        BigDecimal amount = lend.getAmount() == null ? BigDecimal.ZERO : lend.getAmount();
        List<LendItemReturn> detailList = new ArrayList<>();

        // 每个投资人的份额比例
        List<BigDecimal> ratios = new ArrayList<>();
        for (LendItem item : items) {
            BigDecimal invest = item.getInvestAmount() == null ? BigDecimal.ZERO : item.getInvestAmount();
            ratios.add(amount.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : invest.divide(amount, 8, RoundingMode.HALF_UP));
        }
        int itemCount = items.size();

        // 逐期拆分：分币分配法（每人向下取整到分 + 余数分币分发），
        // 数学保证：每项 ≥ 0 且 Σ本金/Σ利息/Σ合计 精确等于该期计划值（任意人数都不会出现负数）
        for (LendReturn plan : planList) {
            BigDecimal planPrincipal = plan.getPrincipal() == null ? BigDecimal.ZERO : plan.getPrincipal();
            BigDecimal planInterest = plan.getInterest() == null ? BigDecimal.ZERO : plan.getInterest();
            // 计划值换算成"分"（整数运算，无浮点误差）
            long planPCents = planPrincipal.movePointRight(2).longValueExact();
            long planICents = planInterest.movePointRight(2).longValueExact();
            long[] pCents = new long[itemCount]; // 每位投资人应得本金（分）
            long[] iCents = new long[itemCount]; // 每位投资人应得利息（分）
            long accPC = 0;
            long accIC = 0;
            for (int idx = 0; idx < itemCount; idx++) {
                // 向下取整到分：plan × 份额，截断分币小数（floor）
                pCents[idx] = planPrincipal.multiply(ratios.get(idx)).movePointRight(2).longValue();
                iCents[idx] = planInterest.multiply(ratios.get(idx)).movePointRight(2).longValue();
                accPC += pCents[idx];
                accIC += iCents[idx];
            }
            // 余数分币（0 ~ 投资人数量-1 之间），按投资顺序每户补 1 分
            long remainP = planPCents - accPC;
            long remainI = planICents - accIC;
            for (int idx = 0; idx < itemCount; idx++) {
                long pc = pCents[idx];
                long ic = iCents[idx];
                if (idx < remainP) {
                    pc += 1;
                }
                if (idx < remainI) {
                    ic += 1;
                }
                BigDecimal principal = BigDecimal.valueOf(pc, 2);
                BigDecimal interest = BigDecimal.valueOf(ic, 2);

                LendItemReturn detail = new LendItemReturn();
                detail.setLendReturnId(plan.getId());
                detail.setLendItemId(items.get(idx).getId());
                detail.setLendId(lend.getId());
                detail.setInvestUserId(items.get(idx).getInvestUserId());
                detail.setInvestAmount(items.get(idx).getInvestAmount());
                detail.setCurrentPeriod(plan.getCurrentPeriod());
                detail.setLendYearRate(plan.getLendYearRate());
                detail.setReturnMethod(plan.getReturnMethod());
                detail.setPrincipal(principal);
                detail.setInterest(interest);
                detail.setTotal(principal.add(interest));
                detail.setFee(BigDecimal.ZERO);
                detail.setReturnDate(plan.getReturnDate());
                detail.setStatus(STATUS_UNPAID);
                detail.setIsOverdue(false);
                detail.setOverdueTotal(BigDecimal.ZERO);
                detail.setDeleted(false);
                detailList.add(detail);
            }
        }

        this.saveBatch(detailList);
        log.info("生成回款明细完成: lendId={}, 投资人={}人, 期数={}期, 共 {} 条",
                lend.getId(), itemCount, planList.size(), detailList.size());
    }

    @Override
    public List<LendItemReturn> listByLendId(Long lendId) {
        return this.list(new LambdaQueryWrapper<LendItemReturn>()
                .eq(LendItemReturn::getLendId, lendId)
                .apply("is_deleted = 0")
                .orderByAsc(LendItemReturn::getCurrentPeriod)
                .orderByAsc(LendItemReturn::getLendItemId));
    }
}
