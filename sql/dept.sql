-- 部门信息表
-- 对应实体类 com.qk.entity.po.Dept
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--      user 表通过 dept_id 关联本表，约束同样由 Service 层保证。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。部门名称不允许重复，唯一索引命名为
--      uk_name，异常信息形如 "dept.uk_name"（GlobalExceptionHandler 据此给出中文提示）。
--   3. 时间字段统一用 datetime NOT NULL，并加 DEFAULT CURRENT_TIMESTAMP（update_time 另有
--      ON UPDATE CURRENT_TIMESTAMP）作为数据库侧兜底：绕过 Service 的裸 SQL 写入也会带上时间。
--      业务写入仍以 Service 层为准，所以接口行为不变。
--   4. status 取值为 1:正常, 0:停用，与 business、clue 等表一致，落库默认 1；
--      /depts/list 下拉框只返回 status = 1 的部门。
-- 逻辑删除：is_deleted = 0 未删除、1 已删除。唯一索引建成函数索引
--           if(is_deleted = 0, 唯一列, NULL)：已删除行的索引键是 NULL，MySQL 视 NULL 互不相同，
--           所以删除后同名数据可以重新创建，反复删除同名记录也不会撞唯一键。
CREATE TABLE IF NOT EXISTS `dept`
(
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '部门id，主键',
    `name`        varchar(10)      NOT NULL COMMENT '部门名称',
    `status`      tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`  tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` ((if(`is_deleted` = 0, `name`, NULL)))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='部门信息表';
