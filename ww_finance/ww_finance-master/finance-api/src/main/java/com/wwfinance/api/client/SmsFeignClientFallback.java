package com.wwfinance.api.client;

import com.wwfinance.common.result.PccAjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信服务降级兜底：service-sms 不可用（服务宕机/网络异常/超时）时触发。
 * 返回 500 标记短信通道异常，由调用方（UserServiceImpl.sendCode）降级为本地生成验证码。
 */
@Slf4j
@Component
public class SmsFeignClientFallback implements SmsFeignClient {

    @Override
    public PccAjaxResult sendSMS(String mobile) {
        log.warn("短信服务(service-sms)不可用，触发 Feign 降级兜底, mobile={}", mobile);
        return new PccAjaxResult(500, "短信服务暂不可用", null);
    }
}
