package com.qk.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.qk.entity.Business;
import com.qk.entity.BusinessTrackRecord;
import com.qk.entity.Customer;
import com.qk.common.PageResult;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.entity.enums.BusinessStatus;
import com.qk.common.exception.BusinessException;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.BusinessTrackRecordMapper;
import com.qk.mapper.CustomerMapper;
import com.qk.service.BusinessService;
import com.qk.common.util.UserHolder;
import com.qk.entity.vo.BusinessVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商机管理Service实现
 */
@Service
public class BusinessServiceImpl extends ServiceImpl<BusinessMapper, Business> implements BusinessService {

    private final BusinessTrackRecordMapper businessTrackRecordMapper;
    private final CustomerMapper customerMapper;

    @Autowired
    public BusinessServiceImpl(BusinessTrackRecordMapper businessTrackRecordMapper, CustomerMapper customerMapper) {
        this.businessTrackRecordMapper = businessTrackRecordMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    public PageResult<BusinessVO> listBusinesses(BusinessQueryDto businessQueryDto) {
        Page<BusinessVO> page = new Page<>(businessQueryDto.getPage(), businessQueryDto.getPageSize());
        IPage<BusinessVO> businessPage = baseMapper.listBusinesses(page, businessQueryDto);
        return new PageResult<>(businessPage.getTotal(), businessPage.getRecords());
    }

    @Override
    public void addBusiness(Business business) {
        if (StrUtil.isBlank(business.getPhone()) || business.getChannel() == null) {
            throw new BusinessException("手机号与渠道来源不能为空");
        }
        business.setId(null);
        business.setStatus(BusinessStatus.WAIT_ALLOT.getCode());
        business.setUserId(null);
        save(business);
    }

    @Override
    public void assignBusiness(Integer businessId, Integer userId) {
        requireBusiness(businessId);
        Business business = new Business();
        business.setId(businessId);
        business.setUserId(userId);
        business.setStatus(BusinessStatus.WAIT_FOLLOW.getCode());
        updateById(business);
    }

    @Override
    public void backToPool(Integer id) {
        requireBusiness(id);
        Business business = new Business();
        business.setId(id);
        business.setStatus(BusinessStatus.RECYCLED.getCode());
        // 踢回公海，需要同时解除归属人
        updateById(business);
        // updateById 默认忽略 null 字段，因此清空归属人交给 Mapper 显式置 null
        baseMapper.clearAssignee(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void convertToCustomer(Integer id) {
        // 1. 更新商机：状态置为转客户
        Business business = requireBusiness(id);
        business.setStatus(BusinessStatus.CONVERT_CUSTOMER.getCode());
        updateById(business);

        // 2. 按商机信息创建客户，并记录来源商机
        Customer customer = BeanUtil.copyProperties(business, Customer.class);
        customer.setId(null);
        customer.setBusinessId(business.getId());
        customerMapper.insert(customer);
    }

    @Override
    public BusinessVO getBusinessById(Integer id) {
        BusinessVO business = baseMapper.getBusinessById(id);
        if (business == null) {
            return null;
        }
        // 一对多拆成两次查询：先查商机，再按 businessId 查跟进记录
        business.setTrackRecords(businessTrackRecordMapper.listTrackRecords(id));
        return business;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trackBusiness(BusinessTrackDto businessTrackDto) {
        requireBusiness(businessTrackDto.getId());
        // 1. 更新商机：状态由服务端固定置为跟进中
        Business business = BeanUtil.copyProperties(businessTrackDto, Business.class);
        business.setStatus(BusinessStatus.FOLLOWING.getCode());
        updateById(business);

        // 2. 新增一条商机跟进记录
        BusinessTrackRecord trackRecord = new BusinessTrackRecord();
        trackRecord.setBusinessId(businessTrackDto.getId());
        trackRecord.setUserId(UserHolder.getCurrentUser());
        trackRecord.setTrackStatus(businessTrackDto.getTrackStatus());
        trackRecord.setKeyItems(businessTrackDto.getKeyItems() == null
                ? "[]" : businessTrackDto.getKeyItems().toString());
        trackRecord.setNextTime(businessTrackDto.getNextTime());
        trackRecord.setRecord(businessTrackDto.getRecord());
        businessTrackRecordMapper.insert(trackRecord);
    }

    @Override
    public PageResult<BusinessVO> getPoolBusinesses(BusinessPoolDto businessPoolDto) {
        Page<BusinessVO> page = new Page<>(businessPoolDto.getPage(), businessPoolDto.getPageSize());
        IPage<BusinessVO> businessPage = baseMapper.getPoolBusinesses(page, businessPoolDto);
        return new PageResult<>(businessPage.getTotal(), businessPage.getRecords());
    }

    /**
     * 校验商机是否存在，不存在直接抛业务异常
     */
    private Business requireBusiness(Integer id) {
        if (id == null) {
            throw new BusinessException("商机ID不能为空");
        }
        Business business = getById(id);
        if (business == null) {
            throw new BusinessException("商机不存在");
        }
        return business;
    }
}
