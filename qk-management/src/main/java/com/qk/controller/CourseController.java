package com.qk.controller;

import com.qk.entity.Course;
import com.qk.common.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.CourseService;
import lombok.extern.slf4j.Slf4j;
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
     * @param course 课程信息
     * @return 操作结果
     */
    @LogOperation
    @PostMapping("/courses")
    public Result addCourse(@RequestBody Course course) {
        log.info("新增课程,参数:{}", course);
        courseService.addCourse(course);
        return Result.success();
    }

    /**
     * 条件分页查询课程
     *
     * @param name     课程名称
     * @param subject  学科
     * @param target   适用人群
     * @param page     页码
     * @param pageSize 每页记录数
     * @return 分页查询结果
     */
    @GetMapping("/courses")
    public Result listCourses(String name, Integer subject, Integer target, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询课程, 参数: name={}, subject={}, target={}, page={}, pageSize={}", name, subject, target, page, pageSize);
        PageResult<Course> pageResult = courseService.findCoursesByPage(name, subject, target, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询课程
     *
     * @param id 课程ID
     * @return 查询结果
     */
    @GetMapping("/courses/{id}")
    public Result findById(@PathVariable Integer id) {
        log.info("查询课程ID为{}的课程信息", id);
        Course course = courseService.findById(id);
        return Result.success(course);
    }

    /**
     * 修改课程
     *
     * @param course 课程信息
     * @return 统一响应结果
     */
    @LogOperation
    @PutMapping("/courses")
    public Result updateCourse(@RequestBody Course course) {
        log.info("修改课程信息：{}", course);
        courseService.updateById(course);
        return Result.success();
    }

    /**
     * 删除课程
     *
     * @param id 课程ID
     * @return 统一响应结果
     */
    @LogOperation
    @DeleteMapping("/courses/{id}")
    public Result deleteCourse(@PathVariable("id") Integer id) {
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
    public Result listAllCourses() {
        log.info("查询所有课程");
        List<Course> courses = courseService.findAll();
        return Result.success(courses);
    }

    /**
     * 根据学科查询课程，不分页
     *
     * @param subject 学科
     * @return 统一响应结果
     */
    @GetMapping("/courses/subject/{subject}")
    public Result listCoursesBySubject(@PathVariable Integer subject) {
        log.info("查询学科为{}的课程", subject);
        List<Course> courses = courseService.findBySubject(subject);
        return Result.success(courses);
    }
}
