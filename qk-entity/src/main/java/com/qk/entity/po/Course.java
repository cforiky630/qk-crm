package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private Long id;

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

    /**
     * 是否删除：0-未删除，1-已删除
     * <p>
     * 数据库列名是 {@code is_deleted}，Java 字段刻意叫 {@code deleted}：以 is 开头的布尔属性
     * 会被部分序列化 / RPC 框架解析成去掉 is 的属性名（isDeleted → deleted），取值对不上，
     * 因此用 {@link TableField} 把两者显式映射起来。
     * <p>
     * 唯一索引是函数索引 {@code if(is_deleted = 0, 唯一列, NULL)}：已删除行的索引键为 NULL，
     * 而 MySQL 视 NULL 互不相同，所以删除后同名数据可以重新创建，反复删除也不会撞唯一键。
     * <p>
     * 该字段不参与对外 JSON（见 {@link JsonIgnore}）。
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    @JsonIgnore
    private Boolean deleted;
}
