package com.qk.entity.dto;

import lombok.Data;

/** 角色列表查询参数，分页参数继承 {@link PageQuery} */
@Data
public class RoleQueryDto extends PageQuery {

    /** 角色名称，模糊匹配 */
    private String name;

    /** 角色标识，模糊匹配 */
    private String label;
}
