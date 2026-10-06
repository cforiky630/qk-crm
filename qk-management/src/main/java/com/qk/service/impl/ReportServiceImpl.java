package com.qk.service.impl;

import com.qk.mapper.ReportMapper;
import com.qk.service.ReportService;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.enums.CodeEnum;
import com.qk.entity.vo.OverviewVO;
import com.qk.entity.vo.StatusCountVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<Integer, Integer> clueCounts = toCountMap(reportMapper.countClueByStatus());
        Map<Integer, Integer> businessCounts = toCountMap(reportMapper.countBusinessByStatus());

        // 库里若出现枚举之外的状态（脏数据），它不会落进下面任何一个具名分组，但会算进总数。
        // 这里留一条告警便于发现，不影响返回结果，保证首页接口始终可用。
        warnUnknownStatus("线索", clueCounts, ClueStatus.class);
        warnUnknownStatus("商机", businessCounts, BusinessStatus.class);

        OverviewVO overview = new OverviewVO();
        overview.setClueTotal(sum(clueCounts));
        overview.setClueWaitAllot(countOf(clueCounts, ClueStatus.WAIT_ALLOT.getCode()));
        overview.setClueWaitFollow(countOf(clueCounts, ClueStatus.WAIT_FOLLOW.getCode()));
        overview.setClueFollowing(countOf(clueCounts, ClueStatus.FOLLOWING.getCode()));
        overview.setClueFalse(countOf(clueCounts, ClueStatus.FALSE_CLUE.getCode()));
        overview.setClueConvertBusiness(countOf(clueCounts, ClueStatus.CONVERT_BUSINESS.getCode()));

        overview.setBusinessTotal(sum(businessCounts));
        overview.setBusinessWaitAllot(countOf(businessCounts, BusinessStatus.WAIT_ALLOT.getCode()));
        overview.setBusinessWaitFollow(countOf(businessCounts, BusinessStatus.WAIT_FOLLOW.getCode()));
        overview.setBusinessFollowing(countOf(businessCounts, BusinessStatus.FOLLOWING.getCode()));
        overview.setBusinessFalse(countOf(businessCounts, BusinessStatus.RECYCLED.getCode()));
        overview.setBusinessConvertCustomer(countOf(businessCounts, BusinessStatus.CONVERT_CUSTOMER.getCode()));
        return overview;
    }

    /** 按状态分组结果 → 「状态码 → 数量」 */
    private static Map<Integer, Integer> toCountMap(List<StatusCountVO> rows) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (StatusCountVO row : rows) {
            if (row.getStatus() != null) {
                counts.put(row.getStatus(), row.getTotal() == null ? 0 : row.getTotal());
            }
        }
        return counts;
    }

    private static Integer countOf(Map<Integer, Integer> counts, Integer status) {
        return counts.getOrDefault(status, 0);
    }

    /** 总数 = 各分组之和，与改造前的 COUNT(*) 等价 */
    private static Integer sum(Map<Integer, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** 发现枚举之外的状态码时告警，帮助定位脏数据或漏补的枚举 */
    private <E extends Enum<E> & CodeEnum<Integer>> void warnUnknownStatus(String name,
                                                                          Map<Integer, Integer> counts,
                                                                          Class<E> type) {
        for (Integer status : counts.keySet()) {
            if (CodeEnum.fromCode(type, status).isEmpty()) {
                log.warn("{}概览发现枚举之外的状态码 {}（{} 条），请检查数据或补充枚举", name, status, counts.get(status));
            }
        }
    }
}
