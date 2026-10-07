package com.qk.config;

import com.qk.interceptor.LoginInterceptor;
import com.qk.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册登录校验拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor, PermissionInterceptor permissionInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 顺序即执行顺序：先认证（你是谁），再授权（你能不能调这个接口）
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                // /login 不能要求携带令牌；/error 是 Spring 的错误转发入口，
                // 放它进拦截器会把真正的错误盖成 401/403
                .excludePathPatterns("/login", "/error");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/error");
    }
}
