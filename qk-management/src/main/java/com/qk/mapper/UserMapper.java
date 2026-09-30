package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.User;
import com.qk.entity.dto.UserDto;
import com.qk.entity.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问接口
 * <p>
 * 单表 CRUD 由 BaseMapper 承担；涉及多表 join 的查询写在同名 XML 中。
 * 遵循 MyBatis 官方约定：XML 与接口**同名同包**（src/main/resources/com/qk/mapper/UserMapper.xml），
 * namespace 为接口全限定名，因此不需要配置 mapper-locations。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 动态条件分页查询用户列表（含部门名称、角色名称）
     */
    IPage<UserVO> getUsers(Page<UserVO> page, @Param("userDto") UserDto userDto);

    /**
     * 根据ID查询用户（含部门名称、角色名称）
     */
    UserVO getUserById(@Param("id") Integer id);

    /**
     * 查询所有用户（下拉框使用）
     */
    List<UserVO> listAll();

    /**
     * 根据角色标识查询用户
     * <p>
     * 只返回正常状态（{@code status = 1}）的用户，供「分配线索 / 分配商机」的人员下拉使用：
     * 停用账号无法登录，分配给它等于没有归属人。
     */
    List<UserVO> findByRoleLabel(@Param("roleLabel") String roleLabel);

    /**
     * 根据部门ID查询用户
     */
    List<UserVO> findByDeptId(@Param("deptId") Integer deptId);

    /**
     * 统计某部门下的用户数
     * <p>
     * 供删除部门前的引用校验使用。放在 Mapper 层而不是 Service 里：
     * 这是「数据访问」职责，且名字即语义，Service 不必感知查询怎么构造。
     */
    default long countByDeptId(Integer deptId) {
        Long count = selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, deptId));
        return count == null ? 0L : count;
    }

    /**
     * 统计某角色下的用户数（用于删除角色前的引用校验）
     */
    default long countByRoleId(Integer roleId) {
        Long count = selectCount(new LambdaQueryWrapper<User>().eq(User::getRoleId, roleId));
        return count == null ? 0L : count;
    }

    /**
     * 按用户名精确查询用户（登录使用）
     * <p>
     * username 上有唯一索引，因此最多只会命中一条。
     */
    default User findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
