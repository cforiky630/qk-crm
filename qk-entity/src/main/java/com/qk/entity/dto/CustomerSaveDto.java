package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户新增 / 修改入参
 * <p>
 * 不包含 id / businessId：主键由数据库自增，
 * businessId 只在「商机转客户」时由服务端写入，不允许客户端伪造来源商机。
 */
@Data
public class CustomerSaveDto {

    /** 客户ID：修改时必填，新增时忽略 */
    private Integer id;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotNull(message = "客户来源不能为空")
    private Integer channel;

    private String name;

    private Integer gender;

    private Integer age;

    private String wechat;

    private String qq;

    private Integer degree;

    private Integer jobStatus;

    private Integer subject;

    private Integer courseId;
}
