package com.qk.interceptor;

import com.qk.common.util.JwtUtil;
import com.qk.common.util.UserHolder;
import com.qk.entity.enums.EnableStatus;
import com.qk.entity.po.User;
import com.qk.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public LoginInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("token");

        if (!StringUtils.hasLength(token) || !jwtUtil.verify(token)) {
            log.info("令牌为空或非法，拒绝访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        // 令牌通过签名校验不代表账号仍然可用：账号被停用或删除后，已签发的令牌在有效期内
        // 依然能通过签名与过期校验。这里按主键查一次状态（走主键索引，代价很小）把这种令牌拒掉。
        if (!isActiveAccount(userId)) {
            log.info("令牌对应的账号不存在或已停用，拒绝访问: {} {}，用户ID={}",
                    request.getMethod(), request.getRequestURI(), userId);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        UserHolder.setCurrentUser(userId);
        return true;
    }

    /** 账号必须存在且处于正常状态（status = 1）；逻辑删除的账号由 @TableLogic 自动排除 */
    private boolean isActiveAccount(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && !EnableStatus.DISABLED.getCode().equals(user.getStatus());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束一定要清理，避免线程池复用导致的用户串号
        UserHolder.removeCurrentUser();
    }
}
