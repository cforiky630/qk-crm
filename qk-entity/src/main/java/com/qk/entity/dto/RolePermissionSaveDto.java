package com.qk.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 配置角色权限的入参
 * <p>
 * <b>覆盖式</b>语义：以本次提交的列表为准——传空数组表示收回该角色的全部权限。
 * 权限码是否合法由 Service 校验（要与权限目录比对，注解做不了这件事）。
 */
@Data
public class RolePermissionSaveDto {

    /** 权限码列表，取值见 com.qk.entity.enums.Permission */
    @NotNull(message = "权限列表不能为空")
    private List<String> permissions;
}
