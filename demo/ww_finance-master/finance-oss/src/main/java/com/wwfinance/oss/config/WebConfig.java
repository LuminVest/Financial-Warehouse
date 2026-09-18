package com.wwfinance.oss.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OSS 静态资源映射：把本地 D:/uploads 目录映射为 http://localhost:8130/uploads/** 可访问，
 * 使前端上传后能用 http URL 展示图片。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地演示目录，与 FileController.UPLOADED_FOLDER 保持一致
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/uploads/");
    }
}
