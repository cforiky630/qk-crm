-- 活动信息表
-- 对应实体类 com.qk.entity.Activity
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。
--   3. 时间字段统一用 datetime NOT NULL，由 Service 层写入，不使用数据库默认值。
--   4. 折扣(discount)与代金券(voucher)只会二选一：折扣活动 discount 有值 voucher 为空，
--      代金券活动反之，因此两列都允许为 NULL。
CREATE TABLE IF NOT EXISTS `activity`
(
    `id`          int unsigned     NOT NULL AUTO_INCREMENT COMMENT '活动id，主键',
    `channel`     tinyint unsigned NOT NULL COMMENT '渠道来源，1:线上活动, 2:推广介绍',
    `name`        varchar(50)      NOT NULL COMMENT '活动名称',
    `start_time`  datetime         NOT NULL COMMENT '开始时间',
    `end_time`    datetime         NOT NULL COMMENT '结束时间',
    `description` varchar(255)     DEFAULT NULL COMMENT '活动简介',
    `type`        tinyint unsigned NOT NULL COMMENT '活动类型，1:课程折扣, 2:代金券',
    `discount`    decimal(3, 1)    DEFAULT NULL COMMENT '课程折扣，例如 8.0 表示 8 折',
    `voucher`     int unsigned     DEFAULT NULL COMMENT '课程代金券，单位：元',
    `create_time` datetime         NOT NULL COMMENT '创建时间',
    `update_time` datetime         NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='活动信息表';
