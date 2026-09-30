package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Customer;
import com.qk.entity.dto.CustomerQueryDto;
import com.qk.entity.vo.CustomerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 客户数据访问接口
 * <p>
 * 列表与详情需要 join 出意向课程名称，SQL 见同包路径下的 CustomerMapper.xml。
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 客户列表（含意向课程名称）
     */
    IPage<CustomerVO> listCustomers(Page<CustomerVO> page, @Param("customerQueryDto") CustomerQueryDto customerQueryDto);

    /**
     * 根据ID查询客户（含意向课程名称）
     */
    CustomerVO getCustomerById(@Param("id") Integer id);
}
