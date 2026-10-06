package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.entity.po.ClueTrackRecord;
import com.qk.entity.vo.ClueTrackRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 线索跟进记录数据访问接口
 */
@Mapper
public interface ClueTrackRecordMapper extends BaseMapper<ClueTrackRecord> {

    /**
     * 查询某条线索的跟进记录列表（含跟进人姓名）
     */
    List<ClueTrackRecordVO> listTrackRecords(@Param("clueId") Long clueId);

    /**
     * 统计某用户的线索跟进记录数（用于删除用户前的引用校验）
     */
    default long countByUserId(Long userId) {
        Long count = selectCount(new LambdaQueryWrapper<ClueTrackRecord>().eq(ClueTrackRecord::getUserId, userId));
        return count == null ? 0L : count;
    }
}
