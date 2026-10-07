package com.qk.interceptor;

import com.qk.common.context.CurrentUser;
import com.qk.common.exception.ErrorCode;
import com.qk.common.exception.ForbiddenException;
import com.qk.common.util.UserHolder;
import com.qk.entity.enums.Permission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口授权拦截器（权限点模型）
 * <p>
 * 在 {@link LoginInterceptor} 之后执行：登录拦截器负责「你是谁」，这里负责「你有没有这个能力」。
 * 判断依据是方法上的 {@link RequirePermission} 与当前用户的权限集合——
 * 权限来自角色授权（超级管理员角色天然拥有全部权限），每次请求重新取，改授权立即生效。
 * <p>
 * <b>默认拒绝</b>：控制器方法没有标注 {@link RequirePermission} 时一律拒绝，而不是放行。
 * 无需登录的接口（如 {@code /login}）由 {@code WebConfig} 显式排除；
 * 这样"新增接口忘了声明权限"的表现是所有人调不通、且在构建期被覆盖率测试拦下，
 * 不会悄悄对所有登录用户开放。
 * <p>
 * 不满足时抛 {@link ForbiddenException}，由 {@code GlobalExceptionHandler} 按业务失败统一渲染成
 * HTTP 200 + {@code {code: 0, msg}}（理由见 {@link ForbiddenException}：前端是已构建产物、不能改，
 * 它只在 401 时登出，其它状态码不会读响应体里的 {@code msg}）。
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

        RequirePermission required = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            // 默认拒绝：漏标注属于配置错误，按"没权限"处理并在日志里点明原因
            log.error("接口未声明权限，已拒绝访问: {} {}（请在方法上标注 @RequirePermission）",
                    request.getMethod(), request.getRequestURI());
            throw new ForbiddenException(ErrorCode.PERMISSION_NOT_DECLARED);
        }

        CurrentUser currentUser = UserHolder.getCurrentUser();
        if (currentUser == null) {
            // 正常流程下登录拦截器已经拒绝过，这里是防御性判断
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        for (Permission permission : required.value()) {
            if (!currentUser.has(permission.getCode())) {
                log.warn("缺少权限，拒绝访问: {} {}，需要 {}，当前权限 {}",
                        request.getMethod(), request.getRequestURI(),
                        permission.getCode(), currentUser.permissions());
                throw new ForbiddenException(ErrorCode.FORBIDDEN);
            }
        }
        return true;
    }
}
