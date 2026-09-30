package com.qk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志视图对象
 * <p>
 * 除日志本身的信息外，还需要返回操作人姓名（由 operate_log 关联 user 表得到）。
 */
@Data
public class OperateLogVO {

    /** ID */
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

    /** 操作用户姓名 */
    private String operateUserName;
}
