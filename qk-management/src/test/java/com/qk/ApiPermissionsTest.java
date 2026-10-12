package com.qk;

import com.qk.domain.ApiPermissions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接口权限规则表单测
 * <p>
 * 规则表是「谁能调哪个接口」的唯一出处，因此不连数据库、不启动 Spring，直接验证三种结局：
 * 管理员放行、内置的非管理员被拦、以及<b>拿不准的情况一律放行</b>。
 * 最后一条是本方案的核心承诺：规则表只登记「明确只属于管理员」的接口，
 * 其余（未登记的接口、身份不明、用户自建角色）都必须放行，避免出现误拦。
 */
class ApiPermissionsTest {

    private static final String ADMIN = "admin";
    private static final String CLUE_OPERATOR = "clue_operator";
    private static final String BUSINESS_OPERATOR = "business_operator";

    /** 受限清单：基础数据/用户的新增改删 + 线索/商机分配 */
    private static final List<String> RESTRICTED = List.of(
            "POST /users", "PUT /users", "DELETE /users/1",
            "POST /depts", "PUT /depts", "DELETE /depts/1",
            "POST /roles", "PUT /roles", "DELETE /roles/1",
            "POST /courses", "PUT /courses", "DELETE /courses/1",
            "POST /activities", "PUT /activities", "DELETE /activities/1",
            "PUT /clues/assign/1/2", "PUT /businesses/assign/1/2");

    @Test
    void adminCanCallEveryRestrictedEndpoint() {
        for (String endpoint : RESTRICTED) {
            String[] parts = endpoint.split(" ");
            assertTrue(ApiPermissions.isAllowed(ADMIN, parts[0], parts[1]),
                    "管理员应能调用: " + endpoint);
        }
    }

    @Test
    void adminOnlyEndpointsAreBlockedForBuiltinNonAdminRoles() {
        for (String role : List.of(CLUE_OPERATOR, BUSINESS_OPERATOR)) {
            for (String endpoint : RESTRICTED) {
                String[] parts = endpoint.split(" ");
                assertFalse(ApiPermissions.isAllowed(role, parts[0], parts[1]),
                        role + " 不应能调用: " + endpoint);
            }
        }
    }

    /**
     * 拿不准就放行
     * <p>
     * 身份不明（未登录 / 没配角色 / 角色被删）与用户自建的角色都不在规则表能识别的范围内，
     * 此时必须放行 —— 宁可少一层保险，也不能把一个正常角色挡在门外。
     */
    @Test
    void unclearIdentityIsAlwaysAllowed() {
        for (String role : new String[]{null, "", "   ", "custom_role", "sales_lead"}) {
            assertTrue(ApiPermissions.isAllowed(role, "POST", "/users"), "身份不明应放行: " + role);
            assertTrue(ApiPermissions.isAllowed(role, "PUT", "/clues/assign/1/2"), "身份不明应放行: " + role);
        }
    }

    /**
     * 业务动作与查询对所有角色放行
     * <p>
     * 这一组是回归守卫：上次「商机专员新增线索被拦」就是被误拦的典型。
     * 「哪个角色负责哪个业务动作」没有确凿依据，因此不拦。
     */
    @Test
    void businessActionsAndReadsAreAllowedForEveryRole() {
        List<String> open = List.of(
                "GET /users", "GET /users/list", "GET /users/role/clue_operator", "GET /users/1",
                "GET /depts", "GET /depts/list", "GET /roles", "GET /roles/list",
                "GET /courses", "GET /courses/list", "GET /activities", "GET /activities/type/1",
                "GET /clues", "GET /clues/pool", "GET /clues/1",
                "POST /clues", "PUT /clues", "PUT /clues/false/1", "PUT /clues/toBusiness/1",
                "GET /businesses", "GET /businesses/pool", "POST /businesses", "PUT /businesses",
                "PUT /businesses/back/1", "POST /businesses/toCustomer/1",
                "GET /customers", "POST /customers", "PUT /customers",
                "GET /report/overview", "GET /logs", "POST /upload");

        // 这里用 Arrays.asList 而不是 List.of：要覆盖「角色为 null」这种身份不明的请求
        for (String role : Arrays.asList(ADMIN, CLUE_OPERATOR, BUSINESS_OPERATOR, "custom_role", null)) {
            for (String endpoint : open) {
                String[] parts = endpoint.split(" ");
                assertTrue(ApiPermissions.isAllowed(role, parts[0], parts[1]),
                        role + " 应能调用: " + endpoint);
            }
        }
    }

    /** 方法不匹配不算命中：GET /depts 不受「POST /depts 仅管理员」的影响 */
    @Test
    void methodMismatchIsNotRestricted() {
        assertTrue(ApiPermissions.isAllowed(CLUE_OPERATOR, "GET", "/depts"));
        assertTrue(ApiPermissions.isAllowed(CLUE_OPERATOR, "GET", "/users"));
    }

    /** 受限清单必须显式可读：当前只有「仅管理员」一种，且不含任何查询接口 */
    @Test
    void restrictedListIsExplicitAndAdminOnly() {
        List<String> restricted = ApiPermissions.restrictedEndpoints();

        assertFalse(restricted.isEmpty(), "受限清单不能为空，否则这里就不是权限隔离");
        assertTrue(restricted.stream().allMatch(endpoint -> endpoint.endsWith("→ admin")),
                "当前只登记「仅管理员」一种受限规则: " + restricted);
        assertTrue(restricted.contains("POST /users → admin"));
        assertTrue(restricted.stream().noneMatch(endpoint -> endpoint.startsWith("GET ")),
                "查询接口一律放行，不应出现在受限清单里: " + restricted);
    }
}
