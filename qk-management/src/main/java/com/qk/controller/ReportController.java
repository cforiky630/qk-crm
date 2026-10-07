package com.qk.controller;

import com.qk.common.Result;
import com.qk.service.ReportService;
import com.qk.entity.enums.Permission;
import com.qk.entity.vo.OverviewVO;
import com.qk.interceptor.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据统计控制器
 * 对应接口文档：9. 接口文档-其他接口 中的首页概览
 */
@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 获取首页概览数据
     */
    @RequirePermission(Permission.REPORT_READ)
    @GetMapping("/overview")
    public Result<OverviewVO> getOverview() {
        log.info("获取首页概览数据");
        OverviewVO overview = reportService.getOverview();
        return Result.success(overview);
    }
}
