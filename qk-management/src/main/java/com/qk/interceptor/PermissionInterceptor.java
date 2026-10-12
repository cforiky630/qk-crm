package com.qk.interceptor;

import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.util.UserHolder;
import com.qk.domain.ApiPermissions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限校验拦截器
 * <p>
 * 与 {@link LoginInterceptor} 分工明确：登录拦截器回答「你是谁」（把身份放进 ThreadLocal），
 * 这里回答「你能不能调这个接口」（读 ThreadLocal + 规则表）。
 * <p>
 * 判定规则完全在 {@link ApiPermissions} 里，本类只做两件事：读当前角色、命中拒绝时报错。
 * 拒绝以业务异常抛出（HTTP 200 + {@code code = 0} + 「权限不足，无法执行此操作」），
 * 与其它业务失败一致，前端按既有约定展示 msg 即可。
 * <p>
 * 注册顺序必须在 {@code LoginInterceptor} 之后：先认证、后有身份可判。
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String roleLabel = UserHolder.getCurrentRoleLabel();
        String method = request.getMethod();
        String path = pathWithinApplication(request);

        if (ApiPermissions.isAllowed(roleLabel, method, path)) {
            return true;
        }

        log.warn("权限不足，拒绝访问: roleLabel={}, {} {}", roleLabel, method, path);
        throw new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    /**
     * 取应用内路径
     * <p>
     * 规则表里写的是 {@code /users} 这类应用内路径；当前没有配 context-path，
     * 但顺手剥掉可以让将来加 context-path 时规则表不用跟着改。
     */
    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
