package com.qk.interceptor;

import com.qk.common.exception.ErrorCode;
import com.qk.common.exception.ForbiddenException;
import com.qk.common.util.UserHolder;
import com.qk.entity.enums.RoleLabel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 接口授权拦截器
 * <p>
 * 在 {@link LoginInterceptor} 之后执行：登录拦截器负责「你是谁」，这里负责「你能不能调这个接口」。
 * 判断依据是方法（或控制器）上的 {@link RequireRole} 与当前登录用户的角色标识——
 * 角色来自每次请求的账号查询，因此改角色立即生效。
 * <p>
 * 不满足时抛 {@link ForbiddenException}，交给 {@code GlobalExceptionHandler} 统一渲染成
 * HTTP 403 + {@code {code: 0, msg}}：与 401（未登录）配套，前端可以区分"重新登录"和"没权限"。
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 静态资源等非控制器请求不参与接口授权
            return true;
        }

        RequireRole required = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (required == null) {
            // 未标注 = 对所有已登录用户开放
            return true;
        }

        String currentRole = UserHolder.getCurrentRoleLabel();
        for (RoleLabel allowed : required.value()) {
            if (allowed.getLabel().equals(currentRole)) {
                return true;
            }
        }

        log.warn("角色不满足接口要求: {} {}，需要 {}，当前 {}",
                request.getMethod(), request.getRequestURI(), Arrays.toString(required.value()), currentRole);
        throw new ForbiddenException(ErrorCode.FORBIDDEN);
    }
}
