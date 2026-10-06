package com.qk.common.util;

/**
 * 当前登录用户工具类
 * <p>
 * 拦截器校验令牌后把当前用户放进 ThreadLocal，Service 层需要「当前登录用户」时从这里取，
 * 请求结束（afterCompletion）必须调用 {@link #removeCurrentUser()} 清理，
 * 避免线程复用造成数据串号 —— 用户串号与角色串号都会导致越权，两者一起清理。
 * <p>
 * 这里同时保存用户ID与角色标识：ID 供业务写「操作人 / 归属人」，角色供接口授权判断。
 * 角色取自每次请求的账号查询（而不是令牌载荷），因此给账号换角色会**立即**生效，
 * 不用等令牌过期。
 */
public class UserHolder {

    /** 当前登录用户ID */
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    /** 当前登录用户的角色标识，未绑定角色时为 null */
    private static final ThreadLocal<String> CURRENT_ROLE_LABEL = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void setCurrentUser(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void setCurrentRoleLabel(String roleLabel) {
        CURRENT_ROLE_LABEL.set(roleLabel);
    }

    /** @return 当前登录用户的角色标识；未登录或未绑定角色时为 null */
    public static String getCurrentRoleLabel() {
        return CURRENT_ROLE_LABEL.get();
    }

    public static void removeCurrentUser() {
        CURRENT_USER.remove();
        CURRENT_ROLE_LABEL.remove();
    }
}
