-- ============================================================================
-- 开发/演示环境：清空所有表 + 重置自增 + 灌入最小数据集
--
-- 警告：会 TRUNCATE 所有业务表，数据不可恢复，请勿在生产执行！
--       执行前先备份：mysqldump --default-character-set=utf8mb4 -uroot -p --databases qk > qk-backup.sql
--
-- 约定：
--   1. TRUNCATE 会把 AUTO_INCREMENT 重置为 1，所以下面的主键从 1 开始。
--   2. 用户密码遵循项目约定 md5(用户名 + 明文密码)，明文统一是 123，
--      因此登录用 123 即可（admin / zhangsan / lisi）。
--      摘要直接写死：不用数据库的 MD5() 函数（MySQL 9 上不可用），
--      而且可读性好——4e7bdb88… 就是 md5("zhangsan123")，与登录代码算出来的一致。
--   3. 自动化测试依赖「主键为 1 的部门」和「主键为 1 的角色(admin)」存在，别删这两条。
-- ============================================================================

SET NAMES utf8mb4;

TRUNCATE TABLE operate_log;
TRUNCATE TABLE business_track_record;
TRUNCATE TABLE business;
TRUNCATE TABLE clue_track_record;
TRUNCATE TABLE clue;
TRUNCATE TABLE customer;
TRUNCATE TABLE activity;
TRUNCATE TABLE course;
TRUNCATE TABLE user;
TRUNCATE TABLE role_permission;
TRUNCATE TABLE role;
TRUNCATE TABLE dept;

-- ---------- 部门 ----------
INSERT INTO dept (id, name, status, create_time, update_time) VALUES
(1, '市场部', 1, NOW(), NOW()),
(2, '销售部', 1, NOW(), NOW()),
(3, '客服部', 1, NOW(), NOW());

-- ---------- 角色 ----------
-- is_super = 1 的角色天然拥有全部权限，不写 role_permission 表
INSERT INTO role (id, name, label, remark, is_super, create_time, update_time) VALUES
(1, '管理员', 'admin', '超级管理员：天然拥有全部权限', 1, NOW(), NOW()),
(2, '线索专员', 'clue_operator', '负责跟进线索', 0, NOW(), NOW()),
(3, '商机专员', 'business_operator', '负责跟进商机', 0, NOW(), NOW());

-- ---------- 角色权限（授权与角色标识无关，改角色名不会影响这里）----------
-- 授权口径：
--   写权限严格按角色链路（线索专员只能动线索链路，商机专员只能动商机链路）；
--   读权限给全（user/dept/role/course/activity/clue/business/customer/log/report），
--   与改造前"查询类接口对所有登录用户开放"的可见范围保持一致 —— 当前前端是已构建产物、
--   菜单是静态的，收窄读权限会让专员点进页面就报无权限。要收紧就把对应 read 权限撤掉。

-- 线索专员：全部只读 + 线索链路 + 上传
INSERT INTO role_permission (role_id, permission, create_time) VALUES
(2, 'user:read', NOW()),
(2, 'dept:read', NOW()),
(2, 'role:read', NOW()),
(2, 'course:read', NOW()),
(2, 'activity:read', NOW()),
(2, 'clue:read', NOW()),
(2, 'business:read', NOW()),
(2, 'customer:read', NOW()),
(2, 'log:read', NOW()),
(2, 'report:read', NOW()),
(2, 'clue:create', NOW()),
(2, 'clue:track', NOW()),
(2, 'clue:mark_false', NOW()),
(2, 'clue:convert_business', NOW()),
(2, 'file:upload', NOW());

-- 商机专员：全部只读 + 商机链路 + 客户新增 + 上传
INSERT INTO role_permission (role_id, permission, create_time) VALUES
(3, 'user:read', NOW()),
(3, 'dept:read', NOW()),
(3, 'role:read', NOW()),
(3, 'course:read', NOW()),
(3, 'activity:read', NOW()),
(3, 'clue:read', NOW()),
(3, 'business:read', NOW()),
(3, 'customer:read', NOW()),
(3, 'log:read', NOW()),
(3, 'report:read', NOW()),
(3, 'business:create', NOW()),
(3, 'business:track', NOW()),
(3, 'business:back_to_pool', NOW()),
(3, 'business:convert_customer', NOW()),
(3, 'customer:create', NOW()),
(3, 'file:upload', NOW());

-- ---------- 用户（密码统一 123）----------
INSERT INTO user (id, username, password, name, phone, email, gender, status, dept_id, role_id, image, remark, create_time, update_time) VALUES
(1, 'admin', '0192023a7bbd73250516f069df18b500', '管理员', '13800000001', 'admin@qk.test', 1, 1, 1, 1, NULL, '系统管理员', NOW(), NOW()),
(2, 'zhangsan', '4e7bdb88640b376ac6646b8f1ecfb558', '张三', '13800000002', 'zhangsan@qk.test', 1, 1, 2, 2, NULL, '线索专员', NOW(), NOW()),
(3, 'lisi', 'c3cb6d12c40908943b64bc0681af47db', '李四', '13800000003', 'lisi@qk.test', 1, 1, 3, 3, NULL, '商机专员', NOW(), NOW());

-- ---------- 课程 ----------
INSERT INTO course (id, subject, name, price, target, description, create_time, update_time) VALUES
(1, 1, 'Java核心与AI开发基础', 599, 1, '面向零基础的 Java + AI 入门课程', NOW(), NOW()),
(2, 2, 'Python大模型应用开发', 899, 2, '有编程基础，进阶大模型应用开发', NOW(), NOW());

-- ---------- 活动 ----------
INSERT INTO activity (id, channel, name, start_time, end_time, description, type, discount, voucher, create_time, update_time) VALUES
(1, 1, '618 Java课程折扣', '2026-06-01 00:00:00', '2026-06-30 23:59:59', 'Java 课程限时 8 折', 1, 8.0, NULL, NOW(), NOW()),
(2, 2, '推广介绍送代金券', '2026-06-01 00:00:00', '2026-12-31 23:59:59', '老学员推荐立减 500', 2, NULL, 500, NOW(), NOW());

-- ---------- 线索：1 条待分配、1 条已转商机（带 1 条跟进记录）----------
INSERT INTO clue (id, phone, channel, activity_id, name, gender, age, wechat, qq, user_id, status, subject, level, next_time, create_time, update_time) VALUES
(1, '13900000001', 1, 1, '王小明', 1, 24, 'wxwangxm', '100200300', NULL, 1, 1, 2, NULL, NOW(), NOW()),
(2, '13900000002', 2, 2, '赵晓丽', 2, 22, 'wxzhaoxl', '300200100', 2, 5, 2, 1, NULL, NOW(), NOW());

INSERT INTO clue_track_record (id, clue_id, user_id, subject, level, record, next_time, type, false_reason, create_time) VALUES
(1, 2, 2, 2, 1, '已电话沟通，学员打算先了解一下课程', NULL, 1, NULL, NOW());

-- ---------- 商机：1 条由线索转入待分配、1 条已回收进公海池（带 1 条跟进记录）----------
INSERT INTO business (id, name, phone, gender, age, wechat, qq, subject, course_id, degree, job_status, channel, remark, status, user_id, clue_id, next_time, create_time, update_time) VALUES
(1, '赵晓丽', '13900000002', 2, 22, 'wxzhaoxl', '300200100', 2, 2, NULL, NULL, 2, '由线索转为商机', 1, NULL, 2, '2026-10-12 09:00:00', NOW(), NOW()),
(2, '周小八', '13900000004', 2, 28, 'wxzhouxb', '600500400', 1, 1, 4, 1, 1, '价格偏高，先回收', 4, 3, NULL, NULL, NOW(), NOW());

INSERT INTO business_track_record (id, business_id, user_id, track_status, key_items, next_time, record, create_time) VALUES
(1, 2, 3, 2, '[价格]', NULL, '觉得价格偏高，先回公海池', NOW());

-- ---------- 客户 ----------
INSERT INTO customer (id, phone, channel, name, gender, age, wechat, qq, degree, job_status, subject, course_id, business_id, create_time, update_time) VALUES
(1, '13900000005', 2, '吴小九', 1, 25, 'wxwuxj', '700800900', 4, 1, 1, 1, NULL, NOW(), NOW());
