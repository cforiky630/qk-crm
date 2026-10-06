package com.qk.service;

import com.qk.entity.vo.PageResult;
import com.qk.entity.vo.RoleVO;
import com.qk.entity.po.Role;

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
     * @param name     角色名称
     * @param label    角色标识
     * @param page     当前页码
     * @param pageSize 每页显示条数
     * @return 分页结果
     */
    PageResult<RoleVO> findRolesByPage(String name, String label, Integer page, Integer pageSize);

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
