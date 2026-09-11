package com.wwfinance.api.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理后台-会员 VO（对齐 ww_finance_admin 前端 Member 类型）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "会员管理VO", description = "管理后台会员列表展示对象")
public class MemberAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "会员ID")
    private Long id;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "身份证号")
    private String idCard;

    @ApiModelProperty(value = "性别(1男 2女，用户表无性别字段，默认未知)")
    private Integer gender;

    @ApiModelProperty(value = "积分")
    private Integer score;

    @ApiModelProperty(value = "等级名称（按积分匹配积分等级表）")
    private String levelName;

    @ApiModelProperty(value = "状态(1正常 0禁用)")
    private Integer status;

    @ApiModelProperty(value = "注册时间")
    private LocalDateTime registerTime;

    @ApiModelProperty(value = "最后登录时间")
    private String lastLoginTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
