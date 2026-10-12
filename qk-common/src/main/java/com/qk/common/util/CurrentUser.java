package com.qk.common.util;

/**
 * 当前登录用户的身份
 * <p>
 * 一次请求里「我是谁」由它表达：用户ID用于数据归属，角色标识用于权限判断。
 * 令牌里只放用户ID、不放角色，角色在每次请求时按主键查一次库，因此改了角色立即生效。
 *
 * @param id        用户ID，关联 {@code user.id}
 * @param roleLabel 角色标识（{@code role.label}）；用户没配角色或角色被删时为 {@code null}，
 *                  此时权限判断按「身份不明」放行
 */
public record CurrentUser(Long id, String roleLabel) {
}
