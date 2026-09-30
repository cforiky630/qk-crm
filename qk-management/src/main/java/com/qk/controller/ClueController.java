package com.qk.controller;

import com.qk.entity.Clue;
import com.qk.common.PageResult;
import com.qk.common.Result;
import com.qk.aspect.anno.LogOperation;
import com.qk.entity.dto.CluePoolDto;
import com.qk.entity.dto.ClueQueryDto;
import com.qk.entity.dto.ClueTrackDto;
import com.qk.entity.dto.MarkFalseClueDto;
import com.qk.service.ClueService;
import com.qk.entity.vo.ClueVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 线索管理控制器
 * 对应接口文档：6. 接口文档-线索管理
 */
@Slf4j
@RestController
@RequestMapping("/clues")
public class ClueController {

    private final ClueService clueService;

    @Autowired
    public ClueController(ClueService clueService) {
        this.clueService = clueService;
    }

    /**
     * 线索列表查询
     */
    @GetMapping
    public Result listClues(ClueQueryDto clueQueryDto) {
        log.info("线索列表查询, 参数: {}", clueQueryDto);
        PageResult<ClueVO> pageResult = clueService.listClues(clueQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 线索池列表查询
     */
    @GetMapping("/pool")
    public Result getPoolClues(CluePoolDto cluePoolDto) {
        log.info("线索池列表查询, 参数: {}", cluePoolDto);
        PageResult<ClueVO> pageResult = clueService.getPoolClues(cluePoolDto);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询线索详细信息（含跟进记录）
     */
    @GetMapping("/{id}")
    public Result getClueById(@PathVariable Integer id) {
        log.info("根据ID查询线索详细信息, id: {}", id);
        ClueVO clue = clueService.getClueById(id);
        return clue != null ? Result.success(clue) : Result.error("线索不存在");
    }

    /**
     * 新增线索
     */
    @LogOperation
    @PostMapping
    public Result addClue(@RequestBody Clue clue) {
        log.info("新增线索: {}", clue);
        clueService.addClue(clue);
        return Result.success();
    }

    /**
     * 分配线索给指定用户
     */
    @LogOperation
    @PutMapping("/assign/{clueId}/{userId}")
    public Result assignClue(@PathVariable Integer clueId, @PathVariable Integer userId) {
        log.info("分配线索: 线索ID={}, 用户ID={}", clueId, userId);
        clueService.assignClue(clueId, userId);
        return Result.success();
    }

    /**
     * 跟进线索
     */
    @LogOperation
    @PutMapping
    public Result trackClue(@RequestBody ClueTrackDto clueTrackDto) {
        log.info("跟进线索: {}", clueTrackDto);
        clueService.trackClue(clueTrackDto);
        return Result.success();
    }

    /**
     * 将线索标记为伪线索
     */
    @LogOperation
    @PutMapping("/false/{id}")
    public Result markFalseClue(@PathVariable Integer id, @RequestBody MarkFalseClueDto markFalseClueDto) {
        log.info("将线索标记为伪线索, id: {}, 原因: {}", id, markFalseClueDto);
        clueService.markFalseClue(id, markFalseClueDto);
        return Result.success();
    }

    /**
     * 将线索转为商机
     */
    @LogOperation
    @PutMapping("/toBusiness/{id}")
    public Result convertToBusiness(@PathVariable Integer id) {
        log.info("将线索转为商机, id: {}", id);
        clueService.convertToBusiness(id);
        return Result.success();
    }
}
