package com.wwfinance.api.client;

import com.wwfinance.common.result.PccAjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信服务远程调用客户端（service-sms / finance-message）
 * 用于注册验证码下发：api 通过 OpenFeign 调用短信服务，失败走 SmsFeignClientFallback 降级
 */
@FeignClient(value = "service-sms", fallback = SmsFeignClientFallback.class)
public interface SmsFeignClient {

    /**
     * 发送注册验证码（短信服务内部会校验手机号是否已注册，并写入 Redis）
     * @param mobile 手机号
     * @return data 为验证码（demo 联调返回；生产环境短信下发后可不返回）
     */
    @PostMapping("/api/sms/user/sendSMS")
    PccAjaxResult sendSMS(@RequestParam("mobile") String mobile);
}
