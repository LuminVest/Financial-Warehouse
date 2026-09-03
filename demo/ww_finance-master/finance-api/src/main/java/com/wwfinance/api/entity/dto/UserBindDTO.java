package com.wwfinance.api.entity.dto;

import lombok.Data;

/**
 * 用户绑定提交参数（前端接收，与老师 UserBindDTO 对齐）
 */
@Data
public class UserBindDTO {

    /** 用户姓名 */
    private String name;

    /** 身份证号 */
    private String idCard;

    /** 银行卡号 */
    private String bankNo;

    /** 银行类型 */
    private String bankType;

    /** 手机号 */
    private String mobile;
}
