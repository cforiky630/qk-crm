package com.qk.entity.dto;

import lombok.Data;

/**
 * 线索池列表查询参数封装
 * 对应 /clues/pool?clueId=36&phone=15809090000&channel=1&page=1&pageSize=5
 */
@Data
public class CluePoolDto extends PageQuery {

    /** 线索ID */
    private Long clueId;

    /** 手机号 */
    private String phone;

    /** 线索来源，1:线上活动, 2:推广介绍 */
    private Integer channel;
}
