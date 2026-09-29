package com.qk.dto;

import lombok.Data;

/**
 * 线索池列表查询参数封装
 * 对应 /clues/pool?clueId=36&phone=15809090000&channel=1&page=1&pageSize=5
 */
@Data
public class CluePoolDto {

    /** 线索ID */
    private Integer clueId;

    /** 手机号 */
    private String phone;

    /** 线索来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 页码，默认第一页 */
    private Integer page = 1;

    /** 每页记录数，默认10条 */
    private Integer pageSize = 10;
}
