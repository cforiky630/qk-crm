package com.qk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
import com.qk.mapper.ActivityMapper;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.OperateLogMapper;
import com.qk.mapper.UserMapper;
import com.qk.common.util.JwtUtil;
import com.qk.entity.vo.OperateLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.Business;
import com.qk.entity.Clue;
import com.qk.entity.User;

/**
 * 其他接口测试，校验 9. 接口文档-其他接口.md 中的日志列表与首页概览
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class OtherControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    private MockMvc mockMvc;

    /** 当前登录用户：由测试自己创建，避免断言写死种子数据里的用户姓名 */
    private User operator;

    @BeforeEach
    void setUp() {
        int seq = SEQ.incrementAndGet();
        operator = new User();
        operator.setUsername("cs_log_op" + seq);
        operator.setName("测试操作人" + seq);
        operator.setPhone("156" + String.format("%08d", seq));
        operator.setEmail("cs_log_op" + seq + "@qk.test");
        operator.setPassword("x");
        operator.setGender(1);
        operator.setStatus(1);
        operator.setDeptId(1);
        operator.setRoleId(1);
        userMapper.insert(operator);

        String token = jwtUtil.generateToken(Map.of("id", operator.getId(), "username", operator.getUsername()));
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    @Test
    void writeOperationIsRecordedToOperateLog() throws Exception {
        String activityName = "日志测试活动" + SEQ.incrementAndGet();

        // 新增活动标注了 @LogOperation，切面应把本次操作写入 operate_log
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"channel":1,"name":"%s","startTime":"2025-06-01 00:00:00",
                                 "endTime":"2025-06-30 23:59:59","description":"日志测试","type":1,"discount":8.0}
                                """.formatted(activityName)))
                .andExpect(jsonPath("$.code").value(1));

        List<com.qk.entity.OperateLog> logs = operateLogMapper.selectList(
                new LambdaQueryWrapper<com.qk.entity.OperateLog>()
                        .eq(com.qk.entity.OperateLog::getMethodName, "addActivity")
                        .eq(com.qk.entity.OperateLog::getClassName, "com.qk.controller.ActivityController"));

        org.junit.jupiter.api.Assertions.assertEquals(1, logs.size(), "增删改接口应记录一条操作日志");
        com.qk.entity.OperateLog log = logs.get(0);
        // 操作人来自 JWT 中的用户ID（令牌里放的就是上面自建的 operator）
        org.junit.jupiter.api.Assertions.assertEquals(operator.getId(), log.getOperateUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(log.getOperateTime());
        org.junit.jupiter.api.Assertions.assertNotNull(log.getCostTime());
        org.junit.jupiter.api.Assertions.assertTrue(log.getMethodParams().contains(activityName));
        org.junit.jupiter.api.Assertions.assertTrue(log.getReturnValue().contains("code=1"));
    }

    @Test
    void listLogs() throws Exception {
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"channel":1,"name":"日志列表活动%s","startTime":"2025-06-01 00:00:00",
                                 "endTime":"2025-06-30 23:59:59","description":"日志列表","type":1,"discount":8.0}
                                """.formatted(SEQ.incrementAndGet())))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/logs").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[*].methodName", hasItem("addActivity")))
                .andExpect(jsonPath("$.data.rows[0].operateUserId").isNumber())
                .andExpect(jsonPath("$.data.rows[0].operateTime").exists())
                .andExpect(jsonPath("$.data.rows[0].className").exists())
                .andExpect(jsonPath("$.data.rows[0].methodParams").exists())
                .andExpect(jsonPath("$.data.rows[0].returnValue").exists())
                .andExpect(jsonPath("$.data.rows[0].costTime").isNumber())
                // 页面原型要展示「操作模块」「操作类型」，由类名/方法名映射得到
                .andExpect(jsonPath("$.data.rows[0].operateModule").value("活动管理"))
                .andExpect(jsonPath("$.data.rows[0].operateType").value("新增活动"));

        // 按操作人姓名模糊查询：令牌里的用户就是上面自建的 operator
        mockMvc.perform(get("/logs").param("operateUserName", operator.getName()))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/logs").param("operateUserName", "不存在的操作人"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));

        // 按操作模块 / 操作类型模糊查询（原型 1.1 / 1.2 的两个搜索条件）
        mockMvc.perform(get("/logs").param("operateModule", "活动管理").param("operateType", "新增活动"))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.rows[*].methodName", hasItem("addActivity")));

        mockMvc.perform(get("/logs").param("operateModule", "不存在的模块"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));

        mockMvc.perform(get("/logs").param("operateType", "不存在的操作类型"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));
    }

    @Test
    void getOverview() throws Exception {
        // 基线：先按枚举把每个状态的当前数量记下来，再用增量断言，避免依赖库的空表状态
        Map<ClueStatus, Long> clueBefore = new EnumMap<>(ClueStatus.class);
        for (ClueStatus status : ClueStatus.values()) {
            clueBefore.put(status, clueMapper.selectCount(
                    new LambdaQueryWrapper<Clue>().eq(Clue::getStatus, status.getCode())));
        }
        Map<BusinessStatus, Long> businessBefore = new EnumMap<>(BusinessStatus.class);
        for (BusinessStatus status : BusinessStatus.values()) {
            businessBefore.put(status, businessMapper.selectCount(
                    new LambdaQueryWrapper<Business>().eq(Business::getStatus, status.getCode())));
        }
        Long clueTotalBefore = clueMapper.selectCount(null);
        Long businessTotalBefore = businessMapper.selectCount(null);

        // 每种状态各插一条：ReportMapper 里 12 个统计口径的任何一个数字写错，下面的断言都会失败，
        // 这就是把枚举编码和那段聚合 SQL 绑在一起的守卫测试
        for (ClueStatus status : ClueStatus.values()) {
            // 注意 name 列只有 varchar(20)，这里用短名字
            insertClue(status.getCode(), "概览线索" + status.getCode());
        }
        for (BusinessStatus status : BusinessStatus.values()) {
            insertBusiness(status.getCode(), "概览商机" + status.getCode());
        }

        String body = mockMvc.perform(get("/report/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn().getResponse().getContentAsString();

        // 直接按数字比较，避免 jsonPath 数值类型（Integer/Long）带来的干扰
        cn.hutool.json.JSONObject data = cn.hutool.json.JSONUtil.parseObj(body).getJSONObject("data");

        org.junit.jupiter.api.Assertions.assertEquals(
                clueTotalBefore + ClueStatus.values().length, data.getLong("clueTotal"));
        org.junit.jupiter.api.Assertions.assertEquals(
                businessTotalBefore + BusinessStatus.values().length, data.getLong("businessTotal"));

        org.junit.jupiter.api.Assertions.assertEquals(
                clueBefore.get(ClueStatus.WAIT_ALLOT) + 1, data.getLong("clueWaitAllot"));
        org.junit.jupiter.api.Assertions.assertEquals(
                clueBefore.get(ClueStatus.WAIT_FOLLOW) + 1, data.getLong("clueWaitFollow"));
        org.junit.jupiter.api.Assertions.assertEquals(
                clueBefore.get(ClueStatus.FOLLOWING) + 1, data.getLong("clueFollowing"));
        org.junit.jupiter.api.Assertions.assertEquals(
                clueBefore.get(ClueStatus.FALSE_CLUE) + 1, data.getLong("clueFalse"));
        org.junit.jupiter.api.Assertions.assertEquals(
                clueBefore.get(ClueStatus.CONVERT_BUSINESS) + 1, data.getLong("clueConvertBusiness"));

        org.junit.jupiter.api.Assertions.assertEquals(
                businessBefore.get(BusinessStatus.WAIT_ALLOT) + 1, data.getLong("businessWaitAllot"));
        org.junit.jupiter.api.Assertions.assertEquals(
                businessBefore.get(BusinessStatus.WAIT_FOLLOW) + 1, data.getLong("businessWaitFollow"));
        org.junit.jupiter.api.Assertions.assertEquals(
                businessBefore.get(BusinessStatus.FOLLOWING) + 1, data.getLong("businessFollowing"));
        org.junit.jupiter.api.Assertions.assertEquals(
                businessBefore.get(BusinessStatus.RECYCLED) + 1, data.getLong("businessFalse"));
        org.junit.jupiter.api.Assertions.assertEquals(
                businessBefore.get(BusinessStatus.CONVERT_CUSTOMER) + 1, data.getLong("businessConvertCustomer"));

        // 不变量：各状态之和必须等于总数。
        // 这条断言专门防「新增了枚举状态、却忘了改 ReportMapper 里的聚合 SQL」——
        // 那种情况下总数会涨，而对应的状态桶不会，只有这条断言能抓住。
        org.junit.jupiter.api.Assertions.assertEquals(
                data.getLong("clueTotal"),
                data.getLong("clueWaitAllot") + data.getLong("clueWaitFollow") + data.getLong("clueFollowing")
                        + data.getLong("clueFalse") + data.getLong("clueConvertBusiness"),
                "线索各状态之和应等于总数");
        org.junit.jupiter.api.Assertions.assertEquals(
                data.getLong("businessTotal"),
                data.getLong("businessWaitAllot") + data.getLong("businessWaitFollow")
                        + data.getLong("businessFollowing") + data.getLong("businessFalse")
                        + data.getLong("businessConvertCustomer"),
                "商机各状态之和应等于总数");

        // 12 个统计字段一个都不能少
        org.junit.jupiter.api.Assertions.assertEquals(12, data.size());
    }

    private void insertClue(Integer status, String name) {
        int seq = SEQ.incrementAndGet();
        Clue clue = new Clue();
        clue.setPhone("157" + String.format("%08d", seq));
        clue.setChannel(1);
        clue.setName(name);
        clue.setGender(1);
        clue.setAge(24);
        clue.setStatus(status);
        clueMapper.insert(clue);
    }

    private void insertBusiness(Integer status, String name) {
        int seq = SEQ.incrementAndGet();
        Business business = new Business();
        business.setName(name);
        business.setPhone("158" + String.format("%08d", seq));
        business.setChannel(1);
        business.setSubject(1);
        business.setStatus(status);
        businessMapper.insert(business);
    }
}
