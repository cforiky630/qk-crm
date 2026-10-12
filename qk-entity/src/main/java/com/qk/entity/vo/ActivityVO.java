package com.qk.entity.vo;

import cn.hutool.core.bean.BeanUtil;
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
 * 字段搬运统一用 {@code BeanUtil.copyProperties}：PO 与 VO 的字段名必须一致，这条契约由
 * {@code BeanNameContractTest} 用反射守住——PO 的对外字段一旦在 VO 里改名或缺失，测试立即失败。
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
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
