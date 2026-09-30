package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Course;
import com.qk.common.PageResult;
import com.qk.common.exception.BusinessException;
import com.qk.mapper.CourseMapper;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.CustomerMapper;
import com.qk.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;


@Service
public class CourseServiceImpl implements CourseService {

    /** 学科取值范围：1~7，分别对应 AI 方向的 7 个学科 */
    private static final int MIN_SUBJECT = 1;
    private static final int MAX_SUBJECT = 7;
    /**
     * 适用人群取值范围：1 小白学员、2 中级程序员、3 初级程序员（原型多出的一档）
     * <p>
     * 库里的 target 只是 tinyint，没有 CHECK 约束，取值靠这里兜底。
     */
    private static final int MIN_TARGET = 1;
    private static final int MAX_TARGET = 3;

    private final CourseMapper courseMapper;
    private final BusinessMapper businessMapper;
    private final CustomerMapper customerMapper;

    @Autowired
    public CourseServiceImpl(CourseMapper courseMapper, BusinessMapper businessMapper, CustomerMapper customerMapper) {
        this.courseMapper = courseMapper;
        this.businessMapper = businessMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    public void addCourse(Course course) {
        course.setId(null);
        // 这四列在库里是 NOT NULL：不在业务层校验的话，前端只能看到 500「系统繁忙」
        if (course.getSubject() == null || StrUtil.isBlank(course.getName())
                || course.getPrice() == null || course.getTarget() == null) {
            throw new BusinessException("课程名称、学科、价格、适用人群均不能为空");
        }
        checkValueRange(course);
        courseMapper.insert(course);
    }

    @Override
    public PageResult<Course> findCoursesByPage(String name, Integer subject, Integer target, Integer page, Integer pageSize) {
        IPage<Course> p = courseMapper.pageCourses(new Page<>(page, pageSize), name, subject, target);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    @Override
    public Course findById(Integer id) {
        return courseMapper.selectById(id);
    }

    @Override
    public void updateById(Course course) {
        requireCourse(course.getId());
        // 修改是部分更新（null 字段不会参与 UPDATE），因此只校验前端实际传了的字段。
        // 注意：NOT NULL 拦不住空字符串，所以名称还要额外判空白
        if (course.getName() != null && StrUtil.isBlank(course.getName())) {
            throw new BusinessException("课程名称不能为空");
        }
        checkValueRange(course);
        courseMapper.updateById(course);
    }

    /**
     * 校验取值范围：库里这几列只是 tinyint/unsigned，没有 CHECK 约束，
     * 不校验的话 subject=99 这类脏数据会被正常写进库。
     */
    private void checkValueRange(Course course) {
        if (course.getPrice() != null && course.getPrice() < 0) {
            throw new BusinessException("价格不能为负数");
        }
        if (course.getSubject() != null
                && (course.getSubject() < MIN_SUBJECT || course.getSubject() > MAX_SUBJECT)) {
            throw new BusinessException("学科取值必须在 " + MIN_SUBJECT + "~" + MAX_SUBJECT + " 之间");
        }
        if (course.getTarget() != null
                && (course.getTarget() < MIN_TARGET || course.getTarget() > MAX_TARGET)) {
            throw new BusinessException("适用人群取值必须在 " + MIN_TARGET + "~" + MAX_TARGET + " 之间");
        }
    }

    @Override
    public void deleteById(Integer id) {
        requireCourse(id);

        // 守卫：仍被商机或客户引用的课程不允许删除。
        // 项目不使用物理外键（见 sql/business.sql、sql/customer.sql 注释），
        // business.course_id / customer.course_id 的引用完整性只能由 Service 层兜底，
        // 否则这些记录的意向课程会变成悬空引用。
        long businessRefs = businessMapper.countByCourseId(id);
        long customerRefs = customerMapper.countByCourseId(id);
        if (businessRefs > 0 || customerRefs > 0) {
            List<String> refs = new ArrayList<>(2);
            if (businessRefs > 0) {
                refs.add(businessRefs + " 条商机");
            }
            if (customerRefs > 0) {
                refs.add(customerRefs + " 条客户");
            }
            throw new BusinessException("该课程已被 " + String.join("、", refs) + "引用，无法删除");
        }

        courseMapper.deleteById(id);
    }

    /**
     * 校验课程是否存在，不存在直接抛业务异常
     */
    private void requireCourse(Integer id) {
        if (id == null) {
            throw new BusinessException("课程ID不能为空");
        }
        if (courseMapper.selectById(id) == null) {
            throw new BusinessException("课程不存在");
        }
    }

    @Override
    public List<Course> findAll() {
        return courseMapper.listAllOrdered();
    }

    @Override
    public List<Course> findBySubject(Integer subject) {
        return courseMapper.listBySubject(subject);
    }

}
