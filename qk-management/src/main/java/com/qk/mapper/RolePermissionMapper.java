package com.qk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qk.entity.po.RolePermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 角色-权限映射数据访问接口
 * <p>
 * 纯映射表的读写都很简单，按项目约定写在 default 方法里，Service 不感知查询 DSL。
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 某角色被授予的权限码
     *
     * @param roleId 角色ID
     * @return 权限码列表；未授权时返回空列表
     */
    default List<String> findPermissionCodes(Long roleId) {
        return selectList(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
                .orderByAsc(RolePermission::getPermission))
                .stream()
                .map(RolePermission::getPermission)
                .toList();
    }

    /**
     * 清空某角色的授权
     * <p>
     * 授权是覆盖式更新（先清空再写入），因此这里用物理删除：映射表不做逻辑删除，
     * 也不希望历史行堆积。
     *
     * @return 删除行数
     */
    default int deleteByRoleId(Long roleId) {
        return delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
    }
}
