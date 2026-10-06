package com.qk.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商机跟进请求参数，对应 PUT /businesses
 * <p>
 * 商机跟进 = 更新商机基本信息 + 追加一条商机跟进记录，属于跨两张表的复合操作，
 * 因此单独定义请求对象。其中 trackStatus / keyItems / record 是跟进记录表的字段，
 * 本就不属于商机实体。状态由服务端固定置为「跟进中」。
 */
@Data
public class BusinessTrackDto {

    /** 商机ID，必填 */
    private Long id;

    /** 客户姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 性别 */
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

    /** 在职情况 */
    private Integer jobStatus;

    /** 渠道来源 */
    private Integer channel;

    /** 备注 */
    private String remark;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 跟进状态，1:接通, 2:拒绝, 3:无人接听 */
    private Integer trackStatus;

    /** 沟通重点 */
    private List<String> keyItems;

    /** 沟通纪要 */
    private String record;
}
