package com.qk.service;

import com.qk.entity.po.Business;
import com.qk.entity.po.Clue;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.entity.vo.BusinessVO;

/**
 * 商机管理Service接口
 */
public interface BusinessService {

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
     * 由线索生成商机（线索转商机）
     * <p>
     * 「商机怎么诞生」属于商机模块自己的规则：状态置为待分配、不带走归属人、校验手机号与意向课程。
     * 线索模块只负责把客户资料搬过来，不再直接往 business 表插数据 ——
     * 否则在 {@link #addBusiness(Business)} 里新增的任何规则，转换链路都会静默漏掉。
     *
     * @param clue 已确认转为商机的线索
     */
    void createFromClue(Clue clue);

    /**
     * 分配商机给指定用户
     *
     * @param businessId 商机ID
     * @param userId     用户ID
     */
    void assignBusiness(Long businessId, Long userId);

    /**
     * 将商机踢回公海
     *
     * @param id 商机ID
     */
    void backToPool(Long id);

    /**
     * 将商机转为客户
     *
     * @param id 商机ID
     */
    void convertToCustomer(Long id);

    /**
     * 根据ID查询商机详细信息（含跟进记录列表）
     *
     * @param id 商机ID
     * @return 商机详细信息
     */
    BusinessVO getBusinessById(Long id);

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
