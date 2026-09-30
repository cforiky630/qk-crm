package com.qk;

import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.enums.ClueTrackType;
import com.qk.entity.enums.CodeEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通用码值枚举接口的行为验证
 */
class CodeEnumTest {

    @Test
    void fromCodeFindsEnumByCode() {
        assertEquals(Optional.of(ClueStatus.FALSE_CLUE), CodeEnum.fromCode(ClueStatus.class, 4));
        assertEquals(Optional.of(BusinessStatus.RECYCLED), CodeEnum.fromCode(BusinessStatus.class, 4));
        assertEquals(Optional.of(ClueTrackType.NORMAL), CodeEnum.fromCode(ClueTrackType.class, 1));
    }

    @Test
    void fromCodeReturnsEmptyWhenCodeIsUnknown() {
        assertTrue(CodeEnum.fromCode(ClueStatus.class, 99).isEmpty());
        assertTrue(CodeEnum.fromCode(BusinessStatus.class, -1).isEmpty());
    }

    @Test
    void fromCodeReturnsEmptyWhenCodeOrTypeIsNull() {
        assertTrue(CodeEnum.fromCode(ClueStatus.class, (Integer) null).isEmpty());
        assertTrue(CodeEnum.<Integer, ClueStatus>fromCode(null, 1).isEmpty());
    }

    /** 每个枚举常量的码值都必须能被反查回它自己 */
    @Test
    void everyConstantIsReversible() {
        for (ClueStatus status : ClueStatus.values()) {
            assertEquals(Optional.of(status), CodeEnum.fromCode(ClueStatus.class, status.getCode()));
        }
        for (BusinessStatus status : BusinessStatus.values()) {
            assertEquals(Optional.of(status), CodeEnum.fromCode(BusinessStatus.class, status.getCode()));
        }
        for (ClueTrackType type : ClueTrackType.values()) {
            assertEquals(Optional.of(type), CodeEnum.fromCode(ClueTrackType.class, type.getCode()));
        }
    }

    /** codes() 的顺序与枚举声明顺序一致，且与既有状态码完全对应 */
    @Test
    void codesFollowDeclarationOrder() {
        assertEquals(List.of(1, 2, 3, 4, 5), CodeEnum.codes(ClueStatus.class));
        assertEquals(List.of(1, 2, 3, 4, 5), CodeEnum.codes(BusinessStatus.class));
        assertEquals(List.of(0, 1), CodeEnum.codes(ClueTrackType.class));
    }

    /** 「已关闭状态」这些派生常量必须是真实存在的码值，避免与枚举脱节 */
    @Test
    void closedCodesAreReversible() {
        for (Integer code : ClueStatus.CLOSED_CODES) {
            assertTrue(CodeEnum.fromCode(ClueStatus.class, code).isPresent(), "线索关闭状态码不存在: " + code);
        }
        for (Integer code : BusinessStatus.CLOSED_CODES) {
            assertTrue(CodeEnum.fromCode(BusinessStatus.class, code).isPresent(), "商机关闭状态码不存在: " + code);
        }
    }
}
