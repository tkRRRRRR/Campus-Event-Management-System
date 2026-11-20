# 摘要

随着高校第二课堂建设的推进，校园活动的数量与参与规模持续增长，活动组织过程中的信息发布、名额分配、现场签到与效果评价逐渐成为管理难点。现有做法多依赖即时通信群与在线表格，存在名额超额、签到记录失真、参与数据难以沉淀等问题。针对上述问题，本文设计并实现了一个基于 Spring Boot 的校园活动管理系统。

系统采用 B/S 架构与前后端分离模式，后端基于 Spring Boot 2.7 与 MyBatis-Plus 构建，前端采用 Vue 2.7 与 Element UI，身份认证使用 JWT 与自定义角色注解。系统面向学生、活动组织者与管理员三类角色，实现活动发布与审核、在线报名、签到管理、活动评价、通知公告与数据统计等功能，共设计 8 张数据表、58 个接口与 19 个页面。

在关键机制上，本文重点解决两个问题。其一，针对报名环节的名额竞争，将容量判断与计数自增合并为一条带条件的更新语句，以影响行数判定报名结果，避免并发写入造成超额报名；对照实验表明，在容量为 10、并发数为 50 的条件下，该方案成功报名 10 人、拒绝 40 人，计数与明细一致，而"先查询后更新"的实现导致 50 人全部报名成功、超额 40 人，且计数因丢失更新仅剩 6。其二，针对签到的真实性问题，采用限时有效的六位签到码，并在活动结束后对未签到者累计违约次数，达到阈值时限制其报名，补签则回退计数。此外，系统以签到率、评分归一化值与完成率加权计算活动质量分，为活动效果提供可比较的量化指标。

系统完成了功能测试与并发测试，并提供内存数据库演示、单机可执行程序与内置运行环境的便携包三种运行形态，具备部署简便、可离线演示的特点。

**关键词**：校园活动管理；Spring Boot；并发控制；签到管理；Vue

# Abstract

With the advancement of the second classroom in universities, the number and scale of campus activities keep growing, which makes information distribution, quota allocation, on-site check-in and effect evaluation increasingly difficult in activity organization. Current practices usually rely on instant messaging groups and online spreadsheets, resulting in over-booked quotas, unreliable attendance records and dispersed participation data. To address these problems, this thesis designs and implements a campus activity management system based on Spring Boot.

The system adopts a B/S architecture with separated front and back ends. The back end is built on Spring Boot 2.7 and MyBatis-Plus, the front end uses Vue 2.7 and Element UI, and authentication is implemented with JWT and a custom role annotation. Three roles are supported: students, organizers and administrators. The system provides activity publishing and approval, online registration, check-in management, activity review, notices and statistics, covering eight tables, forty-two endpoints and nineteen pages.

Two mechanisms are emphasized. For quota competition during registration, capacity checking and counter increment are combined into a single conditional update, and the result is determined by the affected row count, which prevents over-booking under concurrent writes. A control experiment with capacity 10 and 50 concurrent requests shows that the proposed approach accepts 10 requests and rejects 40, with the counter equal to the number of records, whereas the read-then-write implementation accepts all 50 requests, oversells 40 seats and leaves the counter at 6 due to lost updates. For check-in authenticity, a six-digit code with a limited validity period is adopted, absence strikes are accumulated when activities close, registration is suspended once the threshold is reached, and manual check-in rolls the counter back. In addition, a quality score weighted by attendance rate, normalized rating and completion rate is calculated to provide a comparable indicator of activity performance.

Functional and concurrency tests were carried out. The system supports three runtime forms: an in-memory demo, a standalone executable, and a portable package with a bundled runtime environment.

**Key words**: campus activity management; Spring Boot; concurrency control; check-in; Vue

# 第1章 绪论

## 1.1 研究背景与意义

校园活动是高校育人与校园文化建设的重要载体。随着第二课堂成绩单制度的推行，学生参与活动的数量与质量被纳入培养评价体系，活动组织工作随之由"是否举办"转向"参与是否真实、效果如何衡量"。这一转变对活动管理提出了更高的信息管理要求。

目前多数高校的活动组织仍以人工方式为主。活动信息通过即时通信群发布，报名依托在线表格接龙，名额依靠人工估算，签到采用纸质名单或静态二维码，活动结束后缺少系统的参与数据。以某学院迎新晚会为例，活动规模约 300 人，报名通过在线表格收集，出现同一时段并发提交导致表格记录超出容量 5 条的情况；现场签到由志愿者对照名单手工勾选，散场后需逐行与报名表核对才能计算到场率。此类做法在单场活动尚可维持，但难以支撑一学期上百场活动的组织与统计，且组织者更替后经验难以延续。

开发校园活动管理系统具有以下意义。第一，将活动发布、报名、签到、评价等环节纳入统一平台，减少重复性人工操作，提高组织效率。第二，通过并发控制保证名额分配的准确性，避免超额报名引发的参与体验问题。第三，通过限时签到码与违约记录机制提升签到数据的真实性，为第二课堂学分认定提供可靠依据。第四，通过过程数据的积累与统计，为活动质量的评价与改进提供量化支撑。

## 1.2 国内外研究现状

### 1.2.1 校园活动管理系统的研究现状

国内高校活动管理系统的研究大致可分为三个阶段。

早期系统以信息发布与流程线上化为目标。尹君[1]设计了基于 B/S 模式的社团活动管理系统，将活动申请、审批与发布集中到同一平台，学生在线报名、管理人员导出名单，替代了纸质审批与手工登记。王蕾[2]采用 LAMP 架构构建校园微活动管理平台，强调部署成本低、维护简单，适合院系一级的小规模应用。此类系统的共同特征是功能覆盖以"信息发布与名单登记"为主，对名额约束、到场真实性等问题关注较少。

随后，研究者开始将活动管理与校园既有信息系统集成。侯颖等[3]基于校园一卡通设计了学生活动智能管理系统，利用一卡通数据完成身份识别与签到记录，提升了数据的一致性。蒋邢飞等[4]设计的"掌上书院"将活动、社团、事务办理整合到移动端入口，强调学生事务的一站式办理。王素美等[5]提出基于业务管理平台的校园活动管理系统，通过统一平台支撑多部门的业务协同。此类系统解决了数据分散的问题，但活动报名仍以功能实现为主，对并发场景下的名额控制缺乏讨论。

近年来，前后端分离架构在校园系统开发中得到广泛应用。钟育伙[6]采用 SpringBoot 与 Vue 实现了校园活动管理系统，完成了活动发布、报名与签到等基本功能；黄智霖[7]以厦门华厦学院为例设计了高校校园活动管理系统，覆盖活动全流程管理；张银霞等[8]基于 ASP.NET 开发了学校活动智能管理软件，在功能完整性与界面规范性上做了较多工作；李胜等[9]设计的校园活动社交平台引入了活动互动与社交属性。这一阶段的系统在工程实现上较为成熟，但对报名并发控制、签到防代签等关键问题仍缺少针对性设计。

在活动效果评价方面，夏青[10]采用 K-means 算法对学生校园活动数据进行分析，实现了参与行为的聚类；任新钢等[11]讨论了以网络平台开展研究生德育工作的方式；周超[12]从应急管理角度分析了高校大型活动中的风险与组织缺失问题。上述研究为活动数据的利用提供了思路，但尚未形成可直接用于活动质量比较的量化指标。

### 1.2.2 相关技术研究

Spring Boot 以"约定优于配置"为设计原则，通过自动配置与起步依赖简化 Spring 应用的搭建过程[13]，已成为国内中小型 Web 系统的主流选择。持久层框架 MyBatis-Plus 在 MyBatis 基础上提供通用 CRUD 与分页插件，可在保留手写 SQL 能力的同时减少重复代码。前端方面，Vue 采用数据驱动视图的方式，配合 Element UI 组件库可快速构建管理类界面。

在身份认证方面，基于令牌的方案被广泛采用。JSON Web Token 以自包含的方式在客户端与服务端之间传递身份信息[14]，服务端无需保存会话状态，适合前后端分离与无状态部署。

在并发控制方面，常见方案包括数据库悲观锁、乐观锁、条件更新、应用内本地锁以及基于 Redis 的分布式锁。Kleppmann[15]指出，并发写入的正确性取决于"读取—判断—写入"是否作为原子操作完成；若该窗口未被机制覆盖，就会出现丢失更新。对于单库、中小规模并发的应用，将判断条件与更新操作合并为一条语句并由数据库保证原子性，是复杂度与性能较为均衡的做法。

### 1.2.3 现有研究的不足

综合上述研究，现有工作存在两点不足。第一，多数系统侧重功能覆盖，对报名环节的并发写入缺少设计与验证，名额超额问题未被系统讨论。本文对收集的 12 篇校园活动管理系统文献进行全文检索，"并发"与"超卖"两个关键词的出现次数均为 0，说明该问题尚未进入研究视野。第二，签到方式以静态二维码或纸质名单为主，缺少对代签与缺席的约束机制，到场数据难以作为评价依据。本文针对上述两点展开设计，并通过对照实验与测试用例验证。

## 1.3 研究内容与论文结构

本文的主要工作包括四个方面。

（1）需求分析。通过业务流程梳理与角色划分，明确学生、组织者、管理员三类用户的功能边界，形成功能需求与非功能需求，并给出报名、签到、评价三条核心流程。

（2）系统设计。完成总体架构、功能模块、数据库与接口设计，重点给出名额原子扣减、限时签到码与违约限制、活动质量分三项关键机制的设计方案。

（3）系统实现。基于 Spring Boot 与 Vue 实现各功能模块，完成 8 张数据表、58 个接口与 19 个页面，并支持内存数据库演示、单机运行与便携运行三种形态。

（4）系统测试。设计功能测试用例覆盖权限、边界与异常场景，并设计并发对照实验，量化条件更新与"先查询后更新"两种实现的差异。

论文共分七章。第 1 章介绍研究背景、国内外现状与研究内容；第 2 章说明相关技术及其选型依据；第 3 章进行需求分析；第 4 章给出系统设计；第 5 章描述系统实现；第 6 章说明测试方案与结果；第 7 章总结全文并指出不足与改进方向。

# 第2章 相关技术

## 2.1 Spring Boot

Spring Boot 是在 Spring 框架基础上构建的快速开发框架，其核心设计思想是约定优于配置[13]。框架通过自动配置机制根据类路径中的依赖推断所需组件，通过起步依赖统一管理依赖版本，并提供内嵌 Web 容器，使应用可以以可执行 JAR 的形式独立运行。

本文选择 Spring Boot 2.7.18 作为后端框架，主要依据如下。其一，该版本支持 JDK 8，与本课题的运行环境一致；Spring Boot 3.x 要求 JDK 17 及以上，在机房与答辩环境中适配成本较高。其二，2.7 系列为 2.x 的长期维护分支，文档与社区资料完整，便于独立开发过程中的问题排查。其三，框架提供的拦截器、全局异常处理、定时任务与静态资源映射等能力已能覆盖本系统的需求，无需引入额外的企业级组件。

系统中使用的主要能力包括：以 `@RestController` 构建 REST 接口；通过 `WebMvcConfigurer` 注册跨域配置、身份拦截器与静态资源映射；通过 `@Scheduled` 实现活动状态的定时维护；通过 `spring.sql.init` 在启动时执行建表与初始化脚本；通过 `@RestControllerAdvice` 将业务异常、参数校验异常与类型转换异常统一转换为 `{code, msg, data}` 结构。统一的响应结构降低了前端错误处理的复杂度，使前端只需依据业务状态码判断处理分支。

在工程实践中需要注意两点。第一，Spring Boot 2.7.18 的依赖管理未包含 `mysql:mysql-connector-java` 的版本，而新版 MySQL 驱动已迁移至 `com.mysql:mysql-connector-j`，依赖坐标需相应调整。第二，应用运行期间执行打包时，操作系统会锁定已加载的 JAR 文件，导致 Maven 重命名失败，需在打包前停止应用进程。

## 2.2 MyBatis-Plus

MyBatis-Plus 是在 MyBatis 之上增强的持久层框架，提供通用 CRUD 接口、条件构造器、分页插件与字段自动填充等功能，在保留原生 SQL 能力的同时减少样板代码。

本系统的数据访问按三类场景组织。第一类为单表条件查询与更新，使用 `LambdaQueryWrapper` 构造条件，避免手写字段名带来的拼写风险。第二类为多表关联查询与聚合统计，包括报名名单、签到名单、分类统计与报名排行，采用 XML 映射文件编写 SQL，便于维护复杂语句。第三类为返回单一标量的查询，如某活动的签到人数，采用注解方式书写，减少文件数量。

分页功能通过分页插件实现，数据库方言统一配置为 `DbType.MYSQL`。由于演示环境使用的 H2 数据库以 MySQL 兼容模式启动，其分页语法与 MySQL 一致，统一配置可减少分支处理；如后续更换为 PostgreSQL 等数据库，则需同步调整方言配置。

字段自动填充通过 `MetaObjectHandler` 实现，插入时写入创建时间，更新时写入更新时间，保证时间字段的一致性。此外，系统在报名与评价表上建立了 `(activity_id, user_id)` 唯一索引，作为防止重复提交的最终约束，并将唯一键冲突异常转换为统一的业务提示。

开发过程中需注意两个类型相关问题：其一，分页结果由实体分页对象转换为视图对象分页结果时，直接使用泛型方法会触发类型推断约束冲突，需显式构造结果对象；其二，`selectCount` 返回 `Long` 类型，赋值给整型字段时需显式转换。

## 2.3 Vue 与 Element UI

Vue 是一款渐进式前端框架，采用数据驱动视图与组件化开发方式，通过响应式数据绑定减少手动 DOM 操作。Element UI 是面向桌面端管理系统的组件库，提供表格、表单、弹窗、分页等常用组件，与本系统的管理类界面需求匹配。

系统前端基于 Vue 2.7 构建。选择 Vue 2 的主要原因是 Element UI 仅支持 Vue 2 版本，而系统需要在有限开发周期内保证界面组件的稳定性。路由采用 hash 模式，使构建产物在任意静态路径下均可直接访问，无需服务端配置 URL 重写；该模式配合将前端资源打包进后端 JAR 的部署方式，可简化部署流程。状态管理采用 Vuex，仅保存令牌、当前用户信息与未读消息数，并持久化至本地存储，以维持刷新后的登录状态。

网络请求通过封装的 Axios 实例统一处理：请求阶段附加认证头，响应阶段按业务状态码分流，认证失效时清除本地登录状态并跳转登录页，其余错误统一提示。图表展示采用 ECharts 5，统计页面的折线图、环形图与排行榜数据均来自后端聚合接口，前端不重复计算，以保证统计口径一致。

## 2.4 JWT 身份认证

JSON Web Token 是一种基于 JSON 的开放标准[14]，令牌由头部、载荷与签名三部分组成，服务端通过签名校验令牌的完整性与来源。由于令牌自身携带身份信息，服务端无需保存会话，适合前后端分离架构与无状态部署。

系统采用 java-jwt 3.19.4 实现认证。用户登录成功后，服务端签发包含用户标识、用户名与角色的令牌，有效期设为 7 天。请求进入时由拦截器校验令牌，解析成功后将用户信息存入 `ThreadLocal`，供业务层获取当前用户；请求结束后在 `afterCompletion` 中清理该变量。由于 Web 容器复用工作线程，若未及时清理，后续请求可能读取到前一次请求的用户信息，产生身份串扰。

角色控制通过自定义注解 `@RequireRole` 实现。注解声明允许访问的角色集合，拦截器将其与当前用户角色比对，不匹配时返回 403。该方式将权限声明与业务代码分离，新增接口时只需在方法上添加注解，便于维护。

系统未引入 Spring Security，原因有两点：一是其过滤器链与认证流程需额外适配以输出统一的响应结构，配置成本较高；二是对于角色模型简单的系统，自行实现拦截器的代码量更小、逻辑更直观。该选择的代价是缺少框架提供的会话管理与安全防护组件，系统通过 BCrypt 完成密码摘要存储，并在登录与报名环节分别校验账号状态与违约封禁状态，以弥补令牌无法主动失效的不足。

## 2.5 MySQL 与 H2

系统生产环境使用 MySQL 8.0，演示环境使用 H2 1.4.200 内存数据库。为兼容两种数据库，建表脚本仅使用公共语法子集：不指定存储引擎、不书写字段注释、不使用内联索引定义，字段类型限定为整型、变长字符串、日期时间、定点数与微整型。索引定义单独存放于独立的脚本文件，因其语法为 MySQL 特有，H2 不兼容；演示环境数据量较小，缺少索引不影响功能验证。

H2 以内存模式运行，连接串中指定 `MODE=MySQL` 以兼容 MySQL 的部分语法，并指定 `DATABASE_TO_LOWER=TRUE` 以保证表名与列名的大小写形式与 MySQL 一致，避免映射失败。内存数据库在每次启动时重建，便于演示数据的重置。

两种数据库在日期函数上存在差异，若在 SQL 中完成按日聚合，则需维护两套语句。系统采用的处理方式是将时间字段查询至应用层，按日期在 Java 中归并统计，并补齐无数据的日期，从而避免数据库方言差异，同时保证趋势图的横轴连续。

## 2.6 并发控制与原子更新

在关系数据库中，更新语句在执行时会对命中行加锁，"判断条件"与"写入操作"在同一条语句内完成，其他事务无法在该语句执行期间插入修改。这一特性是条件更新能够保证名额准确性的基础。

常见的实现方式是先查询当前报名人数，判断是否小于容量，再执行更新写入新值。两个并发请求可能读取到同一旧值，均判断为有名额，随后先后写回，后写的结果覆盖先写的结果，形成丢失更新；与此同时，两条报名记录均已插入，实际参与人数超出容量。该问题在低并发条件下不易复现，因而容易被忽视。

本文将容量判断写入更新语句的 `WHERE` 子句，将计数自增写入 `SET` 子句，并以影响行数判断操作是否成功。该方案成立的前提是单库部署、行锁有效，且计数更新与明细插入处于同一事务。第 6 章通过同一压测脚本对两种实现进行对照，给出量化结果。
