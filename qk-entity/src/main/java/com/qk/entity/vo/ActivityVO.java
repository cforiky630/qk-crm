package com.qk.entity.vo;

import com.qk.entity.po.Activity;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 活动视图对象
 * <p>
 * 只暴露可展示字段：与 {@link Activity} 相比不含内部列 {@code deleted}，因此持久化对象
 * 不必再依赖 {@code @JsonIgnore} 才能不出现在报文里（注解只是额外保险，不再是唯一防线）。
 * <p>
 * 转换刻意用逐个 setter 而不是 {@code BeanUtil.copyProperties}：后者在字段改名或漏抄时
 * 只会静默写入 null，等于悄悄改掉对外报文；逐个 setter 会让编译期直接报错。
 */
@Data
public class ActivityVO {

    /** 活动id */
    private Long id;

    /** 渠道来源，1:线上活动, 2:推广介绍 */
    private Integer channel;

    /** 活动名称 */
    private String name;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 活动简介 */
    private String description;

    /** 活动类型，1:课程折扣, 2:代金券 */
    private Integer type;

    /** 课程折扣，例如 8.0 表示 8 折 */
    private BigDecimal discount;

    /** 课程代金券，单位：元 */
    private Integer voucher;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /**
     * 由持久化对象转换为视图对象
     *
     * @param po 持久化对象，允许为 null（详情接口查不到数据时保持返回 null 的既有行为）
     * @return 视图对象；入参为 null 时返回 null
     */
    public static ActivityVO from(Activity po) {
        if (po == null) {
            return null;
        }
        ActivityVO vo = new ActivityVO();
        vo.setId(po.getId());
        vo.setChannel(po.getChannel());
        vo.setName(po.getName());
        vo.setStartTime(po.getStartTime());
        vo.setEndTime(po.getEndTime());
        vo.setDescription(po.getDescription());
        vo.setType(po.getType());
        vo.setDiscount(po.getDiscount());
        vo.setVoucher(po.getVoucher());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
