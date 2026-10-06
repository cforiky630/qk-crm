package com.qk.entity.dto;

import lombok.Data;

/**
 * 线索列表查询参数封装
 * 对应 /clues?clueId=55&phone=13309091233&status=1&channel=1&assignName=张三&page=1&pageSize=5
 */
@Data
public class ClueQueryDto extends PageQuery {

    /** 线索ID */
    private Long clueId;

    /** 手机号 */
    private String phone;

    /** 线索状态，1:待分配, 2:待跟进, 3:跟进中, 4:伪线索, 5:转为商机 */
    private Integer status;

    /** 线索来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 线索归属人姓名 */
    private String assignName;
}
