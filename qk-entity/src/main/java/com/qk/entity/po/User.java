package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {
    /**
     * id, 主键
     */
    @TableId
    private Long id;

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 密码
     */
    // WRITE_ONLY：请求体中的密码可以正常反序列化进来（登录需要），
    // 但任何情况下都不会被序列化返回给前端，避免密码摘要泄露。
    // 注意：不能用 @JsonIgnore，它是双向忽略的，会导致登录时 password 永远是 null。
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 姓名
     */
    private String name;

    /**
     * 手机号，唯一
     */
    private String phone;

    /**
     * 邮箱，唯一
     */
    private String email;

    /**
     * 性别，1: 男，2: 女
     */
    private Integer gender;

    /**
     * 状态，1: 正常，0: 停用
     */
    private Integer status;

    /**
     * 部门id，关联部门表主键
     */
    private Long deptId;

    /**
     * 角色id，关联角色表主键
     */
    private Long roleId;

    /**
     * 头像url
     */
    private String image;

    /**
     * 备注，50字以内
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
