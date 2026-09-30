package com.qk;

import com.qk.mapper.CourseMapper;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.qk.entity.Course;

/**
 * 课程管理接口测试，校验 4. 接口文档-课程管理.md 中的契约
 * 注意：使用 @Transactional 保证测试数据不落库，方法结束后自动回滚
 */
@SpringBootTest
@Transactional
class CourseControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private CourseMapper courseMapper;

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
     * 直接落库一条课程，用于构造查询/修改/删除的测试数据
     */
    private Course insertCourse(Integer subject, String name, Integer price, Integer target, String description) {
        Course course = new Course();
        course.setSubject(subject);
        course.setName(name);
        course.setPrice(price);
        course.setTarget(target);
        course.setDescription(description);
        course.setCreateTime(LocalDateTime.now());
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.insert(course);
        return course;
    }

    @Test
    void addCourse() throws Exception {
        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"subject":1,"name":"SpringAI入门","price":199,"target":2,"description":"SpringAI入门"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void pageCourses() throws Exception {
        insertCourse(1, "Java基础入门", 599, 1, "适合零基础学员");
        insertCourse(2, "Python基础入门", 699, 1, "适合零基础学员");

        mockMvc.perform(get("/courses").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows[0].id").isNumber())
                .andExpect(jsonPath("$.data.rows[0].subject").isNumber())
                .andExpect(jsonPath("$.data.rows[0].name").exists())
                .andExpect(jsonPath("$.data.rows[0].price").isNumber())
                .andExpect(jsonPath("$.data.rows[0].target").isNumber())
                .andExpect(jsonPath("$.data.rows[0].description").exists())
                .andExpect(jsonPath("$.data.rows[0].createTime").exists())
                .andExpect(jsonPath("$.data.rows[0].updateTime").exists());
    }

    @Test
    void pageCoursesDefaults() throws Exception {
        // 不传 page / pageSize，应使用默认值 1 / 10
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.rows").isArray());
    }

    @Test
    void pageCoursesWithFilter() throws Exception {
        // 用带唯一前缀的课程名，避免和库里已有课程（种子数据）互相干扰
        insertCourse(1, "测试Java课程A", 599, 1, "零基础");
        insertCourse(1, "测试Java课程B", 1999, 2, "进阶");
        insertCourse(2, "测试Python课程", 699, 1, "零基础");

        // name 模糊 + subject 等值
        mockMvc.perform(get("/courses").param("name", "测试Java").param("subject", "1"))
                .andExpect(jsonPath("$.data.total").value(2));

        // 再叠加 target 等值
        mockMvc.perform(get("/courses").param("name", "测试Java").param("subject", "1").param("target", "2"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].name").value("测试Java课程B"));
    }

    @Test
    void findById() throws Exception {
        Course course = insertCourse(1, "SpringAI入门", 199, 2, "SpringAI入门");

        mockMvc.perform(get("/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(course.getId()))
                .andExpect(jsonPath("$.data.subject").value(1))
                .andExpect(jsonPath("$.data.name").value("SpringAI入门"))
                .andExpect(jsonPath("$.data.price").value(199))
                .andExpect(jsonPath("$.data.target").value(2))
                .andExpect(jsonPath("$.data.description").value("SpringAI入门"))
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists());
    }

    @Test
    void updateCourse() throws Exception {
        Course course = insertCourse(1, "SpringAI入门", 199, 2, "SpringAI入门");

        mockMvc.perform(put("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("""
                                {"id":%d,"subject":1,"name":"SpringAI入门到精通","price":299,"target":2,"description":"SpringAI入门到精通"}
                                """.formatted(course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 回查确认修改生效
        mockMvc.perform(get("/courses/{id}", course.getId()))
                .andExpect(jsonPath("$.data.name").value("SpringAI入门到精通"))
                .andExpect(jsonPath("$.data.price").value(299));
    }

    @Test
    void listAllCourses() throws Exception {
        insertCourse(1, "Java基础入门", 599, 1, "零基础");
        insertCourse(2, "Python基础入门", 699, 1, "零基础");

        // /courses/list 是字面量路径，不能被 /courses/{id} 抢占
        mockMvc.perform(get("/courses/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].subject").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].price").exists())
                .andExpect(jsonPath("$.data[0].target").exists())
                .andExpect(jsonPath("$.data[0].description").exists())
                .andExpect(jsonPath("$.data[0].createTime").exists())
                .andExpect(jsonPath("$.data[0].updateTime").exists());
    }

    @Test
    void listCoursesBySubject() throws Exception {
        insertCourse(1, "测试学科课程A", 599, 1, "零基础");
        insertCourse(1, "测试学科课程B", 1999, 2, "进阶");
        insertCourse(2, "测试学科课程C", 699, 1, "零基础");

        // /courses/subject/{subject} 比 /courses/{id} 多一段，不会被抢占
        mockMvc.perform(get("/courses/subject/{subject}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                // 不断言具体条数（库里可能还有种子课程），只校验「都属于学科1」且包含/不含预期课程
                .andExpect(jsonPath("$.data[*].subject", everyItem(is(1))))
                .andExpect(jsonPath("$.data[*].name", hasItem("测试学科课程A")))
                .andExpect(jsonPath("$.data[*].name", not(hasItem("测试学科课程C"))));
    }

    @Test
    void listCoursesBySubjectNoMatch() throws Exception {
        insertCourse(1, "Java基础入门", 599, 1, "零基础");

        // 没有该学科的课程时返回空数组，而不是报错
        mockMvc.perform(get("/courses/subject/{subject}", 99))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void deleteCourse() throws Exception {
        Course course = insertCourse(1, "临时课程", 100, 1, "用于测试删除");

        mockMvc.perform(delete("/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 删除后应查不到
        mockMvc.perform(get("/courses/{id}", course.getId()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void pageCoursesPagingWorks() throws Exception {
        // 库里可能有种子课程，用「插入前总数 + 3」断言，不依赖空表状态
        Long totalBefore = courseMapper.selectCount(null);

        insertCourse(1, "课程A", 100, 1, "A");
        insertCourse(1, "课程B", 200, 1, "B");
        insertCourse(1, "课程C", 300, 1, "C");

        mockMvc.perform(get("/courses").param("page", "1").param("pageSize", "2"))
                .andExpect(jsonPath("$.data.rows", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(totalBefore.intValue() + 3));
    }
}
