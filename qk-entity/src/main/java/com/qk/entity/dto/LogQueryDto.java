package com.qk.entity.dto;

import lombok.Data;

/**
 * 操作日志列表查询参数封装
 * 对应 /logs?operateUserName=林冲&operateModule=部门管理&operateType=新增部门&page=1&pageSize=10
 */
@Data
public class LogQueryDto {

    /** 操作人姓名 */
    private String operateUserName;

    /** 操作模块，模糊匹配（如「部门」「用户」，对应页面原型的操作模块搜索框） */
    private String operateModule;

    /** 操作类型，模糊匹配（如「新增」「删除用户」，对应页面原型的操作类型搜索框） */
    private String operateType;

    /** 页码，默认第一页 */
    private Integer page = 1;

    /** 每页记录数，默认10条 */
    private Integer pageSize = 10;
}
