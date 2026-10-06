package com.qk.service;

import com.qk.entity.vo.LoginResultVO;

import java.util.Optional;

/**
 * 认证
 * <p>
 * 登录与令牌校验是同一个关注点的两端，因此放在一起：
 * 这里知道「什么样的凭据能换到令牌」，也知道「什么样的令牌仍然有效」。
 * 拦截器、登录接口都只依赖这个接口，不再各自持有 JWT 工具或用户 Mapper。
 */
public interface AuthService {

    /**
     * 令牌对应的账号与角色
     *
     * @param userId    账号ID
     * @param roleLabel 角色标识（数据库 role.label）；账号未绑定角色时为 null
     */
    record Principal(Long userId, String roleLabel) {
    }

    /**
     * 登录：校验账号密码并签发令牌
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录结果；账号不存在、密码错误或账号已停用时返回 null，
     *         由调用方统一转成「用户名或密码错误」（不区分具体原因，避免暴露账号是否存在）
     */
    LoginResultVO login(String username, String password);

    /**
     * 校验令牌并返回可用账号及其角色
     * <p>
     * 同时覆盖三件事：签名与有效期、账号是否存在、账号是否启用。
     * 任一不满足都返回空，调用方一律按 401 处理。
     *
     * @param token 请求头 {@code token} 的值
     * @return 账号与角色；令牌不合法或账号不可用时返回空
     */
    Optional<Principal> authenticate(String token);
}
