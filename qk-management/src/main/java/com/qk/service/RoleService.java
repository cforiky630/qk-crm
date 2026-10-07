package com.qk.service;

import com.qk.entity.vo.PageResult;
import com.qk.entity.vo.RoleVO;
import com.qk.entity.vo.PermissionVO;
import com.qk.entity.po.Role;
import com.qk.entity.dto.RoleQueryDto;

import java.util.List;

public interface RoleService {
    /**
     * 新增角色
     *
     * @param role 角色信息
     */
    void addRole(Role role);

    /**
     * 分页查询角色
     *
     * @param query 查询条件（含分页参数）
     * @return 分页结果
     */
    PageResult<RoleVO> findRolesByPage(RoleQueryDto query);

    /**
     * 根据id查询角色
     *
     * @param id 角色id
     * @return 角色信息
     */
    RoleVO findById(Long id);

    /**
     * 根据id修改角色信息
     *
     * @param role 角色信息
     */
    void updateById(Role role);

    /**
     * 根据id删除角色
     *
     * @param id 角色id
     */
    void deleteById(Long id);

    /**
     * 查询所有角色，不分页，用于下拉框
     *
     * @return 角色列表
     */
    List<RoleVO> findAll();

    /**
     * 角色已授予的权限（按权限目录顺序返回）
     * <p>
     * 超级管理员角色返回全量目录：它天然拥有全部权限，界面上"全部勾选且不可编辑"。
     *
     * @param id 角色ID
     * @return 权限列表
     */
    List<PermissionVO> findPermissions(Long id);

    /**
     * 覆盖式配置角色的权限
     *
     * @param id              角色ID
     * @param permissionCodes 权限码列表；传空表示收回该角色的全部权限
     */
    void updatePermissions(Long id, List<String> permissionCodes);
}
