package com.qk.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果封装类
 * 用于封装分页查询的结果，包含总记录数和当前页的数据列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor

public class PageResult<T> {
    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页的数据列表
     */
    // 泛型，表示当前页的数据列表的类型, 一般是查询表对应的实体类
    private List<T> rows;
}
