package com.qk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商机跟进记录视图对象
 */
@Data
public class BusinessTrackRecordVO {

    /** 跟进记录ID */
    private Long id;

    /** 商机ID */
    private Long businessId;

    /** 跟进人ID */
    private Long userId;

    /** 跟进状态，1:接通, 2:拒绝, 3:无人接听 */
    private Integer trackStatus;

    /** 沟通重点 */
    private String keyItems;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 沟通纪要 */
    private String record;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 跟进人姓名 */
    private String assignName;
}
