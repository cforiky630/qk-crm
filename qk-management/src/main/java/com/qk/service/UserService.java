package com.qk.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.qk.PageResult;
import com.qk.User;
import com.qk.dto.UserDto;
import com.qk.vo.LoginResultVo;
import com.qk.vo.UserVO;

import java.util.List;

/**
 * 用户管理Service接口
 */
public interface UserService extends IService<User> {

    /**
     * 条件分页查询用户列表（含部门名称、角色名称）
     *
     * @param userDto 查询参数
     * @return 查询结果
     */
    PageResult<UserVO> getUsers(UserDto userDto);

    /**
     * 新增用户，未指定密码时使用默认密码（用户名 + 123 的 MD5 摘要）
     *
     * @param user 用户信息
     */
    void addUser(User user);

    /**
     * 根据ID查询用户（含部门名称、角色名称，用于修改回显）
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    UserVO getUserById(Integer id);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     */
    void updateUser(User user);

    /**
     * 查询所有用户，不分页，用于下拉框
     *
     * @return 用户列表
     */
    List<UserVO> listAll();

    /**
     * 根据角色标识查询用户
     *
     * @param roleLabel 角色标识
     * @return 用户列表
     */
    List<UserVO> findByRoleLabel(String roleLabel);

    /**
     * 根据部门ID查询用户
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    List<UserVO> findByDeptId(Integer deptId);

    /**
     * 登录校验，成功返回登录结果（含 JWT 令牌），失败返回 null
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    LoginResultVo login(String username, String password);
}
