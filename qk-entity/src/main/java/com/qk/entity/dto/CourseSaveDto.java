package com.qk.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 课程新增 / 修改入参
 * <p>
 * 取值范围与 Service 层原有的 checkValueRange 保持一致：
 * 学科 1~7，适用人群 1~2，价格不能为负。
 * 与 docs/openapi.yaml 的 CourseBody 一致（subject、name、price、target 必填）。
 */
@Data
public class CourseSaveDto {

    /** 课程ID：修改时必填，新增时忽略 */
    private Integer id;

    @NotNull(message = "学科不能为空")
    @Min(value = 1, message = "学科取值必须在 1~7 之间")
    @Max(value = 7, message = "学科取值必须在 1~7 之间")
    private Integer subject;

    @NotBlank(message = "课程名称不能为空")
    private String name;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private Integer price;

    @NotNull(message = "适用人群不能为空")
    @Min(value = 1, message = "适用人群取值必须在 1~2 之间")
    @Max(value = 2, message = "适用人群取值必须在 1~2 之间")
    private Integer target;

    /** 课程介绍，可空 */
    private String description;
}
