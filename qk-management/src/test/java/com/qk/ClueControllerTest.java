package com.qk;

import com.qk.mapper.BusinessMapper;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.UserMapper;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.Business;
import com.qk.entity.Clue;
import com.qk.entity.User;

/**
 * 线索管理接口测试，校验 6. 接口文档-线索管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class ClueControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    private MockMvc mockMvc;

    /** 当前登录用户，同时用作线索归属人：由测试自己创建，避免断言写死种子数据里的用户姓名 */
    private User operator;

    @BeforeEach
    void setUp() {
        int seq = SEQ.incrementAndGet();
        operator = new User();
        operator.setUsername("cs_clue_op" + seq);
        operator.setName("测试归属人" + seq);
        operator.setPhone("151" + String.format("%08d", seq));
        operator.setEmail("cs_clue_op" + seq + "@qk.test");
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

    private Clue insertClue(String name, Integer status) {
        int seq = SEQ.incrementAndGet();
        Clue clue = new Clue();
        clue.setPhone("150" + String.format("%08d", seq));
        clue.setChannel(1);
        clue.setName(name);
        clue.setGender(1);
        clue.setAge(24);
        clue.setWechat("wx" + seq);
        clue.setQq("qq" + seq);
        clue.setStatus(status);
        clue.setSubject(1);
        clue.setLevel(2);
        clueMapper.insert(clue);
        return clue;
    }

    @Test
    void listClues() throws Exception {
        insertClue("测试线索列表", ClueStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(get("/clues").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].id").isNumber())
                .andExpect(jsonPath("$.data.rows[0].phone").exists())
                .andExpect(jsonPath("$.data.rows[0].channel").isNumber())
                .andExpect(jsonPath("$.data.rows[0].status").isNumber())
                .andExpect(jsonPath("$.data.rows[0].createTime").exists())
                .andExpect(jsonPath("$.data.rows[0].updateTime").exists());
    }

    @Test
    void listCluesExcludesClosedStatus() throws Exception {
        Clue falseClue = insertClue("测试伪线索", ClueStatus.FALSE_CLUE.getCode());
        Clue businessClue = insertClue("测试已转商机线索", ClueStatus.CONVERT_BUSINESS.getCode());
        Clue activeClue = insertClue("测试跟进中线索", ClueStatus.FOLLOWING.getCode());

        // 伪线索（4）与已转商机（5）不应出现在线索列表中
        mockMvc.perform(get("/clues").param("pageSize", "50"))
                .andExpect(jsonPath("$.data.rows[*].id", org.hamcrest.Matchers.not(hasItem(falseClue.getId()))))
                .andExpect(jsonPath("$.data.rows[*].id", org.hamcrest.Matchers.not(hasItem(businessClue.getId()))))
                .andExpect(jsonPath("$.data.rows[*].id", hasItem(activeClue.getId())));
    }

    @Test
    void listCluesWithFilter() throws Exception {
        Clue clue = insertClue("测试条件线索", ClueStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(get("/clues").param("clueId", String.valueOf(clue.getId())))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].name").value("测试条件线索"));

        mockMvc.perform(get("/clues").param("phone", clue.getPhone()))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/clues").param("status", "2"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));
    }

    @Test
    void addClue() throws Exception {
        int seq = SEQ.incrementAndGet();
        String phone = "151" + String.format("%08d", seq);

        mockMvc.perform(post("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"phone":"%s","channel":1,"name":"卫丹","gender":2,"age":24,
                                 "wechat":"wxweidan","qq":"8450313640"}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Clue saved = clueMapper.selectList(null).stream()
                .filter(c -> phone.equals(c.getPhone())).findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        // 新增线索默认为待分配
        org.junit.jupiter.api.Assertions.assertEquals(ClueStatus.WAIT_ALLOT.getCode(), saved.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getCreateTime());
    }

    @Test
    void assignClue() throws Exception {
        Clue clue = insertClue("测试分配线索", ClueStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(put("/clues/assign/{clueId}/{userId}", clue.getId(), operator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Clue assigned = clueMapper.selectById(clue.getId());
        org.junit.jupiter.api.Assertions.assertEquals(operator.getId(), assigned.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(ClueStatus.WAIT_FOLLOW.getCode(), assigned.getStatus());

        // 分配后列表应能按归属人姓名查到
        mockMvc.perform(get("/clues").param("clueId", String.valueOf(clue.getId())))
                .andExpect(jsonPath("$.data.rows[0].assignName").value(operator.getName()));
    }

    @Test
    void getClueByIdWithTrackRecords() throws Exception {
        Clue clue = insertClue("测试线索详情", ClueStatus.WAIT_FOLLOW.getCode());
        clue.setUserId(operator.getId());
        clueMapper.updateById(clue);

        mockMvc.perform(put("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"subject":1,"level":3,"nextTime":"2025-05-22 12:00:00",
                                 "record":"了解一下目前java方向的就业前景"}
                                """.formatted(clue.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/clues/{id}", clue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(clue.getId()))
                .andExpect(jsonPath("$.data.status").value(3))
                .andExpect(jsonPath("$.data.assignName").value(operator.getName()))
                .andExpect(jsonPath("$.data.trackRecords", hasSize(1)))
                .andExpect(jsonPath("$.data.trackRecords[0].record").value("了解一下目前java方向的就业前景"))
                .andExpect(jsonPath("$.data.trackRecords[0].type").value(1))
                .andExpect(jsonPath("$.data.trackRecords[0].userId").value(operator.getId()))
                .andExpect(jsonPath("$.data.trackRecords[0].assignName").value(operator.getName()));
    }

    @Test
    void trackClueUpdatesStatusAndNextTime() throws Exception {
        Clue clue = insertClue("测试跟进线索", ClueStatus.WAIT_FOLLOW.getCode());

        mockMvc.perform(put("/clues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"name":"测试跟进线索","phone":"%s","channel":1,"gender":1,"age":25,
                                 "subject":2,"level":1,"nextTime":"2025-06-23 10:00:00","record":"已沟通"}
                                """.formatted(clue.getId(), clue.getPhone())))
                .andExpect(jsonPath("$.code").value(1));

        Clue updated = clueMapper.selectById(clue.getId());
        org.junit.jupiter.api.Assertions.assertEquals(ClueStatus.FOLLOWING.getCode(), updated.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getNextTime());
        // 跟进后归属人保持不变
        org.junit.jupiter.api.Assertions.assertNull(updated.getUserId());
    }

    @Test
    void markFalseClue() throws Exception {
        Clue clue = insertClue("测试伪线索处理", ClueStatus.WAIT_FOLLOW.getCode());

        mockMvc.perform(put("/clues/false/{id}", clue.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"reason":2,"remark":"电话停机了"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Clue updated = clueMapper.selectById(clue.getId());
        org.junit.jupiter.api.Assertions.assertEquals(ClueStatus.FALSE_CLUE.getCode(), updated.getStatus());

        mockMvc.perform(get("/clues/{id}", clue.getId()))
                .andExpect(jsonPath("$.data.trackRecords[0].type").value(0))
                .andExpect(jsonPath("$.data.trackRecords[0].falseReason").value(2))
                .andExpect(jsonPath("$.data.trackRecords[0].record").value("电话停机了"));
    }

    @Test
    void convertToBusiness() throws Exception {
        Clue clue = insertClue("测试转商机", ClueStatus.WAIT_FOLLOW.getCode());

        mockMvc.perform(put("/clues/toBusiness/{id}", clue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Clue updated = clueMapper.selectById(clue.getId());
        org.junit.jupiter.api.Assertions.assertEquals(ClueStatus.CONVERT_BUSINESS.getCode(), updated.getStatus());

        List<Business> businesses = businessMapper.selectList(null);
        Business created = businesses.stream()
                .filter(b -> clue.getId().equals(b.getClueId())).findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(created, "转商机后应生成一条商机数据");
        org.junit.jupiter.api.Assertions.assertEquals(clue.getPhone(), created.getPhone());
        org.junit.jupiter.api.Assertions.assertEquals(BusinessStatus.WAIT_ALLOT.getCode(), created.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(created.getUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(created.getCreateTime());
    }

    @Test
    void getPoolClues() throws Exception {
        Clue falseClue = insertClue("测试线索池伪线索", ClueStatus.FALSE_CLUE.getCode());

        mockMvc.perform(get("/clues/pool").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[*].id", hasItem(falseClue.getId())))
                .andExpect(jsonPath("$.data.rows[0].activityName").doesNotExist());

        mockMvc.perform(get("/clues/pool").param("clueId", String.valueOf(falseClue.getId())))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].status").value(4));
    }
}
