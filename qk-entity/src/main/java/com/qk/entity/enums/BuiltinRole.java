package com.qk.entity.enums;

import lombok.Getter;

/**
 * 内置角色标识
 * <p>
 * 取值与 {@code role.label}（种子数据）一致，是权限规则里唯一认得的三个身份：
 * {@code admin} 管理员（负责分配）、{@code clue_operator} 线索专员、{@code business_operator} 商机专员。
 * <p>
 * 角色表是用户可维护的主数据，用户可以建出任意 label 的角色。<b>不在本枚举里的 label 一律视为
 * 「身份不明」并按放行处理</b>——拿不准的身份不拦，避免把自建角色的用户挡在门外。
 */
@Getter
public enum BuiltinRole implements CodeEnum<String> {

    /** 管理员：维护基础数据与用户，并负责线索 / 商机的分配 */
    ADMIN("admin"),

    /** 线索专员：跟进线索 */
    CLUE_OPERATOR("clue_operator"),

    /** 商机专员：跟进商机 */
    BUSINESS_OPERATOR("business_operator");

    /** 角色标识：数据库 role.label 的值 */
    private final String value;

    BuiltinRole(String value) {
        this.value = value;
    }

    /** 该 label 是否为内置角色 */
    public static boolean isBuiltin(String label) {
        return CodeEnum.fromCode(BuiltinRole.class, label).isPresent();
    }
}
