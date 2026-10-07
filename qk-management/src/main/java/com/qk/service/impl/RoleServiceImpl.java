package com.qk.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.vo.PageResult;
import com.qk.entity.vo.RoleVO;
import com.qk.entity.vo.PermissionVO;
import com.qk.entity.po.Role;
import com.qk.entity.po.RolePermission;
import com.qk.entity.dto.RoleQueryDto;
import com.qk.entity.enums.Permission;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.mapper.RolePermissionMapper;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;


@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Autowired
    public RoleServiceImpl(RoleMapper roleMapper, UserMapper userMapper,
                           RolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(Role role) {
        role.setId(null);
        roleMapper.insert(role);
    }

    @Override
    public PageResult<RoleVO> findRolesByPage(RoleQueryDto query) {
        IPage<Role> p = roleMapper.pageRoles(new Page<>(query.getPage(), query.getPageSize()),
                query.getName(), query.getLabel());
        return new PageResult<>(p.getTotal(), p.getRecords().stream().map(RoleVO::from).toList());
    }

    @Override
    public RoleVO findById(Long id) {
        return RoleVO.from(requireRole(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Role role) {
        requireRole(role.getId());
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        Role role = requireRole(id);

        // 守卫：超级管理员角色是系统的兜底入口（天然拥有全部权限），不允许删除。
        // 否则把用户都挪走之后删掉它，就再也没人有权限配置角色了。
        if (Boolean.TRUE.equals(role.getSuperRole())) {
            throw new BusinessException(ErrorCode.SUPER_ROLE_CANNOT_DELETE);
        }

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

    @Override
    public List<PermissionVO> findPermissions(Long id) {
        Role role = requireRole(id);
        Set<String> granted = Boolean.TRUE.equals(role.getSuperRole())
                ? Set.copyOf(Permission.allCodes())
                : Set.copyOf(rolePermissionMapper.findPermissionCodes(id));
        return Arrays.stream(Permission.values())
                .filter(permission -> granted.contains(permission.getCode()))
                .map(PermissionVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermissions(Long id, List<String> permissionCodes) {
        Role role = requireRole(id);
        if (Boolean.TRUE.equals(role.getSuperRole())) {
            throw new BusinessException(ErrorCode.SUPER_ROLE_PERMISSION_FIXED);
        }

        // 去重并剔除 null，避免同一权限写多行（唯一索引会报错）与脏值入库
        List<String> codes = permissionCodes == null ? List.of()
                : permissionCodes.stream().filter(Objects::nonNull).distinct().toList();
        List<String> unknown = codes.stream()
                .filter(code -> Permission.ofCode(code).isEmpty())
                .toList();
        if (!unknown.isEmpty()) {
            throw new BusinessException(ErrorCode.PERMISSION_UNKNOWN, String.join("、", unknown));
        }

        // 覆盖式更新：先清空再写入，避免"删掉的权限"因为差分逻辑写错而残留
        rolePermissionMapper.deleteByRoleId(id);
        for (String code : codes) {
            RolePermission grant = new RolePermission();
            grant.setRoleId(id);
            grant.setPermission(code);
            rolePermissionMapper.insert(grant);
        }
    }

}
