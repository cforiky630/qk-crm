package com.qk;

import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.po.Clue;
import com.qk.entity.po.User;
import com.qk.mapper.ClueMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口边界健壮性回归测试（AB 类重构的判据）
 * <p>
 * 这里覆盖的都是「改造前会静默出错」的场景，每一条都对应一次真实缺陷：
 * <ol>
 *   <li>框架层的 404/405/415 被兜底处理器吃成 500，并触发运维告警；</li>
 *   <li>分页参数没有下限校验：{@code ?page=} 拆箱 NPE 变 500，{@code page=0} 静默返回空列表；</li>
 *   <li>入参没有长度/格式校验：超长字段撞库后变 500；</li>
 *   <li>不使用物理外键却没有引用校验：可以把线索分配给不存在的用户；</li>
 *   <li>令牌只验签名不验账号：停用/删除账号的旧令牌仍然可用。</li>
 * </ol>
 */
@SpringBootTest
@Transactional
class ApiRobustnessTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private UserMapper userMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User operator = newUser("ab_op", 1);
        String token = jwtUtil.generateToken(Map.of("id", operator.getId(), "username", operator.getUsername()));
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    // ---------- 1. 框架异常：不再一律 500 ----------

    @Test
    void unknownPathReturns404InsteadOf500() throws Exception {
        mockMvc.perform(get("/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请求的资源不存在"));
    }

    @Test
    void unsupportedMethodReturns405InsteadOf500() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请求方法不被支持"));
    }

    @Test
    void unsupportedContentTypeReturns415InsteadOf500() throws Exception {
        mockMvc.perform(post("/clues")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请求的 Content-Type 不被支持"));
    }

    // ---------- 2. 分页参数校验：不再 500 / 静默 ----------

    @Test
    void emptyPageParamIsRejectedWithFieldMessage() throws Exception {
        // 改造前：page 为 null → new Page<>(null, ...) 拆箱 NPE → 500
        mockMvc.perform(get("/clues").param("page", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("页码不能为空"));
    }

    @Test
    void zeroPageSizeIsRejectedInsteadOfReturningEmptyPage() throws Exception {
        mockMvc.perform(get("/clues").param("pageSize", "0"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("每页条数必须大于 0"));
    }

    @Test
    void negativePageIsRejectedInsteadOfSilentlyReturningData() throws Exception {
        mockMvc.perform(get("/businesses").param("page", "-5"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("页码必须大于 0"));
    }

    @Test
    void oversizedPageSizeIsRejectedWithTheSameLimitAsThePaginationPlugin() throws Exception {
        mockMvc.perform(get("/depts").param("pageSize", "500"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("每页条数不能超过 200"));
    }

    @Test
    void primitiveParamControllersAreCoveredToo() throws Exception {
        // 部门/角色/课程/活动四个接口原先直接接收 @RequestParam 的 page/pageSize，
        // 现在同样走统一的 PageQuery 校验
        mockMvc.perform(get("/depts").param("page", ""))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("页码不能为空"));
        mockMvc.perform(get("/activities").param("pageSize", "0"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("每页条数必须大于 0"));
    }

    @Test
    void defaultPaginationStillWorks() throws Exception {
        mockMvc.perform(get("/clues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    // ---------- 3. 长度与格式校验：不再撞库变 500 ----------

    @Test
    void overlongNameIsRejectedWithFieldMessage() throws Exception {
        mockMvc.perform(post("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"phone":"13777770001","channel":1,"name":"这是一个远远超过二十个字符长度限制的客户姓名"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("客户姓名长度不能超过 20 个字符"));
    }

    @Test
    void invalidPhoneIsRejected() throws Exception {
        mockMvc.perform(post("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"phone":"abc","channel":1,"name":"非法手机号"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("手机号格式不正确"));
    }

    @Test
    void overlongFollowUpRecordIsRejected() throws Exception {
        Clue clue = insertClue(ClueStatus.WAIT_FOLLOW.getCode());

        mockMvc.perform(put("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"record":"%s"}
                                """.formatted(clue.getId(), "跟".repeat(120))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("跟进记录长度不能超过 100 个字符"));
    }

    // ---------- 4. 引用完整性：不再允许悬空引用 ----------

    @Test
    void assigningClueToMissingUserIsRejected() throws Exception {
        Clue clue = insertClue(ClueStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(put("/clues/assign/{clueId}/{userId}", clue.getId(), 99999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("归属人不存在或已停用，请重新选择"));
    }

    @Test
    void assigningClueToDisabledUserIsRejected() throws Exception {
        User disabled = newUser("ab_disabled", 0);
        Clue clue = insertClue(ClueStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(put("/clues/assign/{clueId}/{userId}", clue.getId(), disabled.getId()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("归属人不存在或已停用，请重新选择"));
    }

    @Test
    void clueWithMissingActivityIsRejected() throws Exception {
        mockMvc.perform(post("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"phone":"13777770002","channel":1,"name":"悬空活动","activityId":99999999}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("活动不存在"));
    }

    @Test
    void businessWithMissingCourseIsRejected() throws Exception {
        mockMvc.perform(post("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"悬空课程","phone":"13777770003","courseId":99999999}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("课程不存在"));
    }

    @Test
    void userWithMissingDeptIsRejected() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"ab_nodept","name":"悬空部门","phone":"13777770004",
                                 "email":"ab_nodept@qk.test","gender":1,"status":1,"deptId":99999999,"roleId":1}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("部门不存在"));
    }

    // ---------- 5. 令牌必须对应可用账号 ----------

    @Test
    void tokenOfUnknownAccountIsRejected() throws Exception {
        MockMvc plainMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        String ghostToken = jwtUtil.generateToken(Map.of("id", 88888888, "username", "ghost"));

        plainMvc.perform(get("/users").header("token", ghostToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDisabledAccountIsRejected() throws Exception {
        User disabled = newUser("ab_token_disabled", 0);
        MockMvc plainMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        String token = jwtUtil.generateToken(Map.of("id", disabled.getId(), "username", disabled.getUsername()));

        plainMvc.perform(get("/users").header("token", token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 6. 状态流转守卫在重构后仍然生效 ----------

    @Test
    void duplicateConversionIsRejectedByTheLifecycle() throws Exception {
        Clue clue = insertClue(ClueStatus.WAIT_FOLLOW.getCode());

        mockMvc.perform(put("/clues/toBusiness/{id}", clue.getId()))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(put("/clues/toBusiness/{id}", clue.getId()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该线索当前状态不允许转商机"));
    }

    private User newUser(String prefix, int status) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername(prefix + seq);
        user.setName("边界用户" + seq);
        user.setPhone("166" + String.format("%08d", seq));
        user.setEmail(prefix + seq + "@qk.test");
        user.setPassword("x");
        user.setGender(1);
        user.setStatus(status);
        user.setDeptId(1L);
        user.setRoleId(1L);
        userMapper.insert(user);
        return user;
    }

    private Clue insertClue(Integer status) {
        int seq = SEQ.incrementAndGet();
        Clue clue = new Clue();
        clue.setPhone("167" + String.format("%08d", seq));
        clue.setChannel(1);
        clue.setName("边界线索" + seq);
        clue.setStatus(status);
        clueMapper.insert(clue);
        return clue;
    }
}
