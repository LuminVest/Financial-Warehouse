package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.IntegralGrade;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserIntegral;
import com.wwfinance.api.mapper.IntegralGradeMapper;
import com.wwfinance.api.mapper.UserIntegralMapper;
import com.wwfinance.api.service.UserIntegralService;
import com.wwfinance.api.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分规则（演示口径）：充值/投标/回款/放款按金额 1 元 = 1 分（向下取整）。
 * 总积分存 User.integral，明细存 user_integral。
 */
@Slf4j
@Service
public class UserIntegralServiceImpl extends ServiceImpl<UserIntegralMapper, UserIntegral> implements UserIntegralService {

    @Autowired
    private UserService userService;

    @Autowired
    private IntegralGradeMapper integralGradeMapper;

    @Override
    public List<UserIntegral> listByUser(Long userId) {
        if (userId == null) {
            return new java.util.ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<UserIntegral>()
                .eq(UserIntegral::getUserId, userId)
                .apply("is_deleted = 0")
                .orderByDesc(UserIntegral::getId));
    }

    @Override
    public Map<String, Object> getIntegralInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userId == null ? null : userService.getById(userId);
        int integral = user == null || user.getIntegral() == null ? 0 : user.getIntegral();
        result.put("userId", userId);
        result.put("userName", user == null ? "" : user.getName());
        result.put("integral", integral);
        result.put("records", listByUser(userId));

        // 按积分区间匹配等级（integral_grade 有效数据）
        IntegralGrade matched = null;
        List<IntegralGrade> grades = integralGradeMapper.selectList(
                new LambdaQueryWrapper<IntegralGrade>().apply("is_deleted = 0"));
        if (grades != null) {
            for (IntegralGrade g : grades) {
                if (g.getIntegralStart() != null && g.getIntegralEnd() != null
                        && integral >= g.getIntegralStart() && integral <= g.getIntegralEnd()) {
                    matched = g;
                    break;
                }
            }
        }
        if (matched != null) {
            result.put("gradeName", matched.getGradeName());
            result.put("gradeStart", matched.getIntegralStart());
            result.put("gradeEnd", matched.getIntegralEnd());
            result.put("borrowAmount", matched.getBorrowAmount());
        } else {
            result.put("gradeName", "");
            result.put("gradeStart", null);
            result.put("gradeEnd", null);
            result.put("borrowAmount", null);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addIntegral(Long userId, Integer integral, String content) {
        if (userId == null || integral == null || integral <= 0) {
            log.warn("积分增加跳过: 参数不合法 userId={}, integral={}", userId, integral);
            return;
        }
        // 幂等：同一用户 + 同一业务说明（含单号）只记一次
        long exists = this.count(new LambdaQueryWrapper<UserIntegral>()
                .eq(UserIntegral::getUserId, userId)
                .eq(UserIntegral::getContent, content)
                .apply("is_deleted = 0"));
        if (exists > 0) {
            log.info("积分流水已存在, 跳过: userId={}, content={}", userId, content);
            return;
        }
        // 写积分明细
        UserIntegral record = new UserIntegral()
                .setUserId(userId)
                .setIntegral(integral)
                .setContent(content)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now())
                .setDeleted(false);
        this.save(record);
        // 累加总积分（User.integral）
        User user = userService.getById(userId);
        if (user != null) {
            int old = user.getIntegral() == null ? 0 : user.getIntegral();
            user.setIntegral(old + integral);
            userService.updateById(user);
        }
        log.info("用户增加积分, userId={}, +{}分, 说明={}", userId, integral, content);
    }
}
