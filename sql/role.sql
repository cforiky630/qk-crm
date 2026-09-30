-- 角色信息表
-- 对应实体类 com.qk.entity.Role
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），表之间的关联关系全部通过逻辑约束维护，
--      由业务代码（Service 层）保证数据一致性，避免外键带来的锁竞争与维护成本。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。
--   3. 时间字段统一用 datetime NOT NULL，由 Service 层写入，不使用数据库默认值。
CREATE TABLE IF NOT EXISTS `role`
(
    `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT '角色id，主键',
    `name`        varchar(20)  NOT NULL COMMENT '角色名称',
    `label`       varchar(30)  NOT NULL COMMENT '角色标识，全局唯一，权限判断用',
    `remark`      varchar(100) DEFAULT NULL COMMENT '备注说明',
    `create_time` datetime     NOT NULL COMMENT '创建时间',
    `update_time` datetime     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `label` (`label`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色信息表';
