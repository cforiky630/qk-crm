-- 用户表
-- 对应实体类 com.qk.entity.po.User
--
-- 建表约定：
--   1. 不使用物理外键约束（FOREIGN KEY），dept_id、role_id 通过逻辑约束分别关联
--      部门表与角色表，由业务代码（Service 层）保证数据一致性。
--   2. 唯一性等业务约束仍通过 UNIQUE 索引落库，由 GlobalExceptionHandler 捕获
--      DuplicateKeyException 转换成友好提示。用户名、手机号、邮箱均不允许重复，唯一索引
--      命名为 uk_username / uk_phone / uk_email；dept_id、role_id 供关联查询与删除守卫
--      的计数使用，补 idx_dept_id / idx_role_id 普通索引。
--   3. 时间字段统一用 datetime NOT NULL，并加 DEFAULT CURRENT_TIMESTAMP（update_time 另有
--      ON UPDATE CURRENT_TIMESTAMP）作为数据库侧兜底：绕过 Service 的裸 SQL 写入也会带上时间。
--      业务写入仍以 Service 层为准，所以接口行为不变。
--   4. password 列存放 md5(用户名 + 明文密码) 的摘要，不存明文，实体上标注
--      @JsonProperty(WRITE_ONLY)，任何响应都不会返回该字段。
--
-- 默认账号：
--   仅内置一个管理员账号，用户名 admin、密码 123。
--   该账号的 role_id 为空，登录后 roleLabel 为 null：角色需先通过 POST /roles 创建，
--   再通过 PUT /users 把 role_id 绑定到本账号。后端不校验角色，绑定只影响前端菜单渲染。
use qk;

-- 逻辑删除：is_deleted = 0 未删除、1 已删除。唯一索引建成函数索引
--           if(is_deleted = 0, 唯一列, NULL)：已删除行的索引键是 NULL，MySQL 视 NULL 互不相同，
--           所以删除后同名数据可以重新创建，反复删除同名记录也不会撞唯一键。
CREATE TABLE IF NOT EXISTS `user`
(
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT 'id, 主键',
    `username`    varchar(20)      NOT NULL COMMENT '用户名，唯一',
    `password`    varchar(64)      NOT NULL COMMENT '密码',
    `name`        varchar(20)      NOT NULL COMMENT '姓名',
    `phone`       char(11)         NOT NULL COMMENT '手机号，唯一',
    `email`       varchar(50)      NOT NULL COMMENT '邮箱，唯一',
    `gender`      tinyint unsigned NOT NULL COMMENT '性别，1: 男，2: 女',
    `status`      tinyint unsigned NOT NULL COMMENT '状态，1: 正常，0: 停用',
    `dept_id`     bigint unsigned  DEFAULT NULL COMMENT '部门id，关联部门表主键',
    `role_id`     bigint unsigned  DEFAULT NULL COMMENT '角色id，关联角色表主键',
    `image`       varchar(255)     DEFAULT NULL COMMENT '头像url',
    `remark`      varchar(50)      DEFAULT NULL COMMENT '备注，50字以内',
    `create_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`  tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` ((if(`is_deleted` = 0, `username`, NULL))),
    UNIQUE KEY `uk_phone` ((if(`is_deleted` = 0, `phone`, NULL))),
    UNIQUE KEY `uk_email` ((if(`is_deleted` = 0, `email`, NULL))),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';

-- 默认管理员：admin / 123
-- password = md5('admin' + '123')
INSERT INTO `user` (`username`, `password`, `name`, `phone`, `email`, `gender`, `status`,
                    `dept_id`, `role_id`, `image`, `remark`, `create_time`, `update_time`)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '管理员', '13800000000', 'admin@example.com',
        1, 1, NULL, NULL, NULL, '系统管理员', NOW(), NOW());
