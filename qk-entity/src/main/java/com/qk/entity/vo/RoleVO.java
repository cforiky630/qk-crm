package com.qk.entity.vo;

import com.qk.entity.po.Role;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色视图对象
 * <p>
 * 只暴露可展示字段：与 {@link Role} 相比不含内部列 {@code deleted}，因此持久化对象
 * 不必再依赖 {@code @JsonIgnore} 才能不出现在报文里（注解只是额外保险，不再是唯一防线）。
 * <p>
 * 转换刻意用逐个 setter 而不是 {@code BeanUtil.copyProperties}：后者在字段改名或漏抄时
 * 只会静默写入 null，等于悄悄改掉对外报文；逐个 setter 会让编译期直接报错。
 */
@Data
public class RoleVO {

    /** 角色id */
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色标识，全局唯一，权限判断用 */
    private String label;

    /** 备注说明 */
    private String remark;

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
    public static RoleVO from(Role po) {
        if (po == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setLabel(po.getLabel());
        vo.setRemark(po.getRemark());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
