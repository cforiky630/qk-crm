package com.qk;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类
 * 对应数据库表 role
 */
@Data
@TableName("role")
public class Role {

    /**
     * 角色id，主键
     */
    @TableId
    private Integer id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色标识，全局唯一，权限判断用
     */
    private String label;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
