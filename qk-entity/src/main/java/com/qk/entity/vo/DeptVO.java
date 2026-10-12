package com.qk.entity.vo;

import cn.hutool.core.bean.BeanUtil;
import com.qk.entity.po.Dept;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门视图对象
 * <p>
 * 只暴露可展示字段：与 {@link Dept} 相比不含内部列 {@code deleted}，因此持久化对象
 * 不必再依赖 {@code @JsonIgnore} 才能不出现在报文里（注解只是额外保险，不再是唯一防线）。
 * <p>
 * 字段搬运统一用 {@code BeanUtil.copyProperties}：PO 与 VO 的字段名必须一致，这条契约由
 * {@code BeanNameContractTest} 用反射守住——PO 的对外字段一旦在 VO 里改名或缺失，测试立即失败。
 */
@Data
public class DeptVO {

    /** 部门id */
    private Long id;

    /** 部门名称 */
    private String name;

    /** 状态：0-停用，1-正常 */
    private Integer status;

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
    public static DeptVO from(Dept po) {
        if (po == null) {
            return null;
        }
        DeptVO vo = new DeptVO();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
