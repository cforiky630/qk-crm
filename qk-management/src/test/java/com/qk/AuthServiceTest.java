package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.jwt.JWT;
import com.qk.common.context.CurrentUser;
import com.qk.common.properties.JwtProperties;
import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.Permission;
import com.qk.entity.po.Role;
import com.qk.entity.po.RolePermission;
import com.qk.entity.po.User;
import com.qk.entity.vo.LoginResultVO;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.RolePermissionMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 认证与授权结果守护测试
 * <p>
 * 认证返回值里同时包含「账号ID + 角色标识 + 权限集合」：
 * <ul>
 *   <li>角色标识只用于展示 —— 角色的名称/标识是数据，改它不影响权限；</li>
 *   <li>权限集合才是接口授权的判断依据，来自角色授权表；</li>
 *   <li>超级管理员角色天然拥有全部权限，不查授权表（新增权限点不需要补数据）。</li>
 * </ul>
 * 账号与角色都由测试自建，不依赖种子数据。
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
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    private User activeUser;
    private User disabledUser;
    private String roleLabel;
    private List<String> grantedPermissions;

    @BeforeEach
    void setUp() {
        roleLabel = "auth_ut_role_" + SEQ.incrementAndGet();
        Long roleId = insertRole(roleLabel, false);
        grantedPermissions = List.of(Permission.CLUE_READ.getCode(), Permission.CLUE_TRACK.getCode());
        grantedPermissions.forEach(code -> grant(roleId, code));

        activeUser = insertUser(1, roleId);
        disabledUser = insertUser(0, roleId);
    }

    @Test
    void loginIssuesTokenAndReturnsPermissions() {
        LoginResultVO result = authService.login(activeUser.getUsername(), "123");

        assertNotNull(result, "密码正确时应登录成功");
        assertEquals(activeUser.getId(), result.getId());
        assertEquals(activeUser.getUsername(), result.getUsername());
        assertEquals(roleLabel, result.getRoleLabel(), "角色标识用于展示");
        assertEquals(grantedPermissions, result.getPermissions(), "前端据权限集合控制菜单与按钮");
        assertNotNull(result.getToken());
        assertEquals(activeUser.getId(), jwtUtil.getUserId(result.getToken()), "令牌里应带账号ID");
    }

    @Test
    void loginRejectsWrongPasswordUnknownAccountAndDisabledAccount() {
        assertNull(authService.login(activeUser.getUsername(), "wrong"), "密码错误必须失败");
        assertNull(authService.login("auth_not_exists_" + SEQ.incrementAndGet(), "123"), "账号不存在必须失败");
        assertNull(authService.login(disabledUser.getUsername(), "123"), "停用账号必须失败");
        assertNull(authService.login(null, null), "空凭据必须失败");
    }

    @Test
    void authenticateReturnsAccountWithItsGrantedPermissions() {
        CurrentUser currentUser = authService.authenticate(tokenFor(activeUser)).orElseThrow();

        assertEquals(activeUser.getId(), currentUser.id());
        assertEquals(roleLabel, currentUser.roleLabel(), "角色标识仅供展示");
        assertEquals(Set.copyOf(grantedPermissions), currentUser.permissions(), "授权判断靠权限集合");
        assertTrue(currentUser.has(Permission.CLUE_TRACK.getCode()));
        assertTrue(!currentUser.has(Permission.USER_DELETE.getCode()), "没授予的权限不能凭空拥有");
    }

    @Test
    void superRoleGetsAllPermissionsWithoutGrantRows() {
        Long superRoleId = insertRole("auth_ut_super_" + SEQ.incrementAndGet(), true);
        User superUser = insertUser(1, superRoleId);

        CurrentUser currentUser = authService.authenticate(tokenFor(superUser)).orElseThrow();

        assertEquals(Set.copyOf(Permission.allCodes()), currentUser.permissions(),
                "超级管理员天然拥有全部权限，不依赖 role_permission 表");
    }

    @Test
    void accountWithoutRoleOrWithoutGrantsGetsNoPermission() {
        User noRole = insertUser(1, null);
        assertTrue(authService.authenticate(tokenFor(noRole)).orElseThrow().permissions().isEmpty(),
                "未绑定角色时没有任何权限");

        Long emptyRoleId = insertRole("auth_ut_empty_" + SEQ.incrementAndGet(), false);
        User noGrant = insertUser(1, emptyRoleId);
        assertTrue(authService.authenticate(tokenFor(noGrant)).orElseThrow().permissions().isEmpty(),
                "角色没有授权时没有任何权限（默认拒绝）");
    }

    @Test
    void authenticateRejectsUnknownDisabledTamperedAndExpiredToken() {
        String ghost = jwtUtil.generateToken(Map.of("id", 99999999L, "username", "ghost"));
        assertTrue(authService.authenticate(ghost).isEmpty(), "账号不存在时令牌必须失效");
        assertTrue(authService.authenticate(tokenFor(disabledUser)).isEmpty(), "账号停用时令牌必须失效");

        String token = tokenFor(activeUser);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");
        assertTrue(authService.authenticate(tampered).isEmpty(), "签名被改动必须失效");
        assertTrue(authService.authenticate("not-a-jwt").isEmpty(), "非法格式必须失效");
        assertTrue(authService.authenticate("").isEmpty(), "空令牌必须失效");
        assertTrue(authService.authenticate(null).isEmpty(), "缺令牌必须失效");

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

    private Long insertRole(String label, boolean superRole) {
        Role role = new Role();
        role.setName("认证测试角色" + SEQ.incrementAndGet());
        role.setLabel(label);
        role.setRemark("单元测试数据");
        role.setSuperRole(superRole);
        roleMapper.insert(role);
        return role.getId();
    }

    private void grant(Long roleId, String permissionCode) {
        RolePermission grant = new RolePermission();
        grant.setRoleId(roleId);
        grant.setPermission(permissionCode);
        rolePermissionMapper.insert(grant);
    }

    /** 直接落库一个账号，密码摘要按生产规则计算（md5(用户名 + 明文密码)） */
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
