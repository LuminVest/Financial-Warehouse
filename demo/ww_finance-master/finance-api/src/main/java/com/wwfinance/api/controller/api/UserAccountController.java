package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.api.utils.RequestHelper;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Api(tags = "用户账户")
@RestController
@RequestMapping("/api/core/userAccount")
public class UserAccountController {

    @Resource
    private UserAccountService userAccountService;

    /**
     * 充值（生成托管平台表单）
     * 对齐接口文档：GET /auth/commitCharge/{chargeAmt}，金额走路径变量
     */
    @ApiOperation("充值")
    @GetMapping("/auth/commitCharge/{chargeAmt}")
    public PccAjaxResult commitCharge(@PathVariable BigDecimal chargeAmt) {
        Long userId = LoginUserContext.getUserid().longValue();
        String formStr = userAccountService.commitCharge(String.valueOf(chargeAmt), userId);
        return new PccAjaxResult(200, "账户提交充值数据成功", formStr);
    }

    /**
     * 充值异步回调
     * 注意：第三方支付回调一般不应要求登录 token。
     * 必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试。
     */
    @ApiOperation("充值异步回调")
    @PostMapping(value = "/notify", produces = "text/plain")
    public String notify(HttpServletRequest request) {
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("用户充值异步回调：" + JSON.toJSONString(paramMap));
        // 验签（demo 简化：接入支付平台后在 RequestHelper 中实现真实验签）
        if (RequestHelper.isSignEquals(paramMap)) {
            // 判断业务是否成功
            if ("0001".equals(paramMap.get("resultCode"))) {
                // 同步账户数据
                return userAccountService.notify(paramMap);
            } else {
                return "success";
            }
        } else {
            return "fail";
        }
    }

    /**
     * 查询账户余额
     */
    @ApiOperation("查询账户余额")
    @GetMapping("/auth/getAccount")
    public PccAjaxResult getAccount() {
        Long userId = LoginUserContext.getUserid().longValue();
        BigDecimal account = userAccountService.getAccount(userId);
        return new PccAjaxResult(200, "查询账户余额", account);
    }
}
