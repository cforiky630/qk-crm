package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色新增 / 修改入参
 * <p>
 * 只暴露可写字段：id（修改时必填）、name、label、remark。
 * 与 docs/openapi.yaml 的 RoleBody 一致（name、label 必填）。
 */
@Data
public class RoleSaveDto {

    /** 角色ID：修改时必填，新增时忽略，主键由数据库自增 */
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 20, message = "角色名称长度不能超过 20 个字符")
    private String name;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 30, message = "角色标识长度不能超过 30 个字符")
    private String label;

    /** 备注说明，可空 */
    @Size(max = 100, message = "备注说明长度不能超过 100 个字符")
    private String remark;
}
