package com.qk.service;

import com.qk.entity.po.Clue;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.CluePoolDto;
import com.qk.entity.dto.ClueQueryDto;
import com.qk.entity.dto.ClueTrackDto;
import com.qk.entity.dto.MarkFalseClueDto;
import com.qk.entity.vo.ClueVO;

/**
 * 线索管理Service接口
 */
public interface ClueService {

    /**
     * 线索列表查询
     *
     * @param clueQueryDto 查询参数
     * @return 分页结果
     */
    PageResult<ClueVO> listClues(ClueQueryDto clueQueryDto);

    /**
     * 新增线索，状态置为待分配
     *
     * @param clue 线索信息
     */
    void saveClue(Clue clue);

    /**
     * 分配线索给指定用户
     *
     * @param clueId 线索ID
     * @param userId 用户ID
     */
    void assignClue(Long clueId, Long userId);

    /**
     * 根据ID查询线索详细信息（含跟进记录列表）
     *
     * @param id 线索ID
     * @return 线索详细信息
     */
    ClueVO getClueById(Long id);

    /**
     * 跟进线索：更新线索信息并新增一条跟进记录
     *
     * @param clueTrackDto 线索跟进参数（含本次跟进记录 record）
     */
    void trackClue(ClueTrackDto clueTrackDto);

    /**
     * 将线索标记为伪线索
     *
     * @param id               线索ID
     * @param markFalseClueDto 伪线索原因与备注
     */
    void markFalseClue(Long id, MarkFalseClueDto markFalseClueDto);

    /**
     * 将线索转为商机
     *
     * @param id 线索ID
     */
    void convertToBusiness(Long id);

    /**
     * 线索池列表查询
     *
     * @param cluePoolDto 查询参数
     * @return 分页结果
     */
    PageResult<ClueVO> listPoolClues(CluePoolDto cluePoolDto);
}
