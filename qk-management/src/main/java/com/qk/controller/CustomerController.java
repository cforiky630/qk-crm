package com.qk.controller;

import com.qk.entity.Customer;
import com.qk.entity.dto.CustomerSaveDto;
import cn.hutool.core.bean.BeanUtil;
import com.qk.common.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.entity.dto.CustomerQueryDto;
import com.qk.service.CustomerService;
import com.qk.entity.vo.CustomerVO;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
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
    @GetMapping
    public Result<PageResult<CustomerVO>> listCustomers(CustomerQueryDto customerQueryDto) {
        log.info("客户列表查询, 参数: {}", customerQueryDto);
        PageResult<CustomerVO> pageResult = customerService.listCustomers(customerQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 新增客户
     */
    @LogOperation
    @PostMapping
    public Result<Void> addCustomer(@Valid @RequestBody CustomerSaveDto customerDto) {
        log.info("新增客户: {}", customerDto);
        customerService.addCustomer(toCustomer(customerDto));
        return Result.success();
    }

    /**
     * 根据ID查询客户
     */
    @GetMapping("/{id}")
    public Result<CustomerVO> getCustomerById(@PathVariable Integer id) {
        log.info("根据ID查询客户, id: {}", id);
        CustomerVO customer = customerService.getCustomerById(id);
        return customer != null ? Result.success(customer) : Result.error("客户不存在");
    }

    /**
     * 修改客户
     */
    @LogOperation
    @PutMapping
    public Result<Void> updateCustomer(@Valid @RequestBody CustomerSaveDto customerDto) {
        log.info("修改客户: {}", customerDto);
        customerService.updateCustomer(toCustomer(customerDto));
        return Result.success();
    }

    /** 协议适配：请求 DTO → 领域实体（businessId 由 Service 赋值） */
    private Customer toCustomer(CustomerSaveDto dto) {
        Customer customer = new Customer();
        BeanUtil.copyProperties(dto, customer);
        return customer;
    }
}
