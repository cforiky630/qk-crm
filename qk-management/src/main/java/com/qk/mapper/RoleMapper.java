package com.qk.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 角色数据访问接口
 * <p>
 * 单表查询统一写在本接口的 default 方法里。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 角色分页查询：name 与 label 均为模糊匹配，按 id 升序
     */
    default IPage<Role> pageRoles(Page<Role> page, String name, String label) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), Role::getName, name)
                .like(StrUtil.isNotBlank(label), Role::getLabel, label)
                .orderByAsc(Role::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 全部角色，按 id 升序，用于下拉框
     */
    default List<Role> listAllOrdered() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Role::getId);
        return selectList(wrapper);
    }
}
