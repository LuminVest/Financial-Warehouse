package com.wwfinance.api.entity.dto;

import com.wwfinance.api.entity.BorrowerAttach;
import lombok.Data;

import java.util.List;

/**
 * 借款人认证信息 DTO（对应前端「借款人认证」表单提交）
 * 姓名/身份证/手机号由后端从 user 表取，前端可传其余认证信息与附件
 */
@Data
public class BorrowerDTO {

    /**
     * 性别（1：男 0：女）
     */
    private Integer sex;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 学历
     */
    private Integer education;

    /**
     * 是否结婚（1：是 0：否）
     */
    private Integer isMarry;

    /**
     * 行业
     */
    private Integer industry;

    /**
     * 月收入
     */
    private Integer income;

    /**
     * 还款来源
     */
    private Integer returnSource;

    /**
     * 联系人名称
     */
    private String contactsName;

    /**
     * 联系人手机
     */
    private String contactsMobile;

    /**
     * 联系人关系
     */
    private Integer contactsRelation;

    /**
     * 借款人上传附件（身份证正反面等）
     */
    private List<BorrowerAttach> borrowerAttachList;
}
