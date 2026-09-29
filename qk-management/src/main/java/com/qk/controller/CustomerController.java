package com.qk.controller;

import com.qk.Customer;
import com.qk.PageResult;
import com.qk.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.dto.CustomerQueryDto;
import com.qk.service.CustomerService;
import com.qk.vo.CustomerVO;
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
    @GetMapping
    public Result listCustomers(CustomerQueryDto customerQueryDto) {
        log.info("客户列表查询, 参数: {}", customerQueryDto);
        PageResult<CustomerVO> pageResult = customerService.listCustomers(customerQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 新增客户
     */
    @LogOperation
    @PostMapping
    public Result addCustomer(@RequestBody Customer customer) {
        log.info("新增客户: {}", customer);
        customerService.addCustomer(customer);
        return Result.success();
    }

    /**
     * 根据ID查询客户
     */
    @GetMapping("/{id}")
    public Result getCustomerById(@PathVariable Integer id) {
        log.info("根据ID查询客户, id: {}", id);
        CustomerVO customer = customerService.getCustomerById(id);
        return customer != null ? Result.success(customer) : Result.error("客户不存在");
    }

    /**
     * 修改客户
     */
    @LogOperation
    @PutMapping
    public Result updateCustomer(@RequestBody Customer customer) {
        log.info("修改客户: {}", customer);
        customerService.updateCustomer(customer);
        return Result.success();
    }
}
