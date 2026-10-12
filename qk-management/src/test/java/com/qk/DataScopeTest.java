package com.qk;

import com.qk.domain.DataScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 数据可见范围规则单测
 * <p>
 * 不连库：只验证「谁会被收窄、谁不会」。收窄的判断必须是可预期的，
 * 因为它直接决定用户能不能看见自己的数据。
 */
class DataScopeTest {

    @Test
    void adminIsNotRestricted() {
        assertNull(DataScope.ownerFilterFor("admin", 9L), "管理员看全部数据");
    }

    @Test
    void builtinOperatorsAreRestrictedToThemselves() {
        assertEquals(9L, DataScope.ownerFilterFor("clue_operator", 9L), "线索专员只看归给自己的线索");
        assertEquals(9L, DataScope.ownerFilterFor("business_operator", 9L), "商机专员只看归给自己的商机与客户");
    }

    /**
     * 拿不准就不限制
     * <p>
     * 定时任务（无身份）、用户自建的角色、角色被删导致取不到标识——这些情况都不该把数据藏起来：
     * 藏错了就是「自己的数据看不见」，比多看见几条更难排查。
     */
    @Test
    void unclearIdentityIsNotRestricted() {
        assertNull(DataScope.ownerFilterFor(null, 9L), "拿不到角色时不限制");
        assertNull(DataScope.ownerFilterFor("", 9L), "角色为空时不限制");
        assertNull(DataScope.ownerFilterFor("custom_role_" + 1, 9L), "自建角色不限制");
        assertNull(DataScope.ownerFilterFor("admin", null), "没有用户ID（定时任务）时不限制");
        assertNull(DataScope.ownerFilterFor("business_operator", null), "没有用户ID时不限制");
    }
}
