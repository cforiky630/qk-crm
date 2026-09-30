package com.qk;

import com.qk.mapper.DeptMapper;
import com.qk.common.util.JwtUtil;
import com.qk.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.Dept;
import com.qk.entity.User;
import com.qk.entity.enums.EnableStatus;

/**
 * 部门管理接口测试，覆盖 1. 接口文档-部门管理.md 中 /depts/list
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class DeptControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private UserMapper userMapper;

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

    private Dept insertDept(String name, Integer status) {
        Dept dept = new Dept();
        dept.setName(name);
        dept.setStatus(status);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.insert(dept);
        return dept;
    }

    /** 造一个引用指定部门的用户，用于验证「部门被引用时不可删除」 */
    private User insertUser(Integer deptId) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername("cs_dept_u" + seq);
        user.setName("部门测试用户" + seq);
        user.setPhone("153" + String.format("%08d", seq));
        user.setEmail("cs_dept_u" + seq + "@qk.test");
        user.setPassword("x");
        user.setGender(1);
        user.setStatus(EnableStatus.ENABLED.getCode());
        user.setDeptId(deptId);
        userMapper.insert(user);
        return user;
    }

    @Test
    void listAllDeptsReturnsNormalOnly() throws Exception {
        insertDept("正常部门", 1);
        insertDept("停用部门", 0);

        // 接口描述：查询所有正常状态的部门数据，因此 status=0 不应出现
        mockMvc.perform(get("/depts/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].name", hasItem("正常部门")))
                .andExpect(jsonPath("$.data[*].name", not(hasItem("停用部门"))))
                .andExpect(jsonPath("$.data[*].status", everyItem(is(1))));
    }

    @Test
    void listAllDeptsFieldsMatchDoc() throws Exception {
        insertDept("字段校验部", 1);

        mockMvc.perform(get("/depts/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].status").isNumber())
                .andExpect(jsonPath("$.data[0].createTime").exists())
                .andExpect(jsonPath("$.data[0].updateTime").exists());
    }

    @Test
    void listRouteIsNotCapturedById() throws Exception {
        // /depts/list 是字面量路径，不能被 /depts/{id} 抢占，否则会因 id 转数字失败而报错
        mockMvc.perform(get("/depts/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void deleteEnabledDeptIsRejected() throws Exception {
        Dept dept = insertDept("启用中的部门", EnableStatus.ENABLED.getCode());

        mockMvc.perform(delete("/depts/" + dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("部门处于启用状态，请先停用后再删除"));

        // 不能只断言返回码，必须确认数据真的还在
        assertNotNull(deptMapper.selectById(dept.getId()), "启用状态的部门不应被删除");
    }

    @Test
    void deleteDeptReferencedByUserIsRejected() throws Exception {
        Dept dept = insertDept("仍被引用的部门", EnableStatus.DISABLED.getCode());
        insertUser(dept.getId());

        mockMvc.perform(delete("/depts/" + dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该部门下还有 1 名用户，无法删除"));

        assertNotNull(deptMapper.selectById(dept.getId()), "仍被用户引用的部门不应被删除");
    }

    @Test
    void deleteDisabledDeptWithoutUsersSucceeds() throws Exception {
        Dept dept = insertDept("空的停用部门", EnableStatus.DISABLED.getCode());

        mockMvc.perform(delete("/depts/" + dept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertNull(deptMapper.selectById(dept.getId()), "无引用的停用部门应被删除");
    }
}
