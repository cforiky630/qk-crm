-- 操作日志表
-- 对应实体类 com.qk.OperateLog
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），operate_user_id 通过逻辑约束关联用户表。
--   2. 时间字段统一用 datetime NOT NULL，由 Service 层（切面）写入，不使用数据库默认值。
--   3. 该表由 AOP 切面自动写入，只增不改，因此没有 update_time。
CREATE TABLE IF NOT EXISTS `operate_log`
(
    `id`              int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID，主键',
    `operate_user_id` int unsigned DEFAULT NULL COMMENT '操作用户ID',
    `operate_time`    datetime     NOT NULL COMMENT '操作时间',
    `class_name`      varchar(100) DEFAULT NULL COMMENT '操作的类名',
    `method_name`     varchar(100) DEFAULT NULL COMMENT '操作的方法名',
    `method_params`   varchar(1000) DEFAULT NULL COMMENT '方法参数',
    `return_value`    varchar(2000) DEFAULT NULL COMMENT '返回值',
    `cost_time`       bigint       DEFAULT NULL COMMENT '方法执行耗时，单位：ms',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='操作日志表';
