package com.qk.entity.dto;

import lombok.Data;

/**
 * 部门列表查询参数
 * <p>
 * 分页参数继承 {@link PageQuery}，因此也能被统一的参数校验覆盖
 * （{@code ?page=} 这类空值不会再变成 500）。
 */
@Data
public class DeptQueryDto extends PageQuery {

    /** 部门名称，模糊匹配 */
    private String name;

    /** 状态：0-停用，1-正常 */
    private Integer status;
}
