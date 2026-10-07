package com.qk.service.impl;

import com.qk.entity.enums.Permission;
import com.qk.entity.vo.PermissionVO;
import com.qk.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Override
    public List<PermissionVO> findAll() {
        return Arrays.stream(Permission.values()).map(PermissionVO::from).toList();
    }
}
