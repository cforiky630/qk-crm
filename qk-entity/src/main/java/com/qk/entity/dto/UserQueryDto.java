package com.qk.entity.dto;

import lombok.Data;

/**
 * 用户列表查询入参
 * <p>
 * 与其它查询入参同名同形（{@code XxxQueryDto}），避免与写入口的 {@code UserSaveDto}、
 * 出参的 {@code UserVO} 混在一起时难以分辨。
 */
@Data
public class UserQueryDto extends PageQuery {

    /** 姓名（模糊匹配） */
    private String name;

    /** 状态：1-正常，0-停用 */
    private Integer status;

    /** 手机号（模糊匹配） */
    private String phone;

    /** 部门ID */
    private Long deptId;
}
