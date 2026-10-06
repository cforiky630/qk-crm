package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Course;
import com.qk.entity.vo.CourseVO;
import com.qk.entity.vo.PageResult;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
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
            throw new BusinessException(ErrorCode.COURSE_FIELDS_REQUIRED);
        }
        checkValueRange(course);
        courseMapper.insert(course);
    }

    @Override
    public PageResult<CourseVO> findCoursesByPage(String name, Integer subject, Integer target, Integer page, Integer pageSize) {
        IPage<Course> p = courseMapper.pageCourses(new Page<>(page, pageSize), name, subject, target);
        return new PageResult<>(p.getTotal(), p.getRecords().stream().map(CourseVO::from).toList());
    }

    @Override
    public CourseVO findById(Long id) {
        return CourseVO.from(requireCourse(id));
    }

    @Override
    public void updateById(Course course) {
        requireCourse(course.getId());
        // 修改是部分更新（null 字段不会参与 UPDATE），因此只校验前端实际传了的字段。
        // 注意：NOT NULL 拦不住空字符串，所以名称还要额外判空白
        if (course.getName() != null && StrUtil.isBlank(course.getName())) {
            throw new BusinessException(ErrorCode.COURSE_NAME_REQUIRED);
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
            throw new BusinessException(ErrorCode.COURSE_PRICE_NEGATIVE);
        }
        if (course.getSubject() != null
                && (course.getSubject() < MIN_SUBJECT || course.getSubject() > MAX_SUBJECT)) {
            throw new BusinessException(ErrorCode.COURSE_SUBJECT_RANGE, MIN_SUBJECT, MAX_SUBJECT);
        }
        if (course.getTarget() != null
                && (course.getTarget() < MIN_TARGET || course.getTarget() > MAX_TARGET)) {
            throw new BusinessException(ErrorCode.COURSE_TARGET_RANGE, MIN_TARGET, MAX_TARGET);
        }
    }

    @Override
    public void deleteById(Long id) {
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
            throw new BusinessException(ErrorCode.COURSE_STILL_REFERENCED, String.join("、", refs));
        }

        courseMapper.deleteById(id);
    }

    /**
     * 校验课程是否存在，不存在直接抛业务异常
     *
     * @return 已存在的课程，供调用方复用，避免重复查询
     */
    private Course requireCourse(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.COURSE_ID_REQUIRED);
        }
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    @Override
    public List<CourseVO> findAll() {
        return courseMapper.listAllOrdered().stream().map(CourseVO::from).toList();
    }

    @Override
    public List<CourseVO> findBySubject(Integer subject) {
        return courseMapper.listBySubject(subject).stream().map(CourseVO::from).toList();
    }

}
