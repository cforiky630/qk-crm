package com.qk.entity.vo;

import lombok.Data;

/**
 * 「状态 → 数量」统计投影
 * <p>
 * 首页概览按状态分组统计时使用，避免在 SQL 里逐个状态写 {@code COUNT(IF(status = n, 1, NULL))}：
 * SQL 只负责分组计数，具体哪个状态映射到概览的哪个字段由 Java 侧按枚举决定。
 */
@Data
public class StatusCountVO {

    /** 状态码 */
    private Integer status;

    /** 该状态下的记录数 */
    private Integer total;
}
