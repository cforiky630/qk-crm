package com.qk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.enums.RoleLabel;
import com.qk.entity.po.Business;
import com.qk.entity.po.Clue;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.ClueMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口授权测试
 * <p>
 * 授权模型：
 * <ul>
 *   <li>标了 {@code @RequireRole} 的接口按角色放行 —— 管理/删除类仅 admin，业务流转给对应专员；</li>
 *   <li>未标注的接口（查询类、新增类）对所有已登录用户开放，避免补齐权限时把前端页面整体挡住；</li>
 *   <li>自定义角色只是数据，不具备任何接口授权。</li>
 * </ul>
 * 三种角色按标签查找，库里没有就自建，因此不依赖种子数据。
 */
@SpringBootTest
@Transactional
class AuthorizationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final String FORBIDDEN_MSG = "无权访问该接口，请联系管理员分配角色";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private BusinessMapper businessMapper;

    private User admin;
    private User clueOperator;
    private User businessOperator;
    private User accountWithoutRole;

    @BeforeEach
    void setUp() {
        admin = insertUser(roleIdOf(RoleLabel.ADMIN));
        clueOperator = insertUser(roleIdOf(RoleLabel.CLUE_OPERATOR));
        businessOperator = insertUser(roleIdOf(RoleLabel.BUSINESS_OPERATOR));
        accountWithoutRole = insertUser(null);
    }

    @Test
    void adminCanManageMasterData() throws Exception {
        mvcFor(admin).perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"授权测试角色","label":"authz_admin_%d"}
                                """.formatted(SEQ.incrementAndGet())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void nonAdminCannotManageMasterDataOrDeleteUsers() throws Exception {
        MockMvc mvc = mvcFor(clueOperator);

        mvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"name\":\"越权角色\",\"label\":\"authz_denied\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));

        mvc.perform(delete("/users/{ids}", admin.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"subject\":1,\"name\":\"越权课程\",\"price\":1,\"target\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clueOperatorCanFollowClueFlowButNotBusinessFlowNorAssign() throws Exception {
        MockMvc mvc = mvcFor(clueOperator);
        Clue clue = insertClue(ClueStatus.WAIT_FOLLOW.getCode());

        mvc.perform(put("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"id\":%d,\"record\":\"授权测试跟进\"}".formatted(clue.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Business business = insertBusiness(BusinessStatus.WAIT_FOLLOW.getCode());
        mvc.perform(put("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"id\":%d,\"trackStatus\":1,\"record\":\"越权跟进\"}".formatted(business.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));

        // 分配线索是管理员职责
        Clue waitAllot = insertClue(ClueStatus.WAIT_ALLOT.getCode());
        mvc.perform(put("/clues/assign/{clueId}/{userId}", waitAllot.getId(), clueOperator.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void businessOperatorCanFollowBusinessFlowButNotClueFlow() throws Exception {
        MockMvc mvc = mvcFor(businessOperator);
        Business business = insertBusiness(BusinessStatus.WAIT_FOLLOW.getCode());

        mvc.perform(put("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"id\":%d,\"trackStatus\":1,\"record\":\"授权测试跟进\"}".formatted(business.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Clue clue = insertClue(ClueStatus.WAIT_FOLLOW.getCode());
        mvc.perform(put("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"id\":%d,\"record\":\"越权跟进\"}".formatted(clue.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountWithoutRoleKeepsReadAccessOnly() throws Exception {
        MockMvc mvc = mvcFor(accountWithoutRole);

        mvc.perform(get("/clues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mvc.perform(put("/clues/toBusiness/{id}", insertClue(ClueStatus.WAIT_FOLLOW.getCode()).getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customRoleGetsNoInterfacePermission() throws Exception {
        // 自定义角色（非保留标签）只是数据：查询可用，流转不给
        Role custom = new Role();
        custom.setName("授权测试自定义角色" + SEQ.incrementAndGet());
        custom.setLabel("authz_custom_" + SEQ.get());
        custom.setRemark("不属于保留角色");
        roleMapper.insert(custom);

        MockMvc mvc = mvcFor(insertUser(custom.getId()));

        mvc.perform(get("/clues"))
                .andExpect(jsonPath("$.code").value(1));

        mvc.perform(put("/clues/toBusiness/{id}", insertClue(ClueStatus.WAIT_FOLLOW.getCode()).getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value(FORBIDDEN_MSG));
    }

    private MockMvc mvcFor(User user) {
        String token = jwtUtil.generateToken(Map.of("id", user.getId(), "username", user.getUsername()));
        return MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    /** 按保留标签找角色，找不到就自建（从零安装时库里只有内置 admin） */
    private Long roleIdOf(RoleLabel roleLabel) {
        Role existing = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getLabel, roleLabel.getLabel()));
        if (existing != null) {
            return existing.getId();
        }
        Role role = new Role();
        role.setName("授权测试角色" + SEQ.incrementAndGet());
        role.setLabel(roleLabel.getLabel());
        role.setRemark("接口授权测试");
        roleMapper.insert(role);
        return role.getId();
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

    private Business insertBusiness(Integer status) {
        int seq = SEQ.incrementAndGet();
        Business business = new Business();
        business.setName("授权测试商机" + seq);
        business.setPhone("171" + String.format("%08d", seq));
        business.setStatus(status);
        businessMapper.insert(business);
        return business;
    }
}
