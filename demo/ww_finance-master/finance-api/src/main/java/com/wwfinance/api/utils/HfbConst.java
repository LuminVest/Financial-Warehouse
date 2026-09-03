package com.wwfinance.api.utils;

/**
 * 旺旺银行（托管平台）相关常量
 *
 * demo 简化版：未真正对接托管平台，URL 均为占位配置；
 * 接入真实托管平台后，替换为平台提供的正式地址与代理商参数。
 */
public class HfbConst {

    /** 代理商 id */
    public static final String AGENT_ID = "1001";

    /** 用户绑定地址（旺旺银行托管平台：验签渲染绑定页） */
    public static final String USERBIND_URL = "http://localhost:9090/userBind/BindAgreeUserV2";

    /** 用户绑定同步回调地址（占位） */
    public static final String USERBIND_RETURN_URL = "http://localhost:5173/#/bind/return";

    /** 用户绑定异步回调地址（本项目 notify 接口） */
    public static final String USERBIND_NOTIFY_URL = "http://localhost:8990/api/user/bind/notify";

    /** 充值地址（旺旺银行托管平台：验签渲染充值确认页） */
    public static final String USERCHARGE_URL = "http://localhost:9090/userAccount/AgreeBankCharge";

    /** 充值同步回调地址（占位） */
    public static final String USERCHARGE_RETURN_URL = "http://localhost:5173/#/charge/return";

    /** 充值异步回调地址（本项目 notify 接口） */
    public static final String USERCHARGE_NOTIFY_URL = "http://localhost:8990/api/user/account/notify";
}
