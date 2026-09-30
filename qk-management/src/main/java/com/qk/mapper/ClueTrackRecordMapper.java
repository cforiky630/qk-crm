package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qk.entity.ClueTrackRecord;
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
    List<ClueTrackRecordVO> listTrackRecords(@Param("clueId") Integer clueId);
}
