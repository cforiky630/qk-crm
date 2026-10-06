package com.qk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商机视图对象
 * <p>
 * 对外返回的商机数据，除商机本身的字段外，还包含多表查询得到的归属人姓名、
 * 意向课程名称，以及商机详情才有的跟进记录列表。
 */
@Data
public class BusinessVO {

    /** 商机ID */
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

    /** 意向课程ID */
    private Long courseId;

    /** 学历 */
    private Integer degree;

    /** 在职情况，1:在职, 0:离职 */
    private Integer jobStatus;

    /** 渠道来源 */
    private Integer channel;

    /** 备注 */
    private String remark;

    /** 商机状态，1:待分配, 2:待跟进, 3:跟进中, 4:回收, 5:转客户 */
    private Integer status;

    /** 归属人ID */
    private Long userId;

    /** 关联线索ID */
    private Long clueId;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 归属人姓名 */
    private String assignName;

    /** 意向课程名称 */
    private String courseName;

    /** 跟进记录列表 */
    private List<BusinessTrackRecordVO> trackRecords;
}
