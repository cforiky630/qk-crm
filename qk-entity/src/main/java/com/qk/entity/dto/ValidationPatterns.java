package com.qk.entity.dto;

/**
 * 入参校验用的公共正则
 * <p>
 * 放在常量里而不是在每个 DTO 上重复写一遍：同一个格式只有一处定义，
 * 调整时（例如将来要支持国际号码）不会漏改某个接口。
 */
public final class ValidationPatterns {

    /**
     * 中国大陆手机号：11 位、1 开头、第二位 3~9
     * <p>
     * 与库里 {@code phone char(11)} 的长度约束配合，把「手机号填了 abc」这类
     * 脏数据拦在入口，而不是让它落库后再由别人去猜。
     */
    public static final String PHONE = "^1[3-9]\\d{9}$";

    private ValidationPatterns() {
    }
}
