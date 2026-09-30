package com.qk.service.impl;

import com.qk.mapper.ReportMapper;
import com.qk.service.ReportService;
import com.qk.entity.vo.OverviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    @Autowired
    public ReportServiceImpl(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    public OverviewVO getOverview() {
        OverviewVO overview = reportMapper.getOverview();

        // 聚合 SQL 里的状态编码是写死的（注解里的 SQL 只能是编译期常量），
        // 因此这里校验一条不变量：各状态之和必须等于总数。
        // 一旦有人新增了枚举状态却忘了同步 SQL，或者库里出现了枚举之外的状态，
        // 这里会立刻告警——但只记录日志、不改变返回结果，保证首页接口始终可用。
        checkStatusSum("线索", overview.getClueTotal(),
                overview.getClueWaitAllot(), overview.getClueWaitFollow(), overview.getClueFollowing(),
                overview.getClueFalse(), overview.getClueConvertBusiness());
        checkStatusSum("商机", overview.getBusinessTotal(),
                overview.getBusinessWaitAllot(), overview.getBusinessWaitFollow(), overview.getBusinessFollowing(),
                overview.getBusinessFalse(), overview.getBusinessConvertCustomer());

        return overview;
    }

    /**
     * 校验「总数 = 各状态数量之和」
     */
    private void checkStatusSum(String name, Integer total, Integer... statusCounts) {
        int sum = 0;
        for (Integer count : statusCounts) {
            sum += count == null ? 0 : count;
        }
        if (total == null || total != sum) {
            log.warn("{}概览统计异常: total={}, 各状态之和={}, 请检查是否新增了状态但未同步统计 SQL",
                    name, total, sum);
        }
    }
}
