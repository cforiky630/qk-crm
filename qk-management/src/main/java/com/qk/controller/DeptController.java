package com.qk.controller;

import com.qk.entity.po.Dept;
import com.qk.entity.dto.DeptSaveDto;
import cn.hutool.core.bean.BeanUtil;
import com.qk.entity.vo.DeptVO;
import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class DeptController {

    private final DeptService deptService;

    @Autowired
    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 新增部门
     *
     * @param deptDto 部门信息（新增时忽略 id）
     * @return 操作结果
     */
    @LogOperation
    @PostMapping("/depts") // 对应前端提交时用到的请求方式
    public Result<Void> addDept(@Valid @RequestBody DeptSaveDto deptDto) {
        log.info("新增部门,参数:{}", deptDto);
        deptService.addDept(toDept(deptDto));
        return Result.success();
    }

    /**
     * 条件分页查询部门
     *
     * @param name     部门名称
     * @param status   状态
     * @param page     页码
     * @param pageSize 每页记录数
     * @return 分页查询结果
     */
    @GetMapping("/depts") // 接收前端发送的GET请求
    // 当前端使用(?参数)方式提交请求参数时, 后端需要使用   @RequestParam(可以省略) 参数变量  的方式接收
    // @RequestParam注解的defaultValue属性可以设置默认值
    public Result<PageResult<DeptVO>> listDepts(String name, Integer status, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询部门, 参数: name={}, status={}, page={}, pageSize={}", name, status, page, pageSize);
        PageResult<DeptVO> pageResult = deptService.findDeptsByPage(name, status, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询部门
     *
     * @param id 部门ID
     * @return 查询结果
     */
    @GetMapping("/depts/{id}")
    // 当前端使用请求路径方式提交请求参数时, 后端需要使用   @PathVariable 变量  的方式接收
    public Result<DeptVO> findById(@PathVariable Long id) {
        log.info("查询部门ID为{}的部门信息", id);
        DeptVO dept = deptService.findById(id);
        return Result.success(dept);
    }


    /**
     * 修改部门
     *
     * @param deptDto 部门信息（id 必填）
     * @return 统一响应结果
     */
    @LogOperation
    @PutMapping("/depts")
    public Result<Void> updateDept(@Valid @RequestBody DeptSaveDto deptDto) {
        log.info("修改部门信息：{}", deptDto);
        deptService.updateById(toDept(deptDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体
     * <p>
     * 放在控制器而不是 Service：Service 不应依赖 Web 层的入参对象，
     * 否则接口字段一变，业务层就要跟着改。
     */
    private Dept toDept(DeptSaveDto dto) {
        Dept dept = new Dept();
        BeanUtil.copyProperties(dto, dept);
        return dept;
    }

    /**
     * 删除部门
     *
     * @param id 部门ID
     * @return 统一响应结果
     */
    @LogOperation
    @DeleteMapping("/depts/{id}")
    public Result<Void> deleteDept(@PathVariable("id") Long id) {
        log.info("删除部门：{}", id);
        deptService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询所有正常状态的部门，不分页，用于下拉框
     *
     * @return 统一响应结果
     */
    @GetMapping("/depts/list")
    public Result<List<DeptVO>> listAllDepts() {
        log.info("查询所有正常状态的部门");
        List<DeptVO> depts = deptService.findAllNormal();
        return Result.success(depts);
    }
}
