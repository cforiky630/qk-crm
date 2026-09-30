package com.qk.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.qk.entity.Business;
import com.qk.common.PageResult;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.entity.vo.BusinessVO;

/**
 * 商机管理Service接口
 */
public interface BusinessService extends IService<Business> {

    /**
     * 商机列表查询
     *
     * @param businessQueryDto 查询参数
     * @return 分页结果
     */
    PageResult<BusinessVO> listBusinesses(BusinessQueryDto businessQueryDto);

    /**
     * 新增商机，状态置为待分配
     *
     * @param business 商机信息
     */
    void addBusiness(Business business);

    /**
     * 分配商机给指定用户
     *
     * @param businessId 商机ID
     * @param userId     用户ID
     */
    void assignBusiness(Integer businessId, Integer userId);

    /**
     * 将商机踢回公海
     *
     * @param id 商机ID
     */
    void backToPool(Integer id);

    /**
     * 将商机转为客户
     *
     * @param id 商机ID
     */
    void convertToCustomer(Integer id);

    /**
     * 根据ID查询商机详细信息（含跟进记录列表）
     *
     * @param id 商机ID
     * @return 商机详细信息
     */
    BusinessVO getBusinessById(Integer id);

    /**
     * 跟进商机：更新商机信息并新增一条跟进记录
     *
     * @param businessTrackDto 商机跟进参数（含本次跟进记录）
     */
    void trackBusiness(BusinessTrackDto businessTrackDto);

    /**
     * 公海池列表查询
     *
     * @param businessPoolDto 查询参数
     * @return 分页结果
     */
    PageResult<BusinessVO> getPoolBusinesses(BusinessPoolDto businessPoolDto);
}
