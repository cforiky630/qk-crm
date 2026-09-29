package com.qk;

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
/*
 我们定义类的时候也可以定义泛型, 让使用者来确定, 该类可以用于部门分页, 用户分页...等等 , 提供类的通用性
*/
public class PageResult<T> {
    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页的数据列表
     */
    // 这里的T是泛型，表示当前页的数据列表的类型, 一般就是查询表对应的实体类
    private List<T> rows;
}