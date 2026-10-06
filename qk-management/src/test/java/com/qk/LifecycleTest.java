package com.qk;

import com.qk.common.exception.BusinessException;
import com.qk.domain.BusinessLifecycle;
import com.qk.domain.ClueLifecycle;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机单测
 * <p>
 * 状态流转规则从 Service 的零散布尔判断收敛到 {@link ClueLifecycle} / {@link BusinessLifecycle} 之后，
 * 规则本身可以脱离数据库直接验证：哪些状态允许哪个动作、列表口径排除哪些状态。
 * 这张表就是对外行为的一部分（提示语里会出现动作名），改动必须同步这里的断言。
 */
class LifecycleTest {

    @Test
    void clueAssignableFromWaitAllotAndFalseClueOnly() {
        assertTrue(ClueLifecycle.allows(ClueLifecycle.Action.ASSIGN, ClueStatus.WAIT_ALLOT.getCode()));
        assertTrue(ClueLifecycle.allows(ClueLifecycle.Action.ASSIGN, ClueStatus.FALSE_CLUE.getCode()));
        assertFalse(ClueLifecycle.allows(ClueLifecycle.Action.ASSIGN, ClueStatus.WAIT_FOLLOW.getCode()));
        assertFalse(ClueLifecycle.allows(ClueLifecycle.Action.ASSIGN, ClueStatus.FOLLOWING.getCode()));
        assertFalse(ClueLifecycle.allows(ClueLifecycle.Action.ASSIGN, ClueStatus.CONVERT_BUSINESS.getCode()));
    }

    @Test
    void clueActiveActionsRequireWaitFollowOrFollowing() {
        for (ClueLifecycle.Action action : new ClueLifecycle.Action[]{
                ClueLifecycle.Action.TRACK, ClueLifecycle.Action.MARK_FALSE,
                ClueLifecycle.Action.CONVERT_TO_BUSINESS}) {
            assertTrue(ClueLifecycle.allows(action, ClueStatus.WAIT_FOLLOW.getCode()), action.name());
            assertTrue(ClueLifecycle.allows(action, ClueStatus.FOLLOWING.getCode()), action.name());
            assertFalse(ClueLifecycle.allows(action, ClueStatus.WAIT_ALLOT.getCode()), action.name());
            assertFalse(ClueLifecycle.allows(action, ClueStatus.FALSE_CLUE.getCode()), action.name());
            assertFalse(ClueLifecycle.allows(action, ClueStatus.CONVERT_BUSINESS.getCode()), action.name());
        }
    }

    @Test
    void unknownStatusIsNeverAllowed() {
        assertFalse(ClueLifecycle.allows(ClueLifecycle.Action.TRACK, null));
        assertFalse(ClueLifecycle.allows(ClueLifecycle.Action.TRACK, 99));
    }

    @Test
    void clueFailureMessageMatchesTheExistingContract() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> ClueLifecycle.ensure(ClueLifecycle.Action.CONVERT_TO_BUSINESS, ClueStatus.WAIT_ALLOT.getCode()));
        assertEquals("该线索当前状态不允许转商机", e.getMessage());
    }

    @Test
    void businessAssignableFromWaitAllotAndRecycledOnly() {
        assertTrue(BusinessLifecycle.allows(BusinessLifecycle.Action.ASSIGN, BusinessStatus.WAIT_ALLOT.getCode()));
        assertTrue(BusinessLifecycle.allows(BusinessLifecycle.Action.ASSIGN, BusinessStatus.RECYCLED.getCode()));
        assertFalse(BusinessLifecycle.allows(BusinessLifecycle.Action.ASSIGN, BusinessStatus.WAIT_FOLLOW.getCode()));
        assertFalse(BusinessLifecycle.allows(BusinessLifecycle.Action.ASSIGN,
                BusinessStatus.CONVERT_CUSTOMER.getCode()));
    }

    @Test
    void businessFailureMessageMatchesTheExistingContract() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> BusinessLifecycle.ensure(BusinessLifecycle.Action.BACK_TO_POOL,
                        BusinessStatus.CONVERT_CUSTOMER.getCode()));
        assertEquals("该商机当前状态不允许踢回公海", e.getMessage());
    }

    @Test
    void listAndPoolScopeComeFromTheSameSourceAsTheStateMachine() {
        assertEquals(ClueStatus.CLOSED_CODES, ClueLifecycle.closedCodes());
        assertEquals(BusinessStatus.CLOSED_CODES, BusinessLifecycle.closedCodes());
        assertEquals(ClueStatus.FALSE_CLUE.getCode(), ClueLifecycle.poolStatus());
        assertEquals(BusinessStatus.RECYCLED.getCode(), BusinessLifecycle.poolStatus());
    }
}
