package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.EnableStatus;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.entity.vo.LoginResultVO;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 认证实现
 * <p>
 * 从 {@code UserServiceImpl} 拆出：登录以前挂在「用户管理」上，导致用户服务同时依赖
 * JWT 工具与角色 Mapper，拦截器还要绕过服务直接查 Mapper 才能判断账号是否可用。
 * 现在凭据校验（login）与令牌校验（authenticate）只有这一个实现，
 * 账号停用、逻辑删除、令牌过期这些判断也只在这一个类里。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResultVO login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }

        // 1. 根据用户名查询用户（username 上有唯一索引；逻辑删除的账号由 @TableLogic 自动排除）
        User user = userMapper.findByUsername(username);
        if (user == null || EnableStatus.DISABLED.getCode().equals(user.getStatus())) {
            return null;
        }

        // 2. 校验密码：库中存放的是 md5(用户名 + 明文密码) 的摘要。
        //    这里只接受明文密码（依赖 HTTPS 传输），不接受「直接提交摘要」的方式，
        //    否则数据库里的摘要就等同于一个可复用的登录凭证（pass-the-hash）。
        if (!StrUtil.equals(user.getPassword(), DigestUtil.md5Hex(username + password))) {
            return null;
        }

        // 3. 查询角色标识（供前端渲染菜单；后端目前不做接口级鉴权）
        Role role = roleMapper.selectById(user.getRoleId());

        // 4. 组装登录结果并签发 JWT
        LoginResultVO vo = new LoginResultVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setImage(user.getImage());
        vo.setRoleLabel(role == null ? null : role.getLabel());

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("name", user.getName());
        vo.setToken(jwtUtil.generateToken(claims));
        return vo;
    }

    @Override
    public Optional<Principal> authenticate(String token) {
        // 签名与有效期由 JwtUtil 一并校验（内部把格式错误、签名不匹配、已过期都归为校验失败）
        if (!StringUtils.hasLength(token) || !jwtUtil.verify(token)) {
            log.debug("令牌为空、被篡改或已过期");
            return Optional.empty();
        }

        // 令牌通过签名校验不代表账号仍然可用：停用或删除账号后，已签发的令牌在有效期内
        // 依然能通过签名与过期校验，因此这里按主键查一次账号状态（走主键索引，代价很小）。
        Long userId = jwtUtil.getUserId(token);
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || EnableStatus.DISABLED.getCode().equals(user.getStatus())) {
            log.debug("令牌对应的账号不存在或已停用: userId={}", userId);
            return Optional.empty();
        }

        // 角色每次从库里取（不放进令牌）：给账号换角色要立即生效，与"停用立即失效"同一取舍。
        // 自定义角色只是数据，不具备任何接口授权，因此这里返回的 label 可能不在保留角色之列。
        Role role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        return Optional.of(new Principal(user.getId(), role == null ? null : role.getLabel()));
    }
}
