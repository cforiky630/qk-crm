package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String phone;

    @NotNull(message = "线索来源不能为空")
    private Integer channel;

    /** 关联活动ID，可空 */
    private Long activityId;

    private String name;

    private Integer gender;

    private Integer age;

    private String wechat;

    private String qq;

    private Integer subject;

    private Integer level;

    private LocalDateTime nextTime;
}
