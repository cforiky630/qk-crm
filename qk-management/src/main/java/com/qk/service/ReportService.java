package com.qk.service;

import com.qk.vo.OverviewVO;

/**
 * 首页数据概览Service接口
 */
public interface ReportService {

    /**
     * 获取首页线索与商机的概览数据
     *
     * @return 概览数据
     */
    OverviewVO getOverview();
}
