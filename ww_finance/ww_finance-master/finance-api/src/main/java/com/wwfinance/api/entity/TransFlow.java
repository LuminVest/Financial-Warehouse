package com.wwfinance.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水表
 */
@Data
@Accessors(chain = true)
@TableName("trans_flow")
public class TransFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    @TableField("user_id")
    private Long userId;

    /** 用户名称 */
    @TableField("user_name")
    private String userName;

    /** 交易单号 */
    @TableField("trans_no")
    private String transNo;

    /** 交易类型（1：充值 2：提现 3：投标 4：投资回款 5：放款 6：还款） */
    @TableField("trans_type")
    private Integer transType;

    /** 交易类型名称 */
    @TableField("trans_type_name")
    private String transTypeName;

    /** 交易金额 */
    @TableField("trans_amount")
    private BigDecimal transAmount;

    /** 备注 */
    @TableField("memo")
    private String memo;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除(1:已删除，0:未删除) */
    @TableField("is_deleted")
    private Boolean deleted;
}
