package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客户新增 / 修改入参
 * <p>
 * 不包含 id / businessId：主键由数据库自增，
 * businessId 只在「商机转客户」时由服务端写入，不允许客户端伪造来源商机。
 * <p>
 * 渠道来源（channel）按页面原型 3.3「渠道来源，选填」与接口文档（非必须）均为可空，
 * 因此这里不加 @NotNull；对应 customer.channel 允许为 NULL。
 */
@Data
public class CustomerSaveDto {

    /** 客户ID：修改时必填，新增时忽略 */
    private Long id;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 渠道来源，1:线上活动, 2:推广介绍；选填 */
    private Integer channel;

    private String name;

    private Integer gender;

    private Integer age;

    private String wechat;

    private String qq;

    private Integer degree;

    private Integer jobStatus;

    private Integer subject;

    private Long courseId;
}
