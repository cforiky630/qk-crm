-- 商机表 + 商机跟进记录表
-- 对应实体类 com.qk.entity.Business、com.qk.entity.BusinessTrackRecord
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。商机的手机号不允许重复。
--   3. 时间字段统一用 datetime NOT NULL，由 Service 层写入，不使用数据库默认值。
--   4. status 取值为 1:待分配, 2:待跟进, 3:跟进中, 4:回收, 5:转客户，
--      其中 4 表示已被踢回公海池。

CREATE TABLE IF NOT EXISTS `business`
(
    `id`          int unsigned     NOT NULL AUTO_INCREMENT COMMENT '商机ID，主键',
    `name`        varchar(20)      DEFAULT NULL COMMENT '客户姓名',
    `phone`       char(11)         NOT NULL COMMENT '手机号',
    `gender`      tinyint unsigned DEFAULT NULL COMMENT '性别，1:男, 2:女',
    `age`         tinyint unsigned DEFAULT NULL COMMENT '年龄',
    `wechat`      varchar(50)      DEFAULT NULL COMMENT '微信号',
    `qq`          varchar(20)      DEFAULT NULL COMMENT 'QQ号',
    `subject`     tinyint unsigned DEFAULT NULL COMMENT '意向学科，1:AI智能应用开发(Java), 2:AI大模型开发(Python), 3:AI鸿蒙开发, 4:AI大数据, 5:AI嵌入式, 6:AI测试, 7:AI运维',
    `course_id`   int unsigned     DEFAULT NULL COMMENT '意向课程，课程ID',
    `degree`      tinyint unsigned DEFAULT NULL COMMENT '学历，1:高中, 2:中专, 3:大专, 4:本科, 5:硕士, 6:博士, 7:其他',
    `job_status`  tinyint unsigned DEFAULT NULL COMMENT '在职情况，1:在职, 0:离职',
    `channel`     tinyint unsigned NOT NULL COMMENT '渠道来源，1:线上活动, 2:推广介绍',
    `remark`      varchar(50)      DEFAULT NULL COMMENT '备注',
    `status`      tinyint unsigned NOT NULL DEFAULT 1 COMMENT '商机状态，1:待分配, 2:待跟进, 3:跟进中, 4:回收, 5:转客户',
    `user_id`     int unsigned     DEFAULT NULL COMMENT '归属人ID，关联用户表主键',
    `clue_id`     int unsigned     DEFAULT NULL COMMENT '关联线索ID',
    `next_time`   datetime         DEFAULT NULL COMMENT '下次跟进时间',
    `create_time` datetime         NOT NULL COMMENT '创建时间',
    `update_time` datetime         NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商机表';

CREATE TABLE IF NOT EXISTS `business_track_record`
(
    `id`           int unsigned     NOT NULL AUTO_INCREMENT COMMENT '跟进记录ID，主键',
    `business_id`  int unsigned     NOT NULL COMMENT '商机ID，关联商机表主键',
    `user_id`      int unsigned     NOT NULL COMMENT '跟进人ID，关联用户表主键',
    `track_status` tinyint unsigned NOT NULL COMMENT '跟进状态，1:接通, 2:拒绝, 3:无人接听',
    `key_items`    varchar(50)      DEFAULT NULL COMMENT '沟通重点',
    `next_time`    datetime         DEFAULT NULL COMMENT '下次跟进时间',
    `record`       varchar(100)     DEFAULT NULL COMMENT '沟通纪要',
    `create_time`  datetime         NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='商机跟进记录表';
