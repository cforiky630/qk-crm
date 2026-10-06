package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.qk.entity.po.Clue;
import com.qk.entity.po.ClueTrackRecord;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.CluePoolDto;
import com.qk.entity.dto.ClueQueryDto;
import com.qk.entity.dto.ClueTrackDto;
import com.qk.entity.dto.MarkFalseClueDto;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.enums.ClueTrackType;
import com.qk.entity.enums.EnableStatus;
import com.qk.entity.po.User;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.domain.ClueLifecycle;
import com.qk.mapper.ActivityMapper;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.ClueTrackRecordMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.BusinessService;
import com.qk.service.ClueService;
import com.qk.common.util.UserHolder;
import com.qk.entity.vo.ClueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 线索管理Service实现
 */
@Service
public class ClueServiceImpl extends ServiceImpl<ClueMapper, Clue> implements ClueService {

    private final ClueTrackRecordMapper clueTrackRecordMapper;
    private final BusinessService businessService;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;

    @Autowired
    public ClueServiceImpl(ClueTrackRecordMapper clueTrackRecordMapper, BusinessService businessService,
                           UserMapper userMapper, ActivityMapper activityMapper) {
        this.clueTrackRecordMapper = clueTrackRecordMapper;
        this.businessService = businessService;
        this.userMapper = userMapper;
        this.activityMapper = activityMapper;
    }

    @Override
    public PageResult<ClueVO> listClues(ClueQueryDto clueQueryDto) {
        Page<ClueVO> page = new Page<>(clueQueryDto.getPage(), clueQueryDto.getPageSize());
        IPage<ClueVO> cluePage = baseMapper.listClues(page, clueQueryDto, ClueLifecycle.closedCodes());
        return new PageResult<>(cluePage.getTotal(), cluePage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addClue(Clue clue) {
        if (StrUtil.isBlank(clue.getPhone()) || clue.getChannel() == null) {
            throw new BusinessException(ErrorCode.CLUE_PHONE_CHANNEL_REQUIRED);
        }
        requireExistingActivity(clue.getActivityId());
        clue.setId(null);
        clue.setStatus(ClueStatus.WAIT_ALLOT.getCode());
        clue.setUserId(null);
        save(clue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignClue(Long clueId, Long userId) {
        // 加行锁读取：并发重复分配时，后到的请求会读到已变更的状态并被状态机拒绝，
        // 而不是两个请求都通过守卫、最后一个覆盖前一个的归属人。
        Clue existing = lockClue(clueId);
        ClueLifecycle.ensure(ClueLifecycle.Action.ASSIGN, existing.getStatus());
        requireAssignableUser(userId);

        Clue clue = new Clue();
        clue.setId(clueId);
        clue.setUserId(userId);
        clue.setStatus(ClueStatus.WAIT_FOLLOW.getCode());
        updateById(clue);
    }

    @Override
    public ClueVO getClueById(Long id) {
        ClueVO clue = baseMapper.getClueById(id);
        if (clue == null) {
            throw new BusinessException(ErrorCode.CLUE_NOT_FOUND);
        }
        // 一对多拆成两次查询：先查线索，再按 clueId 查跟进记录，避免 join 产生笛卡尔积
        clue.setTrackRecords(clueTrackRecordMapper.listTrackRecords(id));
        return clue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trackClue(ClueTrackDto clueTrackDto) {
        Clue existing = lockClue(clueTrackDto.getId());
        ClueLifecycle.ensure(ClueLifecycle.Action.TRACK, existing.getStatus());
        // 1. 更新线索：状态由服务端固定置为跟进中（前端即使传了 status 也不生效）
        // 跟进时可以顺带更新客户资料，因此与线索同名的字段一并搬运；id 只用于定位
        Clue clue = new Clue();
        clue.setId(clueTrackDto.getId());
        clue.setPhone(clueTrackDto.getPhone());
        clue.setChannel(clueTrackDto.getChannel());
        clue.setActivityId(clueTrackDto.getActivityId());
        clue.setName(clueTrackDto.getName());
        clue.setGender(clueTrackDto.getGender());
        clue.setAge(clueTrackDto.getAge());
        clue.setWechat(clueTrackDto.getWechat());
        clue.setQq(clueTrackDto.getQq());
        clue.setSubject(clueTrackDto.getSubject());
        clue.setLevel(clueTrackDto.getLevel());
        clue.setNextTime(clueTrackDto.getNextTime());
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
    public void markFalseClue(Long id, MarkFalseClueDto markFalseClueDto) {
        Clue existing = lockClue(id);
        ClueLifecycle.ensure(ClueLifecycle.Action.MARK_FALSE, existing.getStatus());
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
    public void convertToBusiness(Long id) {
        // 1. 更新线索：状态置为转为商机
        Clue clue = lockClue(id);
        ClueLifecycle.ensure(ClueLifecycle.Action.CONVERT_TO_BUSINESS, clue.getStatus());
        clue.setStatus(ClueStatus.CONVERT_BUSINESS.getCode());
        updateById(clue);

        // 2. 按线索信息创建商机：交给商机模块，复用它的新增规则并记录来源线索
        businessService.createFromClue(clue);
    }

    @Override
    public PageResult<ClueVO> getPoolClues(CluePoolDto cluePoolDto) {
        Page<ClueVO> page = new Page<>(cluePoolDto.getPage(), cluePoolDto.getPageSize());
        IPage<ClueVO> cluePage = baseMapper.getPoolClues(page, cluePoolDto, ClueLifecycle.poolStatus());
        return new PageResult<>(cluePage.getTotal(), cluePage.getRecords());
    }

    /**
     * 按主键加行锁读取线索，不存在直接抛业务异常，避免对不存在的数据「更新成功」
     * <p>
     * 必须在事务内调用：状态流转是「先读状态再写状态」，不加锁时并发请求会同时通过守卫
     * （重复点击「转商机」可能生成两条商机、重复「跟进」会写入两条跟进记录）。
     * {@code SELECT ... FOR UPDATE} 把同一行的流转串行化，后到的请求会读到已变更的状态，
     * 按正常路径收到「该线索当前状态不允许…」。
     *
     * @return 已加锁的线索，供调用方复用，避免重复查询
     */
    private Clue lockClue(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.CLUE_ID_REQUIRED);
        }
        Clue clue = baseMapper.lockById(id);
        if (clue == null) {
            throw new BusinessException(ErrorCode.CLUE_NOT_FOUND);
        }
        return clue;
    }

    /**
     * 关联活动必须真实存在。
     * <p>
     * 项目不使用物理外键（见 sql/clue.sql 注释），clue.activity_id 的引用完整性
     * 只能由 Service 层兜底，否则线索的来源活动会变成悬空引用。
     */
    private void requireExistingActivity(Long activityId) {
        if (activityId != null && activityMapper.selectById(activityId) == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    /**
     * 归属人必须是存在且启用（status = 1）的用户。
     * <p>
     * 项目不使用物理外键，user_id 的完整性同样由 Service 保证。停用账号无法登录，
     * 把线索分给它等于这条线索没有归属人，因此与「按角色查人员下拉」的口径保持一致。
     */
    private void requireAssignableUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_ASSIGNABLE);
        }
        User user = userMapper.selectById(userId);
        if (user == null || EnableStatus.DISABLED.getCode().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_ASSIGNABLE);
        }
    }
}
