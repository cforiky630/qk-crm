-- 活动信息表
-- 对应实体类 com.qk.entity.po.Activity
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。
--   3. 时间字段统一用 datetime NOT NULL，并加 DEFAULT CURRENT_TIMESTAMP（update_time 另有
--      ON UPDATE CURRENT_TIMESTAMP）作为数据库侧兜底：绕过 Service 的裸 SQL 写入也会带上时间。
--      业务写入仍以 Service 层为准，所以接口行为不变。
--   4. 折扣(discount)与代金券(voucher)只会二选一：折扣活动 discount 有值 voucher 为空，
--      代金券活动反之，因此两列都允许为 NULL。
-- 逻辑删除：is_deleted = 0 未删除、1 已删除。唯一索引建成函数索引
--           if(is_deleted = 0, 唯一列, NULL)：已删除行的索引键是 NULL，MySQL 视 NULL 互不相同，
--           所以删除后同名数据可以重新创建，反复删除同名记录也不会撞唯一键。
CREATE TABLE IF NOT EXISTS `activity`
(
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '活动id，主键',
    `channel`     tinyint unsigned NOT NULL COMMENT '渠道来源，1:线上活动, 2:推广介绍',
    `name`        varchar(50)      NOT NULL COMMENT '活动名称',
    `start_time`  datetime         NOT NULL COMMENT '开始时间',
    `end_time`    datetime         NOT NULL COMMENT '结束时间',
    `description` varchar(255)     DEFAULT NULL COMMENT '活动简介',
    `type`        tinyint unsigned NOT NULL COMMENT '活动类型，1:课程折扣, 2:代金券',
    `discount`    decimal(3, 1)    DEFAULT NULL COMMENT '课程折扣，例如 8.0 表示 8 折',
    `voucher`     int unsigned     DEFAULT NULL COMMENT '课程代金券，单位：元',
    `create_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`  tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='活动信息表';
