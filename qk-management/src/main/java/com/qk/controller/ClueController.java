package com.qk.controller;

import com.qk.aspect.anno.LogOperation;
import com.qk.common.Result;
import com.qk.entity.dto.*;
import com.qk.entity.po.Clue;
import com.qk.entity.vo.ClueVO;
import com.qk.entity.vo.PageResult;
import com.qk.service.ClueService;
import jakarta.validation.Valid;
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
    public Result<PageResult<ClueVO>> listClues(@Valid ClueQueryDto clueQueryDto) {
        log.info("线索列表查询, 参数: {}", clueQueryDto);
        PageResult<ClueVO> pageResult = clueService.listClues(clueQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 线索池列表查询
     */
    @GetMapping("/pool")
    public Result<PageResult<ClueVO>> getPoolClues(@Valid CluePoolDto cluePoolDto) {
        log.info("线索池列表查询, 参数: {}", cluePoolDto);
        PageResult<ClueVO> pageResult = clueService.getPoolClues(cluePoolDto);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询线索详细信息（含跟进记录）
     */
    @GetMapping("/{id}")
    public Result<ClueVO> getClueById(@PathVariable Long id) {
        log.info("根据ID查询线索详细信息, id: {}", id);
        return Result.success(clueService.getClueById(id));
    }

    /**
     * 新增线索
     */
    @LogOperation
    @PostMapping
    public Result<Void> addClue(@Valid @RequestBody ClueSaveDto clueDto) {
        log.info("新增线索: {}", clueDto);
        clueService.addClue(toClue(clueDto));
        return Result.success();
    }

    /**
     * 协议适配：请求 DTO → 领域实体（status / userId 由 Service 赋值）
     * <p>
     * 逐个字段赋值而不是 {@code BeanUtil.copyProperties}：反射拷贝靠字段名约定，
     * 实体字段改名后会静默停止拷贝（行为悄悄变），这里漏抄则编译期就报错。
     */
    private Clue toClue(ClueSaveDto dto) {
        Clue clue = new Clue();
        clue.setPhone(dto.getPhone());
        clue.setChannel(dto.getChannel());
        clue.setActivityId(dto.getActivityId());
        clue.setName(dto.getName());
        clue.setGender(dto.getGender());
        clue.setAge(dto.getAge());
        clue.setWechat(dto.getWechat());
        clue.setQq(dto.getQq());
        clue.setSubject(dto.getSubject());
        clue.setLevel(dto.getLevel());
        clue.setNextTime(dto.getNextTime());
        return clue;
    }

    /**
     * 分配线索给指定用户
     */
    @LogOperation
    @PutMapping("/assign/{clueId}/{userId}")
    public Result<Void> assignClue(@PathVariable Long clueId, @PathVariable Long userId) {
        log.info("分配线索: 线索ID={}, 用户ID={}", clueId, userId);
        clueService.assignClue(clueId, userId);
        return Result.success();
    }

    /**
     * 跟进线索
     */
    @LogOperation
    @PutMapping
    public Result<Void> trackClue(@Valid @RequestBody ClueTrackDto clueTrackDto) {
        log.info("跟进线索: {}", clueTrackDto);
        clueService.trackClue(clueTrackDto);
        return Result.success();
    }

    /**
     * 将线索标记为伪线索
     */
    @LogOperation
    @PutMapping("/false/{id}")
    public Result<Void> markFalseClue(@PathVariable Long id, @Valid @RequestBody MarkFalseClueDto markFalseClueDto) {
        log.info("将线索标记为伪线索, id: {}, 原因: {}", id, markFalseClueDto);
        clueService.markFalseClue(id, markFalseClueDto);
        return Result.success();
    }

    /**
     * 将线索转为商机
     */
    @LogOperation
    @PutMapping("/toBusiness/{id}")
    public Result<Void> convertToBusiness(@PathVariable Long id) {
        log.info("将线索转为商机, id: {}", id);
        clueService.convertToBusiness(id);
        return Result.success();
    }
}
