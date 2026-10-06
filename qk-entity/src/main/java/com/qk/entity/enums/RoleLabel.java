package com.qk.entity.enums;

import lombok.Getter;

/**
 * 系统保留的角色标识
 * <p>
 * 角色本身是数据（管理员可以自由新建角色），但**接口授权**必须依赖稳定的标识，
 * 因此这三个标签是保留值：它们由建表脚本内置，不能通过接口改名，也不随业务数据变化。
 * 其它自定义角色只是数据，不具备任何接口授权（只能看查询类接口）。
 * <p>
 * 标签取值与 {@code sql/role.sql} 的内置数据、{@code GET /users/role/{roleLabel}} 的入参一致。
 */
@Getter
public enum RoleLabel {

    /** 管理员：拥有全部接口权限（分配线索/商机、维护基础数据与用户） */
    ADMIN("admin"),

    /** 线索专员：线索的跟进、标伪、转商机 */
    CLUE_OPERATOR("clue_operator"),

    /** 商机专员：商机的跟进、踢回公海、转客户 */
    BUSINESS_OPERATOR("business_operator");

    /** 角色标识：数据库 role.label 与接口对外都用它 */
    private final String label;

    RoleLabel(String label) {
        this.label = label;
    }
}
