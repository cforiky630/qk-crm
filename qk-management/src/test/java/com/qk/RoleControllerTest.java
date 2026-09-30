package com.qk;

import com.qk.mapper.RoleMapper;
import com.qk.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.Role;

/**
 * 角色管理接口测试，校验 2. 接口文档-角色管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class RoleControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 除 /login 外的接口都要求携带 JWT 令牌，这里直接签发一个合法令牌作为默认请求头
        String token = jwtUtil.generateToken(Map.of("id", 1, "username", "zhangsan"));
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    /**
     * 直接落库一条角色，用于构造查询/修改/删除的测试数据
     */
    private Role insertRole(String name, String label, String remark) {
        Role role = new Role();
        role.setName(name);
        role.setLabel(label);
        role.setRemark(remark);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    @Test
    void addRole() throws Exception {
        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"线索专员","label":"ut_clue_operator","remark":"负责跟进线索"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"));
    }

    @Test
    void pageRoles() throws Exception {
        insertRole("普通用户", "user_normal", "普通用户");
        insertRole("商机专员", "ut_business_operator", "用于跟进商机信息");

        mockMvc.perform(get("/roles").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].id").isNumber())
                .andExpect(jsonPath("$.data.rows[0].name").exists())
                .andExpect(jsonPath("$.data.rows[0].label").exists())
                .andExpect(jsonPath("$.data.rows[0].remark").exists())
                .andExpect(jsonPath("$.data.rows[0].createTime").exists())
                .andExpect(jsonPath("$.data.rows[0].updateTime").exists());
    }

    @Test
    void pageRolesDefaults() throws Exception {
        // 不传 page / pageSize，应使用默认值 1 / 10
        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.rows").isArray());
    }

    @Test
    void pageRolesWithFilter() throws Exception {
        // 名称与标识都加上「测试」前缀，避免与库里已有的业务种子数据（线索专员/clue_operator）冲突
        insertRole("测试线索专员", "ut_clue_operator", "负责跟进线索");
        insertRole("测试商机专员", "ut_business_operator", "用于跟进商机信息");

        // name 与 label 均为模糊查询，两个条件是与的关系
        mockMvc.perform(get("/roles").param("name", "测试线索").param("label", "ut_clue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].label").value("ut_clue_operator"));
    }

    @Test
    void findById() throws Exception {
        Role role = insertRole("管理员", "ut_admin", "系统管理员");

        mockMvc.perform(get("/roles/{id}", role.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(role.getId()))
                .andExpect(jsonPath("$.data.name").value("管理员"))
                .andExpect(jsonPath("$.data.label").value("ut_admin"))
                .andExpect(jsonPath("$.data.remark").value("系统管理员"))
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists());
    }

    @Test
    void updateRole() throws Exception {
        Role role = insertRole("普通用户", "user_normal", "普通用户");

        mockMvc.perform(put("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"name":"普通用户","label":"user_normal","remark":"改过的备注"}
                                """.formatted(role.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 回查确认修改生效
        mockMvc.perform(get("/roles/{id}", role.getId()))
                .andExpect(jsonPath("$.data.remark").value("改过的备注"));
    }

    @Test
    void listAllRoles() throws Exception {
        insertRole("普通用户", "user_normal", "普通用户");
        insertRole("商机专员", "ut_business_operator", "用于跟进商机信息");

        // /roles/list 是字面量路径，不能被 /roles/{id} 抢占，否则会因 id 转数字失败而报错
        mockMvc.perform(get("/roles/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].label").exists())
                .andExpect(jsonPath("$.data[0].remark").exists())
                .andExpect(jsonPath("$.data[0].createTime").exists())
                .andExpect(jsonPath("$.data[0].updateTime").exists());
    }

    @Test
    void deleteRole() throws Exception {
        Role role = insertRole("临时角色", "temp_role", "用于测试删除");

        mockMvc.perform(delete("/roles/{id}", role.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 删除后应查不到
        mockMvc.perform(get("/roles/{id}", role.getId()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void duplicateLabelReturnsError() throws Exception {
        insertRole("普通用户", "user_normal", "普通用户");

        // label 有唯一索引，重复时应返回 code=0 而不是 500
        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"重复标识","label":"user_normal","remark":"重复"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("角色标识已存在"));
    }

    @Test
    void pageRolesPagingWorks() throws Exception {
        // 角色表里可能已有业务种子数据，因此用「插入前的总数 + 3」来断言，避免依赖库的空表状态
        Long totalBefore = roleMapper.selectCount(null);

        insertRole("角色A", "role_a", "A");
        insertRole("角色B", "role_b", "B");
        insertRole("角色C", "role_c", "C");

        mockMvc.perform(get("/roles").param("page", "1").param("pageSize", "2"))
                .andExpect(jsonPath("$.data.rows", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(totalBefore.intValue() + 3));
    }
}
