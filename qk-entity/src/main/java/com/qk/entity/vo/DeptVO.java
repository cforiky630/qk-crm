package com.qk.entity.vo;

import com.qk.entity.po.Dept;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门视图对象
 * <p>
 * 只暴露可展示字段：与 {@link Dept} 相比不含内部列 {@code deleted}，因此持久化对象
 * 不必再依赖 {@code @JsonIgnore} 才能不出现在报文里（注解只是额外保险，不再是唯一防线）。
 * <p>
 * 转换刻意用逐个 setter 而不是 {@code BeanUtil.copyProperties}：后者在字段改名或漏抄时
 * 只会静默写入 null，等于悄悄改掉对外报文；逐个 setter 会让编译期直接报错。
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
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
