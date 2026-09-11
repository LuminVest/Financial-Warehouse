package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.IntegralGrade;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.vo.MemberAdminVO;
import com.wwfinance.api.mapper.IntegralGradeMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.MemberService;
import com.wwfinance.common.exception.BusinessException;
import com.wwfinance.common.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemberServiceImpl implements MemberService {

    /** 新增会员初始密码 */
    private static final String INIT_PASSWORD = "123456";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IntegralGradeMapper integralGradeMapper;

    @Override
    public Map<String, Object> pageForAdmin(long pageNum, long pageSize, String keyword, Integer status) {
        Page<User> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("is_deleted = 0");
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getMobile, kw)
                    .or().like(User::getNickName, kw)
                    .or().like(User::getName, kw));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(pageParam, wrapper);

        // 一次性查出全部有效等级，按积分匹配等级名称
        List<IntegralGrade> grades = integralGradeMapper.selectList(
                new LambdaQueryWrapper<IntegralGrade>().apply("is_deleted = 0"));
        Map<String, Object> data = new HashMap<>();
        List<MemberAdminVO> voList = result.getRecords().stream()
                .map(u -> toAdminVO(u, grades))
                .collect(Collectors.toList());
        data.put("list", voList);
        data.put("total", result.getTotal());
        return data;
    }

    @Override
    public void addForAdmin(String phone, String nickname, String realName, String idCard,
                            Integer gender, Integer score, String remark) {
        // 手机号查重
        LambdaQueryWrapper<User> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.apply("is_deleted = 0").eq(User::getMobile, phone);
        if (userMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException("该手机号已存在");
        }
        User user = new User();
        user.setUserType(1);
        user.setMobile(phone);
        user.setNickName(nickname == null || nickname.isEmpty() ? phone : nickname);
        user.setName(realName);
        user.setIdCard(idCard);
        user.setPassword(MD5.encrypt(INIT_PASSWORD));
        user.setIntegral(score == null ? 0 : score);
        user.setStatus(1);
        user.setDeleted(false);
        userMapper.insert(user);
        log.info("管理后台新增会员: id={}, mobile={}", user.getId(), phone);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不合法");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("会员不存在");
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("管理后台更新会员状态: id={}, status={}", id, status);
    }

    private MemberAdminVO toAdminVO(User u, List<IntegralGrade> grades) {
        return new MemberAdminVO()
                .setId(u.getId())
                .setPhone(u.getMobile())
                .setNickname(u.getNickName())
                .setRealName(u.getName())
                .setIdCard(u.getIdCard())
                .setGender(u.getGender())
                .setScore(u.getIntegral())
                .setLevelName(matchLevelName(u.getIntegral(), grades))
                .setStatus(u.getStatus())
                .setRegisterTime(u.getCreateTime())
                .setLastLoginTime("")
                .setRemark("");
    }

    /**
     * 按积分匹配等级名称（积分落在 [integral_start, integral_end] 区间）
     */
    private String matchLevelName(Integer integral, List<IntegralGrade> grades) {
        if (integral == null || grades == null || grades.isEmpty()) {
            return "";
        }
        for (IntegralGrade g : grades) {
            Integer start = g.getIntegralStart();
            Integer end = g.getIntegralEnd();
            if (start != null && end != null && integral >= start && integral <= end) {
                return g.getGradeName();
            }
        }
        return "";
    }
}
