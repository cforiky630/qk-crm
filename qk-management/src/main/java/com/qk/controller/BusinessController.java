package com.qk.controller;

import cn.hutool.core.bean.BeanUtil;
import com.qk.entity.po.Business;
import com.qk.entity.dto.BusinessSaveDto;
import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.service.BusinessService;
import com.qk.entity.vo.BusinessVO;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商机管理控制器
 * 对应接口文档：7. 接口文档-商机管理
 */
@Slf4j
@RestController
@RequestMapping("/businesses")
public class BusinessController {

    private final BusinessService businessService;

    @Autowired
    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * 商机列表查询
     */
    @GetMapping
    public Result<PageResult<BusinessVO>> listBusinesses(@Valid BusinessQueryDto businessQueryDto) {
        log.info("商机列表查询, 参数: {}", businessQueryDto);
        PageResult<BusinessVO> pageResult = businessService.listBusinesses(businessQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 公海池列表查询
     */
    @GetMapping("/pool")
    public Result<PageResult<BusinessVO>> getPoolBusinesses(@Valid BusinessPoolDto businessPoolDto) {
        log.info("公海池列表查询, 参数: {}", businessPoolDto);
        PageResult<BusinessVO> pageResult = businessService.listPoolBusinesses(businessPoolDto);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询商机详细信息（含跟进记录）
     */
    @GetMapping("/{id}")
    public Result<BusinessVO> getBusinessById(@PathVariable Long id) {
        log.info("根据ID查询商机详细信息, id: {}", id);
        return Result.success(businessService.getBusinessById(id));
    }

    /**
     * 新增商机
     */
    @LogOperation
    @PostMapping
    public Result<Void> addBusiness(@Valid @RequestBody BusinessSaveDto businessDto) {
        log.info("新增商机: {}", businessDto);
        businessService.saveBusiness(toBusiness(businessDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（status / userId / clueId 由 Service 赋值）
     * <p>
     * 字段搬运统一用 {@code BeanUtil.copyProperties}：DTO 与实体同名字段一一对应，
     * 这条契约由 {@code BeanNameContractTest} 用反射守住（DTO 的字段在实体里改名或不存在，测试立即失败）。
     */
    private Business toBusiness(BusinessSaveDto dto) {
        Business business = new Business();
        BeanUtil.copyProperties(dto, business);
        return business;
    }

    /**
     * 分配商机给指定用户
     */
    @LogOperation
    @PutMapping("/assign/{businessId}/{userId}")
    public Result<Void> assignBusiness(@PathVariable Long businessId, @PathVariable Long userId) {
        log.info("分配商机: 商机ID={}, 用户ID={}", businessId, userId);
        businessService.assignBusiness(businessId, userId);
        return Result.success();
    }

    /**
     * 将商机踢回公海
     */
    @LogOperation
    @PutMapping("/back/{id}")
    public Result<Void> backToPool(@PathVariable Long id) {
        log.info("将商机踢回公海, id: {}", id);
        businessService.backToPool(id);
        return Result.success();
    }

    /**
     * 将商机转为客户
     */
    @LogOperation
    @PostMapping("/toCustomer/{id}")
    public Result<Void> convertToCustomer(@PathVariable Long id) {
        log.info("将商机转为客户, id: {}", id);
        businessService.convertToCustomer(id);
        return Result.success();
    }

    /**
     * 跟进商机
     */
    @LogOperation
    @PutMapping
    public Result<Void> trackBusiness(@Valid @RequestBody BusinessTrackDto businessTrackDto) {
        log.info("跟进商机: {}", businessTrackDto);
        businessService.trackBusiness(businessTrackDto);
        return Result.success();
    }
}
