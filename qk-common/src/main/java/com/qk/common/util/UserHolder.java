package com.qk.common.util;

import com.qk.common.context.CurrentUser;

/**
 * 当前登录用户工具类
 * <p>
 * 拦截器校验令牌后把 {@link CurrentUser} 放进 ThreadLocal，Service 层需要「当前登录用户」时从这里取，
 * 请求结束（afterCompletion）必须调用 {@link #removeCurrentUser()} 清理，
 * 避免线程复用造成数据串号 —— 用户与权限串号都会导致越权。
 */
public class UserHolder {

    /** 当前登录用户 */
    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void setCurrentUser(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    /** @return 当前登录用户；未登录时为 null */
    public static CurrentUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    /** @return 当前登录用户ID；未登录时为 null */
    public static Long getCurrentUserId() {
        CurrentUser currentUser = CURRENT_USER.get();
        return currentUser == null ? null : currentUser.id();
    }

    public static void removeCurrentUser() {
        CURRENT_USER.remove();
    }
}
