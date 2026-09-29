package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.enums.ClueStatus;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.CourseMapper;
import com.qk.mapper.OperateLogMapper;
import com.qk.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上线前的安全与健壮性守护测试
 * <p>
 * 这里验证的都是「出问题会造成真实损失」的行为，而不是普通业务分支：
 * 主键注入、密码摘要复用登录、操作不存在的数据、错误请求体、非法文件上传、未登录访问。
 */
@SpringBootTest
@Transactional
class HardeningTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    /** 不带令牌的 MockMvc，用于验证拦截器 */
    private MockMvc noTokenMvc;

    @BeforeEach
    void setUp() {
        String token = jwtUtil.generateToken(Map.of("id", 1, "username", "zhangsan"));
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
        noTokenMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void clientSuppliedIdIsIgnoredOnInsert() throws Exception {
        // 客户端指定主键，服务端必须忽略，交给数据库自增
        mockMvc.perform(post("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":999999,"phone":"15900000001","channel":1,"name":"主键注入测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Clue saved = clueMapper.selectList(null).stream()
                .filter(c -> "15900000001".equals(c.getPhone()))
                .findFirst().orElse(null);
        Assertions.assertNotNull(saved);
        Assertions.assertNotEquals(999999, saved.getId(), "客户端指定的主键不应生效");
        // 新增线索的状态由服务端固定为「待分配」
        Assertions.assertEquals(ClueStatus.WAIT_ALLOT.getCode(), saved.getStatus());
        // 上传/新增时归属人也由服务端控制
        Assertions.assertNull(saved.getUserId());
    }

    @Test
    void passwordDigestCannotBeReusedForLogin() throws Exception {
        // zhangsan 库中存放的摘要就是 md5("zhangsan" + "123")
        String digest = DigestUtil.md5Hex("zhangsan123");

        // 直接把摘要当密码提交，必须被拒绝（否则摘要等同于可复用的登录凭证）
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"zhangsan","password":"%s"}
                                """.formatted(digest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));

        // 明文密码仍然可以登录
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"zhangsan","password":"123"}
                                """))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void operatingMissingRowsReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/clues/assign/{clueId}/{userId}", 99999999, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("线索不存在"));

        mockMvc.perform(put("/businesses/back/{id}", 99999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("商机不存在"));

        mockMvc.perform(put("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":99999999,"phone":"15900000002","channel":1,"name":"不存在"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("客户不存在"));

        mockMvc.perform(put("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":99999999,"channel":1,"name":"不存在","type":1,
                                 "startTime":"2025-06-01 00:00:00","endTime":"2025-06-30 23:59:59"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("活动不存在"));
    }

    @Test
    void malformedRequestBodyReturns400() throws Exception {
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{这不是JSON}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请求参数格式不正确"));

        // 日期格式非法同样属于请求体问题
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"channel":1,"name":"日期非法","type":1,
                                 "startTime":"2025/06/01","endTime":"2025-06-30 23:59:59"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deptRoleCourseMissingRowsReturnsBusinessError() throws Exception {
        // 修改/删除不存在的数据时，以前会返回 code=1（实际什么都没改），现在必须给出明确原因
        mockMvc.perform(put("/depts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":99999999,"name":"不存在的部门","status":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("部门不存在"));

        mockMvc.perform(delete("/depts/{id}", 99999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("部门不存在"));

        mockMvc.perform(put("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":99999999,"name":"不存在的角色","label":"not_exists_label"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("角色不存在"));

        mockMvc.perform(delete("/roles/{id}", 99999999))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("角色不存在"));

        mockMvc.perform(put("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":99999999,"subject":1,"name":"不存在的课程","price":100,"target":1}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("课程不存在"));

        mockMvc.perform(delete("/courses/{id}", 99999999))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("课程不存在"));
    }

    @Test
    void missingRequiredFieldsReturnsBusinessError() throws Exception {
        // 手机号是数据库必填项，缺失时应给出明确提示，而不是 500
        mockMvc.perform(post("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"channel":1,"name":"缺少手机号"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("手机号与线索来源不能为空"));
    }

    @Test
    void uploadRejectsNonImageFile() throws Exception {
        // 校验在调用 OSS 之前完成，因此这个用例不依赖外网
        MockMultipartFile file = new MockMultipartFile("image", "evil.sh",
                "text/plain", "echo hi".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片"));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        noTokenMvc.perform(get("/clues"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsMaskedInOperateLog() throws Exception {
        // 操作日志会把方法参数落库，User 的 toString 带 password 字段，
        // 因此必须在写日志前脱敏，否则明文密码就进了 operate_log 表
        String rawPassword = "P@ssw0rd-UnitTest";
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String username = "cs_mask" + suffix;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"username":"%s","name":"脱敏测试","phone":"199%08d","email":"%s@qk.test",
                                 "password":"%s","gender":1,"status":1,"deptId":6,"roleId":1}
                                """.formatted(username, Integer.parseInt(suffix), username, rawPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<OperateLog> logs = operateLogMapper.selectList(
                new LambdaQueryWrapper<OperateLog>().eq(OperateLog::getMethodName, "addUser"));
        Assertions.assertEquals(1, logs.size(), "新增用户应记录一条操作日志");

        String params = logs.get(0).getMethodParams();
        Assertions.assertFalse(params.contains(rawPassword), "操作日志里不能出现明文密码");
        Assertions.assertTrue(params.contains("password=***"), "密码字段应被脱敏为 ***");
    }

    @Test
    void courseFieldsAreValidated() throws Exception {
        // 课程这四个字段在库里是 NOT NULL，不校验就会变成 500「系统繁忙」
        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"只传了名字"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("课程名称、学科、价格、适用人群均不能为空"));

        // NOT NULL 拦不住空字符串，所以名称还要单独判空白
        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"subject":1,"name":"   ","price":100,"target":1}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        // 取值范围校验：库里没有 CHECK 约束，不校验就会写入 subject=99 这类脏数据
        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"subject":99,"name":"学科越界","price":100,"target":1}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("学科取值必须在 1~7 之间"));

        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"subject":1,"name":"人群越界","price":100,"target":9}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("适用人群取值必须在 1~2 之间"));

        // price 是 int unsigned，负数会直接被数据库拒绝，这里提前给出可读提示
        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"subject":1,"name":"负价格","price":-1,"target":1}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("价格不能为负数"));

        // 修改是部分更新：只传 id + name 应当成功，只传越界的 subject 应当被拒绝
        Course course = new Course();
        course.setSubject(1);
        course.setName("校验测试课程");
        course.setPrice(100);
        course.setTarget(1);
        courseMapper.insert(course);

        mockMvc.perform(put("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"name":"只改名字"}
                                """.formatted(course.getId())))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(put("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"subject":99}
                                """.formatted(course.getId())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("学科取值必须在 1~7 之间"));
    }
}
