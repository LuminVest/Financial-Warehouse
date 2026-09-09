package com.wwfinance.api.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台-借款记录 VO（对齐 ww_finance_admin 前端 BorrowRecord 类型）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "借款记录管理VO", description = "管理后台借款记录列表展示对象")
public class BorrowRecordAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "借款记录id")
    private Long id;

    @ApiModelProperty(value = "借款人姓名（关联user表）")
    private String borrowerName;

    @ApiModelProperty(value = "借款人用户id")
    private Long borrowerId;

    @ApiModelProperty(value = "借款金额（元）")
    private BigDecimal amount;

    @ApiModelProperty(value = "期限（天，源为月，demo直接映射数值）")
    private Integer term;

    @ApiModelProperty(value = "年化利率（%）")
    private BigDecimal rate;

    @ApiModelProperty(value = "状态 0-待审核 1-审核通过 2-还款中 3-已结清 4-已拒绝")
    private Integer status;

    @ApiModelProperty(value = "借款用途（字典转换后的中文）")
    private String purpose;

    @ApiModelProperty(value = "已还金额（demo返回0）")
    private BigDecimal repayAmount;

    @ApiModelProperty(value = "申请时间")
    private LocalDateTime applyTime;

    @ApiModelProperty(value = "审核时间（简化取最近更新时间）")
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "应还清时间（demo暂无，返回空）")
    private String repayEndTime;

    @ApiModelProperty(value = "拒绝原因（demo暂无字段，返回空）")
    private String rejectReason;
}
