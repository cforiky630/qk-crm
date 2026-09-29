package com.qk.enums;

import lombok.Getter;

/**
 * 线索跟进记录类型
 * <p>
 * 这个字段决定了「这条记录是正常跟进还是伪线索判定」，代码里有分支判断，因此值得枚举化；
 * 像 falseReason、trackStatus 这类只是原样透传给前端的编码，枚举化只会多一层包装，就不做了。
 */
@Getter
public enum ClueTrackType {

    /** 伪线索：跟进过程中判定线索无效 */
    FALSE_CLUE(0),

    /** 正常跟进 */
    NORMAL(1);

    private final int code;

    ClueTrackType(int code) {
        this.code = code;
    }
}
