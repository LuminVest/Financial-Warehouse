package com.wwfinance.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分等级表
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("integral_grade")
public class IntegralGrade implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 等级名称（联调时新增列 grade_name） */
    @TableField("grade_name")
    private String gradeName;

    /** 积分区间开始 */
    @TableField("integral_start")
    private Long integralStart;

    /** 积分区间结束 */
    @TableField("integral_end")
    private Long integralEnd;

    /** 借款额度 */
    @TableField("borrow_amount")
    private BigDecimal borrowAmount;

    /** 可借额度系数（等级越高额度越高，可借额度=收入档位额度×系数） */
    @TableField("borrow_coefficient")
    private BigDecimal borrowCoefficient;

    /** 等级最低年利率（小数，等级越高利率越低，借款利率不得低于此值） */
    @TableField("borrow_rate")
    private BigDecimal borrowRate;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除(1:已删除，0:未删除) */
    @TableField("is_deleted")
    private Boolean deleted;
}
