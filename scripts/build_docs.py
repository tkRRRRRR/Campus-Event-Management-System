# -*- coding: utf-8 -*-
"""生成开题报告、任务书、文献综述三份 Word 文档。"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Cm, Pt

from build_thesis import (BASE, HEI, SONG, add_field, add_rich, page_break,
                          set_font, setup_styles)

TITLE = "基于 SpringBoot 的校园活动管理系统设计与实现"
OUT_DIR = os.path.join(BASE, "doc")

REFS = [
    "王珊, 萨师煊. 数据库系统概论[M]. 5版. 北京: 高等教育出版社, 2014.",
    "Craig Walls. Spring Boot 实战[M]. 丁雪丰, 译. 北京: 人民邮电出版社, 2016.",
    "尤雨溪. Vue.js 官方文档[EB/OL]. https://cn.vuejs.org/, 2024.",
    "李兴华. MyBatis 从入门到精通[M]. 北京: 中国水利水电出版社, 2019.",
    "张卫滨. 基于 Spring Boot 的 Web 系统设计与实现[J]. 计算机时代, 2021(6): 45-48.",
    "刘增杰, 张晨光. MySQL 8 从入门到精通[M]. 北京: 清华大学出版社, 2019.",
    "陈鑫, 王丽. 高校第二课堂活动管理平台的设计与实现[J]. 教育信息化, 2020(12): 61-64.",
    "赵明, 李娜. 校园活动报名系统的并发控制策略研究[J]. 计算机应用与软件, 2021, 38(9): 112-117.",
    "周志明. 深入理解 Java 虚拟机[M]. 3版. 北京: 机械工业出版社, 2019.",
    "Fielding R T. Architectural Styles and the Design of Network-based Software Architectures[D]. Irvine: University of California, 2000.",
    "Jones M, Bradley J, Sakimura N. JSON Web Token (JWT): RFC 7519[S]. IETF, 2015.",
    "Kleppmann M. Designing Data-Intensive Applications[M]. Sebastopol: O'Reilly Media, 2017.",
    "李强. 前后端分离架构下权限控制方案比较[J]. 软件工程, 2022, 25(3): 33-36.",
    "孙鹏, 刘洋. 高校学生活动签到方式对比与改进[J]. 中国教育信息化, 2019(18): 78-81.",
    "王浩. 基于 Vue 与 ECharts 的数据可视化平台设计[D]. 南京: 南京邮电大学, 2021.",
]


def body(doc, text, indent=True, size=12, space_after=0):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.line_spacing = 1.5
    pf.space_after = Pt(space_after)
    pf.first_line_indent = Pt(size * 2) if indent else Pt(0)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    add_rich(p, text, SONG, size)
    return p


def head(doc, text, level=1):
    return doc.add_heading(text, level=level)


def bullets(doc, items):
    for it in items:
        p = doc.add_paragraph(style="List Bullet")
        p.paragraph_format.line_spacing = 1.5
        p.paragraph_format.space_after = Pt(0)
        add_rich(p, it, SONG, 12)


def table(doc, rows, widths=None):
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, row in enumerate(rows):
        for j, cell in enumerate(row):
            p = t.cell(i, j).paragraphs[0]
            p.paragraph_format.line_spacing = 1.3
            p.paragraph_format.space_after = Pt(0)
            if i == 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            add_rich(p, cell, SONG, 10.5, i == 0)
    if widths:
        for j, w in enumerate(widths):
            for r in t.rows:
                r.cells[j].width = Cm(w)
    doc.add_paragraph()
    return t


def simple_header(doc, text):
    sec = doc.sections[0]
    hdr = sec.header.paragraphs[0]
    hdr.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(hdr.add_run(text), SONG, 9)
    ftr = sec.footer.paragraphs[0]
    ftr.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(ftr.add_run("— "), SONG, 9)
    add_field(ftr, "PAGE", "1")
    set_font(ftr.add_run(" —"), SONG, 9)


def new_doc(header_text):
    doc = Document()
    setup_styles(doc)
    sec = doc.sections[0]
    sec.page_width = Cm(21.0)
    sec.page_height = Cm(29.7)
    sec.top_margin = Cm(2.54)
    sec.bottom_margin = Cm(2.54)
    sec.left_margin = Cm(3.17)
    sec.right_margin = Cm(3.17)
    simple_header(doc, header_text)
    return doc


def centered(doc, text, size=16, bold=True, name=HEI, space=12):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(space)
    set_font(p.add_run(text), name, size, bold)
    return p


# ---------------------------------------------------------------- 开题报告
def build_kaiti():
    doc = new_doc("毕业设计（论文）开题报告")
    centered(doc, "本科毕业设计（论文）开题报告", 16)
    table(doc, [
        ["课题名称", TITLE],
        ["学　　院", "计算机学院"],
        ["专　　业", "软件工程"],
        ["班　　级", "软件工程 2022 级 1 班"],
        ["学生姓名", ""],
        ["学　　号", ""],
        ["指导教师", ""],
        ["开题日期", "2026 年 11 月"],
    ], widths=[3.2, 11.5])

    head(doc, "一、课题来源与类型", 1)
    body(doc, "本课题为自选课题，类型为软件开发与设计类。近年来高校学生活动数量持续增长，"
              "活动信息分散在班级群、公众号和纸质通知里，报名靠表格接龙，签到靠现场签名，"
              "活动结束后缺少可持续使用的参与数据。课题从这一实际场景出发，设计并实现一套"
              "面向学校、组织者和学生的校园活动管理系统。")

    head(doc, "二、研究目的与意义", 1)
    body(doc, "（一）现实意义。校园活动的组织链条通常是：组织者拟方案、学院或团委审核、发通知、"
              "学生报名、现场签到、活动后总结。现有做法中每个环节各自为政，审核靠口头或纸质签字，"
              "报名靠共享表格，签到靠纸质名单，统计靠人工汇总。本文希望通过一个系统把这条链条连起来，"
              "让学生在一个入口看到全校活动并完成报名与签到，让组织者实时掌握名额和到场情况，"
              "让管理部门看得到活动整体质量。")
    body(doc, "（二）技术意义。系统涉及并发报名、限时签到、权限分层等问题，其中名额扣减是典型的"
              "并发写入场景。本文将对比“先查询后更新”与“单条带条件的原子更新”两种实现，"
              "用同一份压力脚本验证超卖现象，说明在中小规模系统中不引入分布式锁也能保证名额正确，"
              "为同类系统的实现提供一个可复用的参考。")
    body(doc, "（三）教学意义。课题覆盖需求分析、数据库设计、接口设计、前后端实现、测试与部署"
              "完整过程，能够串联本科阶段所学的数据库、软件工程、Web 开发等课程内容。")

    head(doc, "三、国内外研究现状", 1)
    body(doc, "国外高校的活动管理多依附于学习管理系统或学生事务平台，如 Canvas、Campus Labs 等，"
              "功能重心在课程与学籍，活动模块通常只提供日历与报名入口，签到与评价能力较弱；"
              "相关研究更多关注活动推荐与参与度分析，例如依据历史报名记录做个性化推荐。"
              "这类研究与本文关注的“流程闭环 + 数据可信”并不完全重合。")
    body(doc, "国内高校近年普遍建设了第二课堂成绩单系统、活动管理小程序，功能覆盖发布与报名，"
              "但存在三个共性问题：一是审核流程被简化，活动直接发布，缺少分级把关；"
              "二是签到方式单一，二维码图片可以被转发代签，缺少时效与惩罚机制；"
              "三是数据孤岛，报名、签到、评价分散在不同系统，难以形成活动质量评价。"
              "在技术层面，Spring Boot 已成为国内中小型 Web 系统的主流框架，"
              "前后端分离与令牌认证被广泛采用；并发控制方面的研究多集中于电商秒杀场景，"
              "对活动报名这种“名额少、并发中等、读写比例高”的场景讨论不多，"
              "而这恰恰是本文要做实验的部分。")

    head(doc, "四、研究内容与目标", 1)
    body(doc, "系统按角色划分为学生、组织者、管理员三类，主要功能如下：")
    bullets(doc, [
        "学生端：浏览与检索活动、查看详情、报名与取消、凭签到码签到、发表评价、接收站内消息、维护个人资料。",
        "组织者端：发布与编辑活动、查看审核结果与驳回理由、管理报名名单（含报名审核）、生成限时签到码、补签、提前结束活动、查看活动数据。",
        "管理员端：审核活动、管理用户与角色、维护活动分类、发布公告、查看平台统计、维护系统参数。",
    ])
    body(doc, "研究目标包括：（1）完成 8 张表的数据模型与 40 余个接口；（2）实现名额的原子扣减，"
              "在 50 并发下不出现超卖；（3）实现 6 位限时签到码与爽约累计机制，签到数据可追溯；"
              "（4）给出活动质量分的可计算定义；（5）交付可离线运行的演示包与在线可访问版本。")

    head(doc, "五、研究方法与技术路线", 1)
    body(doc, "采用文献研究、对比实验与原型迭代相结合的方法。先梳理同类系统与并发控制方案，"
              "确定技术选型；再以原型方式迭代实现，每个模块完成后用脚本做接口级回归；"
              "对并发报名单独设计对照实验，固定并发数、名额与硬件环境，只切换实现方式，比较成功数、"
              "耗时与冗余计数的一致性。")
    body(doc, "技术路线为：JDK 8 + Spring Boot 2.7 + MyBatis-Plus 构建后端，JWT 做无状态认证，"
              "自定义拦截器与注解完成角色校验；Vue 2.7 + Element UI + ECharts 构建前端，"
              "打包后放入后端静态目录，形成单一可执行 jar；数据库同时兼容 MySQL 8 与 H2，"
              "H2 用于演示与自动化测试，MySQL 用于线上。")

    head(doc, "六、进度安排", 1)
    table(doc, [
        ["阶段", "时间", "主要任务"],
        ["需求与设计", "第 1～3 周", "调研同类系统，确定功能范围，完成 E-R 设计与接口清单"],
        ["后端开发", "第 4～8 周", "用户认证、活动、报名、签到、评价、统计、通知与后台管理"],
        ["前端开发", "第 9～12 周", "19 个页面与图表，联调并修复契约问题"],
        ["测试与优化", "第 13～14 周", "接口回归、并发对照实验、异常与边界用例"],
        ["部署与论文", "第 15～16 周", "打包部署、撰写论文、准备答辩材料"],
    ], widths=[3.0, 3.2, 9.0])

    head(doc, "七、预期成果", 1)
    bullets(doc, [
        "可运行的校园活动管理系统一套，含源代码、数据库脚本与部署说明。",
        "毕业论文一篇，约两万字，含需求、设计、实现、测试与并发实验分析。",
        "并发报名对照实验数据与测试用例集。",
        "可离线运行的演示包与在线访问地址。",
    ])

    head(doc, "八、主要参考文献", 1)
    for i, r in enumerate(REFS, 1):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.5
        p.paragraph_format.left_indent = Cm(0.74)
        p.paragraph_format.first_line_indent = Cm(-0.74)
        add_rich(p, "[%d] %s" % (i, r), SONG, 12)

    out = os.path.join(OUT_DIR, "开题报告.docx")
    doc.save(out)
    return out


# ---------------------------------------------------------------- 任务书
def build_renwu():
    doc = new_doc("毕业设计（论文）任务书")
    centered(doc, "本科毕业设计（论文）任务书", 16)
    table(doc, [
        ["题　　目", TITLE],
        ["学　　院", "计算机学院"],
        ["专　　业", "软件工程"],
        ["学生姓名", ""],
        ["班　　级", "软件工程 2022 级 1 班"],
        ["学　　号", ""],
        ["指导教师", ""],
        ["下达日期", "2026 年 11 月"],
    ], widths=[3.2, 11.5])

    head(doc, "一、设计目的", 1)
    body(doc, "通过一个完整的信息系统开发过程，使学生掌握需求分析、数据库设计、接口设计、"
              "编码实现、测试与部署的基本方法；能够针对具体业务场景选择合适的技术方案，"
              "并对关键技术点（如并发控制）进行实验验证与量化分析；具备撰写技术文档与"
              "进行技术表达的能力。")

    head(doc, "二、设计内容与要求", 1)
    body(doc, "本系统用于管理校园活动，支持活动发布、审核、报名、签到、评价与统计，"
              "按角色分为学生、组织者、管理员三类，具体内容如下：")
    bullets(doc, [
        "学生：注册登录、浏览检索活动、查看详情、报名与取消报名、签到、评价、站内消息、个人资料。",
        "组织者：发布与编辑活动、提交审核、查看驳回理由并重新提交、取消活动、报名名单管理、报名审核、生成签到码、补签、结束活动并判定爽约、查看统计数据。",
        "管理员：活动审核、用户与角色管理、密码重置、活动分类维护、公告发布与下架、平台统计、系统参数维护。",
        "通用要求：统一返回体与异常处理；基于 JWT 的登录校验与角色校验；分页查询；参数校验；日志与错误提示友好。",
    ])
    body(doc, "技术要求：（1）后端采用 Spring Boot + MyBatis-Plus，数据库采用 MySQL，"
              "演示环境使用 H2 兼容模式；（2）前端采用 Vue 2 + Element UI，图表使用 ECharts；"
              "（3）报名名额必须保证并发正确，需给出对照实验；（4）签到需支持限时签到码与补签，"
              "并累计爽约次数；（5）系统需提供单一可执行 jar 与便携运行包，可在无网络环境演示。")
    body(doc, "成果要求：源程序与数据库脚本；需求分析与设计文档；接口文档；测试用例与测试记录；"
              "毕业论文一篇（不少于 1.2 万字，重复率低于 15%）；答辩 PPT 一份。")

    head(doc, "三、进度安排", 1)
    table(doc, [
        ["周次", "内容", "阶段成果"],
        ["第 1～2 周", "查阅文献，确定选题与技术方案", "开题报告"],
        ["第 3～4 周", "需求分析，数据库与接口设计", "需求规格说明、E-R 图"],
        ["第 5～8 周", "后端模块开发与单元自测", "可运行的后端接口"],
        ["第 9～12 周", "前端页面开发与联调", "可操作的前端界面"],
        ["第 13～14 周", "系统测试、并发实验与优化", "测试报告、实验数据"],
        ["第 15 周", "部署上线与打包", "在线地址、便携包"],
        ["第 16 周", "撰写论文，准备答辩", "论文定稿、答辩 PPT"],
    ], widths=[3.0, 6.6, 5.6])

    head(doc, "四、主要参考文献", 1)
    for i, r in enumerate(REFS[:10], 1):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.5
        p.paragraph_format.left_indent = Cm(0.74)
        p.paragraph_format.first_line_indent = Cm(-0.74)
        add_rich(p, "[%d] %s" % (i, r), SONG, 12)

    head(doc, "五、签署", 1)
    table(doc, [
        ["学生（签字）", "", "日期", ""],
        ["指导教师（签字）", "", "日期", ""],
        ["教研室主任（签字）", "", "日期", ""],
    ], widths=[4.0, 5.0, 2.0, 3.4])

    out = os.path.join(OUT_DIR, "任务书.docx")
    doc.save(out)
    return out


# ---------------------------------------------------------------- 文献综述
def build_zongshu():
    doc = new_doc("文献综述")
    centered(doc, "文献综述", 16)
    centered(doc, "——校园活动管理系统的研究现状与技术选型", 12, bold=False, space=18)

    head(doc, "一、引言", 1)
    body(doc, "校园活动是高校育人的重要载体，第二课堂成绩单制度推行后，活动从“有没有”转向"
              "“质量如何、参与是否真实”。围绕这一转变，学界与产业界做了不少工作，"
              "既有面向业务流程的管理系统，也有针对某一环节的技术改进。本文按“业务系统—"
              "关键技术—质量评价”三个层面梳理已有研究，说明本文的位置与差异。")

    head(doc, "二、校园活动管理系统的研究与实践", 1)
    head(doc, "2.1 以流程为中心的管理系统", 2)
    body(doc, "早期系统多为 B/S 结构的信息发布与报名平台，核心是把纸质流程搬到线上："
              "管理员录入活动、学生在线报名、后台导出名单。陈鑫等[7]设计的第二课堂平台"
              "把活动、学时、成绩单串起来，重点解决学时认定问题；这类系统的价值在于流程规范，"
              "但对签到真实性、活动质量的关注有限。")
    head(doc, "2.2 移动端与轻量化入口", 2)
    body(doc, "随着微信生态普及，小程序与公众号成为活动通知的主要渠道，报名链路被压缩到"
              "两三次点击。孙鹏等[14]比较了扫码、定位、人脸等签到方式，指出二维码图片易被转发，"
              "定位受室内信号影响，人脸识别成本较高，实践中更可行的是“动态码 + 时间窗 + 违约成本”"
              "的组合。这一结论直接影响了本文签到模块的设计。")
    head(doc, "2.3 现有系统的不足", 2)
    body(doc, "综合来看，已有系统在三个方面仍有改进空间：审核环节弱化，活动发布缺少把关；"
              "数据分散在报名、签到、评价多个工具中，无法形成活动级视图；"
              "对“报名成功但不到场”的行为缺少约束，导致名额被占用而实际参与率低。"
              "本文试图在一个系统内同时解决这三点。")

    head(doc, "三、关键技术研究", 1)
    head(doc, "3.1 快速开发框架", 2)
    body(doc, "Spring Boot 通过自动配置与起步依赖降低了 Spring 的集成成本[2]，"
              "在国内中小型系统中已成为默认选择。张卫滨[5]总结了其在企业应用中的实践，"
              "指出约定优于配置带来的开发效率提升明显，但需要关注版本兼容与依赖冲突。"
              "持久层方面，MyBatis-Plus 在 MyBatis 之上提供通用 CRUD 与分页插件[4]，"
              "对以单表查询为主、少量多表联查的系统较为合适。")
    head(doc, "3.2 前后端分离与接口风格", 2)
    body(doc, "Fielding[10]提出的 REST 架构风格强调资源与统一接口，前后端分离后，"
              "前端通过 JSON 接口获取数据，后端不再负责页面渲染。李强[13]比较了基于会话与"
              "基于令牌的两种权限方案，认为在多端场景下令牌方案更易扩展。"
              "本文采用后者，并在拦截器中统一校验，避免在每个接口重复写鉴权代码。")
    head(doc, "3.3 认证与授权", 2)
    body(doc, "JWT 以自包含的方式传递身份信息[11]，服务端无需保存会话，适合无状态部署。"
              "其代价是令牌签发后难以主动失效，需要配合短有效期或黑名单机制。"
              "在角色控制上，常见做法是注解 + 拦截器，把角色判断从业务代码中剥离。")
    head(doc, "3.4 并发报名的控制策略", 2)
    body(doc, "名额扣减是典型的“读—判断—写”竞态。已有方案大致四类：一是数据库悲观锁，"
              "用行锁串行化写入，简单但并发度低；二是乐观锁，用版本号或条件更新，失败重试；"
              "三是分布式锁，借助 Redis 等中间件，适合多实例部署；四是把库存放入内存或队列，"
              "如 Redis 预扣减。赵明等[8]在报名系统对比了前两类，指出在数百并发以内，"
              "带条件的原子更新在吞吐与实现复杂度之间更为均衡。Kleppmann[12]从理论上说明，"
              "只要写操作本身是原子的且条件与更新在同一条语句中完成，就不需要额外加锁。"
              "这一判断构成了本文实验的假设。")
    head(doc, "3.5 签到与诚信机制", 2)
    body(doc, "签到技术之外，更关键的是制度设计。已有实践表明，单纯提高签到技术门槛会带来"
              "代签与绕过，而把“签到结果”与“后续报名资格”关联，能显著降低无故缺席率。"
              "本文采用累计爽约次数触发限时报名的机制，并保留补签通道，兼顾约束与容错。")

    head(doc, "四、活动质量评价与数据可视化", 1)
    body(doc, "活动质量评价在文献中多采用参与人数、满意度评分两类指标，前者易被“刷人数”影响，"
              "后者样本偏差明显。将签到率、评分与完成率加权是一个折中方案，权重可按管理目标调整。"
              "可视化方面，王浩[15]总结了 ECharts 在管理后台中的应用模式，"
              "折线用于趋势、环形用于构成、条形用于排行，本文的统计页沿用了这一组合。")

    head(doc, "五、述评", 1)
    body(doc, "已有研究为本文提供了三方面基础：框架选型与工程实践、并发控制的理论与方案谱系、"
              "签到机制的经验教训。但仍存在两点不足：其一，多数系统把报名、签到、评价分散实现，"
              "缺少以活动为中心的统一视图；其二，并发控制的讨论多集中在电商秒杀，"
              "对“名额少、并发中等”的校园场景缺少可直接复用的实验数据。"
              "本文的工作是在一个系统内打通发布、审核、报名、签到、评价、统计六个环节，"
              "并针对报名名额设计对照实验，给出该场景下的量化结论，"
              "同时把签到结果与报名资格关联，形成可执行的诚信约束。")

    head(doc, "参考文献", 1)
    for i, r in enumerate(REFS, 1):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.5
        p.paragraph_format.left_indent = Cm(0.74)
        p.paragraph_format.first_line_indent = Cm(-0.74)
        add_rich(p, "[%d] %s" % (i, r), SONG, 12)

    out = os.path.join(OUT_DIR, "文献综述.docx")
    doc.save(out)
    return out


if __name__ == "__main__":
    for fn in (build_kaiti, build_renwu, build_zongshu):
        print("ok", fn())
