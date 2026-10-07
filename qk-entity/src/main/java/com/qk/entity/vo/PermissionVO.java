package com.qk.entity.vo;

import com.qk.entity.enums.Permission;
import lombok.Data;

/**
 * 权限点视图对象
 * <p>
 * 管理员配置角色权限时，界面先取全量目录（{@code GET /permissions}）渲染勾选项，
 * 再取某个角色已授予的权限（{@code GET /roles/{id}/permissions}）回显。
 */
@Data
public class PermissionVO {

    /** 权限码，如 {@code clue:track} */
    private String code;

    /** 权限名称，如「跟进线索」 */
    private String description;

    public static PermissionVO from(Permission permission) {
        if (permission == null) {
            return null;
        }
        PermissionVO vo = new PermissionVO();
        vo.setCode(permission.getCode());
        vo.setDescription(permission.getDescription());
        return vo;
    }
}
