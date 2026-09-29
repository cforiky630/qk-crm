-- 部门信息表
-- 对应实体类 com.qk.Dept
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--      user 表通过 dept_id 关联本表，约束同样由 Service 层保证。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。部门名称不允许重复，且唯一索引名必须
--      与列名同为 name：GlobalExceptionHandler 依据异常信息中的 "dept.name"
--      匹配出「部门名称已存在」。
--   3. 时间字段统一用 datetime NOT NULL，由 Service 层写入，不使用数据库默认值。
--   4. status 取值为 1:正常, 0:停用，与 business、clue 等表一致，落库默认 1；
--      /depts/list 下拉框只返回 status = 1 的部门。
CREATE TABLE IF NOT EXISTS `dept`
(
    `id`          int unsigned     NOT NULL AUTO_INCREMENT COMMENT '部门id，主键',
    `name`        varchar(10)      NOT NULL COMMENT '部门名称',
    `status`      tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_time` datetime         NOT NULL COMMENT '创建时间',
    `update_time` datetime         NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='部门信息表';
