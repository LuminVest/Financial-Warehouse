package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.IntegralGrade;

import java.util.Map;

/**
 * 积分等级服务
 */
public interface IntegralGradeService extends IService<IntegralGrade> {

    /**
     * 管理后台：积分等级分页列表（仅未删除数据）
     *
     * @return {list: [PointLevelAdminVO], total}
     */
    Map<String, Object> pageForAdmin(long pageNum, long pageSize);

    /**
     * 管理后台：新增积分等级
     *
     * @param borrowCoefficient 可借额度系数
     * @param borrowRate        等级最低年利率（小数）
     */
    void addForAdmin(String levelName, Long minScore, Long maxScore, java.math.BigDecimal borrowLimit,
                     java.math.BigDecimal borrowCoefficient, java.math.BigDecimal borrowRate);

    /**
     * 管理后台：修改积分等级
     *
     * @param borrowCoefficient 可借额度系数
     * @param borrowRate        等级最低年利率（小数）
     */
    void updateForAdmin(Long id, String levelName, Long minScore, Long maxScore, java.math.BigDecimal borrowLimit,
                        java.math.BigDecimal borrowCoefficient, java.math.BigDecimal borrowRate);

    /**
     * 管理后台：删除积分等级（逻辑删除）
     */
    void deleteByAdmin(Long id);
}
