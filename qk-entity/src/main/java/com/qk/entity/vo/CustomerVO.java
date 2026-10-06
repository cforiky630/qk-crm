package com.qk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户视图对象
 */
@Data
public class CustomerVO {

    /** 客户ID */
    private Long id;

    /** 手机号 */
    private String phone;

    /** 渠道来源 */
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

    /** 学历 */
    private Integer degree;

    /** 在职情况，1:在职, 0:离职 */
    private Integer jobStatus;

    /** 意向学科 */
    private Integer subject;

    /** 意向课程ID */
    private Long courseId;

    /** 关联的商机ID */
    private Long businessId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 意向课程名称 */
    private String courseName;
}
