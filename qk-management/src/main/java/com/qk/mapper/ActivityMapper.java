package com.qk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Activity;
import com.qk.entity.enums.ActivityStatus;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动管理Mapper
 * <p>
 * 单表查询统一写在本接口的 default 方法里。
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 活动分页查询：channel / type 等值匹配
     * <p>
     * activityStatus 是「按时间算出来的状态」，库里没有这一列，因此这里翻译成
     * start_time / end_time 的条件（码值见 ActivityStatus）。
     * 排序与页面原型一致：按更新时间倒序，末尾补 id 保证翻页稳定。
     *
     * @param activityStatus 活动状态（1 未开始 / 2 进行中 / 3 已结束），为空表示不筛选
     */
    default IPage<Activity> pageActivities(Page<Activity> page, Integer channel, Integer type,
                                           Integer activityStatus) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, Activity::getChannel, channel)
                .eq(type != null, Activity::getType, type);

        if (activityStatus != null) {
            // 同一个 now 用于三个分支，避免边界上出现「既不算进行中也不算已结束」的空档
            LocalDateTime now = LocalDateTime.now();
            if (ActivityStatus.NOT_STARTED.getCode().equals(activityStatus)) {
                wrapper.gt(Activity::getStartTime, now);
            } else if (ActivityStatus.IN_PROGRESS.getCode().equals(activityStatus)) {
                wrapper.le(Activity::getStartTime, now).ge(Activity::getEndTime, now);
            } else if (ActivityStatus.FINISHED.getCode().equals(activityStatus)) {
                wrapper.lt(Activity::getEndTime, now);
            }
        }

        wrapper.orderByDesc(Activity::getUpdateTime).orderByDesc(Activity::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 某类型的活动列表，按 id 升序
     */
    default List<Activity> listByType(Integer type) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getType, type).orderByAsc(Activity::getId);
        return selectList(wrapper);
    }
}
