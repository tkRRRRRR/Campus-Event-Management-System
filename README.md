# 校园活动管理系统

基于 Spring Boot 的校园活动管理系统，覆盖**活动发布 → 报名 → 签到 → 评价 → 统计**完整闭环。
学生看活动、报名、签到、评价；组织者发布活动、管理流程、看参与数据；管理员审核活动、管理用户。

> 毕业设计项目，代码量克制、结构清晰，重点在几个能讲清楚的实现细节上：
> 报名名额的并发控制、签到码时效 + 爽约机制、活动质量分。

---

## 技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 运行环境 | JDK | 8 |
| 后端框架 | Spring Boot | 2.7.18 |
| 持久层 | MyBatis-Plus | 3.5.3.2 |
| 数据库 | MySQL | 5.7 / 8.0 |
| 演示数据库 | H2（MySQL 兼容模式） | 1.4.200 |
| 认证 | JWT（java-jwt） | 3.19.4 |
| 工具库 | Hutool | 5.8.25 |
| 前端 | Vue 2 + Element UI | 2.7 / 2.15 |
| 图表 | ECharts | 5.4 |

---

## 目录结构

```
.
├── pom.xml                     Maven 工程
├── run-h2.bat                  一键启动（演示模式，零依赖）
├── run-mysql.bat               一键启动（MySQL 模式）
├── src/main/java/com/campus/activity/
│   ├── common/                 统一返回体、业务异常、全局异常处理、常量
│   ├── config/                 MyBatis-Plus、Web、Jackson、自动填充
│   ├── annotation/             @RequireRole 角色注解
│   ├── interceptor/            JWT 登录拦截器
│   ├── controller/             Auth / User / Ping
│   ├── service/                UserService
│   ├── mapper/                 8 个 BaseMapper
│   ├── entity/                 8 张表对应实体
│   ├── dto/ vo/                请求参数、返回视图
│   ├── util/                   JwtUtil、UserContext
│   └── task/                   定时任务（后续：活动状态流转、爽约判定）
├── src/main/resources/
│   ├── application.yml         MySQL 配置（默认）
│   ├── application-h2.yml      H2 演示配置
│   └── static/                 前端构建产物放这里（打包成单 jar）
├── db/
│   ├── schema.sql              建表（MySQL 与 H2 通用）
│   ├── indexes.sql             索引（仅 MySQL 需要）
│   └── data.sql                演示数据
├── scripts/
│   ├── setup-env.ps1           下载便携 JDK/Maven/Node 到 tools/
│   ├── env.ps1                 当前会话启用便携环境
│   ├── settings.xml            Maven 阿里云镜像
│   └── HashGen.java            BCrypt 哈希生成小工具
├── tools/                      便携运行时，不进 git
├── doc/                        设计文档
└── frontend/                   前端工程（后续添加）
```

---

## 三种运行方式

### 方式一：演示模式（推荐，零依赖）

```bat
run-h2.bat
```

打开 http://localhost:8080 即可。不需要 MySQL、不需要联网，数据在内存中，重启即恢复演示数据。

> 打包后同样可用：`java -jar campus-activity.jar --spring.profiles.active=h2`

### 方式二：MySQL 模式

1. 建库并导入脚本（顺序不能换）：

```sql
CREATE DATABASE campus_activity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bat
mysql -h 127.0.0.1 -P 3306 -u root -p campus_activity < db\schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -p campus_activity < db\indexes.sql
mysql -h 127.0.0.1 -P 3306 -u root -p campus_activity < db\data.sql
```

2. 配置连接（环境变量优先，其次改 `application.yml`）：

```bat
set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=campus_activity
set DB_USER=root
set DB_PASSWORD=你的密码
set DB_SSL=false
run-mysql.bat
```

### 方式三：开发模式

```powershell
. .\scripts\env.ps1                        # 启用便携 JDK/Maven/Node
mvn -s scripts\settings.xml spring-boot:run "-Dspring-boot.run.profiles=h2"
```

---

## 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | 123456 | 管理员 | 审核活动、管理用户与分类 |
| 2022010102 | 123456 | 组织者 | 李静，发布与管理活动 |
| 2023010115 | 123456 | 学生 | 王雨桐，已报名两场活动 |
| 2023010126 | 123456 | 学生 | 陈子豪，有 1 次爽约记录 |

---

## 接口约定

统一返回体：

```json
{ "code": 200, "msg": "操作成功", "data": {} }
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数或业务校验不通过 |
| 401 | 未登录 / token 失效 |
| 403 | 已登录但无权限 |
| 500 | 服务器异常 |

登录后把 token 放进请求头：`Authorization: Bearer <token>`。

---

## 当前进度

- [x] 工程骨架、统一返回体、全局异常、MyBatis-Plus 分页
- [x] 数据库设计（8 张表）+ 演示数据
- [x] 登录 / 注册 / 用户信息（JWT + 拦截器 + BCrypt）
- [ ] 活动模块（发布、审核、列表、详情）
- [ ] 报名模块（并发名额控制）
- [ ] 签到模块（签到码、补签、爽约判定）
- [ ] 评价模块
- [ ] 统计模块
- [ ] 前端页面

---

## 常见问题

**端口 8080 被占用**
```bat
netstat -ano | findstr :8080
taskkill /PID <进程号> /F
```
或改 `application.yml` 里的 `server.port`。

**MySQL 8 连接报时区错误**
连接串已经带了 `serverTimezone=Asia/Shanghai`，如果还报错，检查是否用了旧版驱动。

**中文乱码**
本项目三处编码都固定 UTF-8：Maven 编译、`server.servlet.encoding`、数据库连接串 `characterEncoding=utf8`。若控制台仍乱码，是 Windows 终端代码页问题，`chcp 65001` 即可，不影响接口返回。

**换台电脑跑不起来**
确认 JDK 版本是 8+；直接跑 `run-h2.bat` 不依赖数据库；详细的可移植方案见 `doc/00-项目总体规划.md` 第九节。
