<div align="center">

<h1>轻客管家 · QK</h1>

<p><b>教培行业销售 CRM 的后端服务</b><br>
覆盖「线索 → 分配 → 跟进 → 转商机 → 再分配 → 转客户」的完整销售链路，<br>
以及部门、角色、用户、课程、活动等基础数据维护</p>

<p>
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.8-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.17-blue" alt="MyBatis-Plus">
  <img src="https://img.shields.io/badge/MySQL-8.0%2B-4479A1" alt="MySQL">
  <img src="https://img.shields.io/badge/tests-195%20passed-success" alt="Tests">
</p>

</div>

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置项](#配置项)
- [接口一览](#接口一览)
- [数据库](#数据库)
- [测试](#测试)
- [项目约定](#项目约定)
- [常见问题](#常见问题)
- [Roadmap](#roadmap)
- [已知限制](#已知限制)

## 项目简介

教培机构的销售团队需要把「网上来的一个手机号」一步步跟成「成交客户」。这个后端把这条链路做成了系统：

1. **线索**由活动或推广进来，管理员分配给**线索专员**；
2. 专员跟进后要么标记为**伪线索**（无效），要么**转成商机**；
3. 商机再由管理员分配给**商机专员**，跟进后要么**踢回公海池**（回收），要么**转为客户**（成交）；
4. 首页概览实时给出线索与商机各阶段的数量，操作日志记录每一次增删改。

三个内置角色对应三种视角：`admin`（管理员，负责分配）、`clue_operator`（线索专员）、`business_operator`（商机专员）。

## 功能特性

| 模块 | 能力 |
| --- | --- |
| 线索管理 | 列表（多条件 + 分页）、新增、分配、跟进（写跟进记录）、标记伪线索、转商机、线索池 |
| 商机管理 | 列表、新增、分配、跟进（写跟进记录）、踢回公海、转客户、公海池 |
| 客户管理 | 列表（带意向课程名）、新增、详情、修改 |
| 用户管理 | 列表（多表关联出部门/角色名）、新增（默认密码）、修改、批量删除、按角色/部门筛选 |
| 基础数据 | 部门、角色、课程的增删改查与下拉列表 |
| 活动管理 | 活动的增删改查、按渠道/类型/活动状态（未开始、进行中、已结束）筛选 |
| 统计分析 | 首页概览（线索与商机各阶段数量） |
| 系统能力 | JWT 登录鉴权、图片上传到阿里云 OSS、AOP 操作日志、统一响应与全局异常处理 |

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 | 语言级别 21 |
| Spring Boot | 4.0.8 | Boot 4：Web 用 `spring-boot-starter-webmvc`，JSON 是 **Jackson 3**（`tools.jackson.*`） |
| MyBatis-Plus | 3.5.17 | 用 Boot 4 专用 starter `mybatis-plus-spring-boot4-starter` |
| MySQL | 8.0+ / 9.x | 库名 `qk`，11 张表，不使用物理外键 |
| Hutool | 5.8.47 | 加密、JWT、Bean 拷贝等 |
| 阿里云 OSS SDK | V2 0.6.0 | 图片上传 |
| Lombok | 1.18.48 | 简化实体样板代码 |

## 项目结构

```
qk-parent
├── qk-common/          通用能力（包根 com.qk.common）
│   └── com.qk.common   Result / ResultCode、ErrorCode 错误码、系统异常告警出口与默认实现、文件存储端口 FileStorage 与图片格式 ImageFormat、OSS 客户端与实现、JWT 工具、UserHolder、业务异常
├── qk-entity/          实体 / DTO / VO / 枚举（包根 com.qk.entity）
│   └── com.qk.entity
│       ├── po          表映射实体（Dept、User、Clue、Business…，与表一一对应）
│       ├── dto         入参：PageQuery（分页基类）+ XxxQueryDto、ClueTrackDto、MarkFalseClueDto…
│       ├── vo          出参：UserVO、ClueVO、BusinessVO、CustomerVO、DeptVO、RoleVO、CourseVO、ActivityVO、OverviewVO、LoginResultVO、StatusCountVO、PageResult（分页外壳）
│       └── enums       状态枚举：ClueStatus、BusinessStatus、ClueTrackType、ActivityStatus（查询用）
├── qk-management/      可启动模块（包根 com.qk）
│   └── com.qk
│       ├── controller  接口层
│       ├── service     业务层（接口 + impl）
│       ├── mapper      数据访问层（接口 + 同包同名 XML）
│       ├── domain      领域规则：ClueLifecycle / BusinessLifecycle 状态机
│       ├── aspect      操作日志切面 + @LogOperation
│       ├── handler     全局异常处理 + MyBatis-Plus 字段填充
│       ├── interceptor 登录校验拦截器
│       └── config      MyBatis-Plus、Jackson、Web、OSS 配置
│   └── resources
│       └── com/qk/mapper   Mapper XML（与接口同包同名、namespace 为接口全限定名）
├── sql/                建表脚本 + 最小数据集脚本（+ _backup 备份目录）
└── docs/openapi.yaml   接口契约（36 个路径项 / 57 个操作）
```

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+（本地 3306）

### 1. 建库建表

```bash
mysql -uroot -p -e "create database if not exists qk default charset utf8mb4"
mysql -uroot -p qk < sql/dept.sql
mysql -uroot -p qk < sql/role.sql
mysql -uroot -p qk < sql/user.sql
mysql -uroot -p qk < sql/course.sql
mysql -uroot -p qk < sql/activity.sql
mysql -uroot -p qk < sql/clue.sql
mysql -uroot -p qk < sql/business.sql
mysql -uroot -p qk < sql/customer.sql
mysql -uroot -p qk < sql/operate_log.sql
```

> PowerShell 不支持 `<` 重定向，改用 `cmd /c "mysql -uroot -p qk < sql/dept.sql"`，或 `Get-Content sql\dept.sql -Raw | mysql -uroot -p qk`。

### 2. 灌入最小数据集（可选，方便本地联调）

```bash
mysql -uroot -p qk < sql/reset_and_seed.sql
```

> ⚠️ 该脚本会 **TRUNCATE 所有业务表** 并重置自增，仅用于开发/演示环境。内置账号密码都是 `123`：
>
> | 用户名 | 姓名 | 角色 |
> | --- | --- | --- |
> | `admin` | 管理员 | admin |
> | `zhangsan` | 张三 | clue_operator |
> | `lisi` | 李四 | business_operator |

### 3. 启动

```bash
mvn spring-boot:run -pl qk-management
# 或：mvn clean package -DskipTests && java -jar qk-management/target/qk-management-1.1.12.jar
```

未配置 `server.port`，默认监听 **http://localhost:8080**。

### 4. 验证

```bash
# 登录拿令牌
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}'

# 带上令牌访问业务接口（除 /login 外所有接口都需要这个头）
curl -H "token: <上一步返回的 data.token>" \
  "http://localhost:8080/users?page=1&pageSize=5"

# 首页概览
curl -H "token: <token>" http://localhost:8080/report/overview
```

## 配置项

数据库密码、OSS AccessKey、JWT 密钥**不进仓库**：`application.yml` 只保留环境变量占位（默认值为空），真实值放在 `config/application-local.yml`。该文件已被 `.gitignore` 忽略，也可以改用 `~/.qk/application-local.yml`，两个位置都会被自动加载，且优先级高于 `application.yml`。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `QK_DB_USERNAME` | `root` | 数据库账号 |
| `QK_DB_PASSWORD` | `123456` | 数据库密码（本地开发库，非敏感） |
| `QK_OSS_REGION` | `cn-beijing` | OSS 区域（`qk-bucket-oss` 桶位于华北2） |
| `QK_OSS_ACCESS_KEY_ID` | 空 | OSS 凭证，本地写在 `config/application-local.yml`，生产走环境变量并定期轮换 |
| `QK_OSS_ACCESS_KEY_SECRET` | 空 | 同上 |
| `QK_JWT_SECRET` | 开发占位值 | JWT 签名密钥，生产必须替换 ≥32 字节的随机值 |

日志输出到**项目根目录的 `./logs`**（已在 `.gitignore` 中忽略），历史归档在同级 `history/`。注意 `./logs` 是相对路径，写到哪取决于启动时的工作目录：

| 启动方式 | 工作目录 | 日志位置 |
| --- | --- | --- |
| IDEA（需把运行配置的 `Working directory` 设为 `$PROJECT_DIR$`） | 项目根 | `./logs` |
| `mvn spring-boot:run -pl qk-management` / `java -jar ...`（在项目根执行） | 项目根 | `./logs` |
| `mvn test` | 模块目录 | **不写文件**，仅输出到控制台（`src/test/resources/logback-test.xml`） |

> IDEA 的运行配置不随仓库分发。若不设置工作目录，IDEA 默认使用模块目录 `qk-management`，日志会写到 `qk-management/logs`——该目录同样被 `.gitignore` 忽略，只是会多出一个日志目录。

SQL 日志走 SLF4J，生产把 `logging.level.com.qk` 调成 `info` 即可关闭。

## 接口一览

完整契约（字段、示例、错误码）见 **[docs/openapi.yaml](docs/openapi.yaml)**（36 个路径项 / 57 个操作）。

| 模块 | 基础路径 | 说明 |
| --- | --- | --- |
| 认证 | `/login` | 登录，签发 JWT |
| 用户 | `/users` | 列表 / 详情 / 新增 / 修改 / 批量删除 / 按角色（只返回正常状态，供分配人员下拉）/ 按部门 |
| 部门 | `/depts` | 增删改查 + `/depts/list` |
| 角色 | `/roles` | 增删改查 + `/roles/list` |
| 课程 | `/courses` | 增删改查 + 按学科筛选 |
| 活动 | `/activities` | 增删改查 + 按渠道/类型/活动状态筛选 |
| 线索 | `/clues` | 列表（多条件，含按状态筛选）/ 详情 / 新增 / 分配 / 跟进 / 伪线索 / 转商机 / 线索池（只放伪线索） |
| 商机 | `/businesses` | 列表（多条件，含按状态筛选）/ 详情 / 新增 / 分配 / 跟进 / 回收 / 转客户 / 公海池 |
| 客户 | `/customers` | 列表 / 详情 / 新增 / 修改 |
| 系统 | `/logs`、`/report/overview`、`/upload` | 操作日志（带操作模块/操作类型，支持按模块、类型、操作人筛选）、首页概览、图片上传 |

**通用约定**

- 统一响应 `Result`：`code` 为 `1` 成功、`0` 失败（业务失败同样返回 HTTP 200，按 `code` 判断）；分页统一 `{ total, rows }`，`pageSize` 单页上限 200。
- 状态码语义：`200` 成功或业务失败 · `400` 请求体/参数处理失败 · `401` 未登录或令牌对应的账号不可用（响应体为空）· `403` 已登录但角色不满足接口要求（响应体是 `Result`，`code = 0`）· `404` 路径不存在 · `405` 请求方法不支持 · `415` 请求的 Content-Type 不支持 · `500` 服务端异常。
- 框架层错误不再降级成 500：`GlobalExceptionHandler` 显式接住 404 / 405 / 415 / 400，只有真正的代码或依赖缺陷才返回 `500 + 「系统繁忙,请稍后重试」` 并触发运维告警。
- 除 `POST /login` 外都要带请求头 `token`（JWT，默认 24 小时有效）。

## 数据库

11 张表，全部不使用物理外键（关联关系由业务层保证），时间字段由服务层写入：

| 表 | 说明 |
| --- | --- |
| `dept` / `role` / `user` | 部门、角色、用户 |
| `course` / `activity` | 课程、活动 |
| `clue` / `clue_track_record` | 线索、线索跟进记录 |
| `business` / `business_track_record` | 商机、商机跟进记录 |
| `customer` | 客户 |
| `operate_log` | 操作日志（AOP 自动写入） |

建表脚本在 [`sql/`](sql/)，最小数据集在 [`sql/reset_and_seed.sql`](sql/reset_and_seed.sql)。

字段与索引遵循《阿里巴巴 Java 开发手册》数据库规约：

| 规约 | 落地方式 |
| --- | --- |
| id 必为 `bigint unsigned` | 11 张表的主键与全部逻辑外键（`dept_id`/`role_id`/`user_id`/`course_id`/`activity_id`/`clue_id`/`business_id`/`operate_user_id`）统一 `bigint unsigned`，Java 侧对应 `Long` |
| 索引命名 | 唯一索引 `uk_列名`（`uk_username`/`uk_phone`/`uk_email`/`uk_label`/`uk_name`），逻辑外键补 `idx_列名` 普通索引 |
| 禁用外键与级联 | 全部表不使用物理外键，关联完整性由 Service 层的删除守卫保证 |
| 时间字段兜底 | `create_time` 用 `DEFAULT CURRENT_TIMESTAMP`、`update_time` 用 `ON UPDATE CURRENT_TIMESTAMP`；业务仍由 Service 层显式写入，默认值只兜底绕过 Service 的裸 SQL |
| 逻辑删除 | 部门/角色/用户/课程/活动加 `is_deleted`（`unsigned tinyint`，1 已删除 / 0 未删除——手册建表规约的原话正例）；Java 字段叫 `deleted`（手册命名风格：POJO 布尔变量不得加 `is` 前缀），用 `@TableField` 显式映射。唯一索引建成函数索引 `if(is_deleted = 0, 唯一列, NULL)`：已删除行的索引键是 NULL，MySQL 视 NULL 互不相同，因此不占用唯一值，删除后同名可重建、反复删除也不撞唯一键 |

> **脚本按「全新安装」口径编写，不提供增量迁移。** 已有库请先 `mysqldump` 备份，再 `DROP DATABASE qk` 后重新执行上面的建表脚本；
> 这也是项目一贯的从零启动约定，避免仓库里堆积只对某个历史版本有效的 ALTER。

## 测试

```bash
mvn test                              # 全量：24 个测试类 / 195 个用例（2 个 OSS 手动用例默认跳过）
mvn -Dtest=ClueControllerTest test    # 单个测试类
```

- `*ControllerTest` 覆盖各模块的接口契约（状态码、字段、分页、筛选、状态流转）。
- [`LayeringTest`](qk-management/src/test/java/com/qk/LayeringTest.java) 守分层：扫描全部 `@GetMapping`/`@PostMapping` 等对外方法，断言返回值与参数（含泛型实参）里不出现 `com.qk.entity.po` 的任何类型；另外断言 Web 层（`controller` / `interceptor`）不直接依赖 `com.qk.mapper`。
- [`AuthServiceTest`](qk-management/src/test/java/com/qk/AuthServiceTest.java) 守认证策略：登录成功/密码错误/账号不存在/账号停用，以及令牌的签名被改、格式非法、为空、**已过期**、账号不存在、账号停用 —— 全部应失效。
- [`AuthorizationTest`](qk-management/src/test/java/com/qk/AuthorizationTest.java) 守接口授权：admin 能管基础数据；非 admin 调管理类接口与删除用户返回 403；线索专员只能走线索流转、商机专员只能走商机流转；未绑定角色与自定义角色的账号只能看查询类接口。
- [`OutputModelTest`](qk-management/src/test/java/com/qk/OutputModelTest.java) 守报文：把同一个 PO 分别以实体和 VO 序列化并逐字节比对，VO 漏抄字段即失败。
- [`DateTimeFormatTest`](qk-management/src/test/java/com/qk/DateTimeFormatTest.java) 守时间契约：默认格式锁死 `yyyy-MM-dd HH:mm:ss`，同时证明字段级 `@JsonFormat` 能覆盖出参与入参。
- [`GlobalExceptionHandlerTest`](qk-management/src/test/java/com/qk/GlobalExceptionHandlerTest.java) 守系统异常告警：走了一遍真实 MVC 处理链（兜底处理器带 `HttpServletRequest` 参数，直接调用测不出解析是否正常），并断言告警失败不影响 500 响应。
- [`HardeningTest`](qk-management/src/test/java/com/qk/HardeningTest.java) 守上线级行为：主键注入、摘要不能当密码登录、操作不存在的数据、坏 JSON 返回 400、非法文件上传、操作日志密码脱敏、课程字段校验。
- [`ApiRobustnessTest`](qk-management/src/test/java/com/qk/ApiRobustnessTest.java) 守接口边界：404/405/415 不再变 500、分页参数校验、字段长度与手机号格式、悬空引用（把线索分配给不存在的用户等）、停用/未知账号的令牌被拒、状态流转守卫。
- [`LifecycleTest`](qk-management/src/test/java/com/qk/LifecycleTest.java) 守状态机：不连数据库直接验证「哪些状态允许哪个动作」与提示语。
- [`UploadServiceImplTest`](qk-management/src/test/java/com/qk/service/impl/UploadServiceImplTest.java) 守上传策略：不连数据库、不连 OSS（用内存实现替掉 `FileStorage`），验证扩展名白名单、文件头校验、空内容、读取失败保留根因。
- 所有测试 `@Transactional` 回滚、不污染数据库；**断言只依赖测试自建的 fixture**（唯一例外是 `HardeningTest` 里验证「摘要不能当密码登录」的那条，它需要库里已知密码的种子账号 `zhangsan`）。
- `OssUploadManualTest` 会真实上传对象到 OSS，默认 `@Disabled`，需要时去掉注解再执行。

## 项目约定

**分层**

- **po** 只映射表列，不放 join 结果、不放请求参数；**持久化对象不出现在接口契约里**——对外方法的返回值与参数一律是 DTO / VO，由 [`LayeringTest`](qk-management/src/test/java/com/qk/LayeringTest.java) 从类型层面守卫（控制器内部仍可把请求 DTO 映射成实体再交给 Service，这是刻意的约定，不算破线）；
- **dto** 负责入参（查询条件 `XxxQueryDto`、跨表命令 `ClueTrackDto` / `BusinessTrackDto`、登录 `LoginDto`）；
- **vo** 负责出参，**每张有查询接口的表都有自己的 VO**（`UserVO`、`ClueVO`、`DeptVO`、`RoleVO`、`CourseVO`、`ActivityVO`…），`deptName` / `roleName` / `assignName` / `courseName`、跟进记录列表等展示字段也都在 VO 上；
- **PO → VO 写成 VO 的静态工厂 `XxxVO.from(po)`，且逐个字段 setter，不用 `BeanUtil.copyProperties`**：前者漏抄或改名时编译期就报错，后者只会静默写入 null、悄悄改掉对外报文。[`OutputModelTest`](qk-management/src/test/java/com/qk/OutputModelTest.java) 逐字节比对 PO 与 VO 的序列化结果，锁死报文不变；
- **写接口入参**用 `XxxSaveDto`（如 `DeptSaveDto`），字段上带 Bean Validation 注解（长度与 DDL 列宽一致、手机号/邮箱带格式校验）；**控制器负责把 DTO 映射成实体**（逐个字段赋值，不用 `BeanUtil` 反射拷贝：字段改名后会静默停止拷贝，逐个赋值则编译期报错）再交给 Service，Service 不接受 Web 层的 DTO（避免业务层耦合传输契约）。校验失败由 `GlobalExceptionHandler` 统一转成 `code = 0` + 字段级提示。
- **分页参数只有一处定义**：所有查询 DTO 继承 `PageQuery`（`page` / `pageSize` 的默认值与上下限），控制器入参加 `@Valid`。因此 `?page=`（空串）、`page=0`、`pageSize=0`、`pageSize=99999` 都会返回 `code = 0` + 字段提示，而不是 500 或静默返回空列表；上限 `PageQuery.MAX_PAGE_SIZE` 同时被 MyBatis-Plus 分页插件引用，两处口径不会漂移。
- **超长字段不会变成 500**：DTO 上的 `@Size` 是第一道防线；`GlobalExceptionHandler` 另有 `DataIntegrityViolationException` 兜底（列超长、非空、类型不匹配等），把漏网的完整性错误转成 `code = 0`，不会触发运维告警。
- `XxxSaveDto` 只暴露可写字段：主键、`createTime`/`updateTime`、以及由服务端赋值的字段（如线索的 `status`/`userId`、客户的 `businessId`、用户的 `password`）都不在 DTO 里，从契约上杜绝参数覆盖。

**统一响应（Result / ResultCode）**

- 响应码**只有两个取值**：`1` 成功、`0` 失败，属于对外契约，**不可更改**；业务失败同样返回 HTTP 200，由 `code` 区分，失败原因写在 `msg` 里。
- 常规场景只用 `Result.success(...)` 与 `Result.error(msg)`。`Result.custom(ResultCode, msg, data)` 是逃生舱，**能不用就不用**。
- 确需新增响应码时：先给 `ResultCode` 加带注释的枚举成员，再同步更新 `docs/openapi.yaml` 的状态码约定。`custom` 只接受 `ResultCode`、不接受裸数字，就是为了防止在调用处临时拼码值。
- `Result` 类上标注 `@JsonInclude(NON_NULL)`：`data` 为 `null` 时该字段**整个省略**，报文只剩 `code` 与 `msg`。判定成功请用 `code === 1`，不要用 `code === 0` 判失败，也不要用「有没有 `data` 字段」判断成功——前者在将来新增码值时依然正确，后者在无数据的成功响应上会误判。
- 业务失败的**具体原因统一来自 `com.qk.common.exception.ErrorCode`**：枚举常量名是稳定 code，中文文案集中在枚举里维护，`BusinessException` 只接受错误码、不再接受散写字符串。新增业务规则提示时在枚举里加一条即可。
- **业务异常与系统异常分开处理**：`BusinessException` 是预期内的失败（HTTP 200 + `code=0` + 具体提示）；兜底 `Exception` 是代码或依赖的缺陷（HTTP 500 + 固定提示「系统繁忙,请稍后重试」），并且**异步告警运维** —— 否则只有等用户投诉才会发现。
- 告警能力整体在 `qk-common`（`com.qk.common.notify`）：[`SystemExceptionNotifier`](qk-common/src/main/java/com/qk/common/notify/SystemExceptionNotifier.java) 是出口契约、[`SystemAlert`](qk-common/src/main/java/com/qk/common/notify/SystemAlert.java) 是告警内容（只收纯值，不依赖 `HttpServletRequest`，非 Web 场景也能发起告警）、[`LoggingSystemExceptionNotifier`](qk-common/src/main/java/com/qk/common/notify/LoggingSystemExceptionNotifier.java) 是默认实现（只写日志）。接真实通道时在应用里实现接口并加 `@Primary` 即可，调用方一行不用改。，但异步结构已经搭好：单线程守护线程池 + 有界队列 256 + 队满记 WARN 丢弃，**绝不阻塞请求线程、绝不把异常抛回调用方**。接真实通道（钉钉/企业微信/邮件/webhook）只需替换它的 `send` 方法。
- **错误码不进入响应体**：前端只按 `code`（0/1）判断成败、直接展示 `msg`，不按失败原因分支，因此对外契约保持 `{code, msg}` 不变，`ErrorCode` 只用于结构化日志与文案集中。将来需要按类型分支时再暴露——届时要把参数校验、坏 JSON、类型不匹配、上传超限、兜底 500 这几条路径也补上，否则会是一个时有时无的字段。
- 唯一索引冲突的「表.唯一索引名 → 提示」是一张表（见 `GlobalExceptionHandler.UNIQUE_KEY_ERRORS`），新增唯一索引只需补一行。
- `Result<T>` 为泛型，接口返回类型即数据类型；泛型只在编译期生效，JSON 结构固定为 `code` / `msg` / `data` 三个字段（`data` 为空时省略），由 `ResultTest` 守卫。

**Mapper（wrapper 负责简单查询、XML 负责复杂 SQL）**

- 单表 CRUD 与单表条件查询 → `BaseMapper` + `LambdaQueryWrapper`，**wrapper 写在 Mapper 的 default 方法里**（如 `DeptMapper.pageDepts`、`UserMapper.findByUsername`）；
  Service 只做参数传递、业务判断与结果包装，不感知查询 DSL（分层证据：`service` 包里不再出现 `LambdaQueryWrapper`）；
- 多表 join、聚合统计 → Mapper XML，按 MyBatis 官方约定放在**与接口同包同名**的路径下：
  `src/main/resources/com/qk/mapper/XxxMapper.xml`，`namespace` 为接口全限定名。
  因此 **不需要配置 `mapper-locations`**（MyBatis 会按接口路径自动加载同名 XML）；
- XML 内用 `<sql>` 片段复用公共字段/表连接，动态条件用 `<where>` + `<if>`，返回类型直接指向 VO；
- 一对多详情 → 拆成两次查询由 Service 组装（线索/商机 + 各自跟进记录），避免 join 产生重复行；
- 列表口径的**状态编码由状态机传入 XML**：`com.qk.domain.ClueLifecycle.closedCodes()` / `poolStatus()` 与 `BusinessLifecycle` 的同名方法取值，XML 用 `<foreach>` 拼 `IN`，不再出现 `status NOT IN (4, 5)` 这类裸数字（枚举与 SQL 不会脱节）。
- 首页概览的聚合 SQL 只做 `GROUP BY status` 计数，「哪个状态落到哪个字段」由 `ReportServiceImpl` 按枚举组装，12 个硬编码状态码一次性消除。

**删除接口的守卫**

项目不使用物理外键（见 `sql/user.sql` 等脚本注释），关联完整性由 Service 层保证。因此删除前必须逐个通过守卫，任一不满足即返回 `code = 0`：

| 接口 | 守卫 |
| --- | --- |
| `DELETE /depts/{id}` | 启用状态（`status = 1`）不可删；部门下仍有用户不可删 |
| `DELETE /roles/{id}` | 角色下仍有用户不可删 |
| `DELETE /courses/{id}` | 仍被商机或客户引用不可删 |
| `DELETE /activities/{id}` | 活动必须存在；仍被线索引用不可删 |
| `DELETE /users/{ids}` | 必须存在；不能删当前登录用户；仍被线索、商机或跟进记录引用不可删 |

不再使用的数据应改为**停用**（`status = 0`），而不是删除。引用计数统一放在 Mapper 的具名方法里（如 `UserMapper.countByDeptId`），Service 只负责业务判断，不感知 ORM 的查询 DSL。

**接口授权**

角色是数据（管理员可以自由新建角色），但**接口授权只认保留标签**（`com.qk.entity.enums.RoleLabel`）：`admin` / `clue_operator` / `business_operator`。做法是给需要控制的接口标 `@RequireRole`，由 `PermissionInterceptor` 在登录校验之后读取当前账号的角色——角色每次从库里取（不放进令牌），因此换角色立即生效，与"停用立即失效"同一取舍。

| 范围 | 要求 |
| --- | --- |
| 用户 / 部门 / 角色 / 课程 / 活动的增删改 | `admin` |
| 线索分配、商机分配 | `admin`（分配是管理员职责） |
| 线索跟进 / 标伪 / 转商机 | `admin`、`clue_operator` |
| 商机跟进 / 踢回公海 / 转客户 | `admin`、`business_operator` |
| 其余（查询类、新增线索/商机/客户、上传、登录） | 所有已登录用户 |

不满足时返回 **HTTP 403** + `{code: 0, msg: "无权访问该接口，请联系管理员分配角色"}`，与 401（未登录、响应体为空）配套，前端据此区分"重新登录"和"没权限"。

`admin` 是**内置保留角色**：由 `sql/role.sql` 创建，内置管理员账号在 `sql/user.sql` 里直接绑定它。这步不能省——否则从零安装后库里一个角色都没有，而"管理类仅 admin"会让第一个账号连 `POST /roles` 都调不了，系统被锁死。已有库若内置账号的 `role_id` 为空，执行一条：

```sql
UPDATE `user` SET `role_id` = (SELECT `id` FROM `role` WHERE `label` = 'admin')
WHERE `username` = 'admin' AND `role_id` IS NULL;
```

**跨聚合的读写边界**

- **只读的引用查询可以直接调对方的 Mapper**：`UserMapper.countByDeptId`、`ClueMapper.countByUserId` 这类单表、无规则可绕过的存在性与计数查询，改走 Service 会立刻造出双向依赖（`UserService ↔ ClueService`、`CourseService ↔ BusinessService`、`DeptService ↔ UserService`），收益只是形式上的分层。
- **写入必须走拥有该聚合的 Service**：线索转商机调 `BusinessService.createFromClue`，商机转客户调 `CustomerService.createFromBusiness`。以前这两处直接 `businessMapper.insert` / `customerMapper.insert`，等于绕过 `addBusiness` / `addCustomer` —— 在新增路径上补的校验与默认值，转换链路会静默漏掉。

**写入路径的引用守卫与并发**

「不使用物理外键」意味着**所有**写入路径都要自己校验引用，不只是删除：

| 入口 | 守卫 |
| --- | --- |
| `PUT /clues/assign/{clueId}/{userId}`、`PUT /businesses/assign/{businessId}/{userId}` | 归属人必须存在且 `status = 1`（停用账号无法登录，分给它等于没有归属人） |
| `POST /clues` | `activityId` 非空时必须存在 |
| `POST /businesses`、`POST /customers`、`PUT /customers` | `courseId` 非空时必须存在 |
| `POST /users`、`PUT /users` | `deptId` / `roleId` 非空时必须存在（修改是部分更新，没传的字段不校验） |

状态流转（分配 / 跟进 / 标伪 / 转商机 / 踢回公海 / 转客户）都是「先读状态再写状态」，因此在事务内用 `SELECT ... FOR UPDATE` 锁住目标行（`ClueMapper.lockById` / `BusinessMapper.lockById`），再交给 `ClueLifecycle` / `BusinessLifecycle` 判断。这样重复点击或网络重试只会让第二次请求收到「当前状态不允许…」，而不会重复生成商机、客户或跟进记录。

**其他**

- `createTime` / `updateTime` 由 `MyMetaObjectHandler` 自动填充；时间输出统一 `yyyy-MM-dd HH:mm:ss`，输入额外兼容 `yyyy-MM-dd HH:mm`、`yyyy-MM-dd` 与 ISO 写法。个别字段需要别的格式时，在该字段上加 `@JsonFormat(pattern = "...")` 即可覆盖默认（入参同理，且字段格式解析不了时仍会回落到上面的兼容逻辑）。由 `DateTimeFormatTest` 守住。
- 状态编码集中在枚举（`ClueStatus` / `BusinessStatus` / `ClueTrackType` / `ActivityStatus`），不散落裸数字；它们统一实现 `CodeEnum` 契约，可用 `CodeEnum.fromCode(XxxStatus.class, code)` 按码值反查，或用 `CodeEnum.codes(...)` 取全部码值。其中 `ActivityStatus`（未开始/进行中/已结束）由 `startTime`、`endTime` 与当前时间推算，**不落库**，只作为 `/activities` 的查询条件。
- **状态流转规则只有一处定义**：`com.qk.domain.ClueLifecycle` / `BusinessLifecycle` 用「动作 → 允许的前置状态」表达状态机，Service 只调用 `ensure(action, status)`；动作名直接拼进既有提示语（如「该线索当前状态不允许转商机」），列表口径也取自同一个类，由 `LifecycleTest` 守卫。
- 列表排序按页面原型：部门/角色/课程/活动/用户按最后修改时间倒序，线索/商机/线索池/公海池按修改时间倒序，客户按创建时间倒序；排序末尾都补 `id`，避免排序键不唯一导致翻页重复或丢记录。
- `GET /users/role/{roleLabel}`（分配线索/商机的人员下拉）只返回 `status = 1` 的用户：停用账号登录会被拒绝，分配给它等于这条数据没有归属人。
- 登录与令牌校验集中在 `AuthService`（`login` 签发、`authenticate` 校验签名 + 有效期 + 账号是否存在与启用），`LoginController` 与 `LoginInterceptor` 都只依赖它，Web 层不再持有 JWT 工具或 Mapper（由 `LayeringTest` 守卫）。令牌对应的账号被停用或删除后**立即** 401：这里刻意不加缓存，用每请求一次主键查询换取即时撤销。
- 「操作日志」页面上的**操作模块**与**操作类型**不落库，由 `class_name` / `method_name` 在查询时映射（见 `OperateLogMapper.xml` 的 `moduleExpr` / `typeExpr`），`/logs` 支持 `operateModule`、`operateType` 模糊搜索。
- 增删改接口标注 `@LogOperation`，由切面写入 `operate_log`（密码字段落库前脱敏为 `***`）。
- **上传策略在服务层、存储走端口**：`UploadService` 负责扩展名白名单与文件头（魔术字节）校验，`FileStorage` 是存储出口、`OssTemplate` 是 OSS 实现，控制器只做协议适配（缺文件分片时给出「请选择要上传的图片」）。图片格式（扩展名 + Content-Type + 文件头）只有 `ImageFormat` 一个出处，换存储或改策略都不需要动 Web 层。

## 常见问题

### 接口返回 401，响应体是空的？

除 `/login` 外所有接口都必须带请求头 `token`，值为登录返回的 `data.token`。令牌缺失、被篡改或超过 24 小时都会返回 401 且无响应体。

### 上传图片报 AccessDenied？

多为 OSS 区域配置错误：桶在哪个区域，`QK_OSS_REGION` 就要填哪个（当前桶在 `cn-beijing`）。用错区域的 endpoint 签名会得到 AccessDenied，而不是明确提示。另外上传只接受 jpg/jpeg/png/gif/bmp/webp 且不超过 5MB。

### 改动测试数据后测试挂了？

检查是否有断言依赖"库里现有数据"（例如 `rows[0]`、精确总数）。本项目约定断言只基于测试自建的 fixture（见 [项目约定](#项目约定)）。

## Roadmap

- [x] 10 个模块的接口实现（部门 / 角色 / 课程 / 用户 / 活动 / 线索 / 商机 / 客户 / 日志 / 概览）
- [x] JWT 登录鉴权 + 拦截器
- [x] 文件上传（阿里云 OSS）
- [x] AOP 操作日志（含密码脱敏）
- [x] 实体 / DTO / VO 分层重构
- [x] Mapper 分层：单表用 wrapper、多表 join 与聚合用 XML（去除第三方 join 依赖，XML 按官方约定与接口同包）
- [x] 出参模型收口：dept/role/course/activity 补上 VO，PO 不再出现在任何接口契约里；登录入参由 `User` 改为 `LoginDto`
- [x] 统一详情接口对不存在 id 的返回：dept/role/course/activity 对齐 users/clues，返回 `code = 0` + 「XXX不存在」
- [x] 按《阿里巴巴 Java 开发手册》整改：主键/外键升 `bigint unsigned`、索引规约 `uk_`/`idx_`、实体迁入 `com.qk.entity.po`、`PageResult` 迁入 `qk-entity`、声明未声明的 Jackson 依赖
- [ ] 密码哈希由 MD5 升级为 BCrypt（登录时平滑升级，需先确认前端提交的密码形态）
- [ ] 用户名改为不可变标识（避免改名后旧密码失效）
- [x] 接口级权限控制（`@RequireRole` + 保留角色标识：管理类仅 `admin`，业务流转给对应专员）
- [ ] CORS 配置（前后端同域或走网关时可跳过）
- [ ] 上传图片改用私有读 + 签名 URL，并清理孤儿对象

## 已知限制

1. **密码使用 MD5（`md5(用户名 + 密码)`）**：这是课程约定的历史方案，强度不足。BCrypt 的哈希是 60 字符，现有 `password varchar(64)` 刚好够用，迁移时建议采用"登录成功即升级"的方式避免用户感知。
2. **用户名当盐的副作用**：修改 `username` 后该账号旧密码失效，建议把用户名视为不可变字段。
3. **登录只接受明文密码**（服务端现算摘要比对），不接受直接提交摘要。
4. **课程接口文档与库冲突**：`subject/name/price/target` 在课程文档里标注为"非必须"，但库中为 NOT NULL；服务端已按库口径加了必填与取值范围校验。
5. **课程「适用人群」档位不一致**：接口文档与库注释只有 1 小白学员、2 中级程序员两档，页面原型是小白学员、初级程序员、中级程序员三档；服务端按超集放开为 1~3（3 = 初级程序员），1、2 的含义与文档保持一致。
6. **客户 / 商机的「渠道来源」是选填**：服务端不做必填校验，`customer.channel` 与 `business.channel` 建表即为可空（`DEFAULT NULL`）；线索的 `channel` 仍是必填（原型 2.2 明确必填）。
7. **两个「池」的口径与活动状态**：线索池只返回 `status = 4 伪线索`（与公海池只返回 `status = 4 回收` 一致）；`/clues`、`/businesses` 默认排除已关闭状态，但显式传 `status` 时按传入值筛选。活动状态（未开始/进行中/已结束）不落库，由 `/activities?activityStatus=` 按时间推算。
8. **操作日志只记录增删改**：`@LogOperation` 只标注在写接口上，查询接口（GET）不写日志，因此日志列表里不会出现"查询部门/查询用户"这类记录。
9. **上传**：已校验扩展名、文件头（魔术字节）与大小；对象仍是公共读，尚无孤儿对象清理机制（对象名按内容寻址，重复上传不会新增对象，但删除业务数据不会连带删除对象）。
10. **只增表没有 `update_time`**：`clue_track_record`、`business_track_record`、`operate_log` 是只增表，只有创建时间（`operate_log` 叫 `operate_time`），严格来说不满足手册「表必备三字段」；时间列已补 `DEFAULT CURRENT_TIMESTAMP` 作为数据库侧兜底，业务写入仍以 Service 层为准。
11. **列表检索使用全模糊**：各列表的手机号、姓名等条件为 `LIKE CONCAT('%', ?, '%')`，手册禁止左模糊与全模糊。改用前缀匹配或搜索引擎会改变检索结果，属于对外行为变更，故保留现状，待数据量上来后再评估。
12. **0/1 语义字段未按 `is_xxx` 命名**：手册要求表达是与否的字段用 `is_xxx`，但 `job_status` 等字段改名会同时改变对外 JSON 字段名，属于破坏性契约变更，故保留（`status` 系列语义是「状态」而非布尔，不在该条款范围内）。
13. **金额以整型存「元」**：`course.price`、`activity.voucher` 是 `int unsigned`（单位：元），不受手册「小数必须用 decimal」约束，但无法表达角、分。
14. **操作日志的「操作模块 / 操作类型」尚未落库**：目前仍由 `OperateLogMapper.xml` 的 `CASE` 表达式在查询时从 `class_name` / `method_name` 翻译，`operateModule` / `operateType` 的模糊检索因此无法走索引，`operate_log` 增长后 `/logs` 会退化成全表扫描。彻底做法是在写入时把两个标签落成独立列并建索引（需要一次建表脚本变更），本次未一并处理。
15. **未接入静态检查**：`BeanUtil` 之类的约定目前只靠注释与测试守卫，尚未接入 Alibaba P3C / SpotBugs 规则集，`Hutool` 也仍是 `hutool-all`（本地依赖仓库只有全量包，替换为 `hutool-core` / `hutool-crypto` / `hutool-jwt` 需要联网拉取）。
16. **接口授权是角色级的粗粒度控制**：查询类与新增线索/商机/客户的接口对所有已登录用户开放（有意为之，避免补齐权限时把前端页面整体挡住）。「线索专员只能看到自己名下的线索」这类**数据行级**权限（按归属人过滤）尚未实现。

## 说明

- 项目按《轻客管家》课程的分阶段接口文档实现，接口契约以 [`docs/openapi.yaml`](docs/openapi.yaml) 为准。
- 执行数据清理前的整库备份在 `sql/_backup/`，可用 `mysql -uroot -p < sql/_backup/<文件>.sql` 还原。
