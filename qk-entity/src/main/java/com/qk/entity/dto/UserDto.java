package com.qk.entity.dto;

import lombok.Data;

/**
 * 用户查询参数 DTO
 */
@Data
public class UserDto extends PageQuery {
    private String name;        // 姓名
    private Integer status;     // 状态
    private String phone;       // 手机号
    private Long deptId;     // 部门ID
}
