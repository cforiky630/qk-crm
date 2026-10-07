package com.qk.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录结果 VO
 * <p>
 * 对应接口文档：登录成功响应的 data 部分（id、username、name、image、roleLabel、permissions、token）。
 * 注意：这里刻意不包含 password，避免密码摘要被序列化返回给前端。
 */
@Data
public class LoginResultVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 姓名 */
    private String name;

    /** 头像访问路径 */
    private String image;

    /** 角色标识，仅用于展示（角色名可以随便改，不要用它做权限判断） */
    private String roleLabel;

    /**
     * 该账号拥有的权限码，前端据此控制菜单与按钮显隐
     * <p>
     * 权限是接口级的稳定契约，角色是可变数据，因此前端判断一律用这个列表，不要用 roleLabel。
     */
    private List<String> permissions;

    /** JWT 令牌 */
    private String token;
}
