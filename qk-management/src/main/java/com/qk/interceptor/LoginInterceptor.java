package com.qk.interceptor;

import com.qk.common.util.UserHolder;
import com.qk.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 登录校验拦截器
 * <p>
 * 只做协议适配：取请求头 → 交给 {@link AuthService} 校验 → 把用户ID放进 ThreadLocal。
 * 「令牌怎么校验、账号还能不能用」属于认证规则，不在 Web 层实现，
 * 因此这里既不依赖 JWT 工具，也不依赖任何 Mapper。
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("token");

        if (!StringUtils.hasLength(token)) {
            log.info("未携带令牌，拒绝访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 签名、有效期、账号是否存在与启用，全部由认证服务判断
        Optional<Long> userId = authService.authenticate(token);
        if (userId.isEmpty()) {
            log.info("令牌无效或账号不可用，拒绝访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        UserHolder.setCurrentUser(userId.get());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束一定要清理，避免线程池复用导致的用户串号
        UserHolder.removeCurrentUser();
    }
}
