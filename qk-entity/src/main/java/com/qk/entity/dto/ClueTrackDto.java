package com.qk.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    private Long id;

    /** 手机号 */
    @Pattern(regexp = ValidationPatterns.PHONE, message = "手机号格式不正确")
    private String phone;

    /** 线索来源 */
    private Integer channel;

    /** 关联活动ID */
    private Long activityId;

    /** 客户姓名 */
    @Size(max = 20, message = "客户姓名长度不能超过 20 个字符")
    private String name;

    /** 性别 */
    private Integer gender;

    /** 年龄 */
    private Integer age;

    /** 微信号 */
    @Size(max = 50, message = "微信号长度不能超过 50 个字符")
    private String wechat;

    /** QQ号 */
    @Size(max = 20, message = "QQ号长度不能超过 20 个字符")
    private String qq;

    /** 意向学科 */
    private Integer subject;

    /** 意向等级 */
    private Integer level;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 本次跟进记录 */
    @Size(max = 100, message = "跟进记录长度不能超过 100 个字符")
    private String record;
}
