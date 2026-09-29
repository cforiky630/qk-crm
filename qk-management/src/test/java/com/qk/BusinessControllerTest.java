package com.qk;

import com.qk.mapper.BusinessMapper;
import com.qk.mapper.CustomerMapper;
import com.qk.mapper.UserMapper;
import com.qk.enums.BusinessStatus;
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

/**
 * 商机管理接口测试，校验 7. 接口文档-商机管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class BusinessControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    private MockMvc mockMvc;

    /** 当前登录用户，同时用作商机归属人：由测试自己创建，避免断言写死种子数据里的用户姓名 */
    private User operator;

    @BeforeEach
    void setUp() {
        int seq = SEQ.incrementAndGet();
        operator = new User();
        operator.setUsername("cs_biz_op" + seq);
        operator.setName("测试归属人" + seq);
        operator.setPhone("154" + String.format("%08d", seq));
        operator.setEmail("cs_biz_op" + seq + "@qk.test");
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

    private Business insertBusiness(String name, Integer status) {
        int seq = SEQ.incrementAndGet();
        Business business = new Business();
        business.setName(name);
        business.setPhone("152" + String.format("%08d", seq));
        business.setGender(1);
        business.setAge(24);
        business.setWechat("wx" + seq);
        business.setQq("qq" + seq);
        business.setSubject(1);
        business.setDegree(4);
        business.setJobStatus(1);
        business.setChannel(1);
        business.setStatus(status);
        businessMapper.insert(business);
        return business;
    }

    @Test
    void listBusinesses() throws Exception {
        insertBusiness("测试商机列表", BusinessStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(get("/businesses").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].id").isNumber())
                .andExpect(jsonPath("$.data.rows[0].name").exists())
                .andExpect(jsonPath("$.data.rows[0].phone").exists())
                .andExpect(jsonPath("$.data.rows[0].status").isNumber())
                .andExpect(jsonPath("$.data.rows[0].createTime").exists())
                .andExpect(jsonPath("$.data.rows[0].updateTime").exists());
    }

    @Test
    void listBusinessesExcludesRecycledAndConverted() throws Exception {
        Business recycled = insertBusiness("测试回收商机", BusinessStatus.RECYCLED.getCode());
        Business converted = insertBusiness("测试转客户商机", BusinessStatus.CONVERT_CUSTOMER.getCode());
        Business following = insertBusiness("测试跟进中商机", BusinessStatus.FOLLOWING.getCode());

        mockMvc.perform(get("/businesses").param("pageSize", "50"))
                .andExpect(jsonPath("$.data.rows[*].id", org.hamcrest.Matchers.not(hasItem(recycled.getId()))))
                .andExpect(jsonPath("$.data.rows[*].id", org.hamcrest.Matchers.not(hasItem(converted.getId()))))
                .andExpect(jsonPath("$.data.rows[*].id", hasItem(following.getId())));
    }

    @Test
    void listBusinessesWithFilter() throws Exception {
        Business business = insertBusiness("测试条件商机", BusinessStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(get("/businesses").param("businessId", String.valueOf(business.getId())))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].name").value("测试条件商机"));

        mockMvc.perform(get("/businesses").param("name", "测试条件").param("phone", business.getPhone()))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/businesses").param("name", "不存在的客户"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));
    }

    @Test
    void addBusiness() throws Exception {
        int seq = SEQ.incrementAndGet();
        String phone = "153" + String.format("%08d", seq);

        mockMvc.perform(post("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"phone":"%s","channel":2,"name":"承娟","gender":2,"age":19,
                                 "wechat":"cj2839","qq":"2595964758","subject":1,"degree":4,
                                 "jobStatus":1,"courseId":1,"remark":"无"}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Business saved = businessMapper.selectList(null).stream()
                .filter(b -> phone.equals(b.getPhone())).findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertEquals(BusinessStatus.WAIT_ALLOT.getCode(), saved.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(saved.getUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getCreateTime());
    }

    @Test
    void assignBusiness() throws Exception {
        Business business = insertBusiness("测试分配商机", BusinessStatus.WAIT_ALLOT.getCode());

        mockMvc.perform(put("/businesses/assign/{businessId}/{userId}", business.getId(), operator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Business assigned = businessMapper.selectById(business.getId());
        org.junit.jupiter.api.Assertions.assertEquals(operator.getId(), assigned.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(BusinessStatus.WAIT_FOLLOW.getCode(), assigned.getStatus());

        mockMvc.perform(get("/businesses").param("businessId", String.valueOf(business.getId())))
                .andExpect(jsonPath("$.data.rows[0].assignName").value(operator.getName()));
    }

    @Test
    void backToPool() throws Exception {
        Business business = insertBusiness("测试踢回公海", BusinessStatus.WAIT_FOLLOW.getCode());
        business.setUserId(8);
        businessMapper.updateById(business);

        mockMvc.perform(put("/businesses/back/{id}", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Business recycled = businessMapper.selectById(business.getId());
        org.junit.jupiter.api.Assertions.assertEquals(BusinessStatus.RECYCLED.getCode(), recycled.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(recycled.getUserId(), "踢回公海应清空归属人");

        mockMvc.perform(get("/businesses/pool").param("businessId", String.valueOf(business.getId())))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].status").value(4));
    }

    @Test
    void getPoolBusinessesWithFilter() throws Exception {
        Business recycled = insertBusiness("测试公海商机", BusinessStatus.RECYCLED.getCode());

        mockMvc.perform(get("/businesses/pool")
                        .param("name", "测试公海")
                        .param("subject", "1")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].id").value(recycled.getId()));

        mockMvc.perform(get("/businesses/pool").param("subject", "7"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));
    }

    @Test
    void convertToCustomer() throws Exception {
        Business business = insertBusiness("测试转客户", BusinessStatus.WAIT_FOLLOW.getCode());

        mockMvc.perform(post("/businesses/toCustomer/{id}", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Business converted = businessMapper.selectById(business.getId());
        org.junit.jupiter.api.Assertions.assertEquals(BusinessStatus.CONVERT_CUSTOMER.getCode(), converted.getStatus());

        List<Customer> customers = customerMapper.selectList(null);
        Customer created = customers.stream()
                .filter(c -> business.getId().equals(c.getBusinessId())).findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(created, "转客户后应生成一条客户数据");
        org.junit.jupiter.api.Assertions.assertEquals(business.getPhone(), created.getPhone());
        org.junit.jupiter.api.Assertions.assertNotNull(created.getCreateTime());
    }

    @Test
    void getBusinessByIdWithTrackRecords() throws Exception {
        Business business = insertBusiness("测试商机详情", BusinessStatus.WAIT_FOLLOW.getCode());
        business.setUserId(operator.getId());
        businessMapper.updateById(business);

        mockMvc.perform(put("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"trackStatus":1,"keyItems":["课程","时间"],
                                 "nextTime":"2025-06-23 10:00:00","record":"了解了课程及上课时间"}
                                """.formatted(business.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/businesses/{id}", business.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(business.getId()))
                .andExpect(jsonPath("$.data.status").value(3))
                .andExpect(jsonPath("$.data.assignName").value(operator.getName()))
                .andExpect(jsonPath("$.data.trackRecords", hasSize(1)))
                .andExpect(jsonPath("$.data.trackRecords[0].trackStatus").value(1))
                .andExpect(jsonPath("$.data.trackRecords[0].keyItems").value("[课程, 时间]"))
                .andExpect(jsonPath("$.data.trackRecords[0].record").value("了解了课程及上课时间"))
                .andExpect(jsonPath("$.data.trackRecords[0].userId").value(operator.getId()))
                .andExpect(jsonPath("$.data.trackRecords[0].assignName").value(operator.getName()));
    }
}
