package com.qk;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 对应数据库表 operate_log，由 AOP 切面自动写入
 */
@Data
@TableName("operate_log")
public class OperateLog {

    /** ID，主键 */
    @TableId
    private Integer id;

    /** 操作用户ID */
    private Integer operateUserId;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 操作的类名 */
    private String className;

    /** 操作的方法名 */
    private String methodName;

    /** 方法参数 */
    private String methodParams;

    /** 返回值 */
    private String returnValue;

    /** 方法执行耗时，单位：ms */
    private Long costTime;
}
