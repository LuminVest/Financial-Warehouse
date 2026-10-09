package com.wwfinance.api.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台-标的投资记录 VO（对齐 ww_finance_admin 前端 Investment 类型）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "标的投资记录VO", description = "管理后台标的投资列表展示对象")
public class InvestmentAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "投资记录ID")
    private Long id;

    @ApiModelProperty(value = "标的ID")
    private Long projectId;

    @ApiModelProperty(value = "投资人姓名")
    private String investorName;

    @ApiModelProperty(value = "投资人ID")
    private Long investorId;

    @ApiModelProperty(value = "投资金额")
    private BigDecimal amount;

    @ApiModelProperty(value = "预期收益")
    private BigDecimal expectedReturn;

    @ApiModelProperty(value = "投资期限(天)")
    private Integer term;

    @ApiModelProperty(value = "状态(0投资中 1持有中 2已退出 3已收益)")
    private Integer status;

    @ApiModelProperty(value = "投资时间")
    private LocalDateTime investTime;
}
