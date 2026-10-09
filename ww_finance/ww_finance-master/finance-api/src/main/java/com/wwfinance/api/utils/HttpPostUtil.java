package com.wwfinance.api.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 商户系统 → 旺旺银行 同步接口调用工具（表单提交，JDK8 兼容）
 */
public class HttpPostUtil {

    /**
     * 以 application/x-www-form-urlencoded 提交表单到银行接口，返回响应体字符串
     */
    public static String postForm(String url, Map<String, Object> params) {
        HttpURLConnection conn = null;
        try {
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                if (body.length() > 0) {
                    body.append('&');
                }
                body.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append('=')
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            }

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) {
                return "";
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                buf.write(buffer, 0, len);
            }
            return buf.toString("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("调用旺旺银行接口失败: " + url + ", " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
