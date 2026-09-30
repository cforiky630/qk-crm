package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Activity;
import com.qk.entity.enums.ActivityStatus;
import com.qk.entity.enums.CodeEnum;
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
    public PageResult<Activity> findActivitiesByPage(Integer channel, Integer type,
                                                     Integer activityStatus, Integer page, Integer pageSize) {
        // 活动状态是查询条件而不是库里的列，取值必须先收敛到枚举，
        // 否则前端传 9 之类的脏值会被静默忽略，返回「没有筛选」的全量数据
        if (activityStatus != null && CodeEnum.fromCode(ActivityStatus.class, activityStatus).isEmpty()) {
            throw new BusinessException("活动状态取值为 1（未开始）、2（进行中）、3（已结束）");
        }
        IPage<Activity> p = activityMapper.pageActivities(new Page<>(page, pageSize), channel, type, activityStatus);
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
        return activityMapper.listByType(type);
    }
}
