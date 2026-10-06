package com.qk.service;

import com.qk.entity.vo.PageResult;
import com.qk.entity.vo.RoleVO;
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
}
