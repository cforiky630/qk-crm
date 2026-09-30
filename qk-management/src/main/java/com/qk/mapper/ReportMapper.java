package com.qk.mapper;

import com.qk.entity.vo.OverviewVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页数据概览统计
 * <p>
 * 纯聚合查询，没有对应的实体，SQL 见同包路径下的 ReportMapper.xml。
 */
@Mapper
public interface ReportMapper {

    /**
     * 统计线索与商机各阶段的数量
     */
    OverviewVO getOverview();
}
