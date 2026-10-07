package com.qk.common.exception;

/**
 * 无权限访问
 * <p>
 * 与 {@link BusinessException} 同属"预期内的失败"（提示语来自 {@link ErrorCode}），
 * 对外表现也一致：**HTTP 200 + {@code code = 0}**，前端直接展示 {@code msg}。
 * <p>
 * 为什么不用 HTTP 403：改造前的前端是已构建产物（无源码、不能改），它只在 401 时登出，
 * 其它非 2xx 状态码直接弹 {@code Request failed with status code 403}，不会读响应体里的 {@code msg}。
 * 无权限属于"业务级失败"，用 200 + {@code code = 0} 既符合项目既有约定，
 * 也能让冻结的前端显示「无权访问该接口，请联系管理员分配权限」。
 * 未登录仍然是 401 + 空响应体（前端据此跳登录页），两者不冲突。
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
