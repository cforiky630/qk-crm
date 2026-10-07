package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色标识，全局唯一
     * <p>
     * 只用于展示与「按角色查人员」这类业务筛选，**不参与接口授权**：
     * 授权看的是 {@code role_permission} 表里的权限点，因此标签改名不会影响任何人的权限。
     */
    private String label;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 是否超级管理员角色：1-是，0-否
     * <p>
     * 超级管理员天然拥有全部权限（见 {@link com.qk.entity.enums.Permission#allCodes()}），
     * 不参与 {@code role_permission} 表：这样新增权限点时不需要同步补授权数据，
     * 也不会出现"管理员把自己锁在门外"。
     * <p>
     * 数据库列名是 {@code is_super}，用 {@link TableField} 显式映射（原因见下面的 deleted 字段）。
     */
    @TableField("is_super")
    private Boolean superRole;

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

    /**
     * 是否删除：0-未删除，1-已删除
     * <p>
     * 数据库列名是 {@code is_deleted}，Java 字段刻意叫 {@code deleted}：以 is 开头的布尔属性
     * 会被部分序列化 / RPC 框架解析成去掉 is 的属性名（isDeleted → deleted），取值对不上，
     * 因此用 {@link TableField} 把两者显式映射起来。
     * <p>
     * 唯一索引是函数索引 {@code if(is_deleted = 0, 唯一列, NULL)}：已删除行的索引键为 NULL，
     * 而 MySQL 视 NULL 互不相同，所以删除后同名数据可以重新创建，反复删除也不会撞唯一键。
     * <p>
     * 该字段不参与对外 JSON（见 {@link JsonIgnore}）。
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    @JsonIgnore
    private Boolean deleted;
}
