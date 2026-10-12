package com.iot.qa.config;

import com.iot.qa.common.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：注册登录与权限拦截器。
 * 静态资源由 Spring Boot 默认的 classpath:/static/ 映射处理，无需额外声明。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/admin", "/admin/**", "/chat", "/history")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/admin/login",
                        "/api/auth/register",
                        "/api/auth/logout",
                        "/api/public/**",
                        "/admin/login",
                        "/css/**", "/js/**", "/img/**", "/lib/**", "/error"
                );
    }
}