package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.utils.RequestHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 提现异步回调（第三方托管平台回调，无需登录 token）
 * 注意：必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试
 */
@Slf4j
@Api(tags = "提现回调")
@RestController
@RequestMapping("/api/user/account")
public class WithdrawNotifyController {

    @Resource
    private UserAccountService userAccountService;

    @ApiOperation("提现异步回调")
    @PostMapping(value = "/notifyWithdraw", produces = "text/plain")
    public String notifyWithdraw(HttpServletRequest request) {
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("用户提现异步回调：" + JSON.toJSONString(paramMap));
        // 验签（demo 简化：接入支付平台后在 RequestHelper 中实现真实验签）
        if (RequestHelper.isSignEquals(paramMap)) {
            // 判断业务是否成功
            if ("0001".equals(paramMap.get("resultCode"))) {
                // 同步账户数据
                return userAccountService.notifyWithdraw(paramMap);
            } else {
                return "success";
            }
        } else {
            return "fail";
        }
    }
}
