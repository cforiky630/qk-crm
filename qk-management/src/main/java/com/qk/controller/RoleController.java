package com.qk.controller;

import com.qk.common.PageResult;
import com.qk.common.Result;
import com.qk.entity.Role;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.RoleService;
import lombok.extern.slf4j.Slf4j;
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
     * @param role 角色信息
     * @return 操作结果
     */
    @LogOperation
    @PostMapping("/roles")
    public Result addRole(@RequestBody Role role) {
        log.info("新增角色,参数:{}", role);
        roleService.addRole(role);
        return Result.success();
    }

    /**
     * 条件分页查询角色
     *
     * @param name     角色名称
     * @param label    角色标识
     * @param page     页码
     * @param pageSize 每页记录数
     * @return 分页查询结果
     */
    @GetMapping("/roles")
    public Result listRoles(String name, String label, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询角色, 参数: name={}, label={}, page={}, pageSize={}", name, label, page, pageSize);
        PageResult<Role> pageResult = roleService.findRolesByPage(name, label, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 查询结果
     */
    @GetMapping("/roles/{id}")
    public Result findById(@PathVariable Integer id) {
        log.info("查询角色ID为{}的角色信息", id);
        Role role = roleService.findById(id);
        return Result.success(role);
    }

    /**
     * 修改角色
     *
     * @param role 角色信息
     * @return 统一响应结果
     */
    @LogOperation
    @PutMapping("/roles")
    public Result updateRole(@RequestBody Role role) {
        log.info("修改角色信息：{}", role);
        roleService.updateById(role);
        return Result.success();
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 统一响应结果
     */
    @LogOperation
    @DeleteMapping("/roles/{id}")
    public Result deleteRole(@PathVariable("id") Integer id) {
        log.info("删除角色：{}", id);
        roleService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询所有角色，不分页，用于下拉框
     *
     * @return 统一响应结果
     */
    @GetMapping("/roles/list")
    public Result listAllRoles() {
        log.info("查询所有角色");
        List<Role> roles = roleService.findAll();
        return Result.success(roles);
    }
}
