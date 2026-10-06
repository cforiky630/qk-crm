package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.qk.entity.po.Business;
import com.qk.entity.po.BusinessTrackRecord;
import com.qk.entity.po.Clue;
import com.qk.entity.po.User;
import com.qk.entity.vo.PageResult;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.EnableStatus;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.domain.BusinessLifecycle;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.BusinessTrackRecordMapper;
import com.qk.mapper.CourseMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.BusinessService;
import com.qk.service.CustomerService;
import com.qk.common.util.UserHolder;
import com.qk.entity.vo.BusinessVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商机管理Service实现
 */
@Service
public class BusinessServiceImpl extends ServiceImpl<BusinessMapper, Business> implements BusinessService {

    /** business_track_record.key_items 的列宽，超出时给出明确提示而不是让它变成 500 */
    private static final int MAX_KEY_ITEMS_LENGTH = 50;

    private final BusinessTrackRecordMapper businessTrackRecordMapper;
    private final CustomerService customerService;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;

    @Autowired
    public BusinessServiceImpl(BusinessTrackRecordMapper businessTrackRecordMapper, CustomerService customerService,
                               UserMapper userMapper, CourseMapper courseMapper) {
        this.businessTrackRecordMapper = businessTrackRecordMapper;
        this.customerService = customerService;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public PageResult<BusinessVO> listBusinesses(BusinessQueryDto businessQueryDto) {
        Page<BusinessVO> page = new Page<>(businessQueryDto.getPage(), businessQueryDto.getPageSize());
        IPage<BusinessVO> businessPage = baseMapper.listBusinesses(page, businessQueryDto,
                BusinessLifecycle.closedCodes());
        return new PageResult<>(businessPage.getTotal(), businessPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBusiness(Business business) {
        // 手机号是库里的 NOT NULL + 唯一键，必须校验；
        // 渠道来源按页面原型（2.11 选填）与接口文档（非必须）是可以不填的，因此不参与必填校验
        if (StrUtil.isBlank(business.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_REQUIRED);
        }
        requireExistingCourse(business.getCourseId());
        business.setId(null);
        business.setStatus(BusinessStatus.WAIT_ALLOT.getCode());
        business.setUserId(null);
        save(business);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromClue(Clue clue) {
        Business business = new Business();
        business.setName(clue.getName());
        business.setPhone(clue.getPhone());
        business.setGender(clue.getGender());
        business.setAge(clue.getAge());
        business.setWechat(clue.getWechat());
        business.setQq(clue.getQq());
        business.setSubject(clue.getSubject());
        business.setChannel(clue.getChannel());
        // 来源线索由服务端写入：商机接口不允许外部伪造 clueId
        business.setClueId(clue.getId());
        // 走普通新增，复用同一套规则（编号自增、状态待分配、无归属人、校验手机号与意向课程）；
        // 归属人与下次跟进时间刻意不搬运，商机重新走分配流程
        addBusiness(business);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignBusiness(Long businessId, Long userId) {
        // 加行锁读取：并发重复分配时后到的请求会被状态机拒绝，避免归属人被静默覆盖
        Business existing = lockBusiness(businessId);
        BusinessLifecycle.ensure(BusinessLifecycle.Action.ASSIGN, existing.getStatus());
        requireAssignableUser(userId);

        Business business = new Business();
        business.setId(businessId);
        business.setUserId(userId);
        business.setStatus(BusinessStatus.WAIT_FOLLOW.getCode());
        updateById(business);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void backToPool(Long id) {
        Business existing = lockBusiness(id);
        BusinessLifecycle.ensure(BusinessLifecycle.Action.BACK_TO_POOL, existing.getStatus());
        // 状态与归属人在同一条 UPDATE 里改完，中途失败不会留下半成品状态；也少一次数据库往返
        baseMapper.recycle(id, BusinessStatus.RECYCLED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void convertToCustomer(Long id) {
        // 1. 更新商机：状态置为转客户
        Business business = lockBusiness(id);
        BusinessLifecycle.ensure(BusinessLifecycle.Action.CONVERT_TO_CUSTOMER, business.getStatus());
        business.setStatus(BusinessStatus.CONVERT_CUSTOMER.getCode());
        updateById(business);

        // 2. 按商机信息创建客户：交给客户模块，复用它的新增规则并记录来源商机
        customerService.createFromBusiness(business);
    }

    @Override
    public BusinessVO getBusinessById(Long id) {
        BusinessVO business = baseMapper.getBusinessById(id);
        if (business == null) {
            throw new BusinessException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        // 一对多拆成两次查询：先查商机，再按 businessId 查跟进记录
        business.setTrackRecords(businessTrackRecordMapper.listTrackRecords(id));
        return business;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trackBusiness(BusinessTrackDto businessTrackDto) {
        Business existing = lockBusiness(businessTrackDto.getId());
        BusinessLifecycle.ensure(BusinessLifecycle.Action.TRACK, existing.getStatus());
        // 1. 更新商机：状态由服务端固定置为跟进中
        // 跟进时可以顺带更新客户资料，因此与商机同名的字段一并搬运；id 只用于定位
        Business business = new Business();
        business.setId(businessTrackDto.getId());
        business.setName(businessTrackDto.getName());
        business.setPhone(businessTrackDto.getPhone());
        business.setGender(businessTrackDto.getGender());
        business.setAge(businessTrackDto.getAge());
        business.setWechat(businessTrackDto.getWechat());
        business.setQq(businessTrackDto.getQq());
        business.setSubject(businessTrackDto.getSubject());
        business.setCourseId(businessTrackDto.getCourseId());
        business.setDegree(businessTrackDto.getDegree());
        business.setJobStatus(businessTrackDto.getJobStatus());
        business.setChannel(businessTrackDto.getChannel());
        business.setRemark(businessTrackDto.getRemark());
        business.setNextTime(businessTrackDto.getNextTime());
        business.setStatus(BusinessStatus.FOLLOWING.getCode());
        updateById(business);

        // 2. 新增一条商机跟进记录
        BusinessTrackRecord trackRecord = new BusinessTrackRecord();
        trackRecord.setBusinessId(businessTrackDto.getId());
        trackRecord.setUserId(UserHolder.getCurrentUser());
        trackRecord.setTrackStatus(businessTrackDto.getTrackStatus());
        trackRecord.setKeyItems(toKeyItems(businessTrackDto.getKeyItems()));
        trackRecord.setNextTime(businessTrackDto.getNextTime());
        trackRecord.setRecord(businessTrackDto.getRecord());
        businessTrackRecordMapper.insert(trackRecord);
    }

    @Override
    public PageResult<BusinessVO> getPoolBusinesses(BusinessPoolDto businessPoolDto) {
        Page<BusinessVO> page = new Page<>(businessPoolDto.getPage(), businessPoolDto.getPageSize());
        IPage<BusinessVO> businessPage = baseMapper.getPoolBusinesses(page, businessPoolDto,
                BusinessLifecycle.poolStatus());
        return new PageResult<>(businessPage.getTotal(), businessPage.getRecords());
    }

    /**
     * 按主键加行锁读取商机，不存在直接抛业务异常
     * <p>
     * 必须在事务内调用，理由与 {@code ClueServiceImpl#lockClue} 相同：
     * 状态流转是「先读状态再写状态」，不加锁时并发请求会同时通过守卫。
     */
    private Business lockBusiness(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ID_REQUIRED);
        }
        Business business = baseMapper.lockById(id);
        if (business == null) {
            throw new BusinessException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        return business;
    }

    /**
     * 意向课程必须真实存在（项目不使用物理外键，business.course_id 的完整性由 Service 兜底）
     */
    private void requireExistingCourse(Long courseId) {
        if (courseId != null && courseMapper.selectById(courseId) == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
    }

    /**
     * 归属人必须是存在且启用（status = 1）的用户，理由见 {@code ClueServiceImpl#requireAssignableUser}
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

    /**
     * 沟通重点以列表形式提交，落库前拼成字符串；列宽固定，超长时给出可读提示
     */
    private String toKeyItems(List<String> keyItems) {
        String joined = keyItems == null ? "[]" : keyItems.toString();
        if (joined.length() > MAX_KEY_ITEMS_LENGTH) {
            throw new BusinessException(ErrorCode.BUSINESS_KEY_ITEMS_TOO_LONG, MAX_KEY_ITEMS_LENGTH);
        }
        return joined;
    }
}
