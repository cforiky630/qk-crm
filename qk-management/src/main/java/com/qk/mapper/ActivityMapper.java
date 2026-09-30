package com.qk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 活动管理Mapper
 * <p>
 * 单表查询统一写在本接口的 default 方法里。
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 活动分页查询：channel / type 等值匹配，按 id 升序
     */
    default IPage<Activity> pageActivities(Page<Activity> page, Integer channel, Integer type) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(channel != null, Activity::getChannel, channel)
                .eq(type != null, Activity::getType, type)
                .orderByAsc(Activity::getId);
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
