package com.wwfinance.api.config;

import com.wwfinance.api.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig  implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 依然可以配置 Swagger 资源
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        // 这时 Spring Boot 默认的静态资源路径依然有效，不需要额外配置
        // 补：springfox 原生页面（@EnableWebMvc 后默认映射失效，需手动补）
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                // 拦截所有业务接口
                .addPathPatterns("/api/**", "/admin/**")
                // 白名单：登录/注册/测试/swagger 资源
                .excludePathPatterns(
                        "/api/core/user/hello",                 // 测试接口
                        "/api/core/user/sendCode",              // 发送注册验证码（无需登录）
                        "/api/core/user/checkMobile/**",        // 校验手机号（sms 服务 Feign 调用，无需登录）
                        "/api/core/user/login",                 // 登录
                        "/api/core/user/register",              // 注册
                        "/api/core/userAccount/notify",         // 充值异步回调（第三方支付回调无需登录）
                        "/api/core/userBind/notify",            // 用户绑定异步回调（第三方托管平台回调无需登录）
                        "/api/core/lendItem/notify",            // 投资异步回调（第三方托管平台回调无需登录）
                        "/api/user/lendReturn/notifyUrl",       // 还款异步回调（第三方托管平台回调无需登录）
                        "/api/user/account/notifyWithdraw",     // 提现异步回调（第三方托管平台回调无需登录）
                        // 标的公开浏览（未登录可看首页/投资列表/详情/收益计算/投资记录）
                        "/api/core/lend/list",
                        "/api/core/lend/show/**",
                        "/api/core/lend/getInterestCount/**",
                        "/api/core/lendItem/list/**",
                        "/admin/core/user/login",               // 后台登录（预留）
                        "/admin/core/login",                    // 管理员登录（ww_finance_admin 前端）
                        "/admin/core/logout",                   // 退出登录（幂等，无需登录态）
                        "/admin/core/knowledge/doc/updateStatus",  // AI 服务回调更新文档状态（无需登录）
                        "/api/core/chat/save",                   // 用户端保存聊天记录（无需登录）
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
