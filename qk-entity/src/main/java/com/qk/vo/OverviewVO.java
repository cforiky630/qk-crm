package com.qk.vo;

import lombok.Data;

/**
 * 首页数据概览 VO，封装线索与商机的统计数据
 */
@Data
public class OverviewVO {

    /** 总线索数 */
    private Integer clueTotal;

    /** 待分配线索数 */
    private Integer clueWaitAllot;

    /** 待跟进线索数 */
    private Integer clueWaitFollow;

    /** 跟进中线索数 */
    private Integer clueFollowing;

    /** 伪线索数 */
    private Integer clueFalse;

    /** 转商机线索数 */
    private Integer clueConvertBusiness;

    /** 总商机数 */
    private Integer businessTotal;

    /** 待分配商机数 */
    private Integer businessWaitAllot;

    /** 待跟进商机数 */
    private Integer businessWaitFollow;

    /** 跟进中商机数 */
    private Integer businessFollowing;

    /** 回收商机数 */
    private Integer businessFalse;

    /** 转客户商机数 */
    private Integer businessConvertCustomer;
}
