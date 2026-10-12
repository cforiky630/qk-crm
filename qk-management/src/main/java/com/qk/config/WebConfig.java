package com.qk.config;

import com.qk.interceptor.LoginInterceptor;
import com.qk.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册登录校验与权限校验拦截器
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
        // 顺序即执行顺序：先认证（把身份放进 ThreadLocal），再授权（读身份判接口）
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                // 登录接口本身不能要求携带令牌，否则永远登录不上
                .excludePathPatterns("/login");

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                // /login 没有身份可判，与登录拦截器保持一致地排除
                .excludePathPatterns("/login");
    }
}
