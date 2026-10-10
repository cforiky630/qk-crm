package com.qk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.dto.PageQuery;
import com.qk.entity.enums.UploadStatus;
import com.qk.entity.po.UploadFile;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传文件台账数据访问接口
 * <p>
 * 单表查询与状态变更都由 BaseMapper + wrapper 完成，wrapper 只写在这个 Mapper 的 default 方法里，
 * Service 不感知查询 DSL；本表没有多表 join，因此不需要同名 XML。
 */
@Mapper
public interface UploadFileMapper extends BaseMapper<UploadFile> {

    /**
     * 按访问地址查询台账
     * <p>
     * url 与 object_key 一一对应，最多命中一条；加 LIMIT 1 是为了在出现脏数据（重复登记）
     * 时也不会让 selectOne 抛 TooManyResultsException。
     */
    default UploadFile findByUrl(String url) {
        return selectOne(new LambdaQueryWrapper<UploadFile>()
                .eq(UploadFile::getUrl, url)
                .last("LIMIT 1"));
    }

    /**
     * 按对象键查询台账（内容寻址下 object_key 上唯一，最多一条）
     */
    default UploadFile findByObjectKey(String objectKey) {
        return selectOne(new LambdaQueryWrapper<UploadFile>()
                .eq(UploadFile::getObjectKey, objectKey)
                .last("LIMIT 1"));
    }

    /**
     * 待回收对象：尚未回收、且上传时间早于宽限期截止点的记录
     *
     * @param cutoff 宽限期截止点，早于它的才可能被回收
     * @param limit  单轮处理条数，会被限制在分页插件的单页上限内，避免一次拉取过多
     */
    default List<UploadFile> listRecyclable(LocalDateTime cutoff, int limit) {
        int size = Math.max(1, Math.min(limit, PageQuery.MAX_PAGE_SIZE));
        Page<UploadFile> page = new Page<>(1, size);
        return selectPage(page, new LambdaQueryWrapper<UploadFile>()
                .ne(UploadFile::getStatus, UploadStatus.RECYCLED.getCode())
                .lt(UploadFile::getCreateTime, cutoff)
                .orderByAsc(UploadFile::getCreateTime))
                .getRecords();
    }

    /**
     * 绑定：把对象记到某条业务数据名下（已回收的对象不再绑定，避免把已删除的对象记成在用）
     */
    default int markBound(Long id, String refType, Long refId, LocalDateTime bindTime) {
        return update(null, new LambdaUpdateWrapper<UploadFile>()
                .eq(UploadFile::getId, id)
                .ne(UploadFile::getStatus, UploadStatus.RECYCLED.getCode())
                .set(UploadFile::getStatus, UploadStatus.BOUND.getCode())
                .set(UploadFile::getRefType, refType)
                .set(UploadFile::getRefId, refId)
                .set(UploadFile::getBindTime, bindTime));
    }

    /**
     * 重复上传同一对象：刷新为「临时」并把宽限期从本次重新计算
     * <p>
     * 内容寻址下重复上传是覆盖写，台账不新增记录；把 create_time 刷新为本次时间，
     * 可以避免「刚重新上传、马上要提交表单」时被回收任务按旧时间当成过期对象删掉。
     */
    default int refreshOnReupload(Long id, LocalDateTime uploadTime) {
        return update(null, new LambdaUpdateWrapper<UploadFile>()
                .eq(UploadFile::getId, id)
                .set(UploadFile::getStatus, UploadStatus.TEMP.getCode())
                .set(UploadFile::getCreateTime, uploadTime)
                .setSql("retry_count = 0"));
    }

    /**
     * 抢占回收权：条件更新为「已回收」，返回受影响行数
     * <p>
     * 用一条原子的 UPDATE 抢占，而不是「先查后删」：多实例同时跑回收任务时，
     * 只有一个实例能拿到 1，另一个拿到 0 直接跳过，不会两个实例删同一个对象。
     */
    default int claimForRecycle(Long id) {
        return update(null, new LambdaUpdateWrapper<UploadFile>()
                .eq(UploadFile::getId, id)
                .ne(UploadFile::getStatus, UploadStatus.RECYCLED.getCode())
                .set(UploadFile::getStatus, UploadStatus.RECYCLED.getCode())
                .setSql("retry_count = retry_count + 1"));
    }

    /**
     * 回收失败回退：把误置为「已回收」的记录退回「临时」，留给下一轮重试
     */
    default int revertRecycle(Long id) {
        return update(null, new LambdaUpdateWrapper<UploadFile>()
                .eq(UploadFile::getId, id)
                .eq(UploadFile::getStatus, UploadStatus.RECYCLED.getCode())
                .set(UploadFile::getStatus, UploadStatus.TEMP.getCode()));
    }
}
