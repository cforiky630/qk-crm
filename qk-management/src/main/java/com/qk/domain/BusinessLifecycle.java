package com.qk.domain;

import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.entity.enums.BusinessStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商机状态机
 * <p>
 * 与 {@link ClueLifecycle} 同一思路：商机与线索的状态编码语义一一对应
 * （1 待分配 / 2 待跟进 / 3 跟进中 / 4 回收 / 5 转客户），规则集中在这里定义。
 * 动作名取自既有对外提示语，不允许随意调整。
 */
public final class BusinessLifecycle {

    /** 商机的状态流转动作，label 即提示语中出现的动作名 */
    public enum Action {

        /** 分配商机 */
        ASSIGN("分配"),

        /** 跟进商机 */
        TRACK("跟进"),

        /** 踢回公海 */
        BACK_TO_POOL("踢回公海"),

        /** 转为客户 */
        CONVERT_TO_CUSTOMER("转客户");

        private final String label;

        Action(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * 动作 → 允许的前置状态
     * <p>
     * 待分配(1) 与 已回收(4) 可以重新分配；待跟进(2) 与 跟进中(3) 可以继续跟进、
     * 踢回公海或转客户；已转客户(5) 不再参与任何流转。
     */
    private static final Map<Action, Set<Integer>> ALLOWED = Map.of(
            Action.ASSIGN, Set.of(BusinessStatus.WAIT_ALLOT.getCode(), BusinessStatus.RECYCLED.getCode()),
            Action.TRACK, activeCodes(),
            Action.BACK_TO_POOL, activeCodes(),
            Action.CONVERT_TO_CUSTOMER, activeCodes());

    private BusinessLifecycle() {
    }

    /** 当前状态是否允许该动作 */
    public static boolean allows(Action action, Integer status) {
        return status != null && ALLOWED.getOrDefault(action, Set.of()).contains(status);
    }

    /** 校验状态流转，不允许时抛业务异常（提示语与既有契约逐字一致） */
    public static void ensure(Action action, Integer status) {
        if (!allows(action, status)) {
            throw new BusinessException(ErrorCode.BUSINESS_STATUS_NOT_ALLOWED, action.label());
        }
    }

    /** 可以继续流转的前置状态：待跟进、跟进中 */
    public static Set<Integer> activeCodes() {
        return Set.of(BusinessStatus.WAIT_FOLLOW.getCode(), BusinessStatus.FOLLOWING.getCode());
    }

    /** 列表默认排除的已关闭状态（已回收、已转客户） */
    public static List<Integer> closedCodes() {
        return BusinessStatus.CLOSED_CODES;
    }

    /** 公海池口径：只放已回收的商机 */
    public static Integer poolStatus() {
        return BusinessStatus.RECYCLED.getCode();
    }
}
