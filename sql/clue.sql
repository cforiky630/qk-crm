-- 线索表 + 线索跟进记录表
-- 对应实体类 com.qk.entity.po.Clue、com.qk.entity.po.ClueTrackRecord
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。线索的手机号不允许重复。
--   3. 时间字段统一用 datetime NOT NULL，并加 DEFAULT CURRENT_TIMESTAMP（update_time 另有
--      ON UPDATE CURRENT_TIMESTAMP）作为数据库侧兜底：绕过 Service 的裸 SQL 写入也会带上时间。
--      业务写入仍以 Service 层为准，所以接口行为不变。
--   4. status 取值为 1:待分配, 2:待跟进, 3:跟进中, 4:伪线索, 5:转为商机，
--      与首页概览的 5 个统计口径一一对应。

CREATE TABLE IF NOT EXISTS `clue`
(
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '线索ID，主键',
    `phone`       char(11)         NOT NULL COMMENT '手机号',
    `channel`     tinyint unsigned NOT NULL COMMENT '线索来源，1:线上活动, 2:推广介绍',
    `activity_id` bigint unsigned  DEFAULT NULL COMMENT '关联活动的ID',
    `name`        varchar(20)      DEFAULT NULL COMMENT '客户姓名',
    `gender`      tinyint unsigned DEFAULT NULL COMMENT '性别，1:男, 2:女',
    `age`         tinyint unsigned DEFAULT NULL COMMENT '年龄',
    `wechat`      varchar(50)      DEFAULT NULL COMMENT '微信号',
    `qq`          varchar(20)      DEFAULT NULL COMMENT 'QQ号',
    `user_id`     bigint unsigned  DEFAULT NULL COMMENT '归属人ID，关联用户ID',
    `status`      tinyint unsigned NOT NULL DEFAULT 1 COMMENT '线索状态，1:待分配, 2:待跟进, 3:跟进中, 4:伪线索, 5:转为商机',
    `subject`     tinyint unsigned DEFAULT NULL COMMENT '意向学科，1:AI智能应用开发(Java), 2:AI大模型开发(Python), 3:AI鸿蒙开发, 4:AI大数据, 5:AI嵌入式, 6:AI测试, 7:AI运维',
    `level`       tinyint unsigned DEFAULT NULL COMMENT '意向等级，1:近期学习, 2:打算学习(考虑中), 3:进行了解, 4:打酱油',
    `next_time`   datetime         DEFAULT NULL COMMENT '下次跟进时间',
    `create_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_activity_id` (`activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='线索表';

CREATE TABLE IF NOT EXISTS `clue_track_record`
(
    `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '跟进记录ID，主键',
    `clue_id`      bigint unsigned  NOT NULL COMMENT '线索ID，关联线索表主键',
    `user_id`      bigint unsigned  NOT NULL COMMENT '跟进人ID，关联用户表主键',
    `subject`      tinyint unsigned DEFAULT NULL COMMENT '意向学科',
    `level`        tinyint unsigned DEFAULT NULL COMMENT '意向等级',
    `record`       varchar(100)     DEFAULT NULL COMMENT '跟进记录',
    `next_time`    datetime         DEFAULT NULL COMMENT '下次跟进时间',
    `type`         tinyint unsigned NOT NULL COMMENT '跟进类型，1:正常跟进, 0:伪线索',
    `false_reason` tinyint unsigned DEFAULT NULL COMMENT '伪线索原因，1:空号, 2:停机, 3:竞品, 4:无法联系, 5:其他',
    `create_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_clue_id` (`clue_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='线索跟进记录表';
