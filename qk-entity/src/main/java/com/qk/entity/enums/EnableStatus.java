package com.qk.entity.enums;

import lombok.Getter;

/**
 * 通用启用状态
 * <p>
 * dept.status、user.status 等列共用同一套语义：{@code 1} 正常、{@code 0} 停用。
 * 集中定义在这里，避免 Service 层散落裸数字（例如 {@code status == 1}）。
 */
@Getter
public enum EnableStatus implements CodeEnum<Integer> {

    /** 停用 */
    DISABLED(0),

    /** 正常 */
    ENABLED(1);

    /** 码值：数据库与接口对外都用这个数字 */
    private final Integer value;

    EnableStatus(Integer value) {
        this.value = value;
    }
}
