package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.jwt.JWT;
import com.qk.common.properties.JwtProperties;
import com.qk.common.util.JwtUtil;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.entity.vo.LoginResultVO;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 认证策略守护测试
 * <p>
 * 登录与令牌校验收敛到 {@code AuthService} 之后，「什么样的凭据能换到令牌」「什么样的令牌仍然有效」
 * 可以在一处验证完整：签名、有效期、账号是否存在、账号是否启用。
 * 其中「过期令牌」与「停用账号的令牌」以前只靠代码阅读保证，现在有用例兜住。
 * <p>
 * 用到的账号由测试自己插入（密码摘要按生产规则计算），不依赖种子数据。
 */
@SpringBootTest
@Transactional
class AuthServiceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    private User activeUser;
    private User disabledUser;
    private String roleLabel;

    @BeforeEach
    void setUp() {
        // 自建角色，不依赖种子数据；标签唯一，顺便覆盖"认证结果带角色"这条链路
        roleLabel = "auth_ut_role_" + SEQ.incrementAndGet();
        Long roleId = insertRole(roleLabel);
        activeUser = insertUser(1, roleId);
        disabledUser = insertUser(0, roleId);
    }

    @Test
    void loginIssuesTokenForCorrectPassword() {
        LoginResultVO result = authService.login(activeUser.getUsername(), "123");

        assertNotNull(result, "密码正确时应登录成功");
        assertEquals(activeUser.getId(), result.getId());
        assertEquals(activeUser.getUsername(), result.getUsername());
        assertNotNull(result.getToken());
        assertEquals(activeUser.getId(), jwtUtil.getUserId(result.getToken()), "令牌里应带账号ID");
        assertEquals(roleLabel, result.getRoleLabel(), "登录结果应带角色标识");
    }

    @Test
    void loginRejectsWrongPasswordUnknownAccountAndDisabledAccount() {
        assertNull(authService.login(activeUser.getUsername(), "wrong"), "密码错误必须失败");
        assertNull(authService.login("auth_not_exists_" + SEQ.incrementAndGet(), "123"), "账号不存在必须失败");
        assertNull(authService.login(disabledUser.getUsername(), "123"), "停用账号必须失败");
        assertNull(authService.login(null, null), "空凭据必须失败");
    }

    @Test
    void authenticateAcceptsTokenOfActiveAccount() {
        AuthService.Principal principal = authService.authenticate(tokenFor(activeUser)).orElseThrow();

        assertEquals(activeUser.getId(), principal.userId());
        assertEquals(roleLabel, principal.roleLabel(), "角色随认证结果返回，接口授权靠它判断");
    }

    @Test
    void authenticateRejectsUnknownAndDisabledAccount() {
        String ghost = jwtUtil.generateToken(Map.of("id", 99999999L, "username", "ghost"));

        assertTrue(authService.authenticate(ghost).isEmpty(), "账号不存在时令牌必须失效");
        assertTrue(authService.authenticate(tokenFor(disabledUser)).isEmpty(), "账号停用时令牌必须失效");
    }

    @Test
    void authenticateRejectsTamperedMalformedAndBlankToken() {
        String token = tokenFor(activeUser);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertTrue(authService.authenticate(tampered).isEmpty(), "签名被改动必须失效");
        assertTrue(authService.authenticate("not-a-jwt").isEmpty(), "非法格式必须失效");
        assertTrue(authService.authenticate("").isEmpty(), "空令牌必须失效");
        assertTrue(authService.authenticate(null).isEmpty(), "缺令牌必须失效");
    }

    /**
     * 过期令牌必须失效（有效期是安全边界，不能只靠代码阅读保证）
     * <p>
     * 用同一个密钥手工签一个已过期的令牌，覆盖 {@code JwtUtil.verify} 里的有效期校验分支。
     */
    @Test
    void authenticateRejectsExpiredToken() {
        String expired = JWT.create()
                .setPayload("id", activeUser.getId())
                .setIssuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .setExpiresAt(Date.from(Instant.now().minusSeconds(3600)))
                .setKey(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8))
                .sign();

        assertTrue(authService.authenticate(expired).isEmpty(), "过期令牌必须失效");
    }

    private String tokenFor(User user) {
        return jwtUtil.generateToken(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    /** 直接落库一个账号，密码摘要按生产规则计算（md5(用户名 + 明文密码)） */
    private Long insertRole(String label) {
        Role role = new Role();
        role.setName("认证测试角色" + SEQ.get());
        role.setLabel(label);
        role.setRemark("单元测试数据");
        roleMapper.insert(role);
        return role.getId();
    }

    private User insertUser(int status, Long roleId) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername("auth_ut_" + seq);
        user.setName("认证测试" + seq);
        user.setPhone("168" + String.format("%08d", seq));
        user.setEmail("auth_ut_" + seq + "@qk.test");
        user.setPassword(DigestUtil.md5Hex(user.getUsername() + "123"));
        user.setGender(1);
        user.setStatus(status);
        user.setDeptId(1L);
        user.setRoleId(roleId);
        userMapper.insert(user);
        return user;
    }
}
