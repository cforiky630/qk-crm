package com.qk.controller;

import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.entity.po.User;
import com.qk.entity.dto.UserSaveDto;
import com.qk.entity.enums.RoleLabel;
import com.qk.aspect.anno.LogOperation;
import com.qk.interceptor.RequireRole;
import com.qk.entity.dto.UserDto;
import com.qk.service.UserService;
import com.qk.entity.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 * 对应接口文档：3. 接口文档-用户管理
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 条件分页查询用户列表
     */
    @GetMapping
    public Result<PageResult<UserVO>> getUsers(@Valid UserDto userDto) {
        log.info("条件分页查询用户: {}", userDto);
        PageResult<UserVO> pageResult = userService.getUsers(userDto);
        return Result.success(pageResult);
    }

    /**
     * 查询所有用户（下拉框）
     * 注意：字面量路径必须写在 /{id} 之前，否则会被路径变量抢占
     */
    @GetMapping("/list")
    public Result<List<UserVO>> listAllUsers() {
        log.info("查询所有用户");
        List<UserVO> users = userService.listAll();
        return Result.success(users);
    }

    /**
     * 根据角色标识查询用户（只返回正常状态的用户，用于分配线索/商机时的人员下拉）
     */
    @GetMapping("/role/{roleLabel}")
    public Result<List<UserVO>> findUsersByRole(@PathVariable String roleLabel) {
        log.info("根据角色标识查询用户: {}", roleLabel);
        return Result.success(userService.findByRoleLabel(roleLabel));
    }

    /**
     * 根据部门ID查询用户
     */
    @GetMapping("/dept/{deptId}")
    public Result<List<UserVO>> findUsersByDept(@PathVariable Long deptId) {
        log.info("根据部门ID查询用户: {}", deptId);
        return Result.success(userService.findByDeptId(deptId));
    }

    /**
     * 新增用户
     */
    @LogOperation
    @RequireRole(RoleLabel.ADMIN)
    @PostMapping
    public Result<Void> addUser(@Valid @RequestBody UserSaveDto userDto) {
        log.info("新增用户: {}", userDto.getUsername());
        userService.addUser(toUser(userDto));
        return Result.success();
    }

    /**
     * 根据ID查询用户信息（回显）
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        log.info("根据ID查询用户: {}", id);
        return Result.success(userService.getUserById(id));
    }

    /**
     * 修改用户信息
     */
    @LogOperation
    @RequireRole(RoleLabel.ADMIN)
    @PutMapping
    public Result<Void> updateUser(@Valid @RequestBody UserSaveDto userDto) {
        log.info("修改用户: {}", userDto.getUsername());
        userService.updateUser(toUser(userDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（password 不在 DTO 中，由 Service 决定）
     * <p>
     * 逐个字段赋值而不是 {@code BeanUtil.copyProperties}：反射拷贝靠字段名约定，
     * 实体字段改名后会静默停止拷贝（行为悄悄变），这里漏抄则编译期就报错。
     */
    private User toUser(UserSaveDto dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus());
        user.setDeptId(dto.getDeptId());
        user.setRoleId(dto.getRoleId());
        user.setImage(dto.getImage());
        user.setRemark(dto.getRemark());
        return user;
    }

    /**
     * 批量删除用户（支持单个与批量）
     */
    @LogOperation
    @RequireRole(RoleLabel.ADMIN)
    @DeleteMapping("/{ids}")
    public Result<Void> deleteUsers(@PathVariable List<Long> ids) {
        log.info("批量删除用户: {}", ids);
        userService.deleteUsers(ids);
        return Result.success();
    }
}
