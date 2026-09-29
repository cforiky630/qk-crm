-- 客户表
-- 对应实体类 com.qk.Customer
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。客户的手机号不允许重复。
--   3. 时间字段统一用 datetime NOT NULL，由 Service 层写入，不使用数据库默认值。
--   4. business_id 记录客户是由哪个商机转化而来，手工新增的客户该字段为 NULL。
CREATE TABLE IF NOT EXISTS `customer`
(
    `id`          int unsigned     NOT NULL AUTO_INCREMENT COMMENT '客户ID，主键',
    `phone`       char(11)         NOT NULL COMMENT '手机号',
    `channel`     tinyint unsigned NOT NULL COMMENT '渠道来源，1:线上活动, 2:推广介绍',
    `name`        varchar(20)      DEFAULT NULL COMMENT '客户姓名',
    `gender`      tinyint unsigned DEFAULT NULL COMMENT '性别，1:男, 2:女',
    `age`         tinyint unsigned DEFAULT NULL COMMENT '年龄',
    `wechat`      varchar(50)      DEFAULT NULL COMMENT '微信号',
    `qq`          varchar(20)      DEFAULT NULL COMMENT 'QQ号',
    `degree`      tinyint unsigned DEFAULT NULL COMMENT '学历，1:高中, 2:中专, 3:大专, 4:本科, 5:硕士, 6:博士, 7:其他',
    `job_status`  tinyint unsigned DEFAULT NULL COMMENT '在职情况，1:在职, 0:离职',
    `subject`     tinyint unsigned DEFAULT NULL COMMENT '意向学科，1:AI智能应用开发(Java), 2:AI大模型开发(Python), 3:AI鸿蒙开发, 4:AI大数据, 5:AI嵌入式, 6:AI测试, 7:AI运维',
    `course_id`   int unsigned     DEFAULT NULL COMMENT '意向课程ID',
    `business_id` int unsigned     DEFAULT NULL COMMENT '关联的商机ID',
    `create_time` datetime         NOT NULL COMMENT '创建时间',
    `update_time` datetime         NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='客户表';
