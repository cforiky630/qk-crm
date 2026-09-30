package com.qk.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增 / 修改入参
 * <p>
 * 不直接用 {@link com.qk.entity.Dept} 接参的原因：
 * <ol>
 *   <li>实体映射整张表，用它接参等于把所有列都开放为可写（客户端可以传 id、create_time 等）；</li>
 *   <li>校验规则可以声明式写在字段上，由 GlobalExceptionHandler 统一转成 code=0 + 中文提示；</li>
 *   <li>接口契约与表结构解耦：以后给表加内部列不会自动出现在接口里。</li>
 * </ol>
 * 字段与 docs/openapi.yaml 中的 DeptBody 保持一致（name、status 必填）。
 */
@Data
public class DeptSaveDto {

    /** 部门ID：修改时必填，新增时忽略，主键由数据库自增 */
    private Integer id;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 10, message = "部门名称长度不能超过 10 个字符")
    private String name;

    @NotNull(message = "部门状态不能为空")
    @Min(value = 0, message = "部门状态只能是 0（停用）或 1（正常）")
    @Max(value = 1, message = "部门状态只能是 0（停用）或 1（正常）")
    private Integer status;
}
