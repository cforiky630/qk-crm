package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商机实体类
 * 对应数据库表 business
 */
@Data
@TableName("business")
public class Business {

    /** 商机ID，主键 */
    @TableId
    private Long id;

    /** 客户姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 性别，1:男, 2:女 */
    private Integer gender;

    /** 年龄 */
    private Integer age;

    /** 微信号 */
    private String wechat;

    /** QQ号 */
    private String qq;

    /** 意向学科 */
    private Integer subject;

    /** 意向课程，课程ID */
    private Long courseId;

    /** 学历，1:高中, 2:中专, 3:大专, 4:本科, 5:硕士, 6:博士, 7:其他 */
    private Integer degree;

    /** 在职情况，1:在职, 0:离职 */
    private Integer jobStatus;

    /** 渠道来源，1:线上活动, 2:推广介绍；选填，允许为空 */
    private Integer channel;

    /** 备注 */
    private String remark;

    /** 商机状态，1:待分配, 2:待跟进, 3:跟进中, 4:回收, 5:转客户 */
    private Integer status;

    /** 归属人ID，关联用户表主键 */
    private Long userId;

    /** 关联线索ID */
    private Long clueId;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
