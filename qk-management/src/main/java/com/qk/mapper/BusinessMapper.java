package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Business;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.vo.BusinessVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商机数据访问接口
 * <p>
 * 列表与详情都需要 join 出归属人姓名、意向课程名称，SQL 见同包路径下的 BusinessMapper.xml。
 */
@Mapper
public interface BusinessMapper extends BaseMapper<Business> {

    /**
     * 商机列表（含归属人姓名）
     * <p>
     * 不传 status 时只查询未关闭的商机（排除 4 回收、5 转客户）；
     * 显式传 status 时按传入值筛选，对应前端状态下拉里的全部选项。
     */
    IPage<BusinessVO> listBusinesses(Page<BusinessVO> page, @Param("businessQueryDto") BusinessQueryDto businessQueryDto,
                                     @Param("closedCodes") List<Integer> closedCodes);

    /**
     * 根据ID查询商机基本信息（不含跟进记录）
     */
    BusinessVO getBusinessById(@Param("id") Long id);

    /**
     * 公海池列表（回收的商机）
     */
    IPage<BusinessVO> getPoolBusinesses(Page<BusinessVO> page, @Param("businessPoolDto") BusinessPoolDto businessPoolDto,
                                        @Param("poolStatus") Integer poolStatus);

    /** 按主键加行锁读取商机（{@code SELECT ... FOR UPDATE}），必须在事务内调用，理由见 ClueMapper#lockById */
    Business lockById(@Param("id") Long id);

    /**
     * 统计引用某课程的商机数（用于删除课程前的引用校验）
     */
    default long countByCourseId(Long courseId) {
        Long count = selectCount(new LambdaQueryWrapper<Business>().eq(Business::getCourseId, courseId));
        return count == null ? 0L : count;
    }

    /**
     * 统计归属于某用户的商机数（用于删除用户前的引用校验）
     */
    default long countByUserId(Long userId) {
        Long count = selectCount(new LambdaQueryWrapper<Business>().eq(Business::getUserId, userId));
        return count == null ? 0L : count;
    }

    /**
     * 踢回公海：状态改为给定值，并同时解除归属人
     * <p>
     * 两件事必须在<b>同一条 UPDATE</b> 里完成：{@code updateById} 会忽略 null 字段、没法用它清空
     * {@code user_id}，而分两次写会出现「状态已回收但归属人还在」的中间态。
     * 状态取哪个值属于业务判断，由 Service 传入。
     *
     * @return 受影响行数
     */
    default int recycle(Long businessId, Integer status) {
        return update(null, new LambdaUpdateWrapper<Business>()
                .set(Business::getStatus, status)
                .set(Business::getUserId, null)
                .eq(Business::getId, businessId));
    }
}
