package com.qk;

import com.qk.mapper.ActivityMapper;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.po.Activity;
import com.qk.entity.po.Clue;
import com.qk.entity.enums.ClueStatus;

/**
 * 活动管理接口测试，校验 5. 接口文档-活动管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class ActivityControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        String token = jwtUtil.generateToken(Map.of("id", 1, "username", "zhangsan"));
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    private Activity insertActivity(Integer channel, String name, Integer type) {
        Activity activity = new Activity();
        activity.setChannel(channel);
        activity.setName(name);
        activity.setStartTime(LocalDateTime.of(2025, 6, 1, 0, 0));
        activity.setEndTime(LocalDateTime.of(2025, 6, 30, 23, 59, 59));
        activity.setDescription("测试活动");
        activity.setType(type);
        activityMapper.insert(activity);
        return activity;
    }

    /** 造一条关联指定活动的线索，用于验证「活动被引用时不可删除」 */
    private Clue insertClue(Long activityId) {
        int seq = SEQ.incrementAndGet();
        Clue clue = new Clue();
        clue.setPhone("150" + String.format("%08d", seq));
        clue.setChannel(1);
        clue.setActivityId(activityId);
        clue.setName("活动引用线索" + seq);
        clue.setGender(1);
        clue.setAge(24);
        clue.setWechat("wx" + seq);
        clue.setQq("qq" + seq);
        clue.setStatus(ClueStatus.WAIT_ALLOT.getCode());
        clue.setSubject(1);
        clue.setLevel(2);
        clueMapper.insert(clue);
        return clue;
    }

    /** 造一条指定起止时间的活动，用于验证「活动状态」筛选 */
    private Activity insertActivityWithTime(String name, LocalDateTime start, LocalDateTime end) {
        Activity activity = new Activity();
        activity.setChannel(1);
        activity.setName(name);
        activity.setStartTime(start);
        activity.setEndTime(end);
        activity.setDescription("活动状态筛选测试");
        activity.setType(1);
        activity.setDiscount(new java.math.BigDecimal("8.0"));
        activityMapper.insert(activity);
        return activity;
    }

    /**
     * 页面原型的「活动状态」筛选（未开始 / 进行中 / 已结束）：
     * 库里没有状态列，后端按 start_time / end_time 与当前时间比较来实现。
     */
    @Test
    void listActivitiesByStatus() throws Exception {
        int seq = SEQ.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();

        Activity notStarted = insertActivityWithTime("未开始活动" + seq, now.plusDays(1), now.plusDays(3));
        Activity inProgress = insertActivityWithTime("进行中活动" + seq, now.minusDays(1), now.plusDays(1));
        Activity finished = insertActivityWithTime("已结束活动" + seq, now.minusDays(3), now.minusDays(1));

        // 1 未开始
        mockMvc.perform(get("/activities").param("activityStatus", "1").param("pageSize", "200"))
                .andExpect(jsonPath("$.code").value(1))
        // JsonPath 把 JSON 数字解析为 Integer，主键是 Long，比较前先取 intValue()
                .andExpect(jsonPath("$.data.rows[*].id", hasItem(notStarted.getId().intValue())))
                .andExpect(jsonPath("$.data.rows[*].id", not(hasItem(inProgress.getId().intValue()))))
                .andExpect(jsonPath("$.data.rows[*].id", not(hasItem(finished.getId().intValue()))));

        // 2 进行中
        mockMvc.perform(get("/activities").param("activityStatus", "2").param("pageSize", "200"))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.rows[*].id", hasItem(inProgress.getId().intValue())))
                .andExpect(jsonPath("$.data.rows[*].id", not(hasItem(notStarted.getId().intValue()))))
                .andExpect(jsonPath("$.data.rows[*].id", not(hasItem(finished.getId().intValue()))));

        // 3 已结束
        mockMvc.perform(get("/activities").param("activityStatus", "3").param("pageSize", "200"))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.rows[*].id", hasItem(finished.getId().intValue())))
                .andExpect(jsonPath("$.data.rows[*].id", not(hasItem(inProgress.getId().intValue()))));

        // 非法状态码给出明确提示，而不是静默返回全量数据
        mockMvc.perform(get("/activities").param("activityStatus", "9"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("活动状态取值为 1（未开始）、2（进行中）、3（已结束）"));
    }

    @Test
    void addActivity() throws Exception {
        String name = "测试折扣活动" + SEQ.incrementAndGet();

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"channel":1,"name":"%s","startTime":"2025-06-01 00:00:00",
                                 "endTime":"2025-06-30 23:59:59","description":"限时8折","type":1,"discount":8.0}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"));

        // 创建时间、修改时间由自动填充写入
        Activity saved = activityMapper.selectList(null).stream()
                .filter(a -> name.equals(a.getName())).findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getCreateTime());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getUpdateTime());
    }

    @Test
    void pageActivities() throws Exception {
        insertActivity(1, "测试线上活动" + SEQ.incrementAndGet(), 1);

        mockMvc.perform(get("/activities").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].channel").isNumber())
                .andExpect(jsonPath("$.data.rows[0].name").exists())
                .andExpect(jsonPath("$.data.rows[0].startTime").exists())
                .andExpect(jsonPath("$.data.rows[0].endTime").exists())
                .andExpect(jsonPath("$.data.rows[0].type").isNumber())
                .andExpect(jsonPath("$.data.rows[0].createTime").exists())
                .andExpect(jsonPath("$.data.rows[0].updateTime").exists());
    }

    @Test
    void pageActivitiesWithFilter() throws Exception {
        insertActivity(1, "测试线上折扣" + SEQ.incrementAndGet(), 1);
        insertActivity(2, "测试推广代金券" + SEQ.incrementAndGet(), 2);

        // channel + type 均为等值查询
        mockMvc.perform(get("/activities").param("channel", "2").param("type", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows[*].channel", everyItem(is(2))))
                .andExpect(jsonPath("$.data.rows[*].type", everyItem(is(2))));
    }

    @Test
    void findById() throws Exception {
        Activity activity = insertActivity(1, "测试详情活动" + SEQ.incrementAndGet(), 1);

        mockMvc.perform(get("/activities/{id}", activity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(activity.getId()))
                .andExpect(jsonPath("$.data.name").value(activity.getName()))
                .andExpect(jsonPath("$.data.channel").value(1))
                .andExpect(jsonPath("$.data.type").value(1));
    }

    @Test
    void updateActivity() throws Exception {
        Activity activity = insertActivity(1, "测试修改活动" + SEQ.incrementAndGet(), 1);

        mockMvc.perform(put("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"channel":2,"name":"修改后的活动","startTime":"2025-07-01 00:00:00",
                                 "endTime":"2025-07-31 23:59:59","description":"修改","type":2,"voucher":500}
                                """.formatted(activity.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/activities/{id}", activity.getId()))
                .andExpect(jsonPath("$.data.name").value("修改后的活动"))
                .andExpect(jsonPath("$.data.channel").value(2))
                .andExpect(jsonPath("$.data.voucher").value(500));
    }

    @Test
    void deleteActivity() throws Exception {
        Activity activity = insertActivity(1, "测试删除活动" + SEQ.incrementAndGet(), 1);

        mockMvc.perform(delete("/activities/{id}", activity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertNull(activityMapper.selectById(activity.getId()));
    }

    @Test
    void deleteMissingActivityIsRejected() throws Exception {
        // 原先删除接口没有存在性校验，删不存在的 id 也会返回「成功」
        mockMvc.perform(delete("/activities/{id}", 99999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("活动不存在"));
    }

    @Test
    void deleteActivityReferencedByClueIsRejected() throws Exception {
        Activity activity = insertActivity(1, "被线索引用的活动" + SEQ.incrementAndGet(), 1);
        insertClue(activity.getId());

        mockMvc.perform(delete("/activities/{id}", activity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该活动已关联 1 条线索，无法删除"));

        assertNotNull(activityMapper.selectById(activity.getId()), "仍被线索引用的活动不应被删除");
    }

    @Test
    void listActivitiesByType() throws Exception {
        insertActivity(1, "测试类型活动一" + SEQ.incrementAndGet(), 1);
        insertActivity(2, "测试类型活动二" + SEQ.incrementAndGet(), 2);

        mockMvc.perform(get("/activities/type/{type}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].type", everyItem(is(1))));

        // 没有该类型的活动时返回空数组，而不是报错
        mockMvc.perform(get("/activities/type/{type}", 99))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
