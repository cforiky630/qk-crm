package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.qk.entity.vo.PageResult;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.entity.dto.UserDto;
import com.qk.entity.enums.EnableStatus;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.BusinessTrackRecordMapper;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.ClueTrackRecordMapper;
import com.qk.service.UserService;
import com.qk.common.util.UserHolder;
import com.qk.common.util.JwtUtil;
import com.qk.entity.vo.LoginResultVO;
import com.qk.entity.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final JwtUtil jwtUtil;
    private final ClueMapper clueMapper;
    private final BusinessMapper businessMapper;
    private final ClueTrackRecordMapper clueTrackRecordMapper;
    private final BusinessTrackRecordMapper businessTrackRecordMapper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper, JwtUtil jwtUtil,
                           ClueMapper clueMapper, BusinessMapper businessMapper,
                           ClueTrackRecordMapper clueTrackRecordMapper,
                           BusinessTrackRecordMapper businessTrackRecordMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.jwtUtil = jwtUtil;
        this.clueMapper = clueMapper;
        this.businessMapper = businessMapper;
        this.clueTrackRecordMapper = clueTrackRecordMapper;
        this.businessTrackRecordMapper = businessTrackRecordMapper;
    }

    /**
     * 条件分页查询用户列表
     *
     * @param userDto 查询参数
     * @return 分页查询结果
     */
    @Override
    public PageResult<UserVO> getUsers(UserDto userDto) {
        // 1.设置分页条件
        Page<UserVO> p = new Page<>(userDto.getPage(), userDto.getPageSize());

        // 2. 执行分页查询
        IPage<UserVO> userPage = userMapper.getUsers(p, userDto);

        // 3. 封装返回结果
        return new PageResult<>(userPage.getTotal(), userPage.getRecords());
    }

    @Override
    public void addUser(User user) {
        // 主键由数据库自增，禁止客户端指定
        user.setId(null);
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getName())
                || StrUtil.isBlank(user.getPhone()) || StrUtil.isBlank(user.getEmail())) {
            throw new BusinessException(ErrorCode.USER_FIELDS_REQUIRED);
        }
        // 接口文档中新增用户不接收密码，这里统一使用默认密码：用户名 + 123，落库前做 MD5 加密
        if (StrUtil.isBlank(user.getPassword())) {
            user.setPassword(DigestUtil.md5Hex(user.getUsername() + "123"));
        } else {
            user.setPassword(DigestUtil.md5Hex(user.getUsername() + user.getPassword()));
        }
        userMapper.insert(user);
    }

    @Override
    public UserVO getUserById(Long id) {
        return userMapper.getUserById(id);
    }

    @Override
    public void updateUser(User user) {
        if (user.getId() == null || getById(user.getId()) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 守卫：不允许把当前登录用户自己停用。
        // 登录会拒绝 status=0 的账号，一旦把自己停用，就再也进不来了（与「不能删除自己」同类）。
        Long currentUserId = UserHolder.getCurrentUser();
        if (currentUserId != null && currentUserId.equals(user.getId())
                && EnableStatus.DISABLED.getCode().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_CANNOT_DISABLE_SELF);
        }

        // 接口文档中修改用户不包含密码字段，密码修改走单独的重置流程，
        // 因此这里显式置空，避免前端回传的密码摘要被二次加密后写坏数据
        user.setPassword(null);
        userMapper.updateById(user);
    }

    /**
     * 批量删除用户
     * <p>
     * 原先由 Controller 直接调用 {@code removeBatchByIds}，既绕过了业务层，
     * 也没有任何校验：删自己不拦、删不存在的 id 静默成功、删还有业务数据的用户会留下悬空引用。
     */
    @Override
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_IDS_REQUIRED);
        }
        // 去重并剔除 null，避免同一个 id 传两次导致计数与提示失真
        List<Long> targetIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (targetIds.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_IDS_REQUIRED);
        }

        // 守卫 1：不允许删除当前登录用户自己，避免把自己锁在系统外
        Long currentUserId = UserHolder.getCurrentUser();
        if (currentUserId != null && targetIds.contains(currentUserId)) {
            throw new BusinessException(ErrorCode.USER_CANNOT_DELETE_SELF);
        }

        // 守卫 2：待删 ID 必须真实存在，否则明确报错而不是静默「删除成功」
        List<User> existing = listByIds(targetIds);
        if (existing.size() != targetIds.size()) {
            Set<Long> existingIds = existing.stream().map(User::getId).collect(Collectors.toSet());
            List<Long> missing = targetIds.stream().filter(id -> !existingIds.contains(id)).toList();
            throw new BusinessException(ErrorCode.USER_IDS_NOT_FOUND, missing);
        }

        // 守卫 3：仍被业务数据引用的用户不允许删除。
        // 项目不使用物理外键（见 sql/user.sql 注释），这类引用完整性只能由 Service 层兜底。
        for (Long id : targetIds) {
            long clueRefs = clueMapper.countByUserId(id);
            long businessRefs = businessMapper.countByUserId(id);
            long trackRefs = clueTrackRecordMapper.countByUserId(id) + businessTrackRecordMapper.countByUserId(id);
            if (clueRefs > 0 || businessRefs > 0 || trackRefs > 0) {
                throw new BusinessException(ErrorCode.USER_STILL_REFERENCED, id, clueRefs, businessRefs, trackRefs);
            }
        }

        removeBatchByIds(targetIds);
    }

    @Override
    public List<UserVO> listAll() {
        return userMapper.listAll();
    }

    @Override
    public List<UserVO> findByRoleLabel(String roleLabel) {
        return userMapper.findByRoleLabel(roleLabel);
    }

    @Override
    public List<UserVO> findByDeptId(Long deptId) {
        return userMapper.findByDeptId(deptId);
    }

    @Override
    public LoginResultVO login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }

        // 1. 根据用户名查询用户
        User user = userMapper.findByUsername(username);
        if (user == null || user.getStatus() != null && user.getStatus() == 0) {
            return null;
        }

        // 2. 校验密码：库中存放的是 md5(用户名 + 明文密码) 的摘要。
        //    这里只接受明文密码（依赖 HTTPS 传输），不接受「直接提交摘要」的方式，
        //    否则数据库里的摘要就等同于一个可复用的登录凭证（pass-the-hash）。
        if (!StrUtil.equals(user.getPassword(), DigestUtil.md5Hex(username + password))) {
            return null;
        }

        // 3. 查询角色标识
        Role role = roleMapper.selectById(user.getRoleId());

        // 4. 组装登录结果并签发 JWT
        LoginResultVO vo = new LoginResultVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setImage(user.getImage());
        vo.setRoleLabel(role == null ? null : role.getLabel());

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("name", user.getName());
        vo.setToken(jwtUtil.generateToken(claims));
        return vo;
    }
}
