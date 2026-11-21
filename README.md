# 校园活动管理系统

基于 Spring Boot + Vue 的校园活动管理系统，覆盖**活动发布 → 审核 → 报名 → 签到 → 评价 → 统计**完整闭环。
学生看活动、报名、签到、评价；组织者发布活动、管理流程、看参与数据；管理员审核活动、管理用户与参数。

> 毕业设计项目。代码量克制、结构清晰，重点放在几个能讲清楚的实现细节上：
> 报名名额的并发控制、签到码时效 + 爽约机制、活动质量分。

---

## 线上地址

| 环境 | 地址 |
|---|---|
| 前端 | <https://campus.taikong.de> |
| 后端 API | <https://campus-api.taikong.de> |
| 健康检查 | <https://campus-api.taikong.de/api/ping> |

线上为前后端分离部署：Vercel 托管前端构建产物，Render 以 Docker 运行后端 jar，数据库为云端 MySQL。
免费实例空闲 15 分钟会休眠，首次访问需 30~60 秒；容器文件系统临时，上传的封面/头像重启后丢失。
部署细节、环境变量与验收命令见 `doc/10-线上部署现状.md`。

## 技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 运行环境 | JDK | 8 |
| 后端框架 | Spring Boot | 2.7.18 |
| 持久层 | MyBatis-Plus | 3.5.3.2 |
| 数据库 | MySQL | 8.0（云端 8.4） |
| 演示数据库 | H2（MySQL 兼容模式） | 1.4.200 |
| 认证 | JWT（java-jwt） | 3.19.4 |
| 工具库 | Hutool | 5.8.25 |
| 前端 | Vue 2 + Element UI | 2.7 / 2.15 |
| 图表 | ECharts | 5.4 |
| 构建 | Maven / npm（@vue/cli-service 5） | 3.9 / 8 |

---

## 目录结构

```
.
├── pom.xml                      Maven 工程
├── run-h2.bat                   一键启动（演示模式，零依赖）
├── run-mysql.bat                一键启动（MySQL 模式）
├── Dockerfile / render.yaml     云端部署物料
├── src/main/java/com/campus/activity/
│   ├── common/                  统一返回体、业务异常、全局异常处理、常量
│   ├── config/                  MyBatis-Plus、Web、Jackson、自动填充
│   ├── annotation/              @RequireRole 角色注解
│   ├── interceptor/             JWT 登录拦截器
│   ├── controller/              11 个控制器，40+ 接口
│   ├── service/ + impl/         业务层（报名、签到、统计为核心）
│   ├── mapper/                  8 个 Mapper + XML（多表联查与聚合）
│   ├── entity/ dto/ vo/         实体、入参、出参
│   ├── util/                    JwtUtil、UserContext
│   └── task/                    活动状态定时任务
├── src/main/resources/
│   ├── application.yml          MySQL 配置（默认）
│   ├── application-h2.yml       H2 演示配置
│   ├── mapper/*.xml             自定义 SQL
│   └── static/                  前端构建产物（打包进单 jar）
├── web/                         前端工程（Vue 2）
│   └── src/{api,views,layout,store,router,utils,styles}
├── db/
│   ├── schema.sql               建表（MySQL 与 H2 通用子集）
│   ├── indexes.sql              索引（仅 MySQL）
│   └── data.sql                 演示数据（脚本生成）
├── scripts/                     环境、构建、测试、截图、打包脚本
├── doc/                         设计文档、论文、图表
├── release/                     便携包产物（jar + 内置 JRE）
├── tools/                       便携 JDK / Maven / Node，不进 git
└── config/                      本地数据库凭据，不进 git
```

---

## 运行方式

### 方式一：便携包（答辩现场推荐，零安装）

```
release\CampusActivitySystem\
├── start-demo.bat        双击 → 浏览器打开 http://127.0.0.1:8080
├── start-cloud.bat       连自己的 MySQL
├── app\campus-activity.jar
├── runtime\              内置 Java 8 运行时（无需装 JDK）
└── db\                   建表与演示数据
```

换台电脑解压即用；8080 被占用时执行 `start-demo.bat 8090` 换端口。

### 方式二：演示模式（本机已有 JDK）

```bat
run-h2.bat
```

打开 http://localhost:8080 。数据在内存中，重启即恢复演示数据。

### 方式三：MySQL 模式

```sql
CREATE DATABASE campus_activity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bat
mysql -h 127.0.0.1 -P 3306 -u root -p campus_activity < db\schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -p campus_activity < db\indexes.sql
mysql -h 127.0.0.1 -P 3306 -u root -p campus_activity < db\data.sql
```

```bat
set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=campus_activity
set DB_USER=root
set DB_PASSWORD=你的密码
set DB_SSL=false
run-mysql.bat
```

### 方式四：开发模式

```powershell
. .\scripts\env.ps1
mvn -s scripts\settings.xml spring-boot:run "-Dspring-boot.run.profiles=h2"

cd web
npm install --registry=https://registry.npmmirror.com
npm run serve          # http://127.0.0.1:8081 ，/api 与 /upload 代理到 8080
```

### 打包

```powershell
# 前端 → static → 单 jar
.\scripts\build-all.ps1

# 便携包（jar + 内置 JRE + 启动脚本 + zip）
.\scripts\make-release.ps1
```

---

## 在线部署

前端 Vercel + 后端 Render（Docker）+ 数据库 SQLPub，全部 https，详见 [doc/03-在线部署方案.md](doc/03-在线部署方案.md)。

```bash
# 后端：仓库推到 GitHub，Render 选 Blueprint，填四个 DB_* 环境变量即可
# 前端：cd web && vercel --prod
```

---

## 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | 123456 | 管理员 | 审核活动、管理用户 / 分类 / 公告 / 参数 |
| 2022010102 | 123456 | 组织者 | 李静，发布与管理活动 |
| 2023000201 | 123456 | 学生 | 李嘉怡，有多条报名记录 |
| 2023000202 ~ 2023000210 | 123456 | 学生 | 其余演示学生 |

---

## 核心设计

**报名名额原子扣减**

```sql
update activity
   set signed_count = signed_count + 1
 where id = #{activityId}
   and signed_count < capacity
   and status = 2
```

影响行数 = 1 才算抢到名额，与明细插入在同一事务内。`activity_registration(activity_id, user_id)` 唯一索引负责防重复报名，两者分工明确。

50 并发抢 10 个名额的实测：本方案成功 10、拒绝 40，计数与明细一致；对照组"先查后改"成功 50、超卖 40、冗余计数只剩 6。

**签到码与爽约**

6 位签到码，默认 30 分钟有效，未过期则复用；签到窗口 `[开始前 30 分钟, 活动结束]`；活动结束时未签到者判爽约，累计到阈值（默认 2 次）写 `ban_until`（默认 30 天）；补签会回退违约次数并解除封禁。

**活动质量分**

`签到率 × 40 + 评分归一化 × 40 + 完成率 × 20`，详情页实时计算，不落库。

**封面与名单导出**

活动封面走 `POST /api/activity/cover`（multipart，字段名 `file`，jpg/jpeg/png/gif/webp，≤5MB），返回 `/upload/cover/xxx` 写入 `coverUrl`；组织者在报名名单页点"导出 Excel"，后端用 POI 生成含标题行、表头、冻结窗口与自动筛选的 xlsx，可按状态与关键字筛选后导出，且只能导出自己发布活动的名单。

---

## 接口约定

```json
{ "code": 200, "msg": "操作成功", "data": {} }
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数或业务校验不通过 |
| 401 | 未登录 / token 失效 |
| 403 | 已登录但无权限 |
| 404 | 资源不存在 |
| 500 | 服务器异常 |

登录后把 token 放进请求头：`Authorization: Bearer <token>`。除登录、注册、`/api/ping` 外，所有接口都需要令牌。

完整接口清单见 [doc/02-接口文档.md](doc/02-接口文档.md)。

---

## 测试

```powershell
.\scripts\test-admin.ps1        # 管理员 / 公告 / 通知 / 参数 / 用户 43 项
.\scripts\test-checkin.ps1       # 签到码、签到、爽约、补签
.\scripts\test-review.ps1        # 评价、匿名、权限
.\scripts\test-contract.ps1      # 前后端契约 15 项

# 并发对照实验（先起应用，再建一个 capacity=10 的活动）
java -cp .\scripts\out ConcurrencyTest http://127.0.0.1:8080 <activityId> 1 50
```

功能用例与并发实验数据见 [doc/04-测试用例.md](doc/04-测试用例.md)。

---

## 文档

| 文件 | 内容 |
|---|---|
| [doc/00-项目总体规划.md](doc/00-项目总体规划.md) | 需求、设计、进度流水、踩坑记录 |
| [doc/02-接口文档.md](doc/02-接口文档.md) | 60 个接口的入参与返回 |
| [doc/03-在线部署方案.md](doc/03-在线部署方案.md) | Vercel + Render + SQLPub 部署 |
| [doc/04-测试用例.md](doc/04-测试用例.md) | 104 条功能用例与并发实验 |
| [doc/05-查重与AIGC自检报告.md](doc/05-查重与AIGC自检报告.md) | 稿内重复率、句长分布、与收集文献的重合度 |
| [doc/06-答辩讲稿.md](doc/06-答辩讲稿.md) | 12 分钟逐页讲稿 + 时间分配表 + 压缩说法 |
| [doc/07-答辩问答.md](doc/07-答辩问答.md) | 21 条预判问答与应答策略 |
| [doc/08-演示脚本.md](doc/08-演示脚本.md) | 现场演示流程、检查清单与应急预案 |
| [doc/09-交付清单.md](doc/09-交付清单.md) | 全部材料清单与提交前检查项 |
| `doc/10-线上部署现状.md` | 线上部署现状：前端地址、后端托管方案与验���清单 |
| [doc/答辩材料.docx](doc/答辩材料.docx) | 讲稿 + 问答 + 演示脚本合并版（16 页） |
| [doc/毕业论文初稿.docx](doc/毕业论文初稿.docx) | 论文初稿（60 页，约 2.56 万字，含附录 A~D；15 条参考文献来自 Zotero 分类） |
| [doc/开题报告.docx](doc/开题报告.docx) | 开题报告（5 页） |
| [doc/任务书.docx](doc/任务书.docx) | 毕业设计任务书（3 页） |
| [doc/文献综述.docx](doc/文献综述.docx) | 文献综述（6 页，按 12 篇中文文献重写） |
| [doc/答辩PPT.pptx](doc/答辩PPT.pptx) | 答辩幻灯片（17 页，16:9） |
| [doc/figures/](doc/figures) | 架构图、E-R 图、界面截图 |

---

## 常见问题

**端口 8080 被占用**
```bat
netstat -ano | findstr :8080
taskkill /PID <进程号> /F
```

**打包时报 jar 被占用 / 无法重命名**
应用还在运行。先结束 `java.exe` 中带 `campus-activity.jar` 的进程，再 `mvn clean package`。

**中文乱码**
Maven 编译、`server.servlet.encoding`、数据库连接串三处都固定 UTF-8。控制台乱码是 Windows 代码页问题，`chcp 65001` 即可，不影响接口返回。

**换台电脑跑不起来**
用便携包最省事（自带 JRE）。若自行部署，确认 JDK 8+，MySQL 8 的连接串已带 `serverTimezone=Asia/Shanghai`。
