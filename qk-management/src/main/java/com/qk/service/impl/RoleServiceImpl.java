package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.common.PageResult;
import com.qk.entity.Role;
import com.qk.common.exception.BusinessException;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;

    @Autowired
    public RoleServiceImpl(RoleMapper roleMapper, UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void addRole(Role role) {
        role.setId(null);
        roleMapper.insert(role);
    }

    @Override
    public PageResult<Role> findRolesByPage(String name, String label, Integer page, Integer pageSize) {
        // 1.设置分页参数
        Page<Role> p = new Page<>(page, pageSize);

        // 2. 设置条件参数，name 和 label 均为模糊查询
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), Role::getName, name)
                .like(StrUtil.isNotBlank(label), Role::getLabel, label)
                .orderByAsc(Role::getId); // 固定排序，避免分页时记录重复或丢失

        // 3. 执行分页查询
        p = roleMapper.selectPage(p, wrapper);

        // 4. 返回结果
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    @Override
    public Role findById(Integer id) {
        return roleMapper.selectById(id);
    }

    @Override
    public void updateById(Role role) {
        requireRole(role.getId());
        roleMapper.updateById(role);
    }

    @Override
    public void deleteById(Integer id) {
        requireRole(id);

        // 守卫：仍被用户引用的角色不允许删除。
        // 项目不使用物理外键（见 sql/user.sql 注释），user.role_id 的引用完整性只能由 Service 层兜底，
        // 否则登录时查不到角色，roleLabel 变成 null，前端菜单会渲染异常。
        long userCount = userMapper.countByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException("该角色下还有 " + userCount + " 名用户，无法删除");
        }

        roleMapper.deleteById(id);
    }

    /**
     * 校验角色是否存在，不存在直接抛业务异常
     */
    private void requireRole(Integer id) {
        if (id == null) {
            throw new BusinessException("角色ID不能为空");
        }
        if (roleMapper.selectById(id) == null) {
            throw new BusinessException("角色不存在");
        }
    }

    @Override
    public List<Role> findAll() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Role::getId);
        return roleMapper.selectList(wrapper);
    }

}
