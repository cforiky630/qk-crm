package com.qk.controller;

import com.qk.common.PageResult;
import com.qk.common.Result;
import com.qk.entity.User;
import com.qk.aspect.anno.LogOperation;
import com.qk.entity.dto.UserDto;
import com.qk.service.UserService;
import com.qk.entity.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
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
    public Result getUsers(UserDto userDto) {
        log.info("条件分页查询用户: {}", userDto);
        PageResult<UserVO> pageResult = userService.getUsers(userDto);
        return Result.success(pageResult);
    }

    /**
     * 查询所有用户（下拉框）
     * 注意：字面量路径必须写在 /{id} 之前，否则会被路径变量抢占
     */
    @GetMapping("/list")
    public Result listAllUsers() {
        log.info("查询所有用户");
        List<UserVO> users = userService.listAll();
        return Result.success(users);
    }

    /**
     * 根据角色标识查询用户
     */
    @GetMapping("/role/{roleLabel}")
    public Result findUsersByRole(@PathVariable String roleLabel) {
        log.info("根据角色标识查询用户: {}", roleLabel);
        return Result.success(userService.findByRoleLabel(roleLabel));
    }

    /**
     * 根据部门ID查询用户
     */
    @GetMapping("/dept/{deptId}")
    public Result findUsersByDept(@PathVariable Integer deptId) {
        log.info("根据部门ID查询用户: {}", deptId);
        return Result.success(userService.findByDeptId(deptId));
    }

    /**
     * 新增用户
     */
    @LogOperation
    @PostMapping
    public Result addUser(@RequestBody User user) {
        log.info("新增用户: {}", user.getUsername());
        userService.addUser(user);
        return Result.success();
    }

    /**
     * 根据ID查询用户信息（回显）
     */
    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Integer id) {
        log.info("根据ID查询用户: {}", id);
        UserVO userVO = userService.getUserById(id);
        return userVO != null ? Result.success(userVO) : Result.error("用户不存在");
    }

    /**
     * 修改用户信息
     */
    @LogOperation
    @PutMapping
    public Result updateUser(@RequestBody User user) {
        log.info("修改用户: {}", user.getUsername());
        userService.updateUser(user);
        return Result.success();
    }

    /**
     * 批量删除用户（支持单个与批量）
     */
    @LogOperation
    @DeleteMapping("/{ids}")
    public Result deleteUsers(@PathVariable List<Integer> ids) {
        log.info("批量删除用户: {}", ids);
        userService.removeBatchByIds(ids);
        return Result.success();
    }
}
