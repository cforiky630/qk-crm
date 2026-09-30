package com.qk.entity.dto;

import lombok.Data;

/**
 * 客户列表查询参数封装
 * 对应 /customers?phone=13309091111&name=赵&channel=1&subject=1&page=1&pageSize=10
 */
@Data
public class CustomerQueryDto {

    /** 手机号 */
    private String phone;

    /** 客户姓名 */
    private String name;

    /** 渠道来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 意向学科 */
    private Integer subject;

    /** 页码，默认第一页 */
    private Integer page = 1;

    /** 每页记录数，默认10条 */
    private Integer pageSize = 10;
}
