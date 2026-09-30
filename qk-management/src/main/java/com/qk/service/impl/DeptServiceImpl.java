package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Dept;
import com.qk.common.PageResult;
import com.qk.common.exception.BusinessException;
import com.qk.entity.enums.EnableStatus;
import com.qk.mapper.DeptMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void addDept(Dept dept) {
        dept.setId(null);
        deptMapper.insert(dept);
    }

    @Override
    public PageResult<Dept> findDeptsByPage(String name, Integer status, Integer page, Integer pageSize) {
        // 分页插件使用步骤
        // 1.设置分页参数
        Page<Dept> p = new Page<>(page, pageSize);

        // 2. 设置条件参数
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), Dept::getName, name).eq(status != null, Dept::getStatus, status);

        // 3. 执行分页查询
        p = deptMapper.selectPage(p, wrapper);

        // 4. 返回结果
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    @Override
    public Dept findById(Integer id) {
        return deptMapper.selectById(id);
    }

    @Override
    public void updateById(Dept dept) {
        requireDept(dept.getId());
        deptMapper.updateById(dept);
    }

    @Override
    public void deleteById(Integer id) {
        Dept dept = requireDept(id);

        // 守卫 1：启用状态的部门不允许直接删除，必须先停用。
        // 否则「正常部门」会凭空消失，前端下拉框与历史数据的引用都解释不通。
        if (EnableStatus.ENABLED.getCode().equals(dept.getStatus())) {
            throw new BusinessException("部门处于启用状态，请先停用后再删除");
        }

        // 守卫 2：仍被用户引用的部门不允许删除。
        // 项目不使用物理外键（见 sql/user.sql 注释），这类引用完整性只能由 Service 层兜底，
        // 否则 user.dept_id 会留下悬空引用。
        long userCount = userMapper.countByDeptId(id);
        if (userCount > 0) {
            throw new BusinessException("该部门下还有 " + userCount + " 名用户，无法删除");
        }

        deptMapper.deleteById(id);
    }

    /**
     * 校验部门是否存在，不存在直接抛业务异常，避免对不存在的数据「更新/删除成功」
     *
     * @return 已存在的部门，供调用方复用，避免重复查询
     */
    private Dept requireDept(Integer id) {
        if (id == null) {
            throw new BusinessException("部门ID不能为空");
        }
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return dept;
    }

    @Override
    public List<Dept> findAllNormal() {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        // 下拉框只展示正常状态的部门
        wrapper.eq(Dept::getStatus, EnableStatus.ENABLED.getCode()).orderByAsc(Dept::getId);
        return deptMapper.selectList(wrapper);
    }

}
