package com.qk.common.util;

/**
 * 当前登录用户工具类
 * <p>
 * 拦截器校验令牌后把用户ID放入 ThreadLocal，Service 层需要「当前登录用户」时从这里取，
 * 请求结束（afterCompletion）必须调用 removeCurrentUser 清理，避免线程复用造成数据串号。
 */
public class UserHolder {

    /** 当前登录用户ID */
    private static final ThreadLocal<Integer> CURRENT_USER = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void setCurrentUser(Integer userId) {
        CURRENT_USER.set(userId);
    }

    public static Integer getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void removeCurrentUser() {
        CURRENT_USER.remove();
    }
}
