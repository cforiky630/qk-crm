-- 课程信息表
-- 对应实体类 com.qk.entity.po.Course
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。课程名称无唯一性要求，故不建唯一索引。
--   3. 时间字段统一用 datetime NOT NULL，并加 DEFAULT CURRENT_TIMESTAMP（update_time 另有
--      ON UPDATE CURRENT_TIMESTAMP）作为数据库侧兜底：绕过 Service 的裸 SQL 写入也会带上时间。
--      业务写入仍以 Service 层为准，所以接口行为不变。
-- 逻辑删除：is_deleted = 0 未删除、1 已删除。唯一索引建成函数索引
--           if(is_deleted = 0, 唯一列, NULL)：已删除行的索引键是 NULL，MySQL 视 NULL 互不相同，
--           所以删除后同名数据可以重新创建，反复删除同名记录也不会撞唯一键。
CREATE TABLE IF NOT EXISTS `course`
(
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '课程id，主键',
    `subject`     tinyint unsigned NOT NULL COMMENT '学科：1-AI智能应用开发(Java)，2-AI大模型开发(Python)，3-AI鸿蒙开发，4-AI大数据，5-AI嵌入式，6-AI测试，7-AI运维',
    `name`        varchar(50)      NOT NULL COMMENT '课程名称',
    `price`       int unsigned     NOT NULL COMMENT '价格，单位：元',
    `target`      tinyint unsigned NOT NULL COMMENT '适用人群：1-小白学员，2-中级程序员，3-初级程序员',
    `description` varchar(255)     DEFAULT NULL COMMENT '课程介绍',
    `create_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`  tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='课程信息表';
