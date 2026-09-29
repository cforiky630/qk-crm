package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.User;
import com.qk.dto.UserDto;
import com.qk.vo.UserVO;
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
     */
    List<UserVO> findByRoleLabel(@Param("roleLabel") String roleLabel);

    /**
     * 根据部门ID查询用户
     */
    List<UserVO> findByDeptId(@Param("deptId") Integer deptId);
}
