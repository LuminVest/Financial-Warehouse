package com.wwfinance.api.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求参数处理工具（对齐老师 finance-api 的 RequestHelper）
 *
 * demo 简化版：未接第三方支付平台，验签直接放行；
 * 接入支付平台后，在 isSignEquals 中按平台规则实现真实验签。
 */
public class RequestHelper {

    /**
     * 把 request.getParameterMap() 转换为 Map<String, Object>（每个参数取第一个值）
     */
    public static Map<String, Object> switchMap(Map<String, String[]> paramMap) {
        Map<String, Object> result = new HashMap<>();
        if (paramMap != null) {
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                String[] values = entry.getValue();
                result.put(entry.getKey(), (values != null && values.length > 0) ? values[0] : null);
            }
        }
        return result;
    }

    /**
     * 验签（demo 简化：直接返回 true）
     */
    public static boolean isSignEquals(Map<String, Object> paramMap) {
        return true;
    }
}
