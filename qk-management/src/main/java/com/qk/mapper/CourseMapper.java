package com.qk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 课程数据访问接口
 * <p>
 * 单表查询统一写在本接口的 default 方法里。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 课程分页查询：name 模糊匹配，subject / target 等值匹配
     * <p>
     * 排序与页面原型一致：按更新时间倒序，末尾补 id 保证翻页稳定。
     */
    default IPage<Course> pageCourses(Page<Course> page, String name, Integer subject, Integer target) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Course::getName, name)
                .eq(subject != null, Course::getSubject, subject)
                .eq(target != null, Course::getTarget, target)
                .orderByDesc(Course::getUpdateTime)
                .orderByDesc(Course::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 全部课程，按 id 升序，用于下拉框
     */
    default List<Course> listAllOrdered() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Course::getId);
        return selectList(wrapper);
    }

    /**
     * 某学科下的课程，按 id 升序
     */
    default List<Course> listBySubject(Integer subject) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getSubject, subject).orderByAsc(Course::getId);
        return selectList(wrapper);
    }
}
