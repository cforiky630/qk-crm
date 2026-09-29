package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.Activity;
import com.qk.PageResult;
import com.qk.exception.BusinessException;
import com.qk.mapper.ActivityMapper;
import com.qk.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;

    @Autowired
    public ActivityServiceImpl(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Override
    public void addActivity(Activity activity) {
        activity.setId(null);
        if (StrUtil.isBlank(activity.getName()) || activity.getChannel() == null
                || activity.getType() == null || activity.getStartTime() == null || activity.getEndTime() == null) {
            throw new BusinessException("活动名称、渠道、类型、开始与结束时间均不能为空");
        }
        activityMapper.insert(activity);
    }

    @Override
    public PageResult<Activity> findActivitiesByPage(Integer channel, Integer type, Integer page, Integer pageSize) {
        Page<Activity> p = new Page<>(page, pageSize);

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, Activity::getChannel, channel)
                .eq(type != null, Activity::getType, type)
                .orderByAsc(Activity::getId); // 固定排序，避免分页时记录重复或丢失

        p = activityMapper.selectPage(p, wrapper);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    @Override
    public Activity findById(Integer id) {
        return activityMapper.selectById(id);
    }

    @Override
    public void updateById(Activity activity) {
        if (activity.getId() == null || activityMapper.selectById(activity.getId()) == null) {
            throw new BusinessException("活动不存在");
        }
        activityMapper.updateById(activity);
    }

    @Override
    public void deleteById(Integer id) {
        activityMapper.deleteById(id);
    }

    @Override
    public List<Activity> findByType(Integer type) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getType, type).orderByAsc(Activity::getId);
        return activityMapper.selectList(wrapper);
    }
}
