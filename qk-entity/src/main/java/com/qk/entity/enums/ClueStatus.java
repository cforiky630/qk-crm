package com.qk.entity.enums;

import lombok.Getter;

import java.util.List;

/**
 * 线索状态
 * <p>
 * 状态码集中定义在这里，Service、Mapper、测试统一引用，避免 1~5 这些数字散落在各处。
 * 编码与数据库表注释、首页概览的统计口径保持一致。
 */
@Getter
public enum ClueStatus implements CodeEnum<Integer> {

    /** 待分配 —— 新增线索的初始状态 */
    WAIT_ALLOT(1),

    /** 待跟进 —— 管理员把线索分配给某人之后 */
    WAIT_FOLLOW(2),

    /** 跟进中 —— 跟进人提交过一次跟进记录之后 */
    FOLLOWING(3),

    /** 伪线索 —— 确认无效，不再跟进 */
    FALSE_CLUE(4),

    /** 转为商机 —— 有购买意向，已生成商机 */
    CONVERT_BUSINESS(5);

    /** 码值：数据库与接口对外都用这个数字 */
    private final Integer value;

    ClueStatus(Integer value) {
        this.value = value;
    }

    /**
     * 已关闭的状态：这两类线索不会再出现在线索列表里，
     * 伪线索可以在线索池中重新分配，已转商机的则由商机模块接管。
     */
    public static final List<Integer> CLOSED_CODES = List.of(FALSE_CLUE.value, CONVERT_BUSINESS.value);
}
