package com.qk.vo;

import lombok.Data;

/**
 * 登录结果 VO
 * <p>
 * 对应接口文档：登录成功响应的 data 部分（id、username、name、image、roleLabel、token）。
 * 注意：这里刻意不包含 password，避免密码摘要被序列化返回给前端。
 */
@Data
public class LoginResultVo {

    /** 用户ID */
    private Integer id;

    /** 用户名 */
    private String username;

    /** 姓名 */
    private String name;

    /** 头像访问路径 */
    private String image;

    /** 角色标识，前端据此控制菜单与按钮权限 */
    private String roleLabel;

    /** JWT 令牌 */
    private String token;
}
