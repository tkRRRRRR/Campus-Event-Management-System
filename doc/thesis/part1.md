# 摘要

校园活动的组织看似简单，真正做起来却总在几个地方卡住：通知发在群里，报名靠共享表格接龙，名额没人守；活动当天纸质签到，结束后要一张张核对到场名单；活动办完，除了几张照片，几乎没有留下可用的数据。笔者所在学院一个学期有上百场活动，组织者换一届，经验就断一次。本文要解决的问题很具体——把这套流程放进一个系统里，让名额不出错、签到可追溯、活动质量可以被比较。

系统按使用者分成学生、组织者、管理员三类。学生浏览活动、报名、签到、评价；组织者发布活动、管理名单、生成签到码、查看数据；管理员负责审核、用户、分类、公告与参数。技术上采用前后端分离：后端是 Spring Boot 2.7 + MyBatis-Plus，前端是 Vue 2.7 + Element UI，认证用 JWT 加自定义角色注解，没有引入 Spring Security、Redis 这类偏重的组件。最终实现了 8 张表、42 个接口、19 个页面，并把前端构建产物打进 jar，形成"一个文件就能跑"的交付形态。

工作的重点在报名名额。本文没有用分布式锁，而是把"判断还有没有名额"和"名额加一"合并成一条带条件的 UPDATE，靠影响行数判断是否抢到。为了说明这个选择不是想当然，用同一份压测脚本做了对照：容量 10 的活动、50 个并发学生，条件更新方案成功 10 人、拒绝 40 人，`signed_count` 与报名明细都是 10；换成常见的"先查后改"写法，50 人全部报名成功，超卖 40 人，而冗余计数因为丢失更新最后只剩 6——不仅名额错了，连统计都不可信。

除报名外，签到环节做了两件事：签到码 6 位、默认 30 分钟有效、未过期可复用，避免二维码截图被转发；活动结束时未签到者计一次爽约，累计到阈值就限制报名一段时间，补签则回退计数。活动质量分由签到率、评分归一化、完成率按 4:4:2 加权，详情页实时算，不落库，权重可在参数里调整。

系统在 Windows 上完成了从开发、脚本回归、换机运行到打包发布的验证：内存库演示、单机 jar、内置 JRE 的便携包三种形态都能独立启动，第四种云端形态给出了 Docker 与部署配置，但未在本文写作期间实际上线。

**关键词**：校园活动管理；Spring Boot；并发控制；签到码；Vue

# Abstract

Organizing a campus activity looks straightforward until it is actually run: notices go out in chat groups, sign-ups are collected through a shared spreadsheet that nobody guards, attendance is taken on paper and later compared by hand, and once the event ends little usable data remains. In the author's college more than a hundred activities are held each semester, and every change of organizers loses part of the experience. This thesis addresses a narrow but real problem: putting the whole process into one system so that quotas stay correct, attendance can be traced, and activities become comparable.

The system has three roles. Students browse, register, check in and review; organizers publish activities, manage lists, issue check-in codes and read statistics; administrators review activities and maintain users, categories, announcements and parameters. The back end is built with Spring Boot 2.7 and MyBatis-Plus, the front end with Vue 2.7 and Element UI, and authentication relies on JWT with a custom role annotation. Spring Security and Redis were deliberately left out. The result contains eight tables, forty-two endpoints and nineteen pages, and the front-end bundle is packaged into the jar so that the whole system runs from a single file.

The main technical concern is quota control during registration. Instead of a distributed lock, the system merges the capacity check and the increment into one conditional UPDATE and decides by the affected row count. A control experiment was run with the same script: on an activity with capacity 10 and 50 concurrent students, the conditional update accepts 10 and rejects 40, with the counter and the registration records both equal to 10, whereas the common read-then-write implementation accepts all 50, oversells 40 seats, and leaves the counter at 6 because of lost updates.

Check-in uses a six-digit code valid for thirty minutes by default and reused until it expires, which makes forwarded screenshots useless. Students who never check in receive a no-show strike when the activity closes; reaching the threshold suspends registration for a period, and a manual check-in rolls the counter back. A quality score combines attendance rate, normalized rating and completion rate with weights 4:4:2, computed on read rather than stored, and adjustable through system parameters.

Four deployment forms were designed; three of them — in-memory demo, single jar, and a portable package with a bundled JRE — were actually verified on Windows machines, while the cloud form ships as Docker and configuration files but was not deployed online during the writing of this thesis.

**Key words**: campus activity management; Spring Boot; concurrency control; check-in code; Vue

# 第1章 绪论

## 1.1 研究背景

先说一个具体场景。学院团委要办一场 300 人的迎新晚会，通知发在三个年级群里，报名表是一个在线表格，学生在表格里填学号姓名，填满 300 行就截止。问题几乎每次都会出现：有人同时提交，表格出现 305 行；有人填了但临时不来，现场空了三十多个座位；签到是在入口摆两张桌子，志愿者对着名单打勾，散场后要把纸质名单和表格逐行比对，才能算出实际到场率。整个过程没有任何一步留下结构化数据，第二年再办，只能凭印象估计该准备多少座位。

这样的场景在讲座、比赛、志愿服务、社团招新里反复出现。它们的共同点是：单场人数从几十到几百，报名窗口短则半天、长则两周，签到集中在开场前的十几分钟。人数不多，但时间集中，手工方式刚好卡在"能应付"和"总要出错"之间。

从软件角度看，这类系统的功能并不复杂，真正需要想清楚的是两处。第一处是报名瞬间的并发写入：多个学生同时点"报名"，名额只剩一个时，谁该成功？第二处是签到的现场效率与真实性：既要快，又要防止把签到码转发给别人。本文的大部分设计围绕这两点，其余功能按常规业务实现，不额外增加复杂度。

## 1.2 国内外研究现状

国内高校的活动管理系统大致经历了两个阶段。

早期系统解决的是"信息上网"。尹君[1]提出基于 B/S 模式的社团活动管理系统，功能集中在社联新闻、社团展示与资料下载；王蕾[2]用 LAMP 架构搭建校园微活动平台，强调资源占用低、可应对较大规模并行访问。这一阶段的系统把线下的通知与资料搬到浏览器里，报名仍多由后台人工确认。

随后出现流程贯通的系统。侯颖等[3]借助校园一卡通的刷卡记录统计学生参与情况，把签到与硬件采集结合起来；蒋邢飞等[4]设计的"掌上书院"把活动报名、请假、上课签到与图书借阅整合为统一入口，强调与学生事务管理模式相融合；王素美等[5]依托业务基础平台与统一认证快速搭建活动系统，思路是复用校园既有基础设施，减少重复开发。

近几年，前后端分离与移动端成为主流。钟育伙[6]用 Spring Boot + Vue 实现了包含发布、报名、审核、签到与评价的全流程系统；黄智霖[7]以学分电子化为切入点，通过数据库映射与规则匹配，让学生登录后即可看到自己可参与的活动；张银霞等[8]基于 ASP.NET 与 ADO.NET 开发响应式活动管理软件，可在多种终端使用；李胜等[9]则从社交角度出发，强调活动的发起、互动与信息分享。

除流程型系统外，还有两类相关研究。一类关注事后分析：夏青[10]用优化的 K-means 算法对学生校园行为聚类，为辅导员分类管理提供依据；任新钢等[11]把活动项目纳入研究生德育管理平台，侧重思想政治教育功能。另一类关注安全与规范：周超[12]讨论高校大型活动的应急管理，指出观念滞后、方式落后、体系不健全、处置能力弱等问题，主张建设包含日常管理、风险评估、监测预警、响应处置、善后恢复的闭环体系。

国外高校更多使用通用活动平台，例如 Eventbrite 的票务流程、Campus Labs 的学生事务套件。这些产品流程成熟，支持在线支付、票种与退款，工程完成度高；但其组织模型面向商业活动，与国内"团委—学院—社团"的三级审核结构并不贴合，中文环境与私有化部署也不方便，核心逻辑闭源，不适合作为教学研究对象。相关研究更多集中在活动推荐与参与度预测，与本文关注的流程可信度不是同一个问题。

把上述工作放在一起看，可以发现两处空白。其一是名额准确性。对本文收集的 12 篇中文文献做词频检索，"并发"一词一次也没有出现，"超卖"同样为 0，报名环节普遍停留在"先查后插"的写法，这类实现在低并发下不易暴露问题，一旦多人同时提交就会出现名额超出。其二是签到真实性。一卡通方案[3]依赖终端部署，静态二维码方案可以截图转发，而"限时签到码 + 爽约记录"的组合较少被系统讨论。本文把这两点作为设计约束，并在第 6 章用对照实验给出量化结果。

并发控制方面，工程界已有成熟的方案谱系：数据库悲观锁、乐观锁、条件更新，应用内本地锁，以及分布式场景下的 Redis 锁与消息队列削峰。Kleppmann[15]从原理上说明，只要"判断"与"写入"在同一次原子操作中完成，就不需要额外的锁。这一判断构成了本文的选型依据。需要说明的是，它依赖单库、依赖数据库行锁，一旦分库分表或多实例写入不同库，条件更新不再成立，仍需分布式协调。

## 1.3 研究内容与论文结构

本文做了四件事。

（1）梳理校园活动的实际流程，把参与者分成学生、组织者、管理员三类，明确每类人能做什么、不能做什么，形成功能需求与非功能需求。

（2）完成系统设计，包括总体架构、8 张表的数据模型、42 个接口的约定，以及名额扣减、签到码、爽约限制、质量分四项关键机制的具体做法。

（3）实现系统，共 8 个业务模块、19 个页面，并用脚本对主要流程做接口级回归，覆盖权限、边界与异常。

（4）设计并执行并发对照实验，量化条件更新与"先查后改"的差异；同时完成单机、便携两种形态的换机验证。

论文共七章：第 1 章说明背景与现状；第 2 章交代技术选型及使用中遇到的问题；第 3 章做需求分析；第 4 章给出设计；第 5 章描述实现与界面；第 6 章给出测试方案与结果；第 7 章总结并说明不足。

# 第2章 相关技术

## 2.1 Spring Boot

选 Spring Boot 2.7.18 不是因为它最新，而是因为它仍支持 JDK 8。Spring Boot 的设计目标是把 Spring 的配置工作收敛为约定与自动配置[13]，这一点在本文的收益很直接：项目里没有一份 XML 配置，启动类加上注解即可。学校机房、答辩用机和同学自己的笔记本上装的多数还是 JDK 8，若改用 3.x（要求 JDK 17），演示时很可能因为环境不一致打不开。2.7 是 2.x 的最后一个分支，社区资料多，遇到问题容易搜到答案，这对一个人独立完成的项目很重要。

项目里用到的能力不算多：`@RestController` 写接口，`WebMvcConfigurer` 注册跨域、拦截器和静态资源映射，`@Scheduled` 跑定时任务，`spring.sql.init` 在启动时执行建表与初始化脚本，`@RestControllerAdvice` 把异常统一成 `{code, msg, data}`。最后这项对前端帮助很大：业务校验失败、参数格式错误、令牌过期，前端拿到的都是同一种结构，只要判断 `code` 就能决定提示什么。

使用中遇到两个与框架相关的问题，记在这里。一是 MySQL 驱动的坐标：Spring Boot 2.7.18 的依赖管理里没有 `mysql:mysql-connector-java` 的版本，而新版驱动已改用 `com.mysql:mysql-connector-j`，直接写旧坐标会解析不到版本号。二是打包时若应用正在运行，Windows 会锁住 jar，Maven 报 `Unable to rename ... to .original`，必须先结束进程再打包。这类问题不影响设计，但会实实在在地卡住交付。

## 2.2 MyBatis-Plus

持久层用 MyBatis-Plus 3.5.3.2，主要图它两点：单表增删改查不用写 XML，分页插件能直接改写 SQL。项目里的 SQL 分成三类：单表条件查询用 `LambdaQueryWrapper`；多表联查和聚合（报名名单、统计聚合、排行）写在 `resources/mapper/*.xml`；只返回一个数字的（比如某活动签到人数）用 `@Select` 注解，省掉一个文件。

分页插件配置的是 `DbType.MYSQL`，即使演示环境跑的是 H2 也没有改。原因是 H2 以 `MODE=MySQL` 启动后分页语法与 MySQL 一致，统一配置反而减少了分支；如果将来换成 PostgreSQL，这里必须改，否则生成的 `limit` 写法会报错。

自动填充用 `MetaObjectHandler` 完成，插入时写 `create_time`，更新时写 `update_time`，业务代码不再手工赋值。这一条看似小事，但避免了"某个接口忘了写时间"导致排序错乱。

踩过的坑有两个值得记录。其一是泛型推断：想把 `Page<SysUser>` 直接转成 `PageResult<UserVO>` 时，编译器报类型约束冲突，最后改成显式构造 `new PageResult<>(current, size, total, records)`，先查出实体再逐条转 VO。其二是 `selectCount` 返回 `Long`，直接赋给 `int` 字段编译不过，需要 `.intValue()`。这类问题说明，用封装换来的便利，往往要在类型边界上还一点债。

## 2.3 Vue.js 与 Element UI

前端用 Vue 2.7。没有上 Vue 3 的原因很实际：Element UI 只支持 Vue 2，而 Element Plus 的组件 API 与文档在这两年变动较多，一个人写 19 个页面，稳定比新更重要。

路由用 hash 模式，地址形如 `/#/activities`。这样打包后的静态文件放在任何子路径下都能直接打开，不需要服务器配置 URL 重写；把前端塞进 Spring Boot 的 `static` 目录时，这一点尤其省事。代价是地址栏带 `#`，对内部系统来说可以接受。

状态管理用 Vuex，只存三样东西：token、当前用户、未读消息数，并同步到 `localStorage`，刷新页面不掉登录态。请求封装在 `utils/request.js`：统一加 `Authorization` 头、统一解包 `data`、401 时清登录态并跳登录页、其余错误弹一次提示。各页面因此只写成功分支，代码短了不少。

图表用 ECharts 5。统计页的折线、环形、排行三类图，数据都来自后端的聚合接口，前端不做二次计算，免得两边口径不一致。

## 2.4 JWT 身份认证

认证用 java-jwt 3.19.4。令牌的结构与校验规则依据 RFC 7519[14]：登录成功后签发令牌，载荷里放 `userId`、`username`、`role`，有效期 7 天。服务端用 `HandlerInterceptor` 拦截 `/api/**`，白名单只有登录、注册和健康检查三个接口；解析成功后把用户信息放进 `ThreadLocal`，请求结束在 `afterCompletion` 里清理。这一步不能省——Tomcat 复用线程，不清理就会出现"上一个请求的用户串到下一个请求"的问题，而且这种错误很难复现。

角色控制用自定义注解 `@RequireRole`，写在方法或类上，拦截器读取注解声明的角色集合与当前用户比对，不匹配返回 403。这样增删接口时只改注解，不用在业务代码里写 `if (role != ADMIN)`。

没有用 Spring Security，原因有两点：一是它的过滤器链、`UserDetailsService`、异常处理都要重新适配成项目统一的返回体，配置量不小；二是它对新手不够直观，出问题时排查成本高。自己写拦截器只有一百多行，逻辑一眼能看完。这个选择的代价是缺少现成的密码加密、会话管理等功能，好在 BCrypt 由 Hutool 提供，够用。

JWT 的固有缺点是签发后无法主动失效。系统里做了两处弥补：登录时检查账号状态，被禁用的账号直接拒绝；报名时检查 `ban_until`，被封禁的用户即使拿着有效令牌也报不了名。真正的"踢下线"没有实现，这一点在第 7 章列为不足。

## 2.5 MySQL 与 H2

线上用 MySQL 8.0，演示用 H2 1.4.200 内存库。为了让一份建表脚本两边都能跑，`schema.sql` 只使用公共语法子集：不写 `ENGINE`、`COMMENT`、内联 `KEY`，字段类型限定在 `INT`、`VARCHAR`、`DATETIME`、`DECIMAL`、`TINYINT`。索引单独放在 `indexes.sql`，因为它用了 MySQL 的写法，H2 不认——演示库数据量小，没有索引也不影响。

H2 的连接串是 `jdbc:h2:mem:campus;MODE=MySQL;DATABASE_TO_LOWER=TRUE`。`MODE=MySQL` 让 H2 兼容部分 MySQL 语法，`DATABASE_TO_LOWER=TRUE` 保证表名列名的大小写与 MySQL 一致，否则 MyBatis 映射会因大小写对不上而取不到值。内存库每次启动重建，正好满足演示数据要能重置的需求。

两套库的日期函数差异是个麻烦。按天聚合的统计如果写在 SQL 里，H2 与 MySQL 的写法不同，维护两套 SQL 容易出错。最终的做法是只把时间列查出来，在 Java 层按 `LocalDate` 归并，并补齐没有数据的日期。这样既绕开方言，也顺手解决了折线图缺格的问题。

## 2.6 并发控制与原子更新

InnoDB 在默认隔离级别下，一条 `UPDATE` 会对命中的行加排他锁，"判断"与"写入"在同一条语句中完成，不会被其他事务插进来。这就是条件更新能保证名额正确的原因。

常规写法是先 `select` 当前人数，判断小于容量后再 `update` 成新值。两个请求可能读到同一个旧值，都认为还有名额，然后一前一后写回，后写的覆盖先写的，造成丢失更新；与此同时，两条报名记录都已经插入，实际人数超过容量。Kleppmann[15]把这类现象归为并发写入的一类典型错误，强调"读取—判断—写回"之间的窗口必须由某种原子机制填补。这个错误在低并发下几乎不出现，本地用浏览器点几下也测不出来，所以更容易被忽略。

本文的做法是把条件写进 `where`，自增写进 `set`，用影响行数判断结果。这一方案的前提是单库、单表上的行锁有效，且更新与明细插入在同一个事务中。第 6 章用同一份脚本对两种写法做了对照，给出具体数字。
