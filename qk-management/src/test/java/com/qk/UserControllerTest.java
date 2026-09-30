package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.mapper.UserMapper;
import com.qk.common.util.JwtUtil;
import com.qk.mapper.ClueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.Dept;
import com.qk.entity.Role;
import com.qk.entity.User;
import com.qk.entity.Clue;
import com.qk.entity.enums.ClueStatus;

/**
 * 用户管理接口测试，校验 3. 接口文档-用户管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class UserControllerTest {

    /** 保证同一次测试内生成的手机号不重复 */
    private static final AtomicInteger SEQ = new AtomicInteger();

    /** 测试用部门与角色，对应 sql/reset_and_seed.sql 的最小数据集（1=市场部，1=admin） */
    private static final Integer TEST_DEPT_ID = 1;
    private static final Integer TEST_ROLE_ID = 1;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClueMapper clueMapper;

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
     * 直接落库一条用户，用于构造查询/修改/删除的测试数据
     */
    private User insertUser(String username, String name) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPhone("199" + String.format("%08d", seq));
        user.setEmail(username + seq + "@qk.test");
        user.setPassword(DigestUtil.md5Hex(username + "123"));
        user.setGender(1);
        user.setStatus(1);
        user.setDeptId(TEST_DEPT_ID);
        user.setRoleId(TEST_ROLE_ID);
        user.setRemark("单元测试数据");
        userMapper.insert(user);
        return user;
    }

    /** 造一条归属于指定用户的线索，用于验证「用户被业务数据引用时不可删除」 */
    private Clue insertClue(Integer userId) {
        int seq = SEQ.incrementAndGet();
        Clue clue = new Clue();
        clue.setPhone("151" + String.format("%08d", seq));
        clue.setChannel(1);
        clue.setName("用户引用线索" + seq);
        clue.setGender(1);
        clue.setAge(24);
        clue.setWechat("wx" + seq);
        clue.setQq("qq" + seq);
        clue.setUserId(userId);
        clue.setStatus(ClueStatus.WAIT_ALLOT.getCode());
        clue.setSubject(1);
        clue.setLevel(2);
        clueMapper.insert(clue);
        return clue;
    }

    @Test
    void pageUsers() throws Exception {
        // 库里只有最小数据集，这里自己造 5 条，避免分页断言依赖种子数据的规模
        for (int i = 0; i < 5; i++) {
            insertUser("cs_page" + i, "分页测试" + i);
        }

        mockMvc.perform(get("/users").param("page", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.data.rows", hasSize(5)))
                .andExpect(jsonPath("$.data.rows[0].id").isNumber())
                .andExpect(jsonPath("$.data.rows[0].username").exists())
                // 历史数据里存在角色ID在角色表中不存在的用户，roleName 允许为 null，
                // 因此这里只校验字段本身是否参与序列化，关联名称的准确性在 findById 用例中校验
                .andExpect(jsonPath("$.data.rows[0].deptId").isNumber())
                .andExpect(jsonPath("$.data.rows[0].roleId").isNumber())
                .andExpect(jsonPath("$.data.rows[0].password").doesNotExist());
    }

    @Test
    void pageUsersWithFilter() throws Exception {
        insertUser("cs_zhangsan", "测试张三");

        mockMvc.perform(get("/users").param("name", "测试张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].name").value("测试张三"));

        // 叠加部门条件
        mockMvc.perform(get("/users").param("name", "测试张三").param("deptId", String.valueOf(TEST_DEPT_ID)))
                .andExpect(jsonPath("$.data.total").value(1));

        // 部门不匹配时查不到
        mockMvc.perform(get("/users").param("name", "测试张三").param("deptId", "2"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void findById() throws Exception {
        User user = insertUser("cs_findbyid", "测试回显");

        mockMvc.perform(get("/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.username").value("cs_findbyid"))
                .andExpect(jsonPath("$.data.name").value("测试回显"))
                .andExpect(jsonPath("$.data.deptId").value(TEST_DEPT_ID))
                .andExpect(jsonPath("$.data.roleId").value(TEST_ROLE_ID))
                .andExpect(jsonPath("$.data.deptName").exists())
                .andExpect(jsonPath("$.data.roleName").exists())
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void addUser() throws Exception {
        int seq = SEQ.incrementAndGet();
        String username = "cs_add" + seq;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"%s","name":"测试新增","phone":"198%08d","email":"%s@qk.test",
                                 "gender":2,"status":1,"deptId":6,"roleId":1,"remark":"新增测试"}
                                """.formatted(username, seq, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 新增用户未传密码，应使用默认密码：md5(用户名 + 123)，且创建/修改时间由自动填充写入
        User saved = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertEquals(
                DigestUtil.md5Hex(username + "123"), saved.getPassword());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getCreateTime());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getUpdateTime());
    }

    @Test
    void updateUser() throws Exception {
        User user = insertUser("cs_update", "测试修改");

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"username":"cs_update","name":"测试修改后","phone":"%s",
                                 "email":"%s","gender":1,"status":1,"deptId":6,"roleId":1}
                                """.formatted(user.getId(), user.getPhone(), user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        User updated = userMapper.selectById(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals("测试修改后", updated.getName());
        // 密码不应被修改请求覆盖
        org.junit.jupiter.api.Assertions.assertEquals(
                DigestUtil.md5Hex("cs_update" + "123"), updated.getPassword());
    }

    @Test
    void deleteUsers() throws Exception {
        User first = insertUser("cs_delete1", "测试删除一");
        User second = insertUser("cs_delete2", "测试删除二");

        mockMvc.perform(delete("/users/{ids}", first.getId() + "," + second.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertNull(userMapper.selectById(first.getId()));
        org.junit.jupiter.api.Assertions.assertNull(userMapper.selectById(second.getId()));
    }

    @Test
    void deleteCurrentUserIsRejected() throws Exception {
        // 默认令牌对应的用户 id 是 1，删除自己会被守卫拦下
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("不能删除当前登录用户"));
    }

    @Test
    void deleteMissingUserIsRejected() throws Exception {
        mockMvc.perform(delete("/users/99999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户不存在: [99999999]"));
    }

    @Test
    void deleteUserReferencedByClueIsRejected() throws Exception {
        User user = insertUser("cs_delete_ref", "被线索引用");
        insertClue(user.getId());

        mockMvc.perform(delete("/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户 " + user.getId()
                        + " 仍被业务数据引用（线索 1 条、商机 0 条、跟进记录 0 条），无法删除；如不再使用请改为停用"));

        assertNotNull(userMapper.selectById(user.getId()), "仍被线索引用的用户不应被删除");
    }

    @Test
    void listAllUsers() throws Exception {
        insertUser("cs_list", "测试下拉");

        mockMvc.perform(get("/users/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[*].name", hasItem("测试下拉")))
                .andExpect(jsonPath("$.data[0].password").doesNotExist());
    }

    @Test
    void findUsersByRoleLabel() throws Exception {
        insertUser("cs_role", "测试角色查询");

        mockMvc.perform(get("/users/role/{roleLabel}", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].name", hasItem("测试角色查询")))
                .andExpect(jsonPath("$.data[*].roleName", hasItem("管理员")));

        // 不存在的角色标识返回空数组
        mockMvc.perform(get("/users/role/{roleLabel}", "not_exists_label"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findUsersByDeptId() throws Exception {
        insertUser("cs_dept", "测试部门查询");

        mockMvc.perform(get("/users/dept/{deptId}", TEST_DEPT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].name", hasItem("测试部门查询")));

        mockMvc.perform(get("/users/dept/{deptId}", 99999))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void loginSuccess() throws Exception {
        insertUser("cs_login", "测试登录");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"cs_login","password":"123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("cs_login"))
                .andExpect(jsonPath("$.data.name").value("测试登录"))
                .andExpect(jsonPath("$.data.roleLabel").value("admin"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void loginFailure() throws Exception {
        insertUser("cs_login_fail", "测试登录失败");

        // 密码错误
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"cs_login_fail","password":"wrong"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));

        // 用户不存在
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"not_exists_user","password":"123"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        // 单独构建一个不带默认请求头的 MockMvc，验证未登录时直接响应 401
        MockMvc noTokenMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        noTokenMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithIllegalTokenReturns401() throws Exception {
        MockMvc noTokenMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        // 篡改令牌（把签名最后一位改掉），校验应失败
        String legalToken = jwtUtil.generateToken(Map.of("id", 1, "username", "zhangsan"));
        String illegalToken = legalToken.substring(0, legalToken.length() - 1)
                + (legalToken.endsWith("a") ? "b" : "a");

        noTokenMvc.perform(get("/users").header("token", illegalToken))
                .andExpect(status().isUnauthorized());

        noTokenMvc.perform(get("/users").header("token", "not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
