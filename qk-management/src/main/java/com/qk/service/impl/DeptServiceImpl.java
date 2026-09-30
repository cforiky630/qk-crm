package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Dept;
import com.qk.common.PageResult;
import com.qk.common.exception.BusinessException;
import com.qk.mapper.DeptMapper;
import com.qk.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;

    @Autowired
    public DeptServiceImpl(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
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
        requireDept(id);
        deptMapper.deleteById(id);
    }

    /**
     * 校验部门是否存在，不存在直接抛业务异常，避免对不存在的数据「更新/删除成功」
     */
    private void requireDept(Integer id) {
        if (id == null) {
            throw new BusinessException("部门ID不能为空");
        }
        if (deptMapper.selectById(id) == null) {
            throw new BusinessException("部门不存在");
        }
    }

    @Override
    public List<Dept> findAllNormal() {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        // status：0-停用，1-正常，下拉框只展示正常状态的部门
        wrapper.eq(Dept::getStatus, 1).orderByAsc(Dept::getId);
        return deptMapper.selectList(wrapper);
    }

}
