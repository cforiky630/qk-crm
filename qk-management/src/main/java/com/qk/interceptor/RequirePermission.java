package com.qk.interceptor;

import com.qk.entity.enums.Permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口需要的权限点
 * <p>
 * 由 {@link PermissionInterceptor} 在登录校验之后读取，与当前用户的权限集合比对，
 * {@code value} 里的权限**全部满足**才放行（要表达"或"就拆成两个权限点）。
 * <p>
 * 权限点来自 {@link Permission}（代码里的稳定契约），与角色数据无关：
 * 角色叫什么、有几个角色、怎么改授权，都不影响这里的声明。
 * <p>
 * <b>每个对外接口都必须标注</b>：授权拦截器对未标注的接口默认拒绝，
 * 并由 {@code PermissionCoverageTest} 在构建期扫出漏标注的接口。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 访问本接口需要的权限 */
    Permission[] value();
}
