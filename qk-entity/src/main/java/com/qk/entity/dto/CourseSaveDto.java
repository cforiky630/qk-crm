package com.qk.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程新增 / 修改入参
 * <p>
 * 取值范围与 Service 层原有的 checkValueRange 保持一致：
 * 学科 1~7，适用人群 1~3，价格不能为负。
 * <p>
 * 适用人群放开到 3 个取值是为了对齐页面原型（小白学员、初级程序员、中级程序员）：
 * 接口文档与库注释里只有 1 小白学员、2 中级程序员 两档，原型多出「初级程序员」一档，
 * 后端按超集放开，3 表示初级程序员，已有数据的 1/2 含义不变。
 * 与 docs/openapi.yaml 的 CourseBody 一致（subject、name、price、target 必填）。
 */
@Data
public class CourseSaveDto {

    /** 课程ID：修改时必填，新增时忽略 */
    private Long id;

    @NotNull(message = "学科不能为空")
    @Min(value = 1, message = "学科取值必须在 1~7 之间")
    @Max(value = 7, message = "学科取值必须在 1~7 之间")
    private Integer subject;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 50, message = "课程名称长度不能超过 50 个字符")
    private String name;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private Integer price;

    @NotNull(message = "适用人群不能为空")
    @Min(value = 1, message = "适用人群取值必须在 1~3 之间")
    @Max(value = 3, message = "适用人群取值必须在 1~3 之间")
    private Integer target;

    /** 课程介绍，可空 */
    @Size(max = 255, message = "课程介绍长度不能超过 255 个字符")
    private String description;
}
