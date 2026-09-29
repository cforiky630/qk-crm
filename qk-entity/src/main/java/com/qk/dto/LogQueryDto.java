package com.qk.dto;

import lombok.Data;

/**
 * 操作日志列表查询参数封装
 * 对应 /logs?operateUserName=林冲&page=1&pageSize=10
 */
@Data
public class LogQueryDto {

    /** 操作人姓名 */
    private String operateUserName;

    /** 页码，默认第一页 */
    private Integer page = 1;

    /** 每页记录数，默认10条 */
    private Integer pageSize = 10;
}
