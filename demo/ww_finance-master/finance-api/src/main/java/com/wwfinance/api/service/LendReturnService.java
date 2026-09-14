package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendReturn;

import java.util.List;

public interface LendReturnService extends IService<LendReturn> {

    /**
     * 满标后生成标的的还款计划（按还款方式拆分每期本金/利息）
     *  - 同一标的幂等：已生成过则直接跳过
     *  - 还款日：以满标日为基准，第 i 期还款日 = 满标日 + i 个月
     *  - 一次还本（方式4）只生成 1 期（到期一次还本付息）
     */
    void generateReturnPlan(Lend lend);

    /**
     * 按标的查询还款计划（按期数升序）
     */
    List<LendReturn> listByLendId(Long lendId);
}
