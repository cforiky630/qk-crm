package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Clue;
import com.qk.entity.dto.CluePoolDto;
import com.qk.entity.dto.ClueQueryDto;
import com.qk.entity.vo.ClueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 线索数据访问接口
 * <p>
 * 列表与详情都需要 join 出归属人姓名、活动名称，SQL 见同包路径下的 ClueMapper.xml。
 */
@Mapper
public interface ClueMapper extends BaseMapper<Clue> {

    /**
     * 线索列表（含归属人姓名）
     * <p>
     * 不传 status 时只查询未关闭的线索（排除 4 伪线索、5 转为商机）；
     * 显式传 status 时按传入值筛选，对应前端状态下拉里的全部选项。
     */
    IPage<ClueVO> listClues(Page<ClueVO> page, @Param("clueQueryDto") ClueQueryDto clueQueryDto,
                            @Param("closedCodes") List<Integer> closedCodes);

    /**
     * 根据ID查询线索基本信息（不含跟进记录）
     */
    ClueVO getClueById(@Param("id") Long id);

    /**
     * 线索池列表（含活动名称），只返回伪线索（status = 4），供重新分配
     */
    IPage<ClueVO> getPoolClues(Page<ClueVO> page, @Param("cluePoolDto") CluePoolDto cluePoolDto,
                               @Param("poolStatus") Integer poolStatus);

    /**
     * 按主键加行锁读取线索（{@code SELECT ... FOR UPDATE}），供状态流转使用
     * <p>
     * 必须在事务内调用。状态流转是「先读状态再写状态」，不加锁时并发请求会同时通过守卫，
     * 重复点击「转商机」可能生成两条商机、重复「跟进」会写入两条跟进记录。
     */
    Clue lockById(@Param("id") Long id);

    /**
     * 统计关联某活动的线索数（用于删除活动前的引用校验）
     */
    default long countByActivityId(Long activityId) {
        Long count = selectCount(new LambdaQueryWrapper<Clue>().eq(Clue::getActivityId, activityId));
        return count == null ? 0L : count;
    }

    /**
     * 统计归属于某用户的线索数（用于删除用户前的引用校验）
     */
    default long countByUserId(Long userId) {
        Long count = selectCount(new LambdaQueryWrapper<Clue>().eq(Clue::getUserId, userId));
        return count == null ? 0L : count;
    }
}
