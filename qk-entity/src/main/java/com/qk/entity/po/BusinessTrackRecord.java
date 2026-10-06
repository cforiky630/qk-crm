package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商机跟进记录实体类
 * 对应数据库表 business_track_record
 */
@Data
@TableName("business_track_record")
public class BusinessTrackRecord {

    /** 跟进记录ID，主键 */
    @TableId
    private Long id;

    /** 商机ID，关联商机表主键 */
    private Long businessId;

    /** 跟进人ID，关联用户表主键 */
    private Long userId;

    /** 跟进状态，1:接通, 2:拒绝, 3:无人接听 */
    private Integer trackStatus;

    /** 沟通重点 */
    private String keyItems;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 沟通纪要 */
    private String record;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
