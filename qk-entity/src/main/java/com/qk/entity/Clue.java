package com.qk.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索实体类
 * 对应数据库表 clue
 */
@Data
@TableName("clue")
public class Clue {

    /** 线索ID，主键 */
    @TableId
    private Integer id;

    /** 手机号 */
    private String phone;

    /** 线索来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 关联活动的ID */
    private Integer activityId;

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

    /** 归属人ID，关联用户表主键 */
    private Integer userId;

    /** 线索状态，1:待分配, 2:待跟进, 3:跟进中, 4:伪线索, 5:转为商机 */
    private Integer status;

    /** 意向学科，1~7 分别对应 Java、Python、鸿蒙、大数据、嵌入式、测试、运维 */
    private Integer subject;

    /** 意向等级，1:近期学习, 2:打算学习(考虑中), 3:进行了解, 4:打酱油 */
    private Integer level;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
