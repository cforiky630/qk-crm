-- 上传文件台账表
-- 对应实体类 com.qk.entity.po.UploadFile
--
-- 用途：每一次成功上传（POST /upload）都登记一条，是下面两件事的依据
--   1. 业务数据删除 / 换头像时，同步删除对象存储里的旧对象；
--   2. 定时任务兜底回收「超过宽限期且不再被任何业务数据引用」的对象，
--      典型场景是上传了头像却取消了新增（从未被引用），以及删除用户后遗留的头像。
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），ref_id / uploader_id 通过逻辑约束关联业务表。
--   2. 对象名按内容寻址（同一用户 + 相同内容 = 同一对象），因此 object_key 上建唯一索引，
--      保证「一个对象一条台账」，回收时不会出现「删一条误伤另一条」。
--   3. 是否回收最终以「当前是否仍被业务数据引用」为准，status 只用于记账与观测。
CREATE TABLE IF NOT EXISTS `upload_file`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID，主键',
    `object_key`   varchar(255)    NOT NULL COMMENT '对象存储里的键，如 images/12/<md5>.png',
    `url`          varchar(255)    NOT NULL COMMENT '对外访问地址（写进业务表的那个值）',
    `uploader_id`  bigint unsigned DEFAULT NULL COMMENT '上传人ID',
    `content_md5`  char(32)        NOT NULL COMMENT '内容MD5（内容寻址命名的依据）',
    `size`         int unsigned    NOT NULL COMMENT '字节数',
    `content_type` varchar(64)     DEFAULT NULL COMMENT 'MIME 类型',
    `status`       tinyint unsigned NOT NULL DEFAULT 0 COMMENT '状态：0-临时（待绑定），1-已绑定，2-已回收',
    `ref_type`     varchar(32)     DEFAULT NULL COMMENT '被引用时的业务类型，如 user',
    `ref_id`       bigint unsigned DEFAULT NULL COMMENT '被引用时的业务主键',
    `bind_time`    datetime        DEFAULT NULL COMMENT '绑定（被业务数据引用）时间',
    `retry_count`  tinyint unsigned NOT NULL DEFAULT 0 COMMENT '回收尝试次数',
    `create_time`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间（重复上传同一对象时刷新）',
    `update_time`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_object_key` (`object_key`),
    KEY `idx_url` (`url`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_ref` (`ref_type`, `ref_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='上传文件台账表';
