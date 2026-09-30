package com.qk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索视图对象
 * <p>
 * 对外返回的线索数据，除线索本身的字段外，还包含多表查询得到的归属人姓名、
 * 活动名称，以及线索详情才有的跟进记录列表。
 */
@Data
public class ClueVO {

    /** 线索ID */
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

    /** 归属人ID */
    private Integer userId;

    /** 线索状态，1:待分配, 2:待跟进, 3:跟进中, 4:伪线索, 5:转为商机 */
    private Integer status;

    /** 意向学科 */
    private Integer subject;

    /** 意向等级 */
    private Integer level;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 归属人姓名 */
    private String assignName;

    /** 活动名称 */
    private String activityName;

    /** 跟进记录列表 */
    private List<ClueTrackRecordVO> trackRecords;
}
