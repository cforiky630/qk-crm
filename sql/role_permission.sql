-- 角色-权限映射表
-- 对应实体类 com.qk.entity.po.RolePermission
--
-- 作用：把「角色」（数据，可自由增删改）与「权限点」（代码里的稳定契约，
--       见 com.qk.entity.enums.Permission）关联起来，管理员在运行时配置，
--       新增/改名/删除角色都不会影响任何人的权限。
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），role_id 的完整性由 Service 层保证。
--   2. 这是**纯映射表**：不做逻辑删除，授权采用"先按 role_id 清空、再批量写入"的
--      覆盖式更新，因此没有 is_deleted，也不会堆积历史行。
--   3. 唯一索引 uk_role_permission 保证同一角色下同一权限只有一行；
--      唯一索引命名遵循项目规约（uk_ / idx_）。
--
-- 注意：超级管理员角色（role.is_super = 1）不写本表，它天然拥有全部权限。
CREATE TABLE IF NOT EXISTS `role_permission`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`     bigint unsigned NOT NULL COMMENT '角色ID，关联 role.id',
    `permission`  varchar(50)     NOT NULL COMMENT '权限码，取值见 com.qk.entity.enums.Permission',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission`),
    KEY `idx_permission` (`permission`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色权限映射表';
