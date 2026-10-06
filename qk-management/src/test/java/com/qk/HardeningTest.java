package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.entity.enums.ClueStatus;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.CourseMapper;
import com.qk.mapper.DeptMapper;
import com.qk.mapper.OperateLogMapper;
import com.qk.common.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.po.Clue;
import com.qk.entity.po.Course;
import com.qk.entity.po.Dept;
import com.qk.entity.po.OperateLog;
import com.qk.entity.po.User;

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
    private DeptMapper deptMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                // DTO 校验按字段给出精确提示，不再使用合并文案
                .andExpect(jsonPath("$.msg", containsString("不能为空")));
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
                                 "password":"%s","gender":1,"status":1,"deptId":1,"roleId":1}
                                """.formatted(username, Integer.parseInt(suffix), username, rawPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<OperateLog> logs = operateLogMapper.selectList(
                new LambdaQueryWrapper<OperateLog>().eq(OperateLog::getMethodName, "addUser"));
        Assertions.assertEquals(1, logs.size(), "新增用户应记录一条操作日志");

        String params = logs.get(0).getMethodParams();
        Assertions.assertFalse(params.contains(rawPassword), "操作日志里不能出现明文密码");
        // UserSaveDto 已把 password 从接口契约里移除，日志里连该字段都不会出现；
        // LogAspect 的脱敏逻辑仍保留，作为其他敏感字段的防御性兜底。
        Assertions.assertFalse(params.contains("password"), "接口不应再接收 password 字段");
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
                // DTO 校验改为按字段给出精确提示，具体先报哪个字段由校验器决定，
                // 因此这里只断言「提示了必填」，不再绑定合并文案
                .andExpect(jsonPath("$.msg", containsString("不能为空")));

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
                // 适用人群按页面原型放开为 1~3（3 表示初级程序员），越界值仍然拒绝
                .andExpect(jsonPath("$.msg").value("适用人群取值必须在 1~3 之间"));

        // price 是 int unsigned，负数会直接被数据库拒绝，这里提前给出可读提示
        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"subject":1,"name":"负价格","price":-1,"target":1}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("价格不能为负数"));

        // 修改按文档要求提交完整字段（CourseBody.required = subject/name/price/target），
        // 只是「机制上」null 字段不参与 UPDATE；越界取值必须被拒绝
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
                                {"id":%d,"subject":1,"name":"改后的名字","price":100,"target":1}
                                """.formatted(course.getId())))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(put("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"subject":99,"name":"越界学科","price":100,"target":1}
                                """.formatted(course.getId())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("学科取值必须在 1~7 之间"));
    }

    /**
     * 逻辑删除必须同时满足三件事，缺一都会造成线上事故：
     * <ol>
     *   <li>接口视角看不到该行（与物理删除的表现完全一致）；</li>
     *   <li>库里的行还在，且 is_deleted 被置为 1；</li>
     *   <li>唯一值被释放：同名数据可以重新创建，反复删除同名记录也不会撞唯一键。</li>
     * </ol>
     * 第 3 条靠函数唯一索引 if(is_deleted = 0, 唯一列, NULL) 实现：已删除行的索引键为 NULL，
     * MySQL 视 NULL 互不相同，因此既不占用唯一值，也不会在反复删除时互相冲突。
     */
    @Test
    void softDeleteHidesRowButKeepsItAndFreesUniqueValue() throws Exception {
        String name = "软删部门" + System.currentTimeMillis() % 1000000;

        mockMvc.perform(post("/depts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"%s","status":0}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<Dept> created = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>().eq(Dept::getName, name).orderByDesc(Dept::getId));
        Assertions.assertFalse(created.isEmpty(), "新增的部门应能查到");
        Long id = created.get(0).getId();

        // deleted 是内部列，不能出现在任何对外报文里
        mockMvc.perform(get("/depts/{id}", id))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.deleted").doesNotExist());

        mockMvc.perform(delete("/depts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 1) 接口视角已经查不到
        Assertions.assertNull(deptMapper.selectById(id), "逻辑删除后接口视角应查不到该部门");
        // 2) 库里那行还在，且 is_deleted 被置为 1
        Integer isDeleted = jdbcTemplate.queryForObject("SELECT is_deleted FROM dept WHERE id = ?", Integer.class, id);
        Assertions.assertEquals(1, isDeleted, "逻辑删除应把 is_deleted 置为 1");

        // 3) 同名可以重新创建
        mockMvc.perform(post("/depts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"name":"%s","status":0}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<Dept> recreated = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>().eq(Dept::getName, name).orderByDesc(Dept::getId));
        Assertions.assertFalse(recreated.isEmpty(), "同名部门应能重新创建");
        // 再次删除同名记录：deleted 存 id，所以不会与上一次删除的记录撞唯一键
        mockMvc.perform(delete("/depts/{id}", recreated.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    /**
     * 详情接口对不存在的 id 必须统一返回 code = 0 + 「XXX不存在」
     * <p>
     * 历史上 dept/role/course/activity 会返回 code = 1 并省略 data，与 users/clues 的 code = 0 不一致。
     * 现在这四个接口改为复用各自的 requireXxx 守卫，行为与其他详情接口对齐。
     */
    @Test
    void detailEndpointsReportMissingDataConsistently() throws Exception {
        String[][] cases = {
                {"/depts/999999", "部门不存在"},
                {"/roles/999999", "角色不存在"},
                {"/courses/999999", "课程不存在"},
                {"/activities/999999", "活动不存在"},
                {"/users/999999", "用户不存在"},
                {"/clues/999999", "线索不存在"},
        };
        for (String[] item : cases) {
            mockMvc.perform(get(item[0]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.msg").value(item[1]));
        }
    }
    /**
     * 没有扩展名的文件必须被友好拒绝，而不是 500
     * <p>
     * 旧实现在 {@code lastIndexOf(".") == -1} 时直接 {@code substring(-1)}，
     * 抛的是 StringIndexOutOfBoundsException（兜底处理器转成 500），而不是「格式不支持」。
     */
    @Test
    void uploadRejectsFileWithoutExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "noextension", "image/png",
                "x".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片"));
    }
}
