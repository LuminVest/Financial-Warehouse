package com.wwfinance.api.utils;

import com.wwfinance.common.utils.MD5;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
     * 验签（旺旺银行托管平台真实验签）：
     * 用 getSign 对接收参数重新计算签名，与请求携带的 sign 对比。
     */
    public static boolean isSignEquals(Map<String, Object> paramMap) {
        if (paramMap == null || paramMap.get("sign") == null) {
            return false;
        }
        String sign = String.valueOf(paramMap.get("sign"));
        String calcSign = getSign(paramMap);
        return sign.equals(calcSign);
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long getTimestamp() {
        return new Date().getTime();
    }

    /**
     * 生成签名（旺旺银行托管平台签名规则，对齐老师源码）：
     * 1. 参与签名的参数排除 sign 字段本身
     * 2. 用 TreeMap 按 key 字典序升序排列
     * 3. 按 value1|value2|... 用 | 拼接（只用 value，不用 key；不跳过 null，null 拼成 "null"）
     * 4. 末尾拼接签名密钥 HfbConst.SIGN_KEY
     * 5. 整串做 MD5（32 位小写）
     */
    public static String getSign(Map<String, Object> paramMap) {
        Map<String, Object> params = new HashMap<>();
        if (paramMap != null) {
            params.putAll(paramMap);
        }
        // 排除 sign 字段本身
        params.remove("sign");
        // TreeMap 按 key 字典序排序
        TreeMap<String, Object> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> param : sorted.entrySet()) {
            sb.append(param.getValue()).append("|");
        }
        // 末尾拼接签名密钥
        sb.append(HfbConst.SIGN_KEY);
        // 固定 UTF-8 编码做 MD5，与银行新 jar（ww_bank-1.0.0）一致，保证中文参数签名匹配
        return MD5.md5Encrypt(sb.toString());
    }
}
