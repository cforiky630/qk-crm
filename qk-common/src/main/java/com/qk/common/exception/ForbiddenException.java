package com.qk.common.exception;

/**
 * 无权限访问
 * <p>
 * 与 {@link BusinessException} 同属"预期内的失败"（提示语来自 {@link ErrorCode}），
 * 区别只在对外表现：业务失败返回 HTTP 200 + {@code code = 0}，
 * 而无权限返回 **HTTP 403** + {@code code = 0}，与 401（未登录）配套，
 * 便于前端区分"要重新登录"和"登录了但没这个权限"。
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
