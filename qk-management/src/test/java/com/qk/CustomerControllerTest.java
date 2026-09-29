package com.qk;

import com.qk.mapper.CourseMapper;
import com.qk.mapper.CustomerMapper;
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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 客户管理接口测试，校验 8. 接口文档-客户管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class CustomerControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CourseMapper courseMapper;

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

    /** 课程表用于校验客户列表的 courseName 关联查询 */
    private Integer insertCourse(String name) {
        Course course = new Course();
        course.setSubject(1);
        course.setName(name);
        course.setPrice(599);
        course.setTarget(1);
        course.setDescription("测试课程");
        courseMapper.insert(course);
        return course.getId();
    }

    private Customer insertCustomer(String name, Integer courseId, Integer channel) {
        int seq = SEQ.incrementAndGet();
        Customer customer = new Customer();
        customer.setPhone("155" + String.format("%08d", seq));
        customer.setChannel(channel);
        customer.setName(name);
        customer.setGender(1);
        customer.setAge(22);
        customer.setWechat("wx" + seq);
        customer.setQq("qq" + seq);
        customer.setDegree(4);
        customer.setJobStatus(1);
        customer.setSubject(1);
        customer.setCourseId(courseId);
        customerMapper.insert(customer);
        return customer;
    }

    @Test
    void listCustomers() throws Exception {
        String courseName = "测试意向课程" + SEQ.incrementAndGet();
        Integer courseId = insertCourse(courseName);
        Customer customer = insertCustomer("测试客户列表", courseId, 1);

        mockMvc.perform(get("/customers").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].id").isNumber())
                .andExpect(jsonPath("$.data.rows[0].phone").exists())
                .andExpect(jsonPath("$.data.rows[0].channel").isNumber())
                .andExpect(jsonPath("$.data.rows[0].courseId").isNumber())
                .andExpect(jsonPath("$.data.rows[0].createTime").exists())
                .andExpect(jsonPath("$.data.rows[0].updateTime").exists());

        // courseName 来自 course 表的关联查询
        mockMvc.perform(get("/customers").param("phone", customer.getPhone()))
                .andExpect(jsonPath("$.data.rows[0].courseName").value(courseName));
    }

    @Test
    void listCustomersWithFilter() throws Exception {
        insertCustomer("测试条件客户", null, 1);

        mockMvc.perform(get("/customers").param("name", "测试条件客户"))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/customers").param("name", "测试条件客户").param("channel", "1"))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/customers").param("name", "测试条件客户").param("channel", "2"))
                .andExpect(jsonPath("$.data.rows", hasSize(0)));
    }

    @Test
    void addCustomer() throws Exception {
        int seq = SEQ.incrementAndGet();
        String phone = "156" + String.format("%08d", seq);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"phone":"%s","channel":1,"name":"库家明","gender":1,"age":22,
                                 "wechat":"kujiaming","qq":"3353439142","degree":4,"jobStatus":1,
                                 "subject":1,"courseId":1}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());

        Customer saved = customerMapper.selectList(null).stream()
                .filter(c -> phone.equals(c.getPhone())).findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getCreateTime());
        // 手工新增的客户没有来源商机
        org.junit.jupiter.api.Assertions.assertNull(saved.getBusinessId());
    }

    @Test
    void getCustomerById() throws Exception {
        Customer customer = insertCustomer("测试客户详情", null, 2);

        mockMvc.perform(get("/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(customer.getId()))
                .andExpect(jsonPath("$.data.phone").value(customer.getPhone()))
                .andExpect(jsonPath("$.data.name").value("测试客户详情"))
                .andExpect(jsonPath("$.data.channel").value(2))
                .andExpect(jsonPath("$.data.businessId").doesNotExist());
    }

    @Test
    void updateCustomer() throws Exception {
        Customer customer = insertCustomer("测试修改客户", null, 1);

        mockMvc.perform(put("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"phone":"%s","channel":1,"name":"修改后的客户","gender":1,
                                 "age":23,"degree":4,"jobStatus":2,"subject":1,"courseId":1}
                                """.formatted(customer.getId(), customer.getPhone())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Customer updated = customerMapper.selectById(customer.getId());
        org.junit.jupiter.api.Assertions.assertEquals("修改后的客户", updated.getName());
        org.junit.jupiter.api.Assertions.assertEquals(23, updated.getAge());
    }
}
