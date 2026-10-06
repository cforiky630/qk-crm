package com.qk.service;

import com.qk.entity.po.Dept;
import com.qk.entity.vo.DeptVO;
import com.qk.entity.vo.PageResult;

import java.util.List;

public interface DeptService {
    /**
     * 新增部门
     *
     * @param dept 部门信息（新增时忽略 id）
     */
    void addDept(Dept dept);

    /**
     * 分页查询部门
     *
     * @param name     部门名称
     * @param status   部门状态
     * @param page     当前页码
     * @param pageSize 每页显示条数
     * @return 分页结果
     */
    PageResult<DeptVO> findDeptsByPage(String name, Integer status, Integer page, Integer pageSize);

    /**
     * 根据id查询部门
     *
     * @param id 部门id
     * @return 部门信息
     */
    DeptVO findById(Long id);

    /**
     * 根据id修改部门信息
     *
     * @param dept 部门信息（id 必填）
     */
    void updateById(Dept dept);

    /**
     * 根据id删除部门
     *
     * @param id 部门id
     */
    void deleteById(Long id);

    /**
     * 查询所有正常状态的部门，不分页，用于下拉框
     *
     * @return 部门列表
     */
    List<DeptVO> findAllNormal();
}
