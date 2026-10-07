package com.qk;

import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.enums.Permission;
import com.qk.entity.po.Clue;
import com.qk.entity.po.Role;
import com.qk.entity.po.RolePermission;
import com.qk.entity.po.User;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.RolePermissionMapper;
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

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口授权测试（权限点模型）
 * <p>
 * 模型要点：
 * <ul>
 *   <li>接口声明「需要哪些权限点」，账号能做什么由「角色被授予了哪些权限点」决定；</li>
 *   <li>角色标识（label）只是数据，改名不影响权限 —— 这是解耦前最容易踩的坑；</li>
 *   <li>权限点粒度到单个接口：有 {@code clue:track} 不等于有 {@code clue:assign}；</li>
 *   <li>超级管理员角色天然拥有全部权限、不可删除、不支持单独调整权限。</li>
 * </ul>
 * 无权限时按业务失败返回：**HTTP 200 + {@code code = 0} + 具体 msg**（前端是已构建产物、不能改，
 * 它只在 401 时登出，其它状态码不会读响应体）。
 * 角色、账号、授权全部由测试自建，不依赖种子数据。
 */
@SpringBootTest
@Transactional
class AuthorizationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final String FORBIDDEN_MSG = "无权访问该接口，请联系管理员分配权限";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClueMapper clueMapper;

    private Long superRoleId;
    private User superUser;

    @BeforeEach
    void setUp() {
        superRoleId = createRole(true);
        superUser = insertUser(superRoleId);
    }

    @Test
    void superRolePassesEveryEndpointWithoutGrantRows() throws Exception {
        MockMvc mvc = mvcFor(superUser);

        mvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"超级角色新建","label":"authz_created_%d"}
                                """.formatted(SEQ.incrementAndGet())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mvc.perform(get("/users")).andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/permissions")).andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void roleWithoutGrantsIsRejectedOnProtectedEndpoints() throws Exception {
        MockMvc mvc = mvcFor(insertUser(createRole(false)));

        mvc.perform(get("/clues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"username\":\"authz_denied\",\"name\":\"越权\",\"phone\":\"16900000000\",\"email\":\"authz_denied@qk.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));
    }

    @Test
    void accountWithoutRoleHasNoPermission() throws Exception {
        mvcFor(insertUser(null)).perform(get("/clues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));
    }

    /**
     * 授权与撤销都必须立即生效：这正是把授权放进数据表的目的
     */
    @Test
    void grantingAndRevokingTakesEffectImmediately() throws Exception {
        Long roleId = createRole(false);
        MockMvc mvc = mvcFor(insertUser(roleId));
        Clue clue = insertClue(ClueStatus.WAIT_FOLLOW.getCode());

        mvc.perform(trackClue(clue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));

        grantByApi(roleId, "\"clue:track\"");
        mvc.perform(trackClue(clue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        grantByApi(roleId);
        Clue another = insertClue(ClueStatus.WAIT_FOLLOW.getCode());
        mvc.perform(trackClue(another.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 改角色名/标识不影响权限
     * <p>
     * 解耦前授权依赖角色标识，改名会把权限一起改掉（甚至把管理员自己锁死）；
     * 现在授权只看 role_permission，改名只是改展示。
     */
    @Test
    void renamingRoleLabelDoesNotChangePermissions() throws Exception {
        Long roleId = createRole(false);
        grant(roleId, Permission.CLUE_TRACK);
        MockMvc mvc = mvcFor(insertUser(roleId));

        mvcFor(superUser).perform(put("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"name":"改过的名字","label":"authz_renamed_%d"}
                                """.formatted(roleId, SEQ.incrementAndGet())))
                .andExpect(jsonPath("$.code").value(1));

        Clue clue = insertClue(ClueStatus.WAIT_FOLLOW.getCode());
        mvc.perform(trackClue(clue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    /**
     * 权限粒度到接口：能跟进不等于能分配
     */
    @Test
    void permissionGranularityIsPerEndpoint() throws Exception {
        Long roleId = createRole(false);
        grant(roleId, Permission.CLUE_TRACK);
        User user = insertUser(roleId);
        Clue waitAllot = insertClue(ClueStatus.WAIT_ALLOT.getCode());

        mvcFor(user).perform(put("/clues/assign/{clueId}/{userId}", waitAllot.getId(), user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));
    }

    @Test
    void superRoleIsProtectedFromDeletionAndPermissionEditing() throws Exception {
        MockMvc mvc = mvcFor(superUser);

        mvc.perform(put("/roles/{id}/permissions", superRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"permissions\":[]}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("超级管理员角色天然拥有全部权限，不支持单独调整"));

        mvc.perform(delete("/roles/{id}", superRoleId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("超级管理员角色不可删除"));
    }

    @Test
    void unknownPermissionCodeIsRejected() throws Exception {
        Long roleId = createRole(false);

        mvcFor(superUser).perform(put("/roles/{id}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"permissions\":[\"clue:track\",\"not:a:permission\"]}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("存在无效的权限码：not:a:permission"));
    }

    @Test
    void rolePermissionsCanBeReadBackForTheAdminUi() throws Exception {
        Long roleId = createRole(false);
        grant(roleId, Permission.CLUE_READ, Permission.CLUE_TRACK);

        mvcFor(superUser).perform(get("/roles/{id}/permissions", roleId))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[*].code", hasItems("clue:read", "clue:track")))
                .andExpect(jsonPath("$.data[0].description").exists());
    }

    @Test
    void superRolePermissionsReadBackAsTheWholeCatalog() throws Exception {
        mvcFor(superUser).perform(get("/roles/{id}/permissions", superRoleId))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(Permission.values().length));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder trackClue(Long clueId) {
        return put("/clues")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content("{\"id\":%d,\"record\":\"授权测试跟进\"}".formatted(clueId));
    }

    /** 通过接口覆盖式配置角色权限（不传权限码表示全部收回） */
    private void grantByApi(Long roleId, String... permissionCodes) throws Exception {
        mvcFor(superUser).perform(put("/roles/{id}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"permissions\":[" + String.join(",", permissionCodes) + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    private MockMvc mvcFor(User user) {
        String token = jwtUtil.generateToken(Map.of("id", user.getId(), "username", user.getUsername()));
        return MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    private Long createRole(boolean superRole) {
        Role role = new Role();
        role.setName("授权测试角色" + SEQ.incrementAndGet());
        role.setLabel("authz_role_" + SEQ.incrementAndGet());
        role.setRemark("接口授权测试");
        role.setSuperRole(superRole);
        roleMapper.insert(role);
        return role.getId();
    }

    private void grant(Long roleId, Permission... permissions) {
        for (Permission permission : permissions) {
            RolePermission grant = new RolePermission();
            grant.setRoleId(roleId);
            grant.setPermission(permission.getCode());
            rolePermissionMapper.insert(grant);
        }
    }

    private User insertUser(Long roleId) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername("authz_u" + seq);
        user.setName("授权测试" + seq);
        user.setPhone("169" + String.format("%08d", seq));
        user.setEmail("authz_u" + seq + "@qk.test");
        user.setPassword("x");
        user.setGender(1);
        user.setStatus(1);
        user.setDeptId(1L);
        user.setRoleId(roleId);
        userMapper.insert(user);
        return user;
    }

    private Clue insertClue(Integer status) {
        int seq = SEQ.incrementAndGet();
        Clue clue = new Clue();
        clue.setPhone("170" + String.format("%08d", seq));
        clue.setChannel(1);
        clue.setName("授权测试线索" + seq);
        clue.setStatus(status);
        clueMapper.insert(clue);
        return clue;
    }
}
