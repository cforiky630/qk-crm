package com.qk;

import com.qk.mapper.ActivityMapper;
import com.qk.util.JwtUtil;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
