package com.example.library.config;

import com.example.library.interceptor.LoginInterceptor;
import com.example.library.interceptor.AdminInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器：除登录、注册、静态资源外的所有请求需要登录
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/", "/login", "/register", "/doLogin", "/doRegister",
                        "/css/**", "/js/**", "/images/**", "/fonts/**",
                        "/upload/**",  // 上传的图书封面图片
                        "/error", "/favicon.ico",
                        "/ai/**"  // AI接口（Controller内部自行校验登录状态）
                );

        // 管理员拦截器：/admin/** 路径需要管理员权限
        // 注意：/user/** 不在此拦截 — 用户个人中心（profile/updateProfile/changePassword）
        //       是所有登录用户均可访问的；用户管理功能在 /admin/** 下由本拦截器保护
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**", "/book/add", "/book/edit/**",
                        "/book/delete/**", "/book/doAdd", "/book/doEdit");
    }

    /**
     * 静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:./upload/");
    }
}
