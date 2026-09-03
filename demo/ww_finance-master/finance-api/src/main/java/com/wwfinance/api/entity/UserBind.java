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
 * 用户绑定表（实名 + 银行卡绑定）
 */
@Data
@Accessors(chain = true)
@TableName("user_bind")
public class UserBind implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户姓名
     */
    @TableField("name")
    private String name;

    /**
     * 身份证号
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 银行卡号
     */
    @TableField("bank_no")
    private String bankNo;

    /**
     * 银行类型
     */
    @TableField("bank_type")
    private String bankType;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 绑定账户协议号
     */
    @TableField("bind_code")
    private String bindCode;

    /**
     * 状态（0：未绑定 1：绑定成功 -1：绑定失败）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除(1:已删除，0:未删除)
     */
    @TableField("is_deleted")
    private Boolean deleted;

}
