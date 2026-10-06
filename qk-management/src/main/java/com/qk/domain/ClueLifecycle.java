package com.qk.domain;

import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.entity.enums.ClueStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 线索状态机
 * <p>
 * 改造前「哪些状态允许分配 / 跟进 / 标伪 / 转商机」的规则散落在
 * {@code ClueServiceImpl} 的 {@code requireActiveClue} 与 {@code assignable} 布尔判断里，
 * 而列表口径（排除哪些状态）又写在 {@code ClueMapper.xml} 的裸数字里，两处容易脱节。
 * <p>
 * 现在规则只有这一处定义：Service 通过 {@link #ensure(Action, Integer)} 校验，
 * Mapper 的查询条件通过 {@link #closedCodes()} / {@link #poolStatus()} 取值，
 * 新增状态或调整流转时只需要改这个类。
 * <p>
 * 动作名（{@link Action#label()}）会直接拼进提示语，取值与既有对外契约逐字一致，
 * 改动会让前端提示变化，因此不允许随意调整。
 */
public final class ClueLifecycle {

    /** 线索的状态流转动作，label 即提示语中出现的动作名 */
    public enum Action {

        /** 分配线索 */
        ASSIGN("分配"),

        /** 跟进线索 */
        TRACK("跟进"),

        /** 标记为伪线索 */
        MARK_FALSE("标记为伪线索"),

        /** 转为商机 */
        CONVERT_TO_BUSINESS("转商机");

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
     * 待分配(1) 与 伪线索(4) 可以重新分配；待跟进(2) 与 跟进中(3) 可以继续跟进、
     * 标伪或转商机；已转商机(5) 不再参与任何流转。
     */
    private static final Map<Action, Set<Integer>> ALLOWED = Map.of(
            Action.ASSIGN, Set.of(ClueStatus.WAIT_ALLOT.getCode(), ClueStatus.FALSE_CLUE.getCode()),
            Action.TRACK, activeCodes(),
            Action.MARK_FALSE, activeCodes(),
            Action.CONVERT_TO_BUSINESS, activeCodes());

    private ClueLifecycle() {
    }

    /** 当前状态是否允许该动作 */
    public static boolean allows(Action action, Integer status) {
        return status != null && ALLOWED.getOrDefault(action, Set.of()).contains(status);
    }

    /**
     * 校验状态流转，不允许时抛业务异常
     * <p>
     * 提示语由 {@link ErrorCode#CLUE_STATUS_NOT_ALLOWED} 与动作名拼出，例如
     * 「该线索当前状态不允许转商机」，与改造前完全一致。
     */
    public static void ensure(Action action, Integer status) {
        if (!allows(action, status)) {
            throw new BusinessException(ErrorCode.CLUE_STATUS_NOT_ALLOWED, action.label());
        }
    }

    /** 可以继续流转的前置状态：待跟进、跟进中 */
    public static Set<Integer> activeCodes() {
        return Set.of(ClueStatus.WAIT_FOLLOW.getCode(), ClueStatus.FOLLOWING.getCode());
    }

    /** 列表默认排除的已关闭状态（伪线索、已转商机） */
    public static List<Integer> closedCodes() {
        return ClueStatus.CLOSED_CODES;
    }

    /** 线索池口径：只放伪线索，供重新分配 */
    public static Integer poolStatus() {
        return ClueStatus.FALSE_CLUE.getCode();
    }
}
