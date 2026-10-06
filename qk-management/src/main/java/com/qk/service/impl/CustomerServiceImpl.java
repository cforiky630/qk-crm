package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Customer;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.CustomerQueryDto;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.mapper.CourseMapper;
import com.qk.mapper.CustomerMapper;
import com.qk.service.CustomerService;
import com.qk.entity.vo.CustomerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户管理Service实现
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CourseMapper courseMapper;

    @Autowired
    public CustomerServiceImpl(CustomerMapper customerMapper, CourseMapper courseMapper) {
        this.customerMapper = customerMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public PageResult<CustomerVO> listCustomers(CustomerQueryDto customerQueryDto) {
        Page<CustomerVO> page = new Page<>(customerQueryDto.getPage(), customerQueryDto.getPageSize());
        IPage<CustomerVO> customerPage = customerMapper.listCustomers(page, customerQueryDto);
        return new PageResult<>(customerPage.getTotal(), customerPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCustomer(Customer customer) {
        // 手机号是库里的 NOT NULL + 唯一键，必须校验；
        // 渠道来源按页面原型（3.3 选填）与接口文档（非必须）是可以不填的，因此不参与必填校验
        if (StrUtil.isBlank(customer.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_REQUIRED);
        }
        requireExistingCourse(customer.getCourseId());
        customer.setId(null);
        customerMapper.insert(customer);
    }

    @Override
    public CustomerVO getCustomerById(Long id) {
        CustomerVO customer = customerMapper.getCustomerById(id);
        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(Customer customer) {
        if (customer.getId() == null || customerMapper.selectById(customer.getId()) == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        requireExistingCourse(customer.getCourseId());
        // businessId 由「商机转客户」写入，不允许通过修改接口篡改
        customer.setBusinessId(null);
        customerMapper.updateById(customer);
    }

    /**
     * 意向课程必须真实存在（项目不使用物理外键，customer.course_id 的完整性由 Service 兜底）
     */
    private void requireExistingCourse(Long courseId) {
        if (courseId != null && courseMapper.selectById(courseId) == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
    }
}
