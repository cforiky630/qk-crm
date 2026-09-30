package com.qk.service;

import com.qk.entity.Customer;
import com.qk.common.PageResult;
import com.qk.entity.dto.CustomerQueryDto;
import com.qk.entity.vo.CustomerVO;

/**
 * 客户管理Service接口
 */
public interface CustomerService {

    /**
     * 客户列表查询
     *
     * @param customerQueryDto 查询参数
     * @return 分页结果
     */
    PageResult<CustomerVO> listCustomers(CustomerQueryDto customerQueryDto);

    /**
     * 新增客户
     *
     * @param customer 客户信息
     */
    void addCustomer(Customer customer);

    /**
     * 根据ID查询客户
     *
     * @param id 客户ID
     * @return 客户信息
     */
    CustomerVO getCustomerById(Integer id);

    /**
     * 修改客户信息
     *
     * @param customer 客户信息
     */
    void updateCustomer(Customer customer);
}
