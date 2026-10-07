package com.qk.controller;

import com.qk.entity.vo.PageResult;
import com.qk.entity.vo.RoleVO;
import com.qk.common.Result;
import com.qk.entity.po.Role;
import com.qk.entity.dto.RoleQueryDto;
import com.qk.entity.dto.RoleSaveDto;
import com.qk.entity.dto.RolePermissionSaveDto;
import com.qk.entity.enums.Permission;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.RoleService;
import com.qk.entity.vo.PermissionVO;
import com.qk.interceptor.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 新增角色
     *
     * @param roleDto 角色信息
     * @return 操作结果
     */
    @LogOperation
    @RequirePermission(Permission.ROLE_CREATE)
    @PostMapping("/roles")
    public Result<Void> addRole(@Valid @RequestBody RoleSaveDto roleDto) {
        log.info("新增角色,参数:{}", roleDto);
        roleService.addRole(toRole(roleDto));
        return Result.success();
    }

    /**
     * 条件分页查询角色
     *
     * @param query 查询条件（含分页参数）
     * @return 分页查询结果
     */
    @RequirePermission(Permission.ROLE_READ)
    @GetMapping("/roles")
    public Result<PageResult<RoleVO>> listRoles(@Valid RoleQueryDto query) {
        log.info("分页查询角色, 参数: {}", query);
        return Result.success(roleService.findRolesByPage(query));
    }

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 查询结果
     */
    @RequirePermission(Permission.ROLE_READ)
    @GetMapping("/roles/{id}")
    public Result<RoleVO> findById(@PathVariable Long id) {
        log.info("查询角色ID为{}的角色信息", id);
        RoleVO role = roleService.findById(id);
        return Result.success(role);
    }

    /**
     * 修改角色
     *
     * @param roleDto 角色信息
     * @return 统一响应结果
     */
    @LogOperation
    @RequirePermission(Permission.ROLE_UPDATE)
    @PutMapping("/roles")
    public Result<Void> updateRole(@Valid @RequestBody RoleSaveDto roleDto) {
        log.info("修改角色信息：{}", roleDto);
        roleService.updateById(toRole(roleDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（Service 不依赖 Web 入参对象）
     * <p>
     * 逐个字段赋值而不是 {@code BeanUtil.copyProperties}：反射拷贝靠字段名约定，
     * 实体字段改名后会静默停止拷贝（行为悄悄变），这里漏抄则编译期就报错。
     */
    private Role toRole(RoleSaveDto dto) {
        Role role = new Role();
        role.setId(dto.getId());
        role.setName(dto.getName());
        role.setLabel(dto.getLabel());
        role.setRemark(dto.getRemark());
        return role;
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 统一响应结果
     */
    @LogOperation
    @RequirePermission(Permission.ROLE_DELETE)
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable("id") Long id) {
        log.info("删除角色：{}", id);
        roleService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询所有角色，不分页，用于下拉框
     *
     * @return 统一响应结果
     */
    @RequirePermission(Permission.ROLE_READ)
    @GetMapping("/roles/list")
    public Result<List<RoleVO>> listAllRoles() {
        log.info("查询所有角色");
        List<RoleVO> roles = roleService.findAll();
        return Result.success(roles);
    }

    /**
     * 查询某个角色已授予的权限
     */
    @RequirePermission(Permission.ROLE_READ)
    @GetMapping("/roles/{id}/permissions")
    public Result<List<PermissionVO>> findRolePermissions(@PathVariable Long id) {
        log.info("查询角色权限: 角色ID={}", id);
        return Result.success(roleService.findPermissions(id));
    }

    /**
     * 覆盖式配置某个角色的权限（超级管理员角色不支持单独调整）
     */
    @LogOperation
    @RequirePermission(Permission.ROLE_GRANT)
    @PutMapping("/roles/{id}/permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id,
                                             @Valid @RequestBody RolePermissionSaveDto permissionDto) {
        log.info("配置角色权限: 角色ID={}, 权限={}", id, permissionDto.getPermissions());
        roleService.updatePermissions(id, permissionDto.getPermissions());
        return Result.success();
    }
}
