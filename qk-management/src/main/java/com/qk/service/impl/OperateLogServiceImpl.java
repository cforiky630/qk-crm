package com.qk.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.OperateLog;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.LogQueryDto;
import com.qk.mapper.OperateLogMapper;
import com.qk.service.OperateLogService;
import com.qk.entity.vo.OperateLogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志Service实现
 */
@Service
public class OperateLogServiceImpl implements OperateLogService {

    private final OperateLogMapper operateLogMapper;

    @Autowired
    public OperateLogServiceImpl(OperateLogMapper operateLogMapper) {
        this.operateLogMapper = operateLogMapper;
    }

    @Override
    public void saveLog(OperateLog operateLog) {
        operateLogMapper.insert(operateLog);
    }

    @Override
    public PageResult<OperateLogVO> listLogs(LogQueryDto logQueryDto) {
        Page<OperateLogVO> page = new Page<>(logQueryDto.getPage(), logQueryDto.getPageSize());
        IPage<OperateLogVO> logPage = operateLogMapper.listLogs(page, logQueryDto);
        return new PageResult<>(logPage.getTotal(), logPage.getRecords());
    }
}
