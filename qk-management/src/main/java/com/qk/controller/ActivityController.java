package com.qk.controller;

import com.qk.entity.po.Activity;
import com.qk.entity.dto.ActivityQueryDto;
import com.qk.entity.dto.ActivitySaveDto;
import com.qk.entity.vo.ActivityVO;
import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动管理控制器
 * 对应接口文档：5. 接口文档-活动管理
 */
@RestController
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    @Autowired
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 新增活动
     */
    @LogOperation
    @PostMapping("/activities")
    public Result<Void> addActivity(@Valid @RequestBody ActivitySaveDto activityDto) {
        log.info("新增活动,参数:{}", activityDto);
        activityService.addActivity(toActivity(activityDto));
        return Result.success();
    }

    /**
     * 条件分页查询活动
     *
     * @param query 查询条件（含分页参数）；活动状态按开始/结束时间推算，为空表示不筛选
     */
    @GetMapping("/activities")
    public Result<PageResult<ActivityVO>> listActivities(@Valid ActivityQueryDto query) {
        log.info("分页查询活动, 参数: {}", query);
        return Result.success(activityService.findActivitiesByPage(query));
    }

    /**
     * 查询指定类型的所有活动，不分页
     */
    @GetMapping("/activities/type/{type}")
    public Result<List<ActivityVO>> listActivitiesByType(@PathVariable Integer type) {
        log.info("查询类型为{}的活动", type);
        List<ActivityVO> activities = activityService.findByType(type);
        return Result.success(activities);
    }

    /**
     * 根据ID查询活动
     */
    @GetMapping("/activities/{id}")
    public Result<ActivityVO> findById(@PathVariable Long id) {
        log.info("查询活动ID为{}的活动信息", id);
        return Result.success(activityService.findById(id));
    }

    /**
     * 修改活动
     */
    @LogOperation
    @PutMapping("/activities")
    public Result<Void> updateActivity(@Valid @RequestBody ActivitySaveDto activityDto) {
        log.info("修改活动信息：{}", activityDto);
        activityService.updateById(toActivity(activityDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（Service 不依赖 Web 入参对象）
     * <p>
     * 逐个字段赋值而不是 {@code BeanUtil.copyProperties}：反射拷贝靠字段名约定，
     * 实体字段改名后会静默停止拷贝（行为悄悄变），这里漏抄则编译期就报错。
     */
    private Activity toActivity(ActivitySaveDto dto) {
        Activity activity = new Activity();
        activity.setId(dto.getId());
        activity.setChannel(dto.getChannel());
        activity.setName(dto.getName());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setDescription(dto.getDescription());
        activity.setType(dto.getType());
        activity.setDiscount(dto.getDiscount());
        activity.setVoucher(dto.getVoucher());
        return activity;
    }

    /**
     * 删除活动
     */
    @LogOperation
    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable("id") Long id) {
        log.info("删除活动：{}", id);
        activityService.deleteById(id);
        return Result.success();
    }
}
