package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.IntegralGrade;
import com.wwfinance.api.entity.vo.PointLevelAdminVO;
import com.wwfinance.common.exception.BusinessException;
import com.wwfinance.api.mapper.IntegralGradeMapper;
import com.wwfinance.api.service.IntegralGradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IntegralGradeServiceImpl extends ServiceImpl<IntegralGradeMapper, IntegralGrade> implements IntegralGradeService {

    @Override
    public Map<String, Object> pageForAdmin(long pageNum, long pageSize) {
        Page<IntegralGrade> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<IntegralGrade> wrapper = new LambdaQueryWrapper<>();
        // 手动过滤逻辑删除（未配置全局 logic-delete）
        wrapper.apply("is_deleted = 0");
        wrapper.orderByAsc(IntegralGrade::getIntegralStart);
        Page<IntegralGrade> result = this.page(pageParam, wrapper);
        List<PointLevelAdminVO> voList = result.getRecords().stream()
                .map(g -> new PointLevelAdminVO()
                        .setId(g.getId())
                        .setLevelName(g.getGradeName())
                        .setMinScore(g.getIntegralStart())
                        .setMaxScore(g.getIntegralEnd())
                        .setBorrowLimit(g.getBorrowAmount())
                        .setCreateTime(g.getCreateTime()))
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", voList);
        data.put("total", result.getTotal());
        return data;
    }

    @Override
    public void addForAdmin(String levelName, Integer minScore, Integer maxScore, BigDecimal borrowLimit) {
        IntegralGrade grade = new IntegralGrade();
        grade.setGradeName(levelName);
        grade.setIntegralStart(minScore);
        grade.setIntegralEnd(maxScore);
        grade.setBorrowAmount(borrowLimit);
        grade.setDeleted(false);
        this.save(grade);
    }

    @Override
    public void updateForAdmin(Long id, String levelName, Integer minScore, Integer maxScore, BigDecimal borrowLimit) {
        IntegralGrade grade = this.getById(id);
        if (grade == null) {
            throw new BusinessException("积分等级不存在");
        }
        grade.setGradeName(levelName);
        grade.setIntegralStart(minScore);
        grade.setIntegralEnd(maxScore);
        grade.setBorrowAmount(borrowLimit);
        this.updateById(grade);
    }

    @Override
    public void deleteByAdmin(Long id) {
        IntegralGrade grade = this.getById(id);
        if (grade == null) {
            throw new BusinessException("积分等级不存在");
        }
        // 逻辑删除
        grade.setDeleted(true);
        this.updateById(grade);
    }
}
