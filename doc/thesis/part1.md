# 摘要

高校校园活动数量多、类型杂、参与人流量大，组织者长期依赖微信群和在线表格完成报名与签到，名额统计靠人工核对，超员、漏签、爽约无记录等问题反复出现。本文以"一套系统支撑活动全流程"为目标，设计并实现了一个基于 Spring Boot 的校园活动管理系统。

系统采用前后端分离架构，后端基于 Spring Boot 2.7 与 MyBatis-Plus 构建，数据库使用 MySQL，前端使用 Vue 2.7 与 Element UI，身份认证采用 JWT 无状态令牌配合自定义角色注解，将使用者划分为学生、组织者、管理员三类。功能上覆盖活动发布与审核、在线报名、扫码签到、活动评价、数据统计与系统管理六条主线，共 8 个业务模块、40 余个接口、19 个页面。

针对报名环节最典型的超员问题，本文没有引入分布式锁或消息队列，而是利用数据库条件更新 `signed_count = signed_count + 1 where signed_count < capacity` 的原子性实现名额扣减，并以影响行数判定是否抢到名额。在容量为 10 的活动上做 50 线程并发对照实验：该方案成功 10 人、拒绝 40 人，计数与明细完全一致；而"先查询再更新"的常规写法在同样压力下 50 人全部报名成功，超卖 40 人，且冗余计数因丢失更新只剩 6。实验说明在单库中小规模并发场景下，一条带条件的 UPDATE 足以替代重量级方案。

系统还设计了限时签到码与爽约限制机制，将签到率、评分、完成率加权为活动质量分，为参与者提供选择依据。系统支持内存数据库演示、单机 jar、内置 JRE 便携包以及云端部署四种运行形态，在 Windows 环境下完成了从开发、测试到换机运行的完整验证。

**关键词**：校园活动管理；Spring Boot；并发控制；签到；Vue

# Abstract

Campus activities in universities are numerous and diverse, while organizers still rely on chat groups and online spreadsheets to collect sign-ups and record attendance. Counting is done by hand, and problems such as overbooking, missed check-ins and no-shows without records occur repeatedly. This thesis designs and implements a campus activity management system based on Spring Boot, aiming to support the whole life cycle of an activity with one system.

The system adopts a front-end and back-end separated architecture. The back end is built with Spring Boot 2.7 and MyBatis-Plus against MySQL, the front end uses Vue 2.7 with Element UI, and authentication is handled by stateless JWT tokens together with a custom role annotation that distinguishes students, organizers and administrators. Six functional lines are covered: activity publishing and review, online registration, check-in, evaluation, statistics and system administration, forming 8 modules, more than 40 interfaces and 19 pages.

For the overbooking problem, instead of introducing a distributed lock or a message queue, the system performs an atomic quota deduction with a conditional update, `signed_count = signed_count + 1 where signed_count < capacity`, and decides the result by the number of affected rows. A control experiment with 50 concurrent requests on an activity with capacity 10 shows that the proposed approach accepts exactly 10 requests and rejects 40, keeping the counter consistent with the detailed records, while the naive read-then-write implementation accepts all 50 requests, oversells 40 seats, and leaves the counter at 6 because of lost updates.

The system also provides a time-limited check-in code with a no-show penalty, and computes a quality score from attendance rate, rating and completion rate. Four deployment forms are supported and verified on Windows: in-memory demo, single executable jar, a portable package with a bundled JRE, and cloud deployment.

**Key words**: campus activity management; Spring Boot; concurrency control; check-in; Vue

# 第1章 绪论

## 1.1 研究背景

校园活动是高校第二课堂的主要载体。以笔者所在学院为例，一个学期内由团委、学生会、社团发布的活动在百场以上，形式包括讲座、比赛、演出、志愿服务和招新。这类活动的共同特点是：单场参与人数从几十到几百不等，报名窗口短则半天、长则两周，签到集中在开始前的十几分钟内。

目前多数活动的组织仍停留在"群里发通知、表格收报名、现场纸质签到"的阶段。这种方式在活动规模较小时尚可应付，一旦报名人数接近名额上限就会暴露问题：表格无法约束名额，先提交的人不一定能参加；组织者要人工比对报名表和签到表，才能算出实际到场人数；活动结束后缺少评价沉淀，下一届组织者难以判断某类活动是否值得继续办。更麻烦的是，报名、签到、评价三类数据分散在不同文件里，无法形成可用的统计。

从技术角度看，这类系统并不复杂，难点集中在两处：一是报名瞬间的并发写入，二是签到环节的现场效率。前者处理不好会出现超员和计数错乱，后者处理不好会让签到台排起长队。本文的工作正是围绕这两点展开，其余功能按常规业务实现。

## 1.2 国内外研究现状

高校活动管理系统在国内已有较多实现，常见做法是基于 Java Web 或 PHP 的传统三层架构，功能覆盖活动发布、报名和简单的名单导出。这类系统的问题在于：报名逻辑多为"先查询名额再插入记录"，在并发场景下存在超卖风险；签到依赖组织者手工勾选，缺少防代签手段；统计功能通常只做到数量汇总，不涉及参与质量。

国外高校普遍使用通用活动平台，如 Eventbrite、Microsoft Teams 的活动模块或 Campus Labs 系列产品。这些产品的优势是流程成熟、支持在线支付和票务，但对中文环境和校园组织架构的适配不足，部署成本高，且核心逻辑闭源，不适合作为教学与研究对象。

在并发控制方面，工程界已经形成较成熟的方案谱系：数据库层面的悲观锁、乐观锁与条件更新，应用层面的本地锁，分布式场景下的 Redis 分布式锁与消息队列削峰。对于本文所面对的单体应用、单库、瞬时并发在数百级别的场景，引入 Redis 或消息队列会显著增加部署复杂度，而数据库条件更新在 InnoDB 行锁的保护下已经能保证正确性。本文选择后者，并通过对照实验说明其效果与边界。

## 1.3 研究内容与论文结构

本文的主要工作包括：

（1）分析校园活动的业务流程，抽象出学生、组织者、管理员三类角色及其权限边界，完成功能需求与非功能需求建模。

（2）设计系统的总体架构、数据库结构与接口规范，重点设计名额原子扣减、限时签到码、爽约限制和活动质量分四项机制。

（3）基于 Spring Boot 与 Vue 实现系统，完成 8 个业务模块与 19 个页面，并通过脚本化测试验证主要流程。

（4）通过并发对照实验量化原子扣减的效果，并完成单机、便携、云端三种部署形态的验证。

论文共七章。第 1 章介绍背景与现状；第 2 章说明所用技术；第 3 章进行需求分析；第 4 章给出系统设计；第 5 章描述实现过程与界面；第 6 章给出测试方案与结果；第 7 章总结并展望后续工作。

# 第2章 相关技术

## 2.1 Spring Boot

Spring Boot 通过自动配置和起步依赖简化了 Spring 应用的搭建。本项目使用 2.7.18 版本，选择该版本的原因是它仍支持 JDK 8，而学校机房与答辩用机普遍安装的是 JDK 8，避免因运行环境不一致导致演示失败。

项目中用到的主要能力包括：`@RestController` 提供 REST 接口；`@Configuration` 配合 `WebMvcConfigurer` 注册跨域、拦截器与静态资源映射；`@Scheduled` 实现定时任务；`spring.sql.init` 在启动时执行建表与初始化脚本；`@Value` 与 `@ConfigurationProperties` 读取配置。全局异常处理通过 `@RestControllerAdvice` 实现，把业务异常统一转换为 `{code, msg, data}` 结构，避免异常堆栈直接暴露给前端。

## 2.2 MyBatis-Plus

MyBatis-Plus 在 MyBatis 之上提供了通用 Mapper 与条件构造器，单表增删改查无需编写 XML。项目中使用 3.5.3.2 版本，主要使用三类能力：

（1）`BaseMapper` 提供的通用方法，配合 `LambdaQueryWrapper` 完成条件查询与分页。

（2）`PaginationInnerInterceptor` 实现物理分页，配置 `DbType.MYSQL` 后由插件自动改写 SQL。

（3）自定义 XML 与注解 SQL。涉及多表联查、聚合统计的语句写在 `resources/mapper/*.xml` 中，例如报名名单联查活动与用户表；只返回单个标量的语句用 `@Select` 注解，减少文件数量。

自动填充通过 `MetaObjectHandler` 实现，插入时写入 `createTime`，更新时写入 `updateTime`，业务代码不再关心这两个字段。

## 2.3 Vue.js 与 Element UI

前端使用 Vue 2.7 的单文件组件写法，路由使用 vue-router 3.x 的 hash 模式。选择 hash 模式的原因是打包后的静态资源可以放在任意子路径下直接访问，不依赖服务器配置 URL 重写，这对把前端塞进 Spring Boot 的 `static` 目录以及部署到静态托管平台都很重要。

状态管理使用 Vuex，保存 token、当前用户信息与未读消息数，并在 `localStorage` 中做持久化，刷新页面后登录态不丢失。UI 组件使用 Element UI 2.15，表格、表单、弹窗、分页等组件直接复用，减少样式工作量。图表使用 ECharts 5，统计页的折线图、环形图与排行表均由后端聚合接口提供数据。

请求封装在 `utils/request.js` 中，统一添加 `Authorization` 头、统一解包 `data`、统一处理 401 与业务错误提示。这样各页面只需关心成功分支，失败提示由拦截器集中处理。

## 2.4 JWT 身份认证

系统使用 java-jwt 3.19.4 生成与校验令牌。用户登录成功后，服务端签发包含 `userId`、`username`、`role` 三个声明的令牌，有效期设置为 7 天，前端在后续请求的 `Authorization` 头中携带。

服务端通过 `HandlerInterceptor` 拦截 `/api/**`，白名单只放行登录、注册与健康检查三个接口。拦截器解析令牌后将用户信息放入 `ThreadLocal`，请求结束时在 `afterCompletion` 中清理，避免线程复用导致数据串号。角色校验通过自定义注解 `@RequireRole` 与方法拦截实现：注解声明允许的角色集合，拦截器读取当前用户角色并做匹配，不匹配时返回 403。

选择 JWT 而不是 Session 的原因是：前端与后端分离部署时，Session 需要额外的共享存储或粘性会话；而 JWT 无状态，令牌自带身份信息，服务端不需要保存会话，扩容与迁移都更简单。代价是令牌签发后无法立即失效，因此系统在用户被禁用时会话仍可读取部分数据，这一点在 7.2 节作为改进方向提出。

## 2.5 MySQL 与 H2

生产与演示环境使用 MySQL 8.0。建表脚本刻意只使用 MySQL 与 H2 的公共语法子集：不使用 `ENGINE`、`COMMENT`、内联 `KEY` 等 MySQL 专有写法，字段类型统一用 `INT`、`VARCHAR`、`DATETIME`、`DECIMAL`、`TINYINT`。这样同一份 `schema.sql` 可以同时被 MySQL 与 H2 执行，避免维护两套脚本。

演示环境使用 H2 1.4.200 的内存模式，连接串为 `jdbc:h2:mem:campus;MODE=MySQL;DATABASE_TO_LOWER=TRUE`。`MODE=MySQL` 让 H2 兼容 MySQL 的部分语法与函数，`DATABASE_TO_LOWER=TRUE` 保证表名与列名大小写一致。内存库在每次启动时重新执行建表与数据脚本，天然满足"演示数据可重置"的需求，答辩时不必担心演示数据被改乱。

由于两套数据库的日期函数存在差异，涉及按天聚合的统计没有写在 SQL 里，而是取出时间列后在 Java 层按 `LocalDate` 归并。这样既避免方言问题，也便于补齐没有数据的日期，防止前端折线图缺格。

## 2.6 并发控制与原子更新

关系数据库通过行锁保证并发写入的正确性。在 InnoDB 的默认隔离级别下，一条 `UPDATE` 语句会对命中的行加排他锁，更新与条件判断在同一条语句内完成，因此"判断名额是否已满"和"名额加一"不会被打断。这为"条件更新"方案提供了基础。

与之相对的是常见的"先查询再更新"写法：先 `select` 出当前报名人数，判断是否小于容量，再 `update` 写入新值。两个请求可能同时读到相同的旧值，都判断为有名额，随后先后写回，后写的值覆盖先写的值，造成丢失更新；更严重的是，两条报名记录都插入成功，实际参与人数超过容量。

本文在实现中采用条件更新，并在第 6 章用同一压测脚本对两种实现做对照，量化二者的差异。
