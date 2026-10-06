package com.qk.controller;

import com.qk.entity.po.Course;
import com.qk.entity.dto.CourseQueryDto;
import com.qk.entity.dto.CourseSaveDto;
import com.qk.entity.enums.RoleLabel;
import com.qk.entity.vo.CourseVO;
import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.CourseService;
import com.qk.interceptor.RequireRole;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class CourseController {

    private final CourseService courseService;

    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * 新增课程
     *
     * @param courseDto 课程信息
     * @return 操作结果
     */
    @LogOperation
    @RequireRole(RoleLabel.ADMIN)
    @PostMapping("/courses")
    public Result<Void> addCourse(@Valid @RequestBody CourseSaveDto courseDto) {
        log.info("新增课程,参数:{}", courseDto);
        courseService.addCourse(toCourse(courseDto));
        return Result.success();
    }

    /**
     * 条件分页查询课程
     *
     * @param query 查询条件（含分页参数）
     * @return 分页查询结果
     */
    @GetMapping("/courses")
    public Result<PageResult<CourseVO>> listCourses(@Valid CourseQueryDto query) {
        log.info("分页查询课程, 参数: {}", query);
        return Result.success(courseService.findCoursesByPage(query));
    }

    /**
     * 根据ID查询课程
     *
     * @param id 课程ID
     * @return 查询结果
     */
    @GetMapping("/courses/{id}")
    public Result<CourseVO> findById(@PathVariable Long id) {
        log.info("查询课程ID为{}的课程信息", id);
        CourseVO course = courseService.findById(id);
        return Result.success(course);
    }

    /**
     * 修改课程
     *
     * @param courseDto 课程信息
     * @return 统一响应结果
     */
    @LogOperation
    @RequireRole(RoleLabel.ADMIN)
    @PutMapping("/courses")
    public Result<Void> updateCourse(@Valid @RequestBody CourseSaveDto courseDto) {
        log.info("修改课程信息：{}", courseDto);
        courseService.updateById(toCourse(courseDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（Service 不依赖 Web 入参对象）
     * <p>
     * 逐个字段赋值而不是 {@code BeanUtil.copyProperties}：反射拷贝靠字段名约定，
     * 实体字段改名后会静默停止拷贝（行为悄悄变），这里漏抄则编译期就报错。
     */
    private Course toCourse(CourseSaveDto dto) {
        Course course = new Course();
        course.setId(dto.getId());
        course.setSubject(dto.getSubject());
        course.setName(dto.getName());
        course.setPrice(dto.getPrice());
        course.setTarget(dto.getTarget());
        course.setDescription(dto.getDescription());
        return course;
    }

    /**
     * 删除课程
     *
     * @param id 课程ID
     * @return 统一响应结果
     */
    @LogOperation
    @RequireRole(RoleLabel.ADMIN)
    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@PathVariable("id") Long id) {
        log.info("删除课程：{}", id);
        courseService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询所有课程，不分页，用于下拉框
     *
     * @return 统一响应结果
     */
    @GetMapping("/courses/list")
    public Result<List<CourseVO>> listAllCourses() {
        log.info("查询所有课程");
        List<CourseVO> courses = courseService.findAll();
        return Result.success(courses);
    }

    /**
     * 根据学科查询课程，不分页
     *
     * @param subject 学科
     * @return 统一响应结果
     */
    @GetMapping("/courses/subject/{subject}")
    public Result<List<CourseVO>> listCoursesBySubject(@PathVariable Integer subject) {
        log.info("查询学科为{}的课程", subject);
        List<CourseVO> courses = courseService.findBySubject(subject);
        return Result.success(courses);
    }
}
