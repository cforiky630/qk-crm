package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Activity;
import com.qk.common.PageResult;
import com.qk.common.exception.BusinessException;
import com.qk.mapper.ActivityMapper;
import com.qk.mapper.ClueMapper;
import com.qk.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final ClueMapper clueMapper;

    @Autowired
    public ActivityServiceImpl(ActivityMapper activityMapper, ClueMapper clueMapper) {
        this.activityMapper = activityMapper;
        this.clueMapper = clueMapper;
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
        requireActivity(activity.getId());
        activityMapper.updateById(activity);
    }

    @Override
    public void deleteById(Integer id) {
        requireActivity(id);

        // 守卫：仍被线索引用的活动不允许删除。
        // 项目不使用物理外键（见 sql/clue.sql 注释），clue.activity_id 的引用完整性
        // 只能由 Service 层兜底，否则线索的来源活动会变成悬空引用。
        long clueRefs = clueMapper.countByActivityId(id);
        if (clueRefs > 0) {
            throw new BusinessException("该活动已关联 " + clueRefs + " 条线索，无法删除");
        }

        activityMapper.deleteById(id);
    }

    /**
     * 校验活动是否存在，不存在直接抛业务异常。
     * 原先删除接口缺少这道校验，删一个不存在的 id 也会返回「成功」。
     */
    private Activity requireActivity(Integer id) {
        if (id == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    @Override
    public List<Activity> findByType(Integer type) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getType, type).orderByAsc(Activity::getId);
        return activityMapper.selectList(wrapper);
    }
}
