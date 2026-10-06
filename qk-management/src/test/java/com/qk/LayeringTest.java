package com.qk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 分层约束测试：持久化对象不得出现在接口契约里
 * <p>
 * 判定标准是「对外方法（带 Spring 映射注解的方法）的返回值与参数类型，及其全部泛型实参」，
 * 而不是「controller 有没有 import po」——控制器仍要把请求 DTO 映射成实体再交给 Service，
 * 这是本项目刻意的约定（见 README「项目约定」），所以内部使用 po 是允许的。
 * <p>
 * 之所以需要这条测试：PO 上虽然标了 {@code @JsonIgnore} 之类的注解，但那只在注解还在时才生效。
 * 一旦有人换写 VO、换全局 Jackson 配置或做映射重构，内部列就会静默进入报文，
 * 而字段级断言很难发现「多出来一个字段」。这里从类型层面把它堵死。
 */
class LayeringTest {

    /** 持久化对象所在包：接口契约里不允许出现这个包的任何类型 */
    private static final String PO_PACKAGE = "com.qk.entity.po";

    /** 控制器所在包 */
    private static final String CONTROLLER_PACKAGE = "com.qk.controller";

    @Test
    void controllerEndpointsDoNotExposePersistenceObjects() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(CONTROLLER_PACKAGE);

        Assertions.assertFalse(candidates.isEmpty(), "未扫描到任何控制器，测试本身失效");

        int endpointCount = 0;
        StringBuilder violations = new StringBuilder();
        for (BeanDefinition candidate : candidates) {
            Class<?> controller = Class.forName(candidate.getBeanClassName());
            for (Method method : controller.getDeclaredMethods()) {
                if (!isEndpoint(method)) {
                    continue;
                }
                endpointCount++;
                Set<Class<?>> types = new LinkedHashSet<>();
                collectTypes(method.getGenericReturnType(), types);
                for (Type parameterType : method.getGenericParameterTypes()) {
                    collectTypes(parameterType, types);
                }
                for (Class<?> type : types) {
                    if (type.getPackageName().equals(PO_PACKAGE)) {
                        violations.append(controller.getSimpleName()).append('#').append(method.getName())
                                .append(" 暴露了 ").append(type.getName()).append('\n');
                    }
                }
            }
        }

        Assertions.assertTrue(endpointCount > 30, "扫描到的接口方法太少（" + endpointCount + "），测试本身失效");
        Assertions.assertEquals("", violations.toString(),
                "对外接口不得出现持久化对象，请改用 entity.vo 下的视图对象：\n" + violations);
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

    /**
     * Web 层（控制器、拦截器）不得直接依赖数据访问层
     * <p>
     * 「令牌对应的账号是否可用」这类判断属于认证规则，应该在 Service 里；
     * 一旦 Web 层自己注入 Mapper，就会绕过业务规则、并且每个请求都可能多查一次库而不自知。
     * 登录拦截器曾经就是这种写法（直接注入 UserMapper），因此这里从依赖层面把它堵住。
     */
    @Test
    void webLayerDoesNotDependOnMappers() throws Exception {
        Set<BeanDefinition> candidates = new LinkedHashSet<>();
        for (String pkg : new String[]{"com.qk.controller", "com.qk.interceptor"}) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));
            candidates.addAll(scanner.findCandidateComponents(pkg));
        }

        Assertions.assertTrue(candidates.size() >= 10,
                "扫描到的 Web 层组件太少（" + candidates.size() + "），测试本身失效");

        String mapperPackage = "com.qk.mapper";
        StringBuilder violations = new StringBuilder();
        for (BeanDefinition candidate : candidates) {
            Class<?> component = Class.forName(candidate.getBeanClassName());
            for (Field field : component.getDeclaredFields()) {
                if (field.getType().getPackageName().equals(mapperPackage)) {
                    violations.append(component.getSimpleName()).append('#').append(field.getName()).append('\n');
                }
            }
            for (Constructor<?> constructor : component.getDeclaredConstructors()) {
                for (Class<?> parameterType : constructor.getParameterTypes()) {
                    if (parameterType.getPackageName().equals(mapperPackage)) {
                        violations.append(component.getSimpleName()).append("(构造参数 ").append(parameterType.getSimpleName()).append(")\n");
                    }
                }
            }
        }

        Assertions.assertEquals("", violations.toString(),
                "Web 层不得直接依赖 Mapper，请改为依赖 Service：\n" + violations);
    }

    /** 递归收集一个类型及其全部泛型实参、数组元素、通配符上下界 */
    private static void collectTypes(Type type, Set<Class<?>> sink) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                collectTypes(clazz.getComponentType(), sink);
            } else if (!clazz.isPrimitive()) {
                sink.add(clazz);
            }
            return;
        }
        if (type instanceof ParameterizedType parameterized) {
            collectTypes(parameterized.getRawType(), sink);
            for (Type argument : parameterized.getActualTypeArguments()) {
                collectTypes(argument, sink);
            }
            return;
        }
        if (type instanceof GenericArrayType arrayType) {
            collectTypes(arrayType.getGenericComponentType(), sink);
            return;
        }
        if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) {
                collectTypes(bound, sink);
            }
            for (Type bound : wildcard.getLowerBounds()) {
                collectTypes(bound, sink);
            }
        }
    }
}
