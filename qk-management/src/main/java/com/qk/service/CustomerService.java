package com.qk.service;

import com.qk.entity.po.Customer;
import com.qk.entity.po.Business;
import com.qk.entity.vo.PageResult;
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
    void saveCustomer(Customer customer);

    /**
     * 由商机生成客户（商机转客户）
     * <p>
     * 与 {@link #saveCustomer(Customer)} 同一套规则：编号自增、校验手机号与意向课程；
     * 来源商机ID 由本方法写入，外部接口无法伪造。
     *
     * @param business 已确认转为客户的商机
     */
    void createFromBusiness(Business business);

    /**
     * 根据ID查询客户
     *
     * @param id 客户ID
     * @return 客户信息
     */
    CustomerVO getCustomerById(Long id);

    /**
     * 修改客户信息
     *
     * @param customer 客户信息
     */
    void updateById(Customer customer);
}
