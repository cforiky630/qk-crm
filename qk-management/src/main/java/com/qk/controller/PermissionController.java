package com.qk.controller;

import com.qk.common.Result;
import com.qk.entity.enums.Permission;
import com.qk.entity.vo.PermissionVO;
import com.qk.interceptor.RequirePermission;
import com.qk.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限目录
 * <p>
 * 供管理员配置角色权限时渲染勾选项；能看角色的人就能看这个目录（目录不含敏感信息）。
 */
@Slf4j
@RestController
public class PermissionController {

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @RequirePermission(Permission.ROLE_READ)
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> listPermissions() {
        log.info("查询权限目录");
        return Result.success(permissionService.findAll());
    }
}
