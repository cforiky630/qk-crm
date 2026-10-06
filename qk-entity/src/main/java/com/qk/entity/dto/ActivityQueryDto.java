package com.qk.entity.dto;

import lombok.Data;

/** 活动列表查询参数，分页参数继承 {@link PageQuery} */
@Data
public class ActivityQueryDto extends PageQuery {

    /** 渠道来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 活动类型，1:课程折扣, 2:代金券 */
    private Integer type;

    /** 活动状态（1 未开始 / 2 进行中 / 3 已结束），按开始/结束时间推算，为空表示不筛选 */
    private Integer activityStatus;
}
