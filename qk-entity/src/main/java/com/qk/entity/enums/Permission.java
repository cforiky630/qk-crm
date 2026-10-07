package com.qk.entity.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 权限点（能力）
 * <p>
 * 这是**代码里的稳定契约**：一个权限点对应一个具体能力，接口用 {@code @RequirePermission} 声明自己需要哪些权限。
 * 权限点与角色彻底解耦：
 * <ul>
 *   <li>角色是数据，管理员可以自由增删改，标签叫什么、有几个角色都无所谓；</li>
 *   <li>「哪个角色拥有哪些权限」存在 {@code role_permission} 表里，运行时由管理员配置；</li>
 *   <li>因此新增/改名/删除角色都不会影响授权，也不会出现"把 admin 改名后系统锁死"。</li>
 * </ul>
 * 权限码（{@link #getCode()}）是落库与对外传输用的稳定标识，形如 {@code clue:track}，
 * 一旦发布就不能改（改了等于撤销所有角色的该权限），只能新增。
 */
@Getter
public enum Permission {

    // ---------- 用户 ----------
    USER_READ("user:read", "查看用户"),
    USER_CREATE("user:create", "新增用户"),
    USER_UPDATE("user:update", "修改用户"),
    USER_DELETE("user:delete", "删除用户"),

    // ---------- 部门 ----------
    DEPT_READ("dept:read", "查看部门"),
    DEPT_CREATE("dept:create", "新增部门"),
    DEPT_UPDATE("dept:update", "修改部门"),
    DEPT_DELETE("dept:delete", "删除部门"),

    // ---------- 角色与授权 ----------
    ROLE_READ("role:read", "查看角色"),
    ROLE_CREATE("role:create", "新增角色"),
    ROLE_UPDATE("role:update", "修改角色"),
    ROLE_DELETE("role:delete", "删除角色"),
    ROLE_GRANT("role:grant", "配置角色权限"),

    // ---------- 课程 ----------
    COURSE_READ("course:read", "查看课程"),
    COURSE_CREATE("course:create", "新增课程"),
    COURSE_UPDATE("course:update", "修改课程"),
    COURSE_DELETE("course:delete", "删除课程"),

    // ---------- 活动 ----------
    ACTIVITY_READ("activity:read", "查看活动"),
    ACTIVITY_CREATE("activity:create", "新增活动"),
    ACTIVITY_UPDATE("activity:update", "修改活动"),
    ACTIVITY_DELETE("activity:delete", "删除活动"),

    // ---------- 线索 ----------
    CLUE_READ("clue:read", "查看线索"),
    CLUE_CREATE("clue:create", "新增线索"),
    CLUE_ASSIGN("clue:assign", "分配线索"),
    CLUE_TRACK("clue:track", "跟进线索"),
    CLUE_MARK_FALSE("clue:mark_false", "标记伪线索"),
    CLUE_CONVERT_BUSINESS("clue:convert_business", "线索转商机"),

    // ---------- 商机 ----------
    BUSINESS_READ("business:read", "查看商机"),
    BUSINESS_CREATE("business:create", "新增商机"),
    BUSINESS_ASSIGN("business:assign", "分配商机"),
    BUSINESS_TRACK("business:track", "跟进商机"),
    BUSINESS_BACK_TO_POOL("business:back_to_pool", "踢回公海"),
    BUSINESS_CONVERT_CUSTOMER("business:convert_customer", "商机转客户"),

    // ---------- 客户 ----------
    CUSTOMER_READ("customer:read", "查看客户"),
    CUSTOMER_CREATE("customer:create", "新增客户"),
    CUSTOMER_UPDATE("customer:update", "修改客户"),

    // ---------- 其它 ----------
    LOG_READ("log:read", "查看操作日志"),
    REPORT_READ("report:read", "查看首页概览"),
    FILE_UPLOAD("file:upload", "上传图片");

    /** 权限码：落库与对外传输用的稳定标识 */
    private final String code;

    /** 权限名称：给管理员配置角色权限时展示 */
    private final String description;

    Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 按权限码反查
     *
     * @param code 权限码，允许为 null
     * @return 匹配到的权限点；码值为空或不存在时返回空
     */
    public static Optional<Permission> ofCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(permission -> permission.code.equals(code)).findFirst();
    }

    /** 全部权限码，供"超级管理员天然拥有全部权限"与授权接口校验使用 */
    public static List<String> allCodes() {
        return Arrays.stream(values()).map(Permission::getCode).toList();
    }
}
