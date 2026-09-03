package com.wwfinance.api.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * 表单构建工具：把请求参数拼装成可自动提交的 HTML 表单，
 * 用于模拟浏览器 POST 跳转到第三方托管平台。
 *
 * 结构对齐老师源码（常量.zip / Bank/FormHelper.java）：value 直接拼接（null 拼成 "null"）。
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
        StringBuffer inputStr = new StringBuffer();
        if (paramMap != null) {
            Iterator<Map.Entry<String, Object>> entries = paramMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, Object> entry = entries.next();
                inputStr.append("<input type='hidden' name='").append(entry.getKey())
                        .append("' value='").append(entry.getValue()).append("'/>");
            }
        }
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">\n" +
                "<head>\n" +
                "</head>\n" +
                "<body>\n" +
                "<form name=\"form\" action=\"" + url + "\" method=\"post\">\n" +
                inputStr +
                "</form>\n" +
                "<script>\n" +
                "\tdocument.form.submit();\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }
}
