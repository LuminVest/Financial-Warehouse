package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.Lend;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 标的 Service
 */
public interface LendService extends IService<Lend> {

    /**
     * 标的列表（公开）
     */
    List<Lend> getLendList();

    /**
     * 标的详情（含借款人信息、投资进度）
     */
    Map<String, Object> getLendDetail(Long id);

    /**
     * 投资收益计算器：按还款方式计算总投资收益
     *
     * @param returnMethod 1-等额本息 2-等额本金 3-每月还息一次还本 4-一次还本
     */
    BigDecimal getInterestCount(BigDecimal invest, BigDecimal yearRate, int totalmonth, int returnMethod);

    /**
     * 推荐标的（demo 简化：按热度排序取 topN，真实可替换为 Item-CF 协同过滤）
     */
    List<Lend> getRecommendList(int topN);

    /**
     * 放款：满标后同步调用银行放款接口（解冻投资人资金→划转借款人），
     * 成功后借款人本地账户入账并置标的为已放款(3)。幂等：已放款直接跳过。
     */
    void makeLoan(Long lendId);

}
