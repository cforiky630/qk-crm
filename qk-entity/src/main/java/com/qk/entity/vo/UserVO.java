package com.qk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象 (VO)
 * 用于返回给前端，不包含 password 等敏感字段
 */
@Data
public class UserVO {
    private Integer id;
    private String username;
    private String name;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private Integer deptId;
    private Integer roleId;
    private String image;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 扩展字段
    private String deptName;
    private String roleName;
}