package com.qk.common.context;

import java.util.Set;

/**
 * 当前登录用户（请求级）
 * <p>
 * 一次认证同时得到三样东西，打包在一起避免"设置了用户ID却忘了设置权限"这类不一致：
 *
 * @param id          账号ID，供业务写「操作人 / 归属人」
 * @param roleLabel   角色标识，仅用于展示（角色是可变数据）
 * @param permissions 权限码集合，接口授权判断用
 */
public record CurrentUser(Long id, String roleLabel, Set<String> permissions) {

    public CurrentUser {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    /** 是否拥有指定权限 */
    public boolean has(String permissionCode) {
        return permissionCode != null && permissions.contains(permissionCode);
    }
}
