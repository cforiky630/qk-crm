package com.qk.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程实体类
 * 对应数据库表 course
 */
@Data
@TableName("course")
public class Course {

    /**
     * 课程id，主键
     */
    @TableId
    private Integer id;

    /**
     * 学科：1-AI智能应用开发(Java)，2-AI大模型开发(Python)，3-AI鸿蒙开发，
     * 4-AI大数据，5-AI嵌入式，6-AI测试，7-AI运维
     */
    private Integer subject;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 价格，单位：元
     */
    private Integer price;

    /**
     * 适用人群：1-小白学员，2-中级程序员，3-初级程序员（对齐页面原型的第三档）
     */
    private Integer target;

    /**
     * 课程介绍
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
