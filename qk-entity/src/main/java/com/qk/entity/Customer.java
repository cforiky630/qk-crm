package com.qk.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体类
 * 对应数据库表 customer
 */
@Data
@TableName("customer")
public class Customer {

    /** 客户ID，主键 */
    @TableId
    private Integer id;

    /** 手机号 */
    private String phone;

    /** 渠道来源，1:线上活动, 2:推广介绍；选填，允许为空 */
    private Integer channel;

    /** 客户姓名 */
    private String name;

    /** 性别，1:男, 2:女 */
    private Integer gender;

    /** 年龄 */
    private Integer age;

    /** 微信号 */
    private String wechat;

    /** QQ号 */
    private String qq;

    /** 学历，1:高中, 2:中专, 3:大专, 4:本科, 5:硕士, 6:博士, 7:其他 */
    private Integer degree;

    /** 在职情况，1:在职, 0:离职 */
    private Integer jobStatus;

    /** 意向学科 */
    private Integer subject;

    /** 意向课程ID */
    private Integer courseId;

    /** 关联的商机ID，手工新增的客户为空 */
    private Integer businessId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
