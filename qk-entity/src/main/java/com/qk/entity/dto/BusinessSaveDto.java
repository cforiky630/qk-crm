package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商机新增入参
 * <p>
 * 不包含 id / status / userId / clueId：
 * 新增商机一律为「待分配」且无归属人；clueId 只在「线索转商机」时由服务端写入，
 * 不允许客户端伪造来源线索。
 */
@Data
public class BusinessSaveDto {

    @NotBlank(message = "客户姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private Integer gender;

    private Integer age;

    private String wechat;

    private String qq;

    private Integer subject;

    private Long courseId;

    private Integer degree;

    private Integer jobStatus;

    private Integer channel;

    private String remark;

    private LocalDateTime nextTime;
}
