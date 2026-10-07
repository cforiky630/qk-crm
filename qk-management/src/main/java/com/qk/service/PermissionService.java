package com.qk.service;

import com.qk.entity.vo.PermissionVO;

import java.util.List;

/**
 * 权限目录
 * <p>
 * 权限点定义在代码里（{@link com.qk.entity.enums.Permission}），但管理员界面需要能列出目录来勾选，
 * 因此提供一个只读的查询入口。目录本身不是数据，不会因为业务操作而改变。
 */
public interface PermissionService {

    /**
     * 全部权限点，顺序与枚举声明一致
     */
    List<PermissionVO> findAll();
}
