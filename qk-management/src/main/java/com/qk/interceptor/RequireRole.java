package com.qk.interceptor;

import com.qk.entity.enums.RoleLabel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口授权：声明允许访问本接口的角色
 * <p>
 * 由 {@link PermissionInterceptor} 在登录校验之后读取：
 * <ul>
 *   <li>标在方法上优先级最高；</li>
 *   <li>标在控制器类上表示该控制器下所有接口的默认要求；</li>
 *   <li><b>未标注的接口对所有已登录用户开放</b> —— 查询类与新增类刻意保持开放，
 *       避免在补齐权限的同时把前端页面整体挡在门外；需要收紧时加一个注解即可。</li>
 * </ul>
 * 角色标识必须是 {@link RoleLabel} 里的保留值：自定义角色只是数据，不具备接口授权。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色，命中任意一个即通过 */
    RoleLabel[] value();
}
