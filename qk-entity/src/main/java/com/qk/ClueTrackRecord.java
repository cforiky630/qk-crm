package com.qk;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索跟进记录实体类
 * 对应数据库表 clue_track_record
 */
@Data
@TableName("clue_track_record")
public class ClueTrackRecord {

    /** 跟进记录ID，主键 */
    @TableId
    private Integer id;

    /** 线索ID，关联线索表主键 */
    private Integer clueId;

    /** 跟进人ID，关联用户表主键 */
    private Integer userId;

    /** 意向学科 */
    private Integer subject;

    /** 意向等级 */
    private Integer level;

    /** 跟进记录 */
    private String record;

    /** 下次跟进时间 */
    private LocalDateTime nextTime;

    /** 跟进类型，1:正常跟进, 0:伪线索 */
    private Integer type;

    /** 伪线索原因，1:空号, 2:停机, 3:竞品, 4:无法联系, 5:其他 */
    private Integer falseReason;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
