package com.wwfinance.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户积分记录表（每次获取积分的流水）
 */
@Data
@Accessors(chain = true)
@TableName("user_integral")
public class UserIntegral implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    @TableField("user_id")
    private Long userId;

    /** 积分 */
    @TableField("integral")
    private Integer integral;

    /** 获取积分说明（含业务单号，用于幂等） */
    @TableField("content")
    private String content;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除(1:已删除，0:未删除) */
    @TableField("is_deleted")
    private Boolean deleted;
}
