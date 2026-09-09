package com.wwfinance.api.interceptor;

import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.api.utils.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwfinance.common.result.PccAjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 统一 JWT 授权拦截器
 * 1. 从 Authorization 头取 token
 * 2. 解析 token 拿到手机号，写入 LoginUserContext
 * 3. 解析失败返回 401 JSON
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 兼容两种 token 携带方式：
        // 1. 管理后台前端（ww_finance_admin）：请求头 token（值含 5grcs 前缀）
        // 2. 前台前端：请求头 Authorization: 5grcs xxx
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("5grcs ")) {
                token = authHeader;
            }
        }
        // 没有 token 或格式不对
        if (token == null || token.isEmpty()) {
            return writeUnauthorized(response, "缺少认证token");
        }
        try {
            Map<String, String> info = TokenUtil.getMapInfoFromToken(token);
            String userid = info.get("token_userid");
            if (userid == null || userid.isEmpty()) {
                return writeUnauthorized(response, "token无效");
            }
            // 写入上下文，供 Controller/Service 使用
            LoginUserContext.setUserid(Integer.parseInt(userid));
            return true;
        } catch (Exception e) {
            log.warn("token解析失败: {}", e.getMessage());
            return writeUnauthorized(response, "token解析失败");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，避免线程复用导致的内存泄漏和串号
        LoginUserContext.clear();
    }

    private boolean writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(new PccAjaxResult(401, msg, null)));
        return false;
    }
}
