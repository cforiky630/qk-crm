package com.qk.entity.dto;

import lombok.Data;

/**
 * 商机列表查询参数封装
 * 对应 /businesses?businessId=21&name=李&phone=138012&status=1&assignName=张三&page=1&pageSize=10
 */
@Data
public class BusinessQueryDto {

    /** 商机ID */
    private Long businessId;

    /** 客户姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 商机状态，1:待分配, 2:待跟进, 3:跟进中, 4:回收, 5:转客户 */
    private Integer status;

    /** 归属人姓名 */
    private String assignName;

    /** 页码，默认第一页 */
    private Integer page = 1;

    /** 每页记录数，默认10条 */
    private Integer pageSize = 10;
}
