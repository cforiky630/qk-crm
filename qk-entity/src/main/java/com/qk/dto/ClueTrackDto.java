package com.qk.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索跟进请求参数，对应 PUT /clues
 * <p>
 * 线索跟进 = 更新线索基本信息 + 追加一条跟进记录，属于跨两张表的复合操作，
 * 因此单独定义请求对象，而不是直接拿 Clue 实体接收。
 * 注意：状态由服务端固定置为「跟进中」，归属人只能通过分配接口变更，
 * 因此请求里即使带了 status / userId 也会被忽略。
 */
@Data
public class ClueTrackDto {

    /** 线索ID，必填 */
    private Integer id;

    /** 手机号 */
    private String phone;

    /** 线索来源 */
    private Integer channel;

    /** 关联活动ID */
    private Integer activityId;

    /** 客户姓名 */
    private String name;

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

    /** 意向等级 */
    private Integer level;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 本次跟进记录 */
    private String record;
}
