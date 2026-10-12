package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.common.util.JwtUtil;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限隔离的接口级验证（走真实 MVC 链路 + 真实拦截器）
 * <p>
 * 这里只验证三件事，都是「出问题会造成真实损失」的：
 * <ol>
 *   <li>非管理员做管理类操作被拒（否则等于没有隔离）；</li>
 *   <li>业务动作不被误拦（上次的教训：商机专员新增线索被拦死）；</li>
 *   <li>自建角色不被拦（拿不准就放行的承诺）。</li>
 * </ol>
 * 账号由测试自己插入，只借用种子数据里的三个内置角色（与其它测试一致，reset_and_seed.sql 保证存在）。
 */
@SpringBootTest
@Transactional
class PermissionIsolationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void adminCanManageUsers() throws Exception {
        mockMvc.perform(post("/users")
                        .header("token", tokenFor(userWithBuiltinRole("admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        mockMvc.perform(post("/users")
                        .header("token", tokenFor(userWithBuiltinRole("business_operator")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足，无法执行此操作"));
    }

    @Test
    void assignmentIsAdminOnly() throws Exception {
        mockMvc.perform(put("/clues/assign/{clueId}/{userId}", 1L, 2L)
                        .header("token", tokenFor(userWithBuiltinRole("clue_operator"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足，无法执行此操作"));
    }

    /**
     * 回归守卫：上次「商机专员新增线索」被权限检查拦住，业务动作必须保持对所有登录角色放行
     */
    @Test
    void businessActionIsNotBlockedForNonAdmin() throws Exception {
        mockMvc.perform(post("/clues")
                        .header("token", tokenFor(userWithBuiltinRole("business_operator")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone() + "\",\"channel\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    /** 用户自建的角色不在内置三个里，按「拿不准就放行」处理 */
    @Test
    void customRoleIsNotBlocked() throws Exception {
        mockMvc.perform(post("/users")
                        .header("token", tokenFor(userWithCustomRole()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    /** 没配角色的账号同样按身份不明处理，放行而不是拦死 */
    @Test
    void userWithoutRoleIsNotBlocked() throws Exception {
        mockMvc.perform(post("/users")
                        .header("token", tokenFor(insertUser(null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    private String tokenFor(Long userId) {
        return jwtUtil.generateToken(Map.of("id", userId, "username", "perm_ut_" + userId));
    }

    /** 落一个使用内置角色的账号（角色按 label 从种子数据里取，测试不硬编码角色ID） */
    private Long userWithBuiltinRole(String roleLabel) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getLabel, roleLabel));
        assertNotNull(role, "种子数据缺少内置角色: " + roleLabel);
        return insertUser(role.getId());
    }

    /** 落一个自建角色的账号，用于验证「非内置角色一律放行」 */
    private Long userWithCustomRole() {
        Role role = new Role();
        role.setName("自定义角色" + SEQ.incrementAndGet());
        role.setLabel("custom_role_" + SEQ.incrementAndGet());
        roleMapper.insert(role);
        return insertUser(role.getId());
    }

    private Long insertUser(Long roleId) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername("perm_user_" + seq);
        user.setName("权限测试" + seq);
        user.setPhone("177" + String.format("%08d", seq));
        user.setEmail("perm_user_" + seq + "@qk.test");
        user.setPassword(DigestUtil.md5Hex(user.getUsername() + "123"));
        user.setGender(1);
        user.setStatus(1);
        user.setDeptId(1L);
        user.setRoleId(roleId);
        userMapper.insert(user);
        return user.getId();
    }

    /** 合法的用户新增报文（字段唯一，避免撞唯一索引） */
    private static String userJson() {
        int seq = SEQ.incrementAndGet();
        return """
                {"username":"perm_new_%d","name":"新增测试","phone":"%s","email":"perm_new_%d@qk.test","gender":1,"status":1}
                """.formatted(seq, phone(), seq);
    }

    private static String phone() {
        return "166" + String.format("%08d", SEQ.incrementAndGet());
    }
}
