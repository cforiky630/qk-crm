package com.qk.service;

import com.qk.OperateLog;
import com.qk.PageResult;
import com.qk.dto.LogQueryDto;
import com.qk.vo.OperateLogVO;

/**
 * 操作日志Service接口
 */
public interface OperateLogService {

    /**
     * 保存操作日志（由日志切面调用）
     *
     * @param operateLog 操作日志
     */
    void saveLog(OperateLog operateLog);

    /**
     * 操作日志列表查询
     *
     * @param logQueryDto 查询参数
     * @return 分页结果
     */
    PageResult<OperateLogVO> listLogs(LogQueryDto logQueryDto);
}
