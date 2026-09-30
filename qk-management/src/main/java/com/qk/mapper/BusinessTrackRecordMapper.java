package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qk.entity.BusinessTrackRecord;
import com.qk.entity.vo.BusinessTrackRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商机跟进记录数据访问接口
 */
@Mapper
public interface BusinessTrackRecordMapper extends BaseMapper<BusinessTrackRecord> {

    /**
     * 查询某条商机的跟进记录列表（含跟进人姓名）
     */
    List<BusinessTrackRecordVO> listTrackRecords(@Param("businessId") Integer businessId);
}
