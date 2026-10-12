package com.qk.common.util;

/**
 * 当前登录用户工具类
 * <p>
 * 拦截器校验令牌后把「当前用户身份」（用户ID + 角色标识）放入 ThreadLocal：
 * Service 层需要「当前登录用户」时取用户ID，权限判断需要「当前角色」时取角色标识。
 * 请求结束（afterCompletion）必须调用 removeCurrentUser 清理，避免线程池复用造成用户串号。
 * <p>
 * 用户ID与角色放在同一个值对象里，而不是两个 ThreadLocal：两者一定同生同灭，
 * 分开存会出现「ID 换了、角色还是上一个请求的」这种串号。
 */
public class UserHolder {

    /** 当前登录用户身份 */
    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserHolder() {
    }

    /** 设置当前登录用户身份 */
    public static void setCurrentUser(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    /**
     * 只知道用户ID、拿不到角色时使用（例如单元测试直接调用 Service）
     * <p>
     * 角色为 {@code null} 表示身份不明，权限判断会放行而不是拦截。
     */
    public static void setCurrentUser(Long userId) {
        setCurrentUser(new CurrentUser(userId, null));
    }

    /**
     * 当前登录用户ID
     *
     * @return 用户ID；未登录时返回 {@code null}
     */
    public static Long getCurrentUserId() {
        CurrentUser currentUser = CURRENT_USER.get();
        return currentUser == null ? null : currentUser.id();
    }

    /**
     * 当前登录用户的角色标识
     *
     * @return 角色标识（admin / clue_operator / business_operator 或用户自建的 label）；
     *         未登录、用户没配角色时返回 {@code null}
     */
    public static String getCurrentRoleLabel() {
        CurrentUser currentUser = CURRENT_USER.get();
        return currentUser == null ? null : currentUser.roleLabel();
    }

    public static void removeCurrentUser() {
        CURRENT_USER.remove();
    }
}
