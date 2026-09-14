package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.LendReturn;

import java.util.List;
import java.util.Map;

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

    /**
     * 借款人发起还款：
     * 1. 校验：当前用户是该标的借款人、已绑定托管账户、该期未归还
     * 2. 组装旺旺银行还款表单（fromBindCode / agentBatchNo / totalAmt ...）
     * 3. 返回自动提交表单（前端跳转托管平台完成还款）
     *
     * @param lendId        标的id
     * @param currentPeriod 还款期数
     * @param userId        当前登录用户（借款人）
     * @return 托管平台自动提交表单
     */
    String commitRepayment(Long lendId, Integer currentPeriod, Long userId);

    /**
     * 还款异步回调：
     * 1. 验签 + resultCode 判断
     * 2. 按 agentBatchNo（=该期还款编号 returnNo）定位还款计划，幂等
     * 3. 更新 lend_return 已归还（记录 real_return_time / 逾期标记）
     * 4. 同步该期所有 lend_item_return 已归还（投资人回款到账）
     * 5. 投资人 user_account.amount 入账
     */
    String notifyRepayment(Map<String, Object> paramMap);

}
