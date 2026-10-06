package com.qk.entity.dto;

import lombok.Data;

/**
 * 登录入参
 * <p>
 * 取代原先直接用 {@link com.qk.entity.po.User} 接参：实体映射整张表，
 * 拿它当请求体等于把 password、status、roleId 等所有列都开放成可写字段。
 * <p>
 * 刻意<b>不加</b> Bean Validation 注解：用户名或密码为空时仍由 Service 统一返回
 * 「用户名或密码错误」，避免参数校验产生第二套提示文案、改变既有行为。
 */
@Data
public class LoginDto {

    /** 用户名 */
    private String username;

    /** 明文密码 */
    private String password;
}
