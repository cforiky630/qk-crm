package com.qk.domain;

import com.qk.common.util.UserHolder;
import com.qk.entity.enums.BuiltinRole;

/**
 * 数据可见范围（按归属人隔离）
 * <p>
 * 接口权限回答「能不能调这个接口」，这里回答「能看见哪些行」。规则只有一条：
 * <b>非管理员只能看见「归给自己的」和「还没有归属人的」数据，看不见别人名下的</b>。
 * 也就是「隐藏别人的」，而不是「只显示自己的」——尚无归属人的数据（例如待分配的线索、
 * 还没转成客户的记录）对所有登录用户可见，这样不会出现「刚新增的东西自己就看不见了」。
 * <p>
 * 判定顺序（拿不准就不过滤）：
 * <ol>
 *   <li>没有身份（定时任务、测试未设置上下文）→ 不限制；</li>
 *   <li>身份不明或用户自建的角色 → 不限制；</li>
 *   <li>管理员 → 不限制；</li>
 *   <li>内置的两个专员角色 → 只看归给自己的（外加未分配的）。</li>
 * </ol>
 * <p>
 * 过滤条件最终由 Mapper 的 XML 拼进 SQL（{@code user_id IS NULL OR user_id = ?}），
 * 因此隐藏的是一条记录的存在性：详情接口查不到时与「不存在」表现一致，不泄露别人的数据是否存在。
 */
public final class DataScope {

    private DataScope() {
    }

    /**
     * 当前请求的数据归属过滤值
     *
     * @return {@code null} 表示不限制（看全部）；否则表示「只看归给该用户ID名下的数据」（未分配的仍可见）
     */
    public static Long currentOwnerFilter() {
        return ownerFilterFor(UserHolder.getCurrentRoleLabel(), UserHolder.getCurrentUserId());
    }

    /**
     * 纯函数版本：不依赖 Web 上下文，便于直接验证规则
     *
     * @param roleLabel     当前角色标识，可为 null
     * @param currentUserId 当前用户ID，可为 null
     * @return 归属人过滤值；{@code null} 表示不限制
     */
    public static Long ownerFilterFor(String roleLabel, Long currentUserId) {
        if (currentUserId == null) {
            // 没有身份：定时任务、批处理、测试未设置上下文——不限制
            return null;
        }
        if (!BuiltinRole.isBuiltin(roleLabel)) {
            // 身份不明或用户自建的角色——不限制
            return null;
        }
        if (BuiltinRole.ADMIN.getValue().equals(roleLabel)) {
            // 管理员看全部
            return null;
        }
        // 内置专员：只看归给自己的（未分配的仍可见，由 SQL 里的 IS NULL 分支保证）
        return currentUserId;
    }
}
