package com.qk.entity.dto;

import lombok.Data;

/** 课程列表查询参数，分页参数继承 {@link PageQuery} */
@Data
public class CourseQueryDto extends PageQuery {

    /** 课程名称，模糊匹配 */
    private String name;

    /** 学科（1~7） */
    private Integer subject;

    /** 适用人群（1~3） */
    private Integer target;
}
