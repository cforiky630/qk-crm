package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动实体类
 * 对应数据库表 activity
 */
@Data
@TableName("activity")
public class Activity {

    /** 活动id，主键 */
    @TableId
    private Long id;

    /** 渠道来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 活动名称 */
    private String name;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 活动简介 */
    private String description;

    /** 活动类型，1:课程折扣, 2:代金券 */
    private Integer type;

    /** 课程折扣，例如 8.0 表示 8 折；与 voucher 二选一 */
    private BigDecimal discount;

    /** 课程代金券，单位：元；与 discount 二选一 */
    private Integer voucher;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
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
