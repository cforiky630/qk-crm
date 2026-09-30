package com.qk.entity.enums;

import lombok.Getter;

/**
 * 活动状态（查询用）
 * <p>
 * 与 {@link ClueStatus} / {@link BusinessStatus} 不同，活动状态**不落库**：
 * 库里只有 start_time / end_time，状态是由当前时间与这两个时间比较算出来的。
 * 页面原型的「活动状态」筛选项（未开始、进行中、已结束）就是这三个取值，
 * 后端把它翻译成对 start_time / end_time 的条件，而不是新增一列状态。
 * <p>
 * 编解码与其它状态枚举保持一致：实现 {@link CodeEnum}，用 {@link #getCode()} 取值。
 */
@Getter
public enum ActivityStatus implements CodeEnum<Integer> {

    /** 未开始 —— 开始时间还没到（start_time > now） */
    NOT_STARTED(1),

    /** 进行中 —— 已开始且未结束（start_time <= now <= end_time） */
    IN_PROGRESS(2),

    /** 已结束 —— 结束时间已过（end_time < now） */
    FINISHED(3);

    /** 码值：接口对外用这个数字 */
    private final Integer value;

    ActivityStatus(Integer value) {
        this.value = value;
    }
}
