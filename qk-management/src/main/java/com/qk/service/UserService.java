package com.qk.service;

import com.qk.entity.vo.PageResult;
import com.qk.entity.po.User;
import com.qk.entity.dto.UserDto;
import com.qk.entity.vo.UserVO;

import java.util.List;

/**
 * 用户管理Service接口
 */
public interface UserService {

    /**
     * 条件分页查询用户列表（含部门名称、角色名称）
     *
     * @param userDto 查询参数
     * @return 查询结果
     */
    PageResult<UserVO> listUsers(UserDto userDto);

    /**
     * 新增用户，未指定密码时使用默认密码（用户名 + 123 的 MD5 摘要）
     *
     * @param user 用户信息
     */
    void saveUser(User user);

    /**
     * 根据ID查询用户（含部门名称、角色名称，用于修改回显）
     *
     * @param id 用户ID
     * @return 用户信息；不存在时抛业务异常（对外是 code = 0 + 「用户不存在」）
     */
    UserVO getUserById(Long id);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     */
    void updateById(User user);

    /**
     * 批量删除用户（支持单个与批量删除）
     * <p>
     * 删除前会做三道守卫：禁止删除当前登录用户；待删 ID 必须真实存在；
     * 仍被线索、商机或跟进记录引用的用户不允许删除（项目不使用物理外键，
     * 引用完整性由 Service 层保证），此类账号应改为停用。
     *
     * @param ids 待删除的用户ID，不能为空
     */
    void deleteUsers(List<Long> ids);

    /**
     * 查询所有用户，不分页，用于下拉框
     *
     * @return 用户列表
     */
    List<UserVO> listAll();

    /**
     * 根据角色标识查询用户（只返回正常状态的用户，供分配人员下拉使用）
     *
     * @param roleLabel 角色标识
     * @return 用户列表
     */
    List<UserVO> listByRoleLabel(String roleLabel);

    /**
     * 根据部门ID查询用户
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    List<UserVO> listByDeptId(Long deptId);

}
