package com.qk.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增 / 修改入参
 * <p>
 * 关键点：<b>不包含 password</b>。新增时由 Service 使用默认密码（用户名+123 的 MD5），
 * 修改时忽略密码字段，因此客户端无法回传摘要来影响登录凭据（原实现是在 Service 里
 * 手动 setPassword(null) 兜底，现在从契约上就不开放）。
 */
@Data
public class UserSaveDto {

    /** 用户ID：修改时必填，新增时忽略 */
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名长度不能超过 20 个字符")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名长度不能超过 20 个字符")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer gender;

    private Integer status;

    private Long deptId;

    private Long roleId;

    @Size(max = 255, message = "头像地址长度不能超过 255 个字符")
    private String image;

    @Size(max = 50, message = "备注长度不能超过 50 个字符")
    private String remark;
}
