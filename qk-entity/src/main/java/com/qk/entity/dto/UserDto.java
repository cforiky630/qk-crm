package com.qk.entity.dto;

import lombok.Data;

/**
 * 用户查询参数 DTO
 */
@Data
public class UserDto {
    private String name;        // 姓名
    private Integer status;     // 状态
    private String phone;       // 手机号
    private Integer deptId;     // 部门ID
    private Integer page = 1;       // 页码，默认第一页
    private Integer pageSize = 10;  // 每页条数，默认10条
}