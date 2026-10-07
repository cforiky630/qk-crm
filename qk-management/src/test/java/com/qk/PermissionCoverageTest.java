package com.qk;

import com.qk.interceptor.RequirePermission;
import com.qk.entity.enums.Permission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 权限声明覆盖率
 * <p>
 * 授权拦截器对未声明权限的接口**默认拒绝**，所以"漏标注"不会变成安全漏洞，
 * 但会让接口对所有人不可用。这个测试把这种错误提前到构建期报出来：
 * <ol>
 *   <li>每个对外接口都必须声明 {@link RequirePermission}，只有显式列入白名单的接口（登录）例外；</li>
 *   <li>权限目录里不允许出现没有任何接口使用的权限点（要么是漏标注，要么是该删的权限）。</li>
 * </ol>
 */
class PermissionCoverageTest {

    /** 唯一无需权限的接口：登录（与 WebConfig 的 excludePathPatterns 保持一致） */
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of("LoginController#login");

    /** 对外接口数量下限，防止扫描失效导致测试变成"空跑通过" */
    private static final int MIN_ENDPOINT_COUNT = 40;

    @Test
    void everyEndpointDeclaresPermission() throws Exception {
        int endpointCount = 0;
        StringBuilder violations = new StringBuilder();

        for (Class<?> controller : scanControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isEndpoint(method)) {
                    continue;
                }
                endpointCount++;
                String endpoint = controller.getSimpleName() + "#" + method.getName();
                RequirePermission required = method.getAnnotation(RequirePermission.class);
                if (PUBLIC_ENDPOINTS.contains(endpoint)) {
                    if (required != null) {
                        violations.append(endpoint).append(" 已列入公开白名单，不应再声明权限\n");
                    }
                } else if (required == null || required.value().length == 0) {
                    violations.append(endpoint).append(" 未声明 @RequirePermission\n");
                }
            }
        }

        Assertions.assertTrue(endpointCount >= MIN_ENDPOINT_COUNT,
                "扫描到的接口太少（" + endpointCount + "），测试本身失效");
        Assertions.assertEquals("", violations.toString(),
                "以下接口缺少权限声明（默认拒绝会让它们对所有人不可用）：\n" + violations);
    }

    @Test
    void everyCatalogPermissionIsUsedByAtLeastOneEndpoint() throws Exception {
        Set<Permission> used = EnumSet.noneOf(Permission.class);
        for (Class<?> controller : scanControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                RequirePermission required = method.getAnnotation(RequirePermission.class);
                if (required != null) {
                    used.addAll(Arrays.asList(required.value()));
                }
            }
        }

        List<Permission> unused = Arrays.stream(Permission.values()).filter(p -> !used.contains(p)).toList();
        Assertions.assertEquals(List.of(), unused,
                "权限目录里存在没有任何接口使用的权限点：请确认是漏标注，还是应该从 Permission 里删掉");
    }

    /** 与 LayeringTest 相同的扫描方式：控制器包下的所有组件 */
    private static Set<Class<?>> scanControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        Set<Class<?>> controllers = new java.util.LinkedHashSet<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.qk.controller")) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        return controllers;
    }

    /** 是否是对外接口方法：带任意 Spring Web 映射注解 */
    private static boolean isEndpoint(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if (name.startsWith("org.springframework.web.bind.annotation.") && name.endsWith("Mapping")) {
                return true;
            }
        }
        return false;
    }
}
