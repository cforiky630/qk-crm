package com.qk.enums;

import lombok.Getter;

import java.util.List;

/**
 * 商机状态
 * <p>
 * 与 {@link ClueStatus} 相同的思路：编码集中定义，避免数字散落。
 */
@Getter
public enum BusinessStatus {

    /** 待分配 —— 新增商机（含线索转过来的商机）的初始状态 */
    WAIT_ALLOT(1),

    /** 待跟进 —— 管理员把商机分配给某人之后 */
    WAIT_FOLLOW(2),

    /** 跟进中 —— 跟进人提交过一次跟进记录之后 */
    FOLLOWING(3),

    /** 回收 —— 被踢回公海池 */
    RECYCLED(4),

    /** 转客户 —— 已成单，转入客户列表 */
    CONVERT_CUSTOMER(5);

    private final int code;

    BusinessStatus(int code) {
        this.code = code;
    }

    /**
     * 已关闭的状态：回收的商机在公海池里，转客户的在客户列表里，两者都不出现在商机列表。
     */
    public static final List<Integer> CLOSED_CODES = List.of(RECYCLED.code, CONVERT_CUSTOMER.code);
}
