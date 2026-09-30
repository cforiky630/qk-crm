package com.qk.controller;

import com.qk.entity.Activity;
import com.qk.entity.dto.ActivitySaveDto;
import cn.hutool.core.bean.BeanUtil;
import com.qk.common.PageResult;
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
     * @param activityStatus 活动状态（1 未开始 / 2 进行中 / 3 已结束），按开始/结束时间推算，为空表示不筛选
     */
    @GetMapping("/activities")
    public Result<PageResult<Activity>> listActivities(Integer channel, Integer type, Integer activityStatus,
                                 @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询活动, 参数: channel={}, type={}, activityStatus={}, page={}, pageSize={}",
                channel, type, activityStatus, page, pageSize);
        PageResult<Activity> pageResult =
                activityService.findActivitiesByPage(channel, type, activityStatus, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询指定类型的所有活动，不分页
     */
    @GetMapping("/activities/type/{type}")
    public Result<List<Activity>> listActivitiesByType(@PathVariable Integer type) {
        log.info("查询类型为{}的活动", type);
        List<Activity> activities = activityService.findByType(type);
        return Result.success(activities);
    }

    /**
     * 根据ID查询活动
     */
    @GetMapping("/activities/{id}")
    public Result<Activity> findById(@PathVariable Integer id) {
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

    /** 协议适配：请求 DTO → 领域实体（Service 不依赖 Web 入参对象） */
    private Activity toActivity(ActivitySaveDto dto) {
        Activity activity = new Activity();
        BeanUtil.copyProperties(dto, activity);
        return activity;
    }

    /**
     * 删除活动
     */
    @LogOperation
    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable("id") Integer id) {
        log.info("删除活动：{}", id);
        activityService.deleteById(id);
        return Result.success();
    }
}
