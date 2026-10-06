package com.qk.entity.dto;

import lombok.Data;

/**
 * 商机公海池列表查询参数封装
 * 对应 /businesses/pool?businessId=15&name=赵&phone=13344432121&subject=1&page=1&pageSize=10
 */
@Data
public class BusinessPoolDto extends PageQuery {

    /** 商机ID */
    private Long businessId;

    /** 手机号 */
    private String phone;

    /** 客户姓名 */
    private String name;

    /** 意向学科 */
    private Integer subject;
}
