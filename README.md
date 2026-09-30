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
  <img src="https://img.shields.io/badge/tests-83%20passed-success" alt="Tests">
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
| 活动管理 | 活动的增删改查、按渠道/类型筛选 |
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
│   └── com.qk.common   Result / PageResult、OSS 客户端与模板、JWT 工具、UserHolder、业务异常
├── qk-entity/          实体 / DTO / VO / 枚举（包根 com.qk.entity）
│   └── com.qk.entity   实体（Dept、User、Clue、Business…）
│       ├── dto         入参：XxxQueryDto、ClueTrackDto、MarkFalseClueDto…
│       ├── vo          出参：UserVO、ClueVO、BusinessVO、OverviewVO…
│       └── enums       状态枚举：ClueStatus、BusinessStatus、ClueTrackType
├── qk-management/      可启动模块（包根 com.qk）
│   └── com.qk
│       ├── controller  接口层
│       ├── service     业务层（接口 + impl）
│       ├── mapper      数据访问层（接口 + 同包同名 XML）
│       ├── aspect      操作日志切面 + @LogOperation
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
| 用户 | `/users` | 列表 / 详情 / 新增 / 修改 / 批量删除 / 按角色 / 按部门 |
| 部门 | `/depts` | 增删改查 + `/depts/list` |
| 角色 | `/roles` | 增删改查 + `/roles/list` |
| 课程 | `/courses` | 增删改查 + 按学科筛选 |
| 活动 | `/activities` | 增删改查 + 按类型筛选 |
| 线索 | `/clues` | 列表 / 详情 / 新增 / 分配 / 跟进 / 伪线索 / 转商机 / 线索池 |
| 商机 | `/businesses` | 列表 / 详情 / 新增 / 分配 / 跟进 / 回收 / 转客户 / 公海池 |
| 客户 | `/customers` | 列表 / 详情 / 新增 / 修改 |
| 系统 | `/logs`、`/report/overview`、`/upload` | 操作日志、首页概览、图片上传 |

**通用约定**

- 统一响应 `Result`：`code` 为 `1` 成功、`0` 失败（业务失败同样返回 HTTP 200，按 `code` 判断）；分页统一 `{ total, rows }`，`pageSize` 单页上限 200。
- 状态码语义：`200` 成功或业务失败 · `400` 请求体/参数格式错误 · `401` 未登录（响应体为空）· `500` 服务端异常。
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

## 测试

```bash
mvn test                              # 全量：11 个测试类 / 83 个用例（2 个手动用例默认跳过）
mvn -Dtest=ClueControllerTest test    # 单个测试类
```

- `*ControllerTest` 覆盖各模块的接口契约（状态码、字段、分页、筛选、状态流转）。
- [`HardeningTest`](qk-management/src/test/java/com/qk/HardeningTest.java) 守上线级行为：主键注入、摘要不能当密码登录、操作不存在的数据、坏 JSON 返回 400、非法文件上传、操作日志密码脱敏、课程字段校验。
- 所有测试 `@Transactional` 回滚、不污染数据库；**断言只依赖测试自建的 fixture**，不依赖库里已有数据的规模与姓名。
- `OssUploadManualTest` 会真实上传对象到 OSS，默认 `@Disabled`，需要时去掉注解再执行。

## 项目约定

**分层**

- **entity** 只映射表列，不放 join 结果、不放请求参数；
- **dto** 负责入参（查询条件 `XxxQueryDto`、跨表命令 `ClueTrackDto` / `BusinessTrackDto`）；
- **vo** 负责出参（`deptName` / `roleName` / `assignName` / `courseName`、跟进记录列表等展示字段都在 VO 上）。
- **写接口入参**用 `XxxSaveDto`（如 `DeptSaveDto`），字段上带 Bean Validation 注解；**控制器负责把 DTO 映射成实体**再交给 Service，Service 不接受 Web 层的 DTO（避免业务层耦合传输契约）。校验失败由 `GlobalExceptionHandler` 统一转成 `code = 0` + 字段级提示。
- `XxxSaveDto` 只暴露可写字段：主键、`createTime`/`updateTime`、以及由服务端赋值的字段（如线索的 `status`/`userId`、客户的 `businessId`、用户的 `password`）都不在 DTO 里，从契约上杜绝参数覆盖。

**统一响应（Result / ResultCode）**

- 响应码**只有两个取值**：`1` 成功、`0` 失败，属于对外契约，**不可更改**；业务失败同样返回 HTTP 200，由 `code` 区分，失败原因写在 `msg` 里。
- 常规场景只用 `Result.success(...)` 与 `Result.error(msg)`。`Result.custom(ResultCode, msg, data)` 是逃生舱，**能不用就不用**。
- 确需新增响应码时：先给 `ResultCode` 加带注释的枚举成员，再同步更新 `docs/openapi.yaml` 的状态码约定。`custom` 只接受 `ResultCode`、不接受裸数字，就是为了防止在调用处临时拼码值。
- `data` 为 `null` 时字段依然存在，不会被省略。前端判定成功请用 `code === 1`，不要用 `code === 0` 判失败——前者在将来新增码值时依然正确。
- `Result<T>` 为泛型，接口返回类型即数据类型；泛型只在编译期生效，JSON 结构固定为 `code` / `msg` / `data`，由 `ResultTest` 守卫。

**Mapper（wrapper 负责简单查询、XML 负责复杂 SQL）**

- 单表 CRUD 与单表条件查询 → `BaseMapper` + `LambdaQueryWrapper`，**wrapper 写在 Mapper 的 default 方法里**（如 `DeptMapper.pageDepts`、`UserMapper.findByUsername`）；
  Service 只做参数传递、业务判断与结果包装，不感知查询 DSL（分层证据：`service` 包里不再出现 `LambdaQueryWrapper`）；
- 多表 join、聚合统计 → Mapper XML，按 MyBatis 官方约定放在**与接口同包同名**的路径下：
  `src/main/resources/com/qk/mapper/XxxMapper.xml`，`namespace` 为接口全限定名。
  因此 **不需要配置 `mapper-locations`**（MyBatis 会按接口路径自动加载同名 XML）；
- XML 内用 `<sql>` 片段复用公共字段/表连接，动态条件用 `<where>` + `<if>`，返回类型直接指向 VO；
- 一对多详情 → 拆成两次查询由 Service 组装（线索/商机 + 各自跟进记录），避免 join 产生重复行；
- 状态编码（如"列表排除 4 伪线索、5 转商机"）在 XML 里有注释标注对应的枚举，并由测试守卫。

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

**其他**

- `createTime` / `updateTime` 由 `MyMetaObjectHandler` 自动填充；时间输出统一 `yyyy-MM-dd HH:mm:ss`，输入兼容多种写法。
- 状态编码集中在枚举（`ClueStatus` / `BusinessStatus` / `ClueTrackType`），不散落裸数字；它们统一实现 `CodeEnum` 契约，可用 `CodeEnum.fromCode(XxxStatus.class, code)` 按码值反查，或用 `CodeEnum.codes(...)` 取全部码值。
- 增删改接口标注 `@LogOperation`，由切面写入 `operate_log`（密码字段落库前脱敏为 `***`）。

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
- [ ] 密码哈希由 MD5 升级为 BCrypt（登录时平滑升级，需先确认前端提交的密码形态）
- [ ] 用户名改为不可变标识（避免改名后旧密码失效）
- [ ] 接口级权限控制（当前只校验登录，未区分角色）
- [ ] CORS 配置（前后端同域或走网关时可跳过）
- [ ] 上传图片改用私有读 + 签名 URL，并清理孤儿对象

## 已知限制

1. **密码使用 MD5（`md5(用户名 + 密码)`）**：这是课程约定的历史方案，强度不足。BCrypt 的哈希是 60 字符，现有 `password varchar(64)` 刚好够用，迁移时建议采用"登录成功即升级"的方式避免用户感知。
2. **用户名当盐的副作用**：修改 `username` 后该账号旧密码失效，建议把用户名视为不可变字段。
3. **登录只接受明文密码**（服务端现算摘要比对），不接受直接提交摘要。
4. **课程接口文档与库冲突**：`subject/name/price/target` 在课程文档里标注为"非必须"，但库中为 NOT NULL；服务端已按库口径加了必填与取值范围校验。
5. **上传**：仅校验扩展名与大小；对象为公共读，尚无孤儿对象清理机制。

## 说明

- 项目按《轻客管家》课程的分阶段接口文档实现，接口契约以 [`docs/openapi.yaml`](docs/openapi.yaml) 为准。
- 执行数据清理前的整库备份在 `sql/_backup/`，可用 `mysql -uroot -p < sql/_backup/<文件>.sql` 还原。
