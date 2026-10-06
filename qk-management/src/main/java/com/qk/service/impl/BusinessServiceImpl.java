package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.qk.entity.po.Business;
import com.qk.entity.po.BusinessTrackRecord;
import com.qk.entity.po.Customer;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.entity.enums.BusinessStatus;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
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
        // 手机号是库里的 NOT NULL + 唯一键，必须校验；
        // 渠道来源按页面原型（2.11 选填）与接口文档（非必须）是可以不填的，因此不参与必填校验
        if (StrUtil.isBlank(business.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_REQUIRED);
        }
        business.setId(null);
        business.setStatus(BusinessStatus.WAIT_ALLOT.getCode());
        business.setUserId(null);
        save(business);
    }

    @Override
    public void assignBusiness(Long businessId, Long userId) {
        Business existing = requireBusiness(businessId);

        // 守卫：只有「待分配」或「已回收（回公海后重新分配）」的商机才能分配
        Integer status = existing.getStatus();
        boolean assignable = BusinessStatus.WAIT_ALLOT.getCode().equals(status)
                || BusinessStatus.RECYCLED.getCode().equals(status);
        if (!assignable) {
            throw new BusinessException(ErrorCode.BUSINESS_STATUS_NOT_ALLOWED, "分配");
        }

        Business business = new Business();
        business.setId(businessId);
        business.setUserId(userId);
        business.setStatus(BusinessStatus.WAIT_FOLLOW.getCode());
        updateById(business);
    }

    @Override
    public void backToPool(Long id) {
        requireActiveBusiness(id, "踢回公海");
        // 状态与归属人在同一条 UPDATE 里改完，中途失败不会留下半成品状态；也少一次数据库往返
        baseMapper.recycle(id, BusinessStatus.RECYCLED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void convertToCustomer(Long id) {
        // 1. 更新商机：状态置为转客户
        Business business = requireActiveBusiness(id, "转客户");
        business.setStatus(BusinessStatus.CONVERT_CUSTOMER.getCode());
        updateById(business);

        // 2. 按商机信息创建客户，并记录来源商机：只搬运客户资料，状态/归属人都不带
        Customer customer = new Customer();
        customer.setName(business.getName());
        customer.setPhone(business.getPhone());
        customer.setGender(business.getGender());
        customer.setAge(business.getAge());
        customer.setWechat(business.getWechat());
        customer.setQq(business.getQq());
        customer.setSubject(business.getSubject());
        customer.setCourseId(business.getCourseId());
        customer.setDegree(business.getDegree());
        customer.setJobStatus(business.getJobStatus());
        customer.setChannel(business.getChannel());
        customer.setBusinessId(business.getId());
        customerMapper.insert(customer);
    }

    @Override
    public BusinessVO getBusinessById(Long id) {
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
        requireActiveBusiness(businessTrackDto.getId(), "跟进");
        // 1. 更新商机：状态由服务端固定置为跟进中
        // 跟进时可以顺带更新客户资料，因此与商机同名的字段一并搬运；id 只用于定位
        Business business = new Business();
        business.setId(businessTrackDto.getId());
        business.setName(businessTrackDto.getName());
        business.setPhone(businessTrackDto.getPhone());
        business.setGender(businessTrackDto.getGender());
        business.setAge(businessTrackDto.getAge());
        business.setWechat(businessTrackDto.getWechat());
        business.setQq(businessTrackDto.getQq());
        business.setSubject(businessTrackDto.getSubject());
        business.setCourseId(businessTrackDto.getCourseId());
        business.setDegree(businessTrackDto.getDegree());
        business.setJobStatus(businessTrackDto.getJobStatus());
        business.setChannel(businessTrackDto.getChannel());
        business.setRemark(businessTrackDto.getRemark());
        business.setNextTime(businessTrackDto.getNextTime());
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
    private Business requireBusiness(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ID_REQUIRED);
        }
        Business business = getById(id);
        if (business == null) {
            throw new BusinessException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        return business;
    }

    /**
     * 守卫：只有「待跟进」或「跟进中」的商机才能继续流转（跟进 / 踢回公海 / 转客户）。
     * <p>
     * 与线索同样的思路：这是重复提交的第二道防线，避免重复生成跟进记录，
     * 或把同一条商机重复转成客户（后者原先只能靠客户手机号唯一索引挡下）。
     */
    private Business requireActiveBusiness(Long id, String action) {
        Business business = requireBusiness(id);
        Integer status = business.getStatus();
        boolean active = BusinessStatus.WAIT_FOLLOW.getCode().equals(status)
                || BusinessStatus.FOLLOWING.getCode().equals(status);
        if (!active) {
            throw new BusinessException(ErrorCode.BUSINESS_STATUS_NOT_ALLOWED, action);
        }
        return business;
    }
}
