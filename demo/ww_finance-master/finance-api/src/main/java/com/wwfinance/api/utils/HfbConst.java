package com.wwfinance.api.utils;

/**
 * 旺旺银行（托管平台）相关常量
 *
 * 常量结构对齐老师源码（常量.zip / Bank/HfbConst.java）；
 * 接口 URL 为模拟银行 9090 真实地址；异步/同步回调地址为本项目实际地址（老师的 localhost 占位不适用）。
 */
public class HfbConst {

    // 给商户分配的唯一标识
    public static final String AGENT_ID = "999888";

    // 签名 key
    public static final String SIGN_KEY = "9876543210";

    /**
     * 用户绑定
     */
    // 用户绑定旺旺银行平台 url 地址
    public static final String USERBIND_URL = "http://localhost:9090/userBind/BindAgreeUserV2";
    // 用户绑定异步回调
    public static final String USERBIND_NOTIFY_URL = "http://localhost:8990/api/user/bind/notify";
    // 用户绑定同步回调
    public static final String USERBIND_RETURN_URL = "http://localhost:5173/#/bind/return";

    /**
     * 充值
     */
    // 充值旺旺银行平台 url 地址
    public static final String RECHARGE_URL = "http://localhost:9090/userAccount/AgreeBankCharge";
    // 充值异步回调
    public static final String RECHARGE_NOTIFY_URL = "http://localhost:8990/api/user/account/notify";
    // 充值同步回调
    public static final String RECHARGE_RETURN_URL = "http://localhost:5173/#/charge/return";

    /**
     * 投标
     */
    // 投标旺旺银行平台 url 地址
    public static final String INVEST_URL = "http://localhost:9090/userInvest/AgreeUserVoteProject";
    // 投标异步回调
    public static final String INVEST_NOTIFY_URL = "http://localhost:8990/api/user/lendItem/notify";
    // 投标同步回调
    public static final String INVEST_RETURN_URL = "http://localhost:5173/#/invest/return";

    /**
     * 放款
     */
    public static final String MAKE_LOAD_URL = "http://localhost:9090/userInvest/AgreeAccountLendProject";

    /**
     * 提现
     */
    // 提现旺旺银行平台 url 地址
    public static final String WITHDRAW_URL = "http://localhost:9090/userAccount/CashBankManager";
    // 提现异步回调
    public static final String WITHDRAW_NOTIFY_URL = "http://localhost:8990/api/user/account/notifyWithdraw";
    // 提现同步回调
    public static final String WITHDRAW_RETURN_URL = "http://localhost:5173/#/withdraw/return";

    /**
     * 还款扣款
     */
    // 还款扣款旺旺银行平台 url 地址
    public static final String BORROW_RETURN_URL = "http://localhost:9090/lendReturn/AgreeUserRepayment";
    // 还款扣款异步回调
    public static final String BORROW_RETURN_NOTIFY_URL = "http://localhost:8990/api/user/lendReturn/notifyUrl";
    // 还款扣款同步回调
    public static final String BORROW_RETURN_RETURN_URL = "http://localhost:5173/#/borrowReturn/return";
}
