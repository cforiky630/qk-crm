package com.qk.service;

import com.qk.entity.Activity;
import com.qk.common.PageResult;

import java.util.List;

/**
 * 活动管理Service接口
 */
public interface ActivityService {

    /**
     * 新增活动
     *
     * @param activity 活动信息
     */
    void addActivity(Activity activity);

    /**
     * 分页查询活动
     *
     * @param channel        渠道来源
     * @param type           活动类型
     * @param activityStatus 活动状态（1 未开始 / 2 进行中 / 3 已结束），为空表示不筛选
     * @param page           当前页码
     * @param pageSize       每页显示条数
     * @return 分页结果
     */
    PageResult<Activity> findActivitiesByPage(Integer channel, Integer type,
                                              Integer activityStatus, Integer page, Integer pageSize);

    /**
     * 根据id查询活动
     *
     * @param id 活动id
     * @return 活动信息
     */
    Activity findById(Integer id);

    /**
     * 根据id修改活动信息
     *
     * @param activity 活动信息
     */
    void updateById(Activity activity);

    /**
     * 根据id删除活动
     *
     * @param id 活动id
     */
    void deleteById(Integer id);

    /**
     * 查询指定类型的所有活动，不分页
     *
     * @param type 活动类型
     * @return 活动列表
     */
    List<Activity> findByType(Integer type);
}
