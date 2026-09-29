-- 课程信息表
-- 对应实体类 com.qk.Course
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。课程名称无唯一性要求，故不建唯一索引。
--   3. 时间字段统一用 datetime NOT NULL，由 Service 层写入，不使用数据库默认值。
CREATE TABLE IF NOT EXISTS `course`
(
    `id`          int unsigned     NOT NULL AUTO_INCREMENT COMMENT '课程id，主键',
    `subject`     tinyint unsigned NOT NULL COMMENT '学科：1-AI智能应用开发(Java)，2-AI大模型开发(Python)，3-AI鸿蒙开发，4-AI大数据，5-AI嵌入式，6-AI测试，7-AI运维',
    `name`        varchar(50)      NOT NULL COMMENT '课程名称',
    `price`       int unsigned     NOT NULL COMMENT '价格，单位：元',
    `target`      tinyint unsigned NOT NULL COMMENT '适用人群：1-小白学员，2-中级程序员',
    `description` varchar(255)     DEFAULT NULL COMMENT '课程介绍',
    `create_time` datetime         NOT NULL COMMENT '创建时间',
    `update_time` datetime         NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='课程信息表';
