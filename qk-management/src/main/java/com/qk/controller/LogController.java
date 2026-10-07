package com.qk.controller;

import com.qk.entity.vo.PageResult;
import com.qk.common.Result;
import com.qk.entity.dto.LogQueryDto;
import com.qk.service.OperateLogService;
import com.qk.entity.vo.OperateLogVO;
import com.qk.entity.enums.Permission;
import com.qk.interceptor.RequirePermission;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器
 * 对应接口文档：9. 接口文档-其他接口 中的日志列表
 */
@Slf4j
@RestController
public class LogController {

    private final OperateLogService operateLogService;

    @Autowired
    public LogController(OperateLogService operateLogService) {
        this.operateLogService = operateLogService;
    }

    /**
     * 操作日志列表查询
     */
    @RequirePermission(Permission.LOG_READ)
    @GetMapping("/logs")
    public Result<PageResult<OperateLogVO>> listLogs(@Valid LogQueryDto logQueryDto) {
        log.info("查询操作日志, 参数: {}", logQueryDto);
        PageResult<OperateLogVO> pageResult = operateLogService.listLogs(logQueryDto);
        return Result.success(pageResult);
    }
}
