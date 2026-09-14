package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendItemReturn;

import java.util.List;

public interface LendItemReturnService extends IService<LendItemReturn> {

    /**
     * 满标后生成回款明细：把每期还款计划按投资人份额拆分到每个投资人
     *  - 同一标的幂等：已生成过则直接跳过
     *  - 拆分规则：投资人第 p 期回款 = 还款计划第 p 期 × (invest_amount / lend.amount)
     *  - 末期/最后一位投资人兜底：保证每期各投资人本金、利息合计 = 该期还款计划
     *  - 单投资人（投满全部份额）时，回款明细 = 还款计划原样复制
     */
    void generateReturnDetail(Lend lend);

    /**
     * 按标的查询回款明细
     */
    List<LendItemReturn> listByLendId(Long lendId);
}
