package com.wwfinance.api.utils;

import java.util.Map;

/**
 * 表单构建工具：把请求参数拼装成可自动提交的 HTML 表单，
 * 用于模拟浏览器 POST 跳转到第三方托管平台。
 */
public class FormHelper {

    /**
     * 构建自动提交表单
     *
     * @param url      目标地址
     * @param paramMap 表单参数
     * @return HTML 表单字符串
     */
    public static String buildForm(String url, Map<String, Object> paramMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>")
          .append("<form id='autoForm' action='").append(url).append("' method='post'>");
        if (paramMap != null) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                sb.append("<input type='hidden' name='").append(entry.getKey())
                  .append("' value='").append(entry.getValue() == null ? "" : entry.getValue()).append("'/>");
            }
        }
        sb.append("</form>")
          .append("<script>document.getElementById('autoForm').submit();</script>")
          .append("</body></html>");
        return sb.toString();
    }
}
