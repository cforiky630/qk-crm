package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-权限映射
 * <p>
 * 权限码来自 {@link com.qk.entity.enums.Permission}（代码里的稳定契约），
 * 角色是数据；两者通过本表在运行时关联，管理员改授权不需要改代码。
 * <p>
 * 这张表是**纯映射表**：不做逻辑删除，授权采用"先按 role_id 清空、再批量写入"的覆盖式更新，
 * 因此不需要 {@code is_deleted}，也不会有历史行堆积。
 */
@Data
@TableName("role_permission")
public class RolePermission {

    /** 主键 */
    @TableId
    private Long id;

    /** 角色ID，关联 role.id */
    private Long roleId;

    /** 权限码，取值见 com.qk.entity.enums.Permission */
    private String permission;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
