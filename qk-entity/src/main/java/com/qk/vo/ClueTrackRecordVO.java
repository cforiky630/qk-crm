package com.qk.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索跟进记录视图对象
 */
@Data
public class ClueTrackRecordVO {

    /** 跟进记录ID */
    private Integer id;

    /** 线索ID */
    private Integer clueId;

    /** 跟进人ID */
    private Integer userId;

    /** 意向学科 */
    private Integer subject;

    /** 意向等级 */
    private Integer level;

    /** 跟进记录 */
    private String record;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 跟进类型，1:正常跟进, 0:伪线索 */
    private Integer type;

    /** 伪线索原因 */
    private Integer falseReason;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 跟进人姓名 */
    private String assignName;
}
