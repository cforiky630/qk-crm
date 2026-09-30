package com.qk.controller;

import com.qk.entity.Dept;
import com.qk.common.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.DeptService;
import lombok.extern.slf4j.Slf4j;
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
     * @param dept 部门信息
     * @return 操作结果
     */
    @LogOperation
    @PostMapping("/depts") // 对应前端提交时用到的请求方式
    // 当前端使用POST和PUT的请求体提交json格式的参数时，后端需要使用  @RequestBody 类对象 的方式接收
    public Result<Void> addDept(@RequestBody Dept dept) {
        log.info("新增部门,参数:{}", dept);
        deptService.addDept(dept);
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
    public Result<PageResult<Dept>> listDepts(String name, Integer status, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询部门, 参数: name={}, status={}, page={}, pageSize={}", name, status, page, pageSize);
        PageResult<Dept> pageResult = deptService.findDeptsByPage(name, status, page, pageSize);
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
    public Result<Dept> findById(@PathVariable Integer id) {
        log.info("查询部门ID为{}的部门信息", id);
        Dept dept = deptService.findById(id);
        return Result.success(dept);
    }


    /**
     * 修改部门
     *
     * @param dept 部门信息
     * @return 统一响应结果
     */
    @LogOperation
    @PutMapping("/depts")
    public Result<Void> updateDept(@RequestBody Dept dept) {
        log.info("修改部门信息：{}", dept);
        deptService.updateById(dept);
        return Result.success();
    }

    /**
     * 删除部门
     *
     * @param id 部门ID
     * @return 统一响应结果
     */
    @LogOperation
    @DeleteMapping("/depts/{id}")
    public Result<Void> deleteDept(@PathVariable("id") Integer id) {
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
    public Result<List<Dept>> listAllDepts() {
        log.info("查询所有正常状态的部门");
        List<Dept> depts = deptService.findAllNormal();
        return Result.success(depts);
    }
}
