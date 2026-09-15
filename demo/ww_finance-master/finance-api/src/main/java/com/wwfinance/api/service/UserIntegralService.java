package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.UserIntegral;

import java.util.List;
import java.util.Map;

/**
 * 用户积分 Service
 */
public interface UserIntegralService extends IService<UserIntegral> {

    /**
     * 给用户增加积分：写 user_integral 流水 + 累加 User.integral 总积分。
     * 幂等：content（含业务单号）相同且用户相同的记录已存在则跳过，防止重复回调重复加分。
     */
    void addIntegral(Long userId, Integer integral, String content);

    /**
     * 我的积分信息：总积分 + 匹配等级（积分区间→等级名/借款额度）+ 积分明细
     */
    Map<String, Object> getIntegralInfo(Long userId);

    /**
     * 用户积分明细（倒序）
     */
    List<UserIntegral> listByUser(Long userId);
}
