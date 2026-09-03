package com.wwfinance.api.utils;

import com.wwfinance.common.utils.MD5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
     * 验签（demo 简化：直接返回 true；接入托管平台后按平台签名规则实现真实验签）
     */
    public static boolean isSignEquals(Map<String, Object> paramMap) {
        return true;
    }

    /**
     * 获取当前时间戳（毫秒字符串）
     */
    public static String getTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 生成签名（旺旺银行托管平台签名规则）：
     * 1. 参与签名的参数排除 sign 字段本身
     * 2. 参数按 key 字典序升序排列
     * 3. 按 value1|value2|... 用 | 拼接（只用 value，不用 key）
     * 4. 末尾拼接一个 |，再拼接签名密钥 9876543210
     * 5. 整串做 MD5（32 位小写）
     */
    public static String getSign(Map<String, Object> paramMap) {
        String signKey = "9876543210";
        Map<String, Object> params = new HashMap<>();
        if (paramMap != null) {
            params.putAll(paramMap);
        }
        // 排除 sign 字段本身
        params.remove("sign");
        // key 字典序升序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            Object val = params.get(key);
            if (val != null) {
                sb.append(val).append("|");
            }
        }
        sb.append(signKey);
        return MD5.encrypt(sb.toString());
    }
}
