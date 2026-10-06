package com.qk.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分页查询公共参数
 * <p>
 * 所有分页查询 DTO 继承本类，分页参数只在这里定义一次：
 * <ul>
 *   <li>字段上带 Bean Validation 约束，因此 {@code ?page=}（空串被 Spring 转成 null）、
 *       {@code page=0}、{@code pageSize=0}、{@code pageSize=99999} 都会在进入 Service 前
 *       被拒绝并给出明确提示，而不是抛 NullPointerException 变成 500，
 *       或静默返回一页空数据；</li>
 *   <li>{@link #MAX_PAGE_SIZE} 与 MyBatis-Plus 分页插件的 {@code setMaxLimit} 同源，
 *       避免"插件允许 200、DTO 允许 1000"这种两处口径不一致；</li>
 *   <li>默认值 {@code 1 / 10} 保持与改造前完全一致，不传参时行为不变。</li>
 * </ul>
 * 使用方（控制器）必须在参数上加 {@code @Valid}，否则约束不会生效。
 */
@Data
public abstract class PageQuery {

    /** 单页条数上限，与分页插件的 setMaxLimit 保持一致 */
    public static final int MAX_PAGE_SIZE = 200;

    /** 页码，默认第一页 */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;

    /** 每页记录数，默认10条 */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数不能超过 200")
    private Integer pageSize = 10;
}
