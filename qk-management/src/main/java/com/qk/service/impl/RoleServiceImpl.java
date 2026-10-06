package com.qk.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.vo.PageResult;
import com.qk.entity.vo.RoleVO;
import com.qk.entity.po.Role;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
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
    public PageResult<RoleVO> findRolesByPage(String name, String label, Integer page, Integer pageSize) {
        IPage<Role> p = roleMapper.pageRoles(new Page<>(page, pageSize), name, label);
        return new PageResult<>(p.getTotal(), p.getRecords().stream().map(RoleVO::from).toList());
    }

    @Override
    public RoleVO findById(Long id) {
        return RoleVO.from(requireRole(id));
    }

    @Override
    public void updateById(Role role) {
        requireRole(role.getId());
        roleMapper.updateById(role);
    }

    @Override
    public void deleteById(Long id) {
        requireRole(id);

        // 守卫：仍被用户引用的角色不允许删除。
        // 项目不使用物理外键（见 sql/user.sql 注释），user.role_id 的引用完整性只能由 Service 层兜底，
        // 否则登录时查不到角色，roleLabel 变成 null，前端菜单会渲染异常。
        long userCount = userMapper.countByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS, userCount);
        }

        roleMapper.deleteById(id);
    }

    /**
     * 校验角色是否存在，不存在直接抛业务异常
     *
     * @return 已存在的角色，供调用方复用，避免重复查询
     */
    private Role requireRole(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.ROLE_ID_REQUIRED);
        }
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    @Override
    public List<RoleVO> findAll() {
        return roleMapper.listAllOrdered().stream().map(RoleVO::from).toList();
    }

}
