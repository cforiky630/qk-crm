package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动新增 / 修改入参
 * <p>
 * 只暴露可写字段；create_time / update_time 由服务端自动填充，不在入参里。
 * 与 docs/openapi.yaml 的 ActivityBody 一致（name、channel、type、startTime、endTime 必填）。
 */
@Data
public class ActivitySaveDto {

    /** 活动ID：修改时必填，新增时忽略 */
    private Long id;

    @NotNull(message = "活动渠道不能为空")
    private Integer channel;

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 50, message = "活动名称长度不能超过 50 个字符")
    private String name;

    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    /** 活动简介，可空 */
    @Size(max = 255, message = "活动简介长度不能超过 255 个字符")
    private String description;

    @NotNull(message = "活动类型不能为空")
    private Integer type;

    /** 折扣，可空 */
    private BigDecimal discount;

    /** 代金券金额，可空 */
    private Integer voucher;
}
