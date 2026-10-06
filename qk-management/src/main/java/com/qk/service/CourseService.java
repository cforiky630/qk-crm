package com.qk.service;

import com.qk.entity.po.Course;
import com.qk.entity.vo.CourseVO;
import com.qk.entity.vo.PageResult;

import java.util.List;

public interface CourseService {
    /**
     * 新增课程
     *
     * @param course 课程信息
     */
    void addCourse(Course course);

    /**
     * 分页查询课程
     *
     * @param name     课程名称
     * @param subject  学科
     * @param target   适用人群
     * @param page     当前页码
     * @param pageSize 每页显示条数
     * @return 分页结果
     */
    PageResult<CourseVO> findCoursesByPage(String name, Integer subject, Integer target, Integer page, Integer pageSize);

    /**
     * 根据id查询课程
     *
     * @param id 课程id
     * @return 课程信息
     */
    CourseVO findById(Long id);

    /**
     * 根据id修改课程信息
     *
     * @param course 课程信息
     */
    void updateById(Course course);

    /**
     * 根据id删除课程
     *
     * @param id 课程id
     */
    void deleteById(Long id);

    /**
     * 查询所有课程，不分页，用于下拉框
     *
     * @return 课程列表
     */
    List<CourseVO> findAll();

    /**
     * 根据学科查询课程，不分页
     *
     * @param subject 学科
     * @return 课程列表
     */
    List<CourseVO> findBySubject(Integer subject);
}
