package com.qk.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Integer id;

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
}
