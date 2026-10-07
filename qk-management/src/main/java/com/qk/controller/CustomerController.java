package com.qk.controller;

import com.qk.aspect.anno.LogOperation;
import com.qk.common.Result;
import com.qk.entity.dto.CustomerQueryDto;
import com.qk.entity.dto.CustomerSaveDto;
import com.qk.entity.enums.Permission;
import com.qk.entity.po.Customer;
import com.qk.entity.vo.CustomerVO;
import com.qk.entity.vo.PageResult;
import com.qk.service.CustomerService;
import com.qk.interceptor.RequirePermission;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理控制器
 * 对应接口文档：8. 接口文档-客户管理
 */
@Slf4j
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 客户列表查询
     */
    @RequirePermission(Permission.CUSTOMER_READ)
    @GetMapping
    public Result<PageResult<CustomerVO>> listCustomers(@Valid CustomerQueryDto customerQueryDto) {
        log.info("客户列表查询, 参数: {}", customerQueryDto);
        PageResult<CustomerVO> pageResult = customerService.listCustomers(customerQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 新增客户
     */
    @LogOperation
    @RequirePermission(Permission.CUSTOMER_CREATE)
    @PostMapping
    public Result<Void> addCustomer(@Valid @RequestBody CustomerSaveDto customerDto) {
        log.info("新增客户: {}", customerDto);
        customerService.addCustomer(toCustomer(customerDto));
        return Result.success();
    }

    /**
     * 根据ID查询客户
     */
    @RequirePermission(Permission.CUSTOMER_READ)
    @GetMapping("/{id}")
    public Result<CustomerVO> getCustomerById(@PathVariable Long id) {
        log.info("根据ID查询客户, id: {}", id);
        return Result.success(customerService.getCustomerById(id));
    }

    /**
     * 修改客户
     */
    @LogOperation
    @RequirePermission(Permission.CUSTOMER_UPDATE)
    @PutMapping
    public Result<Void> updateCustomer(@Valid @RequestBody CustomerSaveDto customerDto) {
        log.info("修改客户: {}", customerDto);
        customerService.updateCustomer(toCustomer(customerDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（businessId 由 Service 赋值）
     * <p>
     * 逐个字段赋值而不是 {@code BeanUtil.copyProperties}：反射拷贝靠字段名约定，
     * 实体字段改名后会静默停止拷贝（行为悄悄变），这里漏抄则编译期就报错。
     */
    private Customer toCustomer(CustomerSaveDto dto) {
        Customer customer = new Customer();
        customer.setId(dto.getId());
        customer.setPhone(dto.getPhone());
        customer.setChannel(dto.getChannel());
        customer.setName(dto.getName());
        customer.setGender(dto.getGender());
        customer.setAge(dto.getAge());
        customer.setWechat(dto.getWechat());
        customer.setQq(dto.getQq());
        customer.setDegree(dto.getDegree());
        customer.setJobStatus(dto.getJobStatus());
        customer.setSubject(dto.getSubject());
        customer.setCourseId(dto.getCourseId());
        return customer;
    }
}
