package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索新增入参
 * <p>
 * 不包含 id / status / userId：这三项由服务端决定
 * （新增线索一律为「待分配」状态，且初始无归属人）。
 */
@Data
public class ClueSaveDto {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "线索来源不能为空")
    private Integer channel;

    /** 关联活动ID，可空 */
    private Long activityId;

    @Size(max = 20, message = "客户姓名长度不能超过 20 个字符")
    private String name;

    private Integer gender;

    private Integer age;

    @Size(max = 50, message = "微信号长度不能超过 50 个字符")
    private String wechat;

    @Size(max = 20, message = "QQ号长度不能超过 20 个字符")
    private String qq;

    private Integer subject;

    private Integer level;

    private LocalDateTime nextTime;
}
