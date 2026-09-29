package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.OperateLog;
import com.qk.dto.LogQueryDto;
import com.qk.vo.OperateLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志数据访问接口
 */
@Mapper
public interface OperateLogMapper extends BaseMapper<OperateLog> {

    /**
     * 操作日志列表（含操作人姓名）
     */
    IPage<OperateLogVO> listLogs(Page<OperateLogVO> page, @Param("logQueryDto") LogQueryDto logQueryDto);
}
