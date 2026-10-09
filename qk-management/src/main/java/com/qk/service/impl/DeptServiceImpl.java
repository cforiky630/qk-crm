package com.qk.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Dept;
import com.qk.entity.dto.DeptQueryDto;
import com.qk.entity.vo.DeptVO;
import com.qk.entity.vo.PageResult;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.entity.enums.EnableStatus;
import com.qk.mapper.DeptMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    @Autowired
    public DeptServiceImpl(DeptMapper deptMapper, UserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDept(Dept dept) {
        // 主键由数据库自增，禁止客户端指定
        dept.setId(null);
        deptMapper.insert(dept);
    }

    @Override
    public PageResult<DeptVO> listDeptsByPage(DeptQueryDto query) {
        // 查询条件与排序由 Mapper 负责，Service 只做参数传递与结果包装
        IPage<Dept> p = deptMapper.pageDepts(new Page<>(query.getPage(), query.getPageSize()),
                query.getName(), query.getStatus());
        return new PageResult<>(p.getTotal(), p.getRecords().stream().map(DeptVO::from).toList());
    }

    @Override
    public DeptVO getById(Long id) {
        return DeptVO.from(requireDept(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Dept dept) {
        requireDept(dept.getId());
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        Dept dept = requireDept(id);

        // 守卫 1：启用状态的部门不允许直接删除，必须先停用。
        // 否则「正常部门」会凭空消失，前端下拉框与历史数据的引用都解释不通。
        if (EnableStatus.ENABLED.getCode().equals(dept.getStatus())) {
            throw new BusinessException(ErrorCode.DEPT_ENABLED_CANNOT_DELETE);
        }

        // 守卫 2：仍被用户引用的部门不允许删除。
        // 项目不使用物理外键（见 sql/user.sql 注释），这类引用完整性只能由 Service 层兜底，
        // 否则 user.dept_id 会留下悬空引用。
        long userCount = userMapper.countByDeptId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_USERS, userCount);
        }

        deptMapper.deleteById(id);
    }

    /**
     * 校验部门是否存在，不存在直接抛业务异常，避免对不存在的数据「更新/删除成功」
     *
     * @return 已存在的部门，供调用方复用，避免重复查询
     */
    private Dept requireDept(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.DEPT_ID_REQUIRED);
        }
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    @Override
    public List<DeptVO> listAllNormal() {
        return deptMapper.listEnabled().stream().map(DeptVO::from).toList();
    }

}
