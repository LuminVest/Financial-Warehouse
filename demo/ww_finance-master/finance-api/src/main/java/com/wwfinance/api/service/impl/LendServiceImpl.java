package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Lend;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.mapper.LendMapper;
import com.wwfinance.api.service.LendService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.Amount1Helper;
import com.wwfinance.api.utils.Amount2Helper;
import com.wwfinance.api.utils.Amount3Helper;
import com.wwfinance.api.utils.Amount4Helper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LendServiceImpl extends ServiceImpl<LendMapper, Lend> implements LendService {

    @Autowired
    private UserService userService;

    /**
     * 标的列表：未删除，按 id 倒序（最新在前）
     */
    @Override
    public List<Lend> getLendList() {
        return this.list(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getDeleted, false)
                .orderByDesc(Lend::getId));
    }

    /**
     * 标的详情：返回 标的 + 借款人姓名/手机号 + 投资进度
     */
    @Override
    public Map<String, Object> getLendDetail(Long id) {
        Lend lend = this.getById(id);
        Map<String, Object> result = new HashMap<>();
        if (lend == null) {
            return result;
        }
        result.put("lend", lend);

        // 借款人信息
        User user = userService.getById(lend.getUserId());
        if (user != null) {
            result.put("borrowerName", user.getName());
            result.put("borrowerMobile", user.getMobile());
        }

        // 投资进度：已投/总额（百分比，保留两位）
        BigDecimal amount = lend.getAmount() == null ? BigDecimal.ZERO : lend.getAmount();
        BigDecimal invested = lend.getInvestAmount() == null ? BigDecimal.ZERO : lend.getInvestAmount();
        BigDecimal progress = (amount.compareTo(BigDecimal.ZERO) > 0)
                ? invested.multiply(new BigDecimal(100)).divide(amount, 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        result.put("investProgress", progress);
        return result;
    }

    /**
     * 投资收益计算器：按还款方式路由到对应利息工具（Amount1-4 对应 returnMethod 1-4）
     */
    @Override
    public BigDecimal getInterestCount(BigDecimal invest, BigDecimal yearRate, int totalmonth, int returnMethod) {
        BigDecimal interest;
        switch (returnMethod) {
            case 1:  // 等额本息
                interest = Amount1Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 2:  // 等额本金
                interest = Amount2Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 3:  // 每月还息一次还本
                interest = Amount3Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 4:  // 一次还本
                interest = Amount4Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            default:
                throw new RuntimeException("不支持的还款方式：" + returnMethod);
        }
        return interest == null ? BigDecimal.ZERO : interest;
    }

    /**
     * 推荐标的：demo 简化实现（按投资人数降序 + 未满标优先取 topN）。
     * 说明：接口文档标注为 Item-CF 协同过滤，真实场景可按用户历史投标行为计算相似度；
     * 当前演示口径按热度推荐，答辩可说明为简化版。
     */
    @Override
    public List<Lend> getRecommendList(int topN) {
        return this.list(new LambdaQueryWrapper<Lend>()
                .eq(Lend::getDeleted, false)
                .ne(Lend::getStatus, 2)        // 未满标的才推荐
                .orderByDesc(Lend::getInvestNum)
                .last("limit " + topN));
    }
}
