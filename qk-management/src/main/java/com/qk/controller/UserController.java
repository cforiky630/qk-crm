package com.qk.controller;

import cn.hutool.core.bean.BeanUtil;
import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.entity.po.User;
import com.qk.entity.dto.UserSaveDto;
import com.qk.aspect.anno.LogOperation;
import com.qk.entity.dto.UserQueryDto;
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
    public Result<PageResult<UserVO>> getUsers(@Valid UserQueryDto userQueryDto) {
        log.info("条件分页查询用户: {}", userQueryDto);
        PageResult<UserVO> pageResult = userService.listUsers(userQueryDto);
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
        return Result.success(userService.listByRoleLabel(roleLabel));
    }

    /**
     * 根据部门ID查询用户
     */
    @GetMapping("/dept/{deptId}")
    public Result<List<UserVO>> findUsersByDept(@PathVariable Long deptId) {
        log.info("根据部门ID查询用户: {}", deptId);
        return Result.success(userService.listByDeptId(deptId));
    }

    /**
     * 新增用户
     */
    @LogOperation
    @PostMapping
    public Result<Void> addUser(@Valid @RequestBody UserSaveDto userSaveDto) {
        log.info("新增用户: {}", userSaveDto.getUsername());
        userService.saveUser(toUser(userSaveDto));
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
    @PutMapping
    public Result<Void> updateUser(@Valid @RequestBody UserSaveDto userSaveDto) {
        log.info("修改用户: {}", userSaveDto.getUsername());
        userService.updateById(toUser(userSaveDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（password 不在 DTO 中，由 Service 决定）
     * <p>
     * 字段搬运统一用 {@code BeanUtil.copyProperties}：DTO 与实体同名字段一一对应，
     * 这条契约由 {@code BeanNameContractTest} 用反射守住（DTO 的字段在实体里改名或不存在，测试立即失败）。
     */
    private User toUser(UserSaveDto dto) {
        User user = new User();
        BeanUtil.copyProperties(dto, user);
        return user;
    }

    /**
     * 批量删除用户（支持单个与批量）
     */
    @LogOperation
    @DeleteMapping("/{ids}")
    public Result<Void> deleteUsers(@PathVariable List<Long> ids) {
        log.info("批量删除用户: {}", ids);
        userService.deleteUsers(ids);
        return Result.success();
    }
}
