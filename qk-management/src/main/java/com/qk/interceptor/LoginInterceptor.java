package com.qk.interceptor;

import com.qk.common.util.JwtUtil;
import com.qk.common.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器
 * <p>
 * 请求头 token 携带 JWT 令牌，校验不通过直接响应 401。
 * 校验通过后把用户ID放进 ThreadLocal，供业务层获取「当前登录用户」。
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("token");

        if (!StringUtils.hasLength(token) || !jwtUtil.verify(token)) {
            log.info("令牌为空或非法，拒绝访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        UserHolder.setCurrentUser(jwtUtil.getUserId(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束一定要清理，避免线程池复用导致的用户串号
        UserHolder.removeCurrentUser();
    }
}
