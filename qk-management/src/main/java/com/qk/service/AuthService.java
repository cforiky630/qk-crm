package com.qk.service;

import com.qk.common.util.CurrentUser;
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
     * 登录：校验账号密码并签发令牌
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录结果；账号不存在、密码错误或账号已停用时返回 null，
     *         由调用方统一转成「用户名或密码错误」（不区分具体原因，避免暴露账号是否存在）
     */
    LoginResultVO login(String username, String password);

    /**
     * 校验令牌并返回可用账号的身份
     * <p>
     * 同时覆盖四件事：签名与有效期、账号是否存在、账号是否启用、账号当前的角色。
     * 任一不满足都返回空，调用方一律按 401 处理。
     * <p>
     * 角色在这里按主键现查，而不是塞进令牌：改了角色下一次请求就生效，
     * 代价是每请求多一次主键查询（与「账号停用立即失效」用的是同一种取舍）。
     *
     * @param token 请求头 {@code token} 的值
     * @return 账号身份（用户ID + 角色标识，角色取不到时为 null）；令牌不合法或账号不可用时返回空
     */
    Optional<CurrentUser> authenticate(String token);
}
