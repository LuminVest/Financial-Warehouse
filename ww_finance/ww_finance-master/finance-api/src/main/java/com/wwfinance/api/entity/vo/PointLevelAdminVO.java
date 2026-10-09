package com.wwfinance.api.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台-积分等级 VO（对齐 ww_finance_admin 前端 PointLevel 类型）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "积分等级管理VO", description = "管理后台积分等级列表展示对象")
public class PointLevelAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "编号")
    private Long id;

    @ApiModelProperty(value = "等级名称")
    private String levelName;

    @ApiModelProperty(value = "积分区间开始")
    private Long minScore;

    @ApiModelProperty(value = "积分区间结束")
    private Long maxScore;

    @ApiModelProperty(value = "借款额度")
    private BigDecimal borrowLimit;

    @ApiModelProperty(value = "可借额度系数")
    private BigDecimal borrowCoefficient;

    @ApiModelProperty(value = "等级最低年利率(小数)")
    private BigDecimal borrowRate;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
