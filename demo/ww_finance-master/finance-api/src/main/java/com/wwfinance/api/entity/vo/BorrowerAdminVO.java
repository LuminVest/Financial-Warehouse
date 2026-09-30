package com.wwfinance.api.entity.vo;

import com.wwfinance.api.entity.BorrowerAttach;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台-借款人列表 VO（对齐 ww_finance_admin 前端 Borrower 类型）
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "借款人管理VO", description = "管理后台借款人列表展示对象")
public class BorrowerAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "借款人记录id")
    private Long id;

    @ApiModelProperty(value = "会员id（user表id）")
    private Long memberId;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "身份证号")
    private String idCard;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "性别 0-未知 1-男 2-女")
    private Integer gender;

    @ApiModelProperty(value = "年龄")
    private Integer age;

    @ApiModelProperty(value = "是否结婚 0-否 1-是")
    private Integer isMarry;

    @ApiModelProperty(value = "审核状态 0-待审核 1-审核通过 2-已拒绝")
    private Integer auditStatus;

    @ApiModelProperty(value = "授信额度（demo 暂无额度体系，返回 0）")
    private Integer creditLimit;

    @ApiModelProperty(value = "已用额度（demo 返回 0）")
    private Integer usedLimit;

    @ApiModelProperty(value = "银行卡号（取绑定表，脱敏展示由前端处理）")
    private String bankCard;

    @ApiModelProperty(value = "工作单位（demo 暂无字段，返回空）")
    private String employer;

    @ApiModelProperty(value = "学历文字（字典翻译）")
    private String educationText;

    @ApiModelProperty(value = "行业文字（字典翻译）")
    private String industryText;

    @ApiModelProperty(value = "还款来源文字（字典翻译）")
    private String returnSourceText;

    @ApiModelProperty(value = "联系人姓名")
    private String contactsName;

    @ApiModelProperty(value = "联系人手机号")
    private String contactsMobile;

    @ApiModelProperty(value = "联系人关系文字（字典翻译）")
    private String contactsRelationText;

    @ApiModelProperty(value = "月收入（元，income 为字典编号，此处返回 0）")
    private Integer monthlyIncome;

    @ApiModelProperty(value = "月收入档位文字（字典翻译，如 500000以上）")
    private String incomeText;

    @ApiModelProperty(value = "申请时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "审核时间（简化取最近更新时间）")
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "认证材料附件列表（身份证正反面/车辆/房产等）")
    private List<BorrowerAttach> attachList;
}
