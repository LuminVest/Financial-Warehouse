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
    private Integer minScore;

    @ApiModelProperty(value = "积分区间结束")
    private Integer maxScore;

    @ApiModelProperty(value = "借款额度")
    private BigDecimal borrowLimit;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
