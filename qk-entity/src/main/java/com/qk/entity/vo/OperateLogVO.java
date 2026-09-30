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

    /**
     * 操作模块（业务化名称，如「部门管理」「用户管理」）
     * <p>
     * 库里没有这一列，由 class_name 映射得到（见 OperateLogMapper.xml），
     * 对应页面原型日志列表的「操作模块」列；映射不到时为 null。
     */
    private String operateModule;

    /**
     * 操作类型（业务化名称，如「新增部门」「删除用户」）
     * <p>
     * 由 class_name + method_name 映射得到，对应页面原型的「操作类型」列；
     * 未配置映射的方法回退为原始方法名。
     */
    private String operateType;
}
