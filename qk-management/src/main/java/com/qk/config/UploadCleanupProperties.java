package com.qk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上传对象回收配置，对应 application.yml 中的 qk.upload.cleanup
 * <p>
 * 与 qk.jwt / qk.oss 同样的写法：默认值写在字段上，环境变量或本地配置可以覆盖。
 */
@Component
@ConfigurationProperties("qk.upload.cleanup")
@Data
public class UploadCleanupProperties {

    /** 是否启用定时回收。测试或不想跑调度时可置为 false */
    private boolean enabled = true;

    /** 定时任务执行时间（Spring cron，6 段），默认每天凌晨 3 点 */
    private String cron = "0 0 3 * * ?";

    /**
     * 宽限期（小时）：上传后多久仍没有被任何业务数据引用，才允许回收
     * <p>
     * 太短会误伤「上传后过一会儿才提交」的正常操作，太长则取消新增的脏数据留存久。
     */
    private int retentionHours = 24;

    /** 单轮最多处理条数，最终会被限制在分页插件单页上限（200）内 */
    private int batchSize = 200;
}
