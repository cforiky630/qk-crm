package com.qk.mapper;

import com.qk.entity.vo.StatusCountVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 首页数据概览统计
 * <p>
 * 纯聚合查询，没有对应的实体，SQL 见同包路径下的 ReportMapper.xml。
 */
@Mapper
public interface ReportMapper {

    /**
     * 按状态分组统计线索数量
     * <p>
     * 一次 {@code GROUP BY} 取代改造前 6 个标量子查询，状态码也不再硬编码在 SQL 里。
     */
    List<StatusCountVO> countClueByStatus();

    /**
     * 按状态分组统计商机数量
     */
    List<StatusCountVO> countBusinessByStatus();
}
