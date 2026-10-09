package com.wwfinance.oss.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OSS 静态资源映射：把上传目录映射为 http://localhost:8130/uploads/** 可访问，
 * 使前端上传后能用 http URL 展示图片。目录与 FileController.uploadFolder 一致。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 上传存储目录（与 FileController 同一配置项） */
    @Value("${oss.upload-dir:D:/uploads/}")
    private String uploadFolder;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadFolder);
    }
}
