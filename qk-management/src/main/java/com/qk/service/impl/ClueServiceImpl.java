package com.qk.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.qk.Business;
import com.qk.Clue;
import com.qk.ClueTrackRecord;
import com.qk.PageResult;
import com.qk.dto.CluePoolDto;
import com.qk.dto.ClueQueryDto;
import com.qk.dto.ClueTrackDto;
import com.qk.dto.MarkFalseClueDto;
import com.qk.enums.BusinessStatus;
import com.qk.enums.ClueStatus;
import com.qk.enums.ClueTrackType;
import com.qk.exception.BusinessException;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.ClueTrackRecordMapper;
import com.qk.service.ClueService;
import com.qk.util.UserHolder;
import com.qk.vo.ClueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 线索管理Service实现
 */
@Service
public class ClueServiceImpl extends ServiceImpl<ClueMapper, Clue> implements ClueService {

    private final ClueTrackRecordMapper clueTrackRecordMapper;
    private final BusinessMapper businessMapper;

    @Autowired
    public ClueServiceImpl(ClueTrackRecordMapper clueTrackRecordMapper, BusinessMapper businessMapper) {
        this.clueTrackRecordMapper = clueTrackRecordMapper;
        this.businessMapper = businessMapper;
    }

    @Override
    public PageResult<ClueVO> listClues(ClueQueryDto clueQueryDto) {
        Page<ClueVO> page = new Page<>(clueQueryDto.getPage(), clueQueryDto.getPageSize());
        IPage<ClueVO> cluePage = baseMapper.listClues(page, clueQueryDto);
        return new PageResult<>(cluePage.getTotal(), cluePage.getRecords());
    }

    @Override
    public void addClue(Clue clue) {
        if (StrUtil.isBlank(clue.getPhone()) || clue.getChannel() == null) {
            throw new BusinessException("手机号与线索来源不能为空");
        }
        clue.setId(null);
        clue.setStatus(ClueStatus.WAIT_ALLOT.getCode());
        clue.setUserId(null);
        save(clue);
    }

    @Override
    public void assignClue(Integer clueId, Integer userId) {
        requireClue(clueId);
        Clue clue = new Clue();
        clue.setId(clueId);
        clue.setUserId(userId);
        clue.setStatus(ClueStatus.WAIT_FOLLOW.getCode());
        updateById(clue);
    }

    @Override
    public ClueVO getClueById(Integer id) {
        ClueVO clue = baseMapper.getClueById(id);
        if (clue == null) {
            return null;
        }
        // 一对多拆成两次查询：先查线索，再按 clueId 查跟进记录，避免 join 产生笛卡尔积
        clue.setTrackRecords(clueTrackRecordMapper.listTrackRecords(id));
        return clue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trackClue(ClueTrackDto clueTrackDto) {
        requireClue(clueTrackDto.getId());
        // 1. 更新线索：状态由服务端固定置为跟进中（前端即使传了 status 也不生效）
        Clue clue = BeanUtil.copyProperties(clueTrackDto, Clue.class);
        clue.setStatus(ClueStatus.FOLLOWING.getCode());
        updateById(clue);

        // 2. 新增一条正常跟进记录
        ClueTrackRecord trackRecord = new ClueTrackRecord();
        trackRecord.setClueId(clueTrackDto.getId());
        trackRecord.setUserId(UserHolder.getCurrentUser());
        trackRecord.setSubject(clueTrackDto.getSubject());
        trackRecord.setLevel(clueTrackDto.getLevel());
        trackRecord.setRecord(clueTrackDto.getRecord());
        trackRecord.setNextTime(clueTrackDto.getNextTime());
        trackRecord.setType(ClueTrackType.NORMAL.getCode());
        clueTrackRecordMapper.insert(trackRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFalseClue(Integer id, MarkFalseClueDto markFalseClueDto) {
        requireClue(id);
        // 1. 更新线索：状态置为伪线索
        Clue clue = new Clue();
        clue.setId(id);
        clue.setStatus(ClueStatus.FALSE_CLUE.getCode());
        updateById(clue);

        // 2. 新增一条伪线索跟进记录
        ClueTrackRecord trackRecord = new ClueTrackRecord();
        trackRecord.setClueId(id);
        trackRecord.setUserId(UserHolder.getCurrentUser());
        trackRecord.setType(ClueTrackType.FALSE_CLUE.getCode());
        trackRecord.setFalseReason(markFalseClueDto.getReason());
        trackRecord.setRecord(markFalseClueDto.getRemark());
        clueTrackRecordMapper.insert(trackRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void convertToBusiness(Integer id) {
        // 1. 更新线索：状态置为转为商机
        Clue clue = requireClue(id);
        clue.setStatus(ClueStatus.CONVERT_BUSINESS.getCode());
        updateById(clue);

        // 2. 按线索信息创建商机：商机重新走分配流程，因此归属人、下次跟进时间都清空
        Business business = BeanUtil.copyProperties(clue, Business.class);
        business.setId(null);
        business.setUserId(null);
        business.setNextTime(null);
        business.setStatus(BusinessStatus.WAIT_ALLOT.getCode());
        business.setClueId(clue.getId());
        businessMapper.insert(business);
    }

    @Override
    public PageResult<ClueVO> getPoolClues(CluePoolDto cluePoolDto) {
        Page<ClueVO> page = new Page<>(cluePoolDto.getPage(), cluePoolDto.getPageSize());
        IPage<ClueVO> cluePage = baseMapper.getPoolClues(page, cluePoolDto);
        return new PageResult<>(cluePage.getTotal(), cluePage.getRecords());
    }

    /**
     * 校验线索是否存在，不存在直接抛业务异常，避免对不存在的数据「更新成功」
     */
    private Clue requireClue(Integer id) {
        if (id == null) {
            throw new BusinessException("线索ID不能为空");
        }
        Clue clue = getById(id);
        if (clue == null) {
            throw new BusinessException("线索不存在");
        }
        return clue;
    }
}
