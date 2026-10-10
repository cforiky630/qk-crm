package com.qk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关
 * <p>
 * 项目此前没有调度能力，上传对象的兜底回收是第一个用到定时任务的地方。
 * 单独放一个配置类而不是写在启动类上：后续新增定时任务时不必再动引导类。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
