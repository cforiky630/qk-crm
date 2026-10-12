package com.qk.domain;

import com.qk.entity.enums.BuiltinRole;
import com.qk.entity.enums.CodeEnum;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * 接口权限规则
 * <p>
 * 整个后端的权限判断只有这一处定义：下面这张表就是「谁能调用哪个接口」的全部答案，
 * 拦截器只调用 {@link #isAllowed(String, String, String)}，不在任何 Controller 里写零散的检查。
 * <p>
 * <b>默认放行</b>是刻意的：规则表只登记「明确只属于管理员」的接口，凡是没登记的一律放行。
 * 之所以这样定，是因为角色表是用户可维护的主数据、前端又不可改，服务端无法确知每个角色
 * 到底该看到哪些页面；漏拦一个接口只是少一层保险，而误拦一个接口会直接让正常操作失败。
 * 因此判定顺序是：
 * <ol>
 *   <li>身份不明（未登录、用户没配角色、角色被删）→ 放行；</li>
 *   <li>角色标识不是内置的三个（用户自建的角色）→ 放行；</li>
 *   <li>接口没有命中任何规则 → 放行；</li>
 *   <li>命中规则且角色在允许名单里 → 放行，否则拒绝。</li>
 * </ol>
 * <p>
 * 需要收紧某个业务动作（例如「跟进线索只允许线索专员」）时，在 {@link #RULES} 里加一行即可，
 * 不需要改动任何 Controller。
 */
public final class ApiPermissions {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 一条规则：某个接口只允许哪些角色调用 */
    private record Rule(String method, String pathPattern, Set<String> allowedRoles) {

        boolean matches(String method, String path) {
            return this.method.equalsIgnoreCase(method) && MATCHER.match(pathPattern, path);
        }

        @Override
        public String toString() {
            return method + " " + pathPattern + " → " + String.join("/", allowedRoles);
        }
    }

    /** 只有管理员可用的接口 */
    private static Rule adminOnly(String method, String pathPattern) {
        return new Rule(method, pathPattern, Set.of(BuiltinRole.ADMIN.getValue()));
    }

    /**
     * 受限接口清单（仅管理员）
     * <p>
     * 分两类，都是「管理员在维护、其他角色只读」的东西：
     * <ul>
     *   <li>基础数据与用户的新增 / 修改 / 删除：部门、角色、课程、活动、用户；</li>
     *   <li>分配：线索分配、商机分配（页面原型里分配只由管理员操作）。</li>
     * </ul>
     * 查询类接口（各种列表、详情、下拉、概览、操作日志、上传）以及线索 / 商机 / 客户的
     * 业务动作都不在这里，也就是对所有登录角色放行。
     */
    private static final List<Rule> RULES = List.of(
            adminOnly("POST", "/depts"),
            adminOnly("PUT", "/depts"),
            adminOnly("DELETE", "/depts/**"),
            adminOnly("POST", "/roles"),
            adminOnly("PUT", "/roles"),
            adminOnly("DELETE", "/roles/**"),
            adminOnly("POST", "/courses"),
            adminOnly("PUT", "/courses"),
            adminOnly("DELETE", "/courses/**"),
            adminOnly("POST", "/activities"),
            adminOnly("PUT", "/activities"),
            adminOnly("DELETE", "/activities/**"),
            adminOnly("POST", "/users"),
            adminOnly("PUT", "/users"),
            adminOnly("DELETE", "/users/**"),
            adminOnly("PUT", "/clues/assign/**"),
            adminOnly("PUT", "/businesses/assign/**"));

    private ApiPermissions() {
    }

    /**
     * 判断当前角色能否调用该接口
     *
     * @param roleLabel 当前角色标识，可为 null（未登录 / 没配角色）
     * @param method    HTTP 方法，如 GET
     * @param path      应用内路径（不含 context-path），如 {@code /clues/assign/1/2}
     * @return 允许返回 true；只有「内置角色 + 命中规则 + 不在允许名单」这一种情况返回 false
     */
    public static boolean isAllowed(String roleLabel, String method, String path) {
        if (roleLabel == null || roleLabel.isBlank()
                || CodeEnum.fromCode(BuiltinRole.class, roleLabel).isEmpty()) {
            // 身份不明：不确定就不拦
            return true;
        }
        for (Rule rule : RULES) {
            if (rule.matches(method, path)) {
                return rule.allowedRoles().contains(roleLabel);
            }
        }
        // 没有规则：放行
        return true;
    }

    /**
     * 受限接口清单，形如 {@code POST /users}，供测试与文档引用
     * <p>
     * 暴露出来是为了让「哪些接口受限」可以被断言、被打印，而不是只存在于代码阅读里。
     */
    public static List<String> restrictedEndpoints() {
        return RULES.stream().map(Rule::toString).toList();
    }
}
