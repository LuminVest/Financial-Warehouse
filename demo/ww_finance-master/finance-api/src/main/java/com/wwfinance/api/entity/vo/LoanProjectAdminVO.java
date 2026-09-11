package com.wwfinance.api.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理后台-标的 VO（对齐 ww_finance_admin 前端 LoanProject 类型）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "标的管理VO", description = "管理后台标的列表展示对象")
public class LoanProjectAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "标的ID")
    private Long id;

    @ApiModelProperty(value = "标的名称")
    private String title;

    @ApiModelProperty(value = "借款人姓名")
    private String borrowerName;

    @ApiModelProperty(value = "借款人ID")
    private Long borrowerId;

    @ApiModelProperty(value = "标的金额")
    private BigDecimal amount;

    @ApiModelProperty(value = "年化利率(%)")
    private BigDecimal rate;

    @ApiModelProperty(value = "借款期限(天)")
    private Integer term;

    @ApiModelProperty(value = "已募集金额")
    private BigDecimal raisedAmount;

    @ApiModelProperty(value = "募集进度(%)")
    private Integer progress;

    @ApiModelProperty(value = "状态(0待发布 1募资中 2已完成 3已逾期 4已下架)")
    private Integer status;

    @ApiModelProperty(value = "借款用途")
    private String purpose;

    @ApiModelProperty(value = "风险等级(1低 2中 3高，lend表无此字段)")
    private Integer riskLevel;

    @ApiModelProperty(value = "发布时间")
    private LocalDateTime publishTime;

    @ApiModelProperty(value = "募集截止时间")
    private LocalDate endTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
