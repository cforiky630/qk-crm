package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.entity.po.BusinessTrackRecord;
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
    List<BusinessTrackRecordVO> listTrackRecords(@Param("businessId") Long businessId);

    /**
     * 统计某用户的商机跟进记录数（用于删除用户前的引用校验）
     */
    default long countByUserId(Long userId) {
        Long count = selectCount(new LambdaQueryWrapper<BusinessTrackRecord>().eq(BusinessTrackRecord::getUserId, userId));
        return count == null ? 0L : count;
    }
}
