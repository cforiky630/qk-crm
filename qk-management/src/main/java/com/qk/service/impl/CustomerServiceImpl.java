package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Customer;
import com.qk.common.PageResult;
import com.qk.entity.dto.CustomerQueryDto;
import com.qk.common.exception.BusinessException;
import com.qk.mapper.CustomerMapper;
import com.qk.service.CustomerService;
import com.qk.entity.vo.CustomerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 客户管理Service实现
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Autowired
    public CustomerServiceImpl(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public PageResult<CustomerVO> listCustomers(CustomerQueryDto customerQueryDto) {
        Page<CustomerVO> page = new Page<>(customerQueryDto.getPage(), customerQueryDto.getPageSize());
        IPage<CustomerVO> customerPage = customerMapper.listCustomers(page, customerQueryDto);
        return new PageResult<>(customerPage.getTotal(), customerPage.getRecords());
    }

    @Override
    public void addCustomer(Customer customer) {
        if (StrUtil.isBlank(customer.getPhone()) || customer.getChannel() == null) {
            throw new BusinessException("手机号与渠道来源不能为空");
        }
        customer.setId(null);
        customerMapper.insert(customer);
    }

    @Override
    public CustomerVO getCustomerById(Integer id) {
        return customerMapper.getCustomerById(id);
    }

    @Override
    public void updateCustomer(Customer customer) {
        if (customer.getId() == null || customerMapper.selectById(customer.getId()) == null) {
            throw new BusinessException("客户不存在");
        }
        // businessId 由「商机转客户」写入，不允许通过修改接口篡改
        customer.setBusinessId(null);
        customerMapper.updateById(customer);
    }
}
