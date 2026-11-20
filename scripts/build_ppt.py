# -*- coding: utf-8 -*-
"""生成答辩 PPT（16:9）与其中的数据对比图。"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.util import Emu, Inches, Pt

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_thesis import BASE  # noqa: E402

FIG_DIR = os.path.join(BASE, "doc", "figures")
OUT = os.path.join(BASE, "doc", "答辩PPT.pptx")

PRIMARY = RGBColor(0x2F, 0x7D, 0x6B)
DARK = RGBColor(0x1F, 0x3A, 0x34)
GREY = RGBColor(0x5A, 0x66, 0x63)
LIGHT = RGBColor(0xEA, 0xF3, 0xF0)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
ORANGE = RGBColor(0xE0, 0x8A, 0x3C)

FONT = "微软雅黑"
YAHEI = r"C:\Windows\Fonts\msyh.ttc"
YAHEI_B = r"C:\Windows\Fonts\msyhbd.ttc"


def font(size, bold=False):
    return ImageFont.truetype(YAHEI_B if bold else YAHEI, size)


# ---------------------------------------------------------------- 对比图
def draw_compare():
    W, H = 1300, 760
    img = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(img)

    d.text((50, 26), "成功报名人数", font=font(30, True), fill=(60, 70, 68))

    groups = [
        ("原子更新（本文）", [("成功", 10, PRIMARY), ("拒绝", 40, RGBColor(0x9A, 0xB5, 0xAE))],
         "signed = joined = 10", PRIMARY),
        ("先查后改（对照）", [("成功", 50, ORANGE), ("拒绝", 0, RGBColor(0xD8, 0xC4, 0xAE))],
         "signed = 6，joined = 50", ORANGE),
    ]

    base_y = 600
    max_h = 360
    for gi, (title, bars, note, color) in enumerate(groups):
        ox = 90 + gi * 640
        d.text((ox, 120), title, font=font(30, True), fill=(31, 58, 52))
        d.line([ox, base_y, ox + 520, base_y], fill=(200, 205, 203), width=3)
        for bi, (label, val, c) in enumerate(bars):
            x = ox + 80 + bi * 190
            h = max(6, int(max_h * val / 50.0))
            d.rectangle([x, base_y - h, x + 120, base_y], fill=c)
            d.text((x + 6, base_y - h - 50), str(val), font=font(44, True), fill=(40, 48, 46))
            tw = d.textlength(label, font=font(28))
            d.text((x + 60 - tw / 2, base_y + 16), label, font=font(28), fill=(70, 78, 76))
        d.text((ox, base_y + 72), note, font=font(28, True), fill=color)

    path = os.path.join(FIG_DIR, "fig-compare.png")
    img.save(path)
    return path


def draw_quality():
    W, H = 1400, 720
    img = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(img)
    d.text((70, 40), "活动质量分构成", font=font(42, True), fill=(31, 58, 52))
    items = [
        ("签到率", "40", PRIMARY, "已签到 / 报名人数"),
        ("评分归一化", "40", RGBColor(0x4E, 0x9A, 0x86), "(均分 − 1) / 4，映射到 0~1"),
        ("完成率", "20", ORANGE, "1 − 取消率"),
    ]
    for i, (name, val, c, desc) in enumerate(items):
        y = 170 + i * 170
        d.rectangle([70, y, 1330, y + 140], fill=(246, 249, 248), outline=(220, 226, 224))
        d.rectangle([70, y, 90, y + 140], fill=c)
        d.text((130, y + 30), name, font=font(36, True), fill=(31, 58, 52))
        d.text((130, y + 86), desc, font=font(24), fill=(100, 112, 108))
        d.text((1180, y + 34), val + " 分", font=font(40, True), fill=c)
    path = os.path.join(FIG_DIR, "fig-quality.png")
    img.save(path)
    return path


# ---------------------------------------------------------------- 版式
def blank(prs):
    return prs.slides.add_slide(prs.slide_layouts[6])


def rect(slide, l, t, w, h, fill, line=None):
    from pptx.enum.shapes import MSO_SHAPE
    shp = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, l, t, w, h)
    shp.fill.solid()
    shp.fill.fore_color.rgb = fill
    if line is None:
        shp.line.fill.background()
    else:
        shp.line.color.rgb = line
        shp.line.width = Pt(1)
    shp.shadow.inherit = False
    return shp


def _apply_font(run, size, bold, color):
    run.font.name = FONT
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    rPr = run.font._rPr
    from lxml import etree
    ns = "http://schemas.openxmlformats.org/drawingml/2006/main"
    for tag in ("ea", "cs"):
        el = rPr.find("{%s}%s" % (ns, tag))
        if el is None:
            el = etree.SubElement(rPr, "{%s}%s" % (ns, tag))
        el.set("typeface", FONT)


def text(slide, s, l, t, w, h, size=20, bold=False, color=DARK, align=PP_ALIGN.LEFT,
         anchor=MSO_ANCHOR.TOP, spacing=1.15):
    tb = slide.shapes.add_textbox(l, t, w, h)
    tf = tb.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = 0
    tf.margin_right = 0
    tf.margin_top = 0
    tf.margin_bottom = 0
    lines = s.split("\n")
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        r = p.add_run()
        r.text = line
        _apply_font(r, size, bold, color)
    return tb


def title(slide, s, sub=None):
    rect(slide, Inches(0.7), Inches(0.52), Inches(0.09), Inches(0.62), PRIMARY)
    text(slide, s, Inches(0.95), Inches(0.45), Inches(10.5), Inches(0.8), 30, True, DARK)
    if sub:
        text(slide, sub, Inches(0.95), Inches(1.18), Inches(11.5), Inches(0.5), 16, False, GREY)


def bullets(slide, items, t, l=Inches(0.95), w=Inches(11.2), size=19, gap=0.62):
    y = t
    for it in items:
        dot = slide.shapes.add_shape(1, l, y + Inches(0.12), Inches(0.09), Inches(0.09))
        dot.fill.solid()
        dot.fill.fore_color.rgb = PRIMARY
        dot.line.fill.background()
        dot.shadow.inherit = False
        text(slide, it, l + Inches(0.28), y, w - Inches(0.3), Inches(gap), size, False, GREY)
        y += Inches(gap)


def image_fit(slide, path, l, t, max_w, max_h):
    from PIL import Image as PImage
    with PImage.open(path) as im:
        iw, ih = im.size
    scale = min(max_w / iw, max_h / ih)
    w, h = int(iw * scale), int(ih * scale)
    left = l + int((max_w - w) / 2)
    top = t + int((max_h - h) / 2)
    slide.shapes.add_picture(path, left, top, width=w, height=h)


def kpi(slide, items, t, size=34):
    n = len(items)
    gap = Inches(0.25)
    total_w = Inches(11.9)
    w = int((total_w - gap * (n - 1)) / n)
    x = Inches(0.7)
    for value, label, color in items:
        rect(slide, x, t, w, Inches(1.35), LIGHT)
        text(slide, value, x, t + Inches(0.16), w, Inches(0.7), size, True, color, PP_ALIGN.CENTER)
        text(slide, label, x, t + Inches(0.86), w, Inches(0.4), 15, False, GREY, PP_ALIGN.CENTER)
        x += w + gap


def notes(slide, s):
    slide.notes_slide.notes_text_frame.text = s


# ---------------------------------------------------------------- 组装
def build():
    cmp_img = draw_compare()
    q_img = draw_quality()

    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    # 1 封面
    s = blank(prs)
    rect(s, 0, 0, prs.slide_width, prs.slide_height, RGBColor(0x1F, 0x3A, 0x34))
    rect(s, 0, Inches(4.62), prs.slide_width, Inches(0.06), PRIMARY)
    text(s, "本科毕业设计（论文）答辩", Inches(1.0), Inches(1.35), Inches(9), Inches(0.5), 18, False,
         RGBColor(0x9A, 0xC8, 0xB8))
    text(s, "基于 SpringBoot 的校园活动管理系统", Inches(1.0), Inches(1.95), Inches(11.4),
         Inches(1.0), 42, True, WHITE)
    text(s, "设计与实现", Inches(1.0), Inches(2.85), Inches(6), Inches(0.8), 42, True, WHITE)
    text(s, "活动发布 · 在线报名 · 限时签到 · 参与评价 · 数据统计", Inches(1.0), Inches(3.95),
         Inches(11), Inches(0.5), 20, False, RGBColor(0xBF, 0xDA, 0xD1))
    text(s, "答辩人：_____________　　学号：_____________", Inches(1.0), Inches(5.05), Inches(10),
         Inches(0.45), 18, False, WHITE)
    text(s, "指导教师：_____________　　专业：软件工程", Inches(1.0), Inches(5.6), Inches(10),
         Inches(0.45), 18, False, WHITE)
    text(s, "2026 年 6 月", Inches(1.0), Inches(6.35), Inches(6), Inches(0.45), 16, False,
         RGBColor(0x9A, 0xC8, 0xB8))
    notes(s, "各位老师好，我汇报的题目是基于 SpringBoot 的校园活动管理系统设计与实现。")

    # 2 选题背景
    s = blank(prs)
    title(s, "选题背景：活动管理流程中的薄弱环节", "调研本校 6 个学院的活动组织流程，问题集中于报名、签到与统计环节")
    cols = [
        ("审核", "以口头或纸质签字确认，活动直接上线，缺少把关"),
        ("报名", "借助共享表格接龙，名额人工统计，易出现超卖或漏统计"),
        ("签到", "纸质签到或转发二维码，代签难以识别"),
        ("统计", "报名、签到、评价分散于不同工具，活动结束后数据难以沉淀"),
    ]
    for i, (h, d) in enumerate(cols):
        x = Inches(0.7) + i * Inches(3.05)
        card = rect(s, x, Inches(1.95), Inches(2.85), Inches(2.5), RGBColor(0xF6, 0xF9, 0xF8),
                    RGBColor(0xDD, 0xE6, 0xE3))
        text(s, h, x + Inches(0.25), Inches(2.15), Inches(2.4), Inches(0.5), 22, True, PRIMARY)
        text(s, d, x + Inches(0.25), Inches(2.7), Inches(2.4), Inches(1.5), 15, False, GREY)
    text(s, "系统目标：将六个环节纳入统一流程，使参与数据可核验、可追溯",
         Inches(0.7), Inches(5.0), Inches(11.9), Inches(0.6), 22, True, DARK)
    bullets(s, ["发布、审核、报名、签到、评价、统计在同一系统内完成",
                "名额扣减、签到码与违约约束三处进行针对性设计，并通过实验验证"],
            Inches(5.6), Inches(0.7), Inches(11.6), 17, 0.55)
    notes(s, "四个环节各自为政，数据难以对应，这是选题的直接原因。")

    # 3 需求与角色
    s = blank(prs)
    title(s, "需求分析：三类角色与功能边界", "8 张表、58 个接口、21 个页面")
    roles = [
        ("学生", "活动浏览、报名、签到与评价", "浏览检索 / 报名取消 / 扫码签到 / 提交评价 / 消息"),
        ("组织者", "活动发布、名单管理与数据查看", "发布编辑 / 报名审核 / 签到码与补签 / 活动统计"),
        ("管理员", "活动审核、用户与参数管理", "活动审核 / 用户角色 / 分类公告 / 平台统计 / 参数"),
    ]
    for i, (name, tag, detail) in enumerate(roles):
        y = Inches(1.95) + i * Inches(1.62)
        rect(s, Inches(0.7), y, Inches(2.0), Inches(1.3), PRIMARY)
        text(s, name, Inches(0.7), y + Inches(0.42), Inches(2.0), Inches(0.5), 22, True, WHITE,
             PP_ALIGN.CENTER)
        text(s, tag, Inches(3.0), y + Inches(0.1), Inches(4.4), Inches(0.5), 19, True, DARK)
        text(s, detail, Inches(3.0), y + Inches(0.62), Inches(9.4), Inches(0.5), 15, False, GREY)
        rect(s, Inches(0.7), y + Inches(1.38), Inches(11.9), Inches(0.02), RGBColor(0xE6, 0xEC, 0xEA))
    notes(s, "三类角色，权限用角色加注解控制，学生看到的功能和组织者、管理员完全不同。")

    # 4 架构
    s = blank(prs)
    title(s, "系统架构：分层单体与前后端分离", "前端构建产物打包进 jar，同一套代码支持多种运行形态")
    image_fit(s, os.path.join(FIG_DIR, "fig-arch.png"), Inches(0.7), Inches(1.55),
              Inches(8.6), Inches(5.1))
    cards = [
        ("后端", "Spring Boot 2.7\nMyBatis-Plus\nJWT + 拦截器"),
        ("前端", "Vue 2.7\nElement UI\nECharts"),
        ("数据库", "MySQL 8（线上）\nH2（演示）\n同一份 SQL"),
    ]
    for i, (h, d) in enumerate(cards):
        y = Inches(1.65) + i * Inches(1.62)
        rect(s, Inches(9.6), y, Inches(3.05), Inches(1.42), RGBColor(0xF6, 0xF9, 0xF8),
             RGBColor(0xDD, 0xE6, 0xE3))
        text(s, h, Inches(9.85), y + Inches(0.12), Inches(2.6), Inches(0.4), 17, True, PRIMARY)
        text(s, d, Inches(9.85), y + Inches(0.55), Inches(2.6), Inches(0.85), 13, False, GREY)
    notes(s, "后端分控制、服务、持久三层，认证放在拦截器，业务只关心参数与状态流转。")

    # 5 数据库
    s = blank(prs)
    title(s, "数据库设计：以活动为中心的 8 张表", "报名与评价都对 (activity_id, user_id) 建唯一索引")
    image_fit(s, os.path.join(FIG_DIR, "fig-er.png"), Inches(0.55), Inches(1.5),
              Inches(9.2), Inches(5.2))
    bullets(s, ["用户、分类、活动三张主表", "报名、评价两张关联表",
                "消息、公告、参数三张支撑表", "冗余字段 signed_count 用于名额扣减与列表展示"],
            Inches(1.85), Inches(9.95), Inches(3.2), 15, 0.62)
    notes(s, "八张表，核心是活动、报名、评价。报名表的唯一索引同时解决重复报名问题。")

    # 6 难点一：并发报名
    s = blank(prs)
    title(s, "关键问题一：名额扣减的原子性", "将条件判断与自增写入合并为一条 SQL，由数据库保证原子性")
    code = ("update activity\n"
            "   set signed_count = signed_count + 1\n"
            " where id = #{activityId}\n"
            "   and signed_count < capacity\n"
            "   and status = 2")
    rect(s, Inches(0.7), Inches(1.75), Inches(7.2), Inches(1.72), RGBColor(0x1F, 0x3A, 0x34))
    text(s, code, Inches(1.0), Inches(1.92), Inches(6.6), Inches(1.45), 15, False,
         RGBColor(0xD6, 0xEC, 0xE2))
    text(s, "影响行数为 1 表示获得名额；与明细插入处于同一事务，失败即回滚",
         Inches(0.7), Inches(3.66), Inches(7.2), Inches(0.5), 15, False, GREY)
    bullets(s, ["唯一索引 (activity_id, user_id) 防止重复报名",
                "无需分布式锁，也未引入缓存中间件",
                "对照组将“先查询后更新”拆为两步，并加入延迟以放大竞态窗口",
                "该方案依赖单库行锁，分库分表后需额外协调"],
            Inches(1.85), Inches(8.2), Inches(4.3), 16, 0.8)
    notes(s, "条件与自增位于同一语句，原子性由数据库行锁保证。")

    # 7 实验
    s = blank(prs)
    title(s, "关键问题一验证：50 并发对照实验", "固定并发量与名额，仅切换实现方式")
    image_fit(s, cmp_img, Inches(0.5), Inches(1.6), Inches(8.0), Inches(4.9))
    cards = [
        ("本文实现", "成功 10 · 拒绝 40", "signed = joined = 10", "77 ms", PRIMARY),
        ("对照组", "成功 50 · 拒绝 0", "signed 6 / joined 50", "212 ms", ORANGE),
    ]
    for i, (name, main, detail, cost, color) in enumerate(cards):
        y = Inches(1.7) + i * Inches(2.35)
        rect(s, Inches(8.85), y, Inches(3.85), Inches(2.05), RGBColor(0xF6, 0xF9, 0xF8),
             RGBColor(0xDD, 0xE6, 0xE3))
        text(s, name, Inches(9.1), y + Inches(0.14), Inches(3.3), Inches(0.4), 16, True, color)
        text(s, main, Inches(9.1), y + Inches(0.58), Inches(3.3), Inches(0.45), 18, True, DARK)
        text(s, detail, Inches(9.1), y + Inches(1.1), Inches(3.3), Inches(0.4), 14, False, GREY)
        text(s, "耗时 " + cost, Inches(9.1), y + Inches(1.55), Inches(3.3), Inches(0.35), 13,
             False, GREY)
    notes(s, "对照组超卖 40 人，且冗余计数仅剩 6，说明先查询后更新会同时导致超卖与计数失真。")
    text(s, "结论：条件判断与自增位于同一语句时，单库场景即可避免超卖；对照组出现典型的丢失更新。",
         Inches(0.7), Inches(6.55), Inches(12.0), Inches(0.4), 15, False, GREY)

    # 8 难点二：签到
    s = blank(prs)
    title(s, "关键问题二：签到真实性与纠错机制", "6 位动态码、时间窗口与违约成本")
    steps = [("生成", "组织者点击生成\n6 位码，默认 30 分钟\n未过期则复用"),
             ("展示", "学生输入码签到\n窗口为开始前 30 分钟\n至活动结束"),
             ("判定", "活动结束时\n未签到者记爽约\n计数 +1"),
             ("约束", "累计 2 次\n限制报名 30 天\n补签可恢复")]
    for i, (h, d) in enumerate(steps):
        x = Inches(0.7) + i * Inches(3.1)
        rect(s, x, Inches(1.9), Inches(2.75), Inches(2.4), RGBColor(0xF6, 0xF9, 0xF8),
             RGBColor(0xDD, 0xE6, 0xE3))
        text(s, h, x + Inches(0.25), Inches(2.05), Inches(2.2), Inches(0.45), 20, True, PRIMARY)
        text(s, d, x + Inches(0.25), Inches(2.6), Inches(2.3), Inches(1.5), 14, False, GREY)
        if i < 3:
            text(s, "→", x + Inches(2.72), Inches(2.85), Inches(0.4), Inches(0.5), 22, True, PRIMARY)
    bullets(s, ["签到码归属于活动，组织者可随时重新获取",
                "补签会回退违约次数，低于阈值时自动解除限制",
                "阈值与封禁天数存放于系统参数，管理员可调整"],
            Inches(4.75), Inches(0.7), Inches(11.6), 16, 0.55)
    notes(s, "动态码与时间窗口解决代签问题，违约成本解决报名后缺席问题。")

    # 9 难点三：质量分
    s = blank(prs)
    title(s, "关键问题三：活动质量的可比性", "签到率 40% + 评分 40% + 完成率 20%，读取时计算，不落库")
    image_fit(s, q_img, Inches(0.6), Inches(1.7), Inches(7.4), Inches(3.9))
    rect(s, Inches(8.3), Inches(1.8), Inches(4.3), Inches(3.5), RGBColor(0xF6, 0xF9, 0xF8),
         RGBColor(0xDD, 0xE6, 0xE3))
    text(s, "三项加权的理由", Inches(8.55), Inches(2.0), Inches(3.8), Inches(0.45), 17, True, PRIMARY)
    text(s, "报名人数易被刷高；\n仅看评分则样本偏差较大；\n仅看签到率难以反映内容质量。\n"
            "三项加权后，参与规模较小但组织扎实的活动\n同样能够获得较高分数。",
         Inches(8.55), Inches(2.5), Inches(3.8), Inches(2.6), 14, False, GREY)
    text(s, "质量分 = 签到率 × 40 + (均分 − 1) / 4 × 40 + 完成率 × 20", Inches(0.7),
         Inches(5.55), Inches(11.9), Inches(0.5), 20, True, DARK)
    text(s, "示例：80 人报名、60 人签到、均分 4.2、无人取消 → 30 + 32 + 20 = 82 分",
         Inches(0.7), Inches(6.15), Inches(11.9), Inches(0.5), 15, False, GREY)
    notes(s, "质量分用于活动之间的横向比较，权重取默认经验值，详见论文 4.4 节。")

    # 10 界面：学生端
    s = blank(prs)
    title(s, "实现效果：学生端", "登录后展示公告、热门活动与最新发布内容")
    image_fit(s, os.path.join(FIG_DIR, "04-student-home.png"), Inches(0.55), Inches(1.55),
              Inches(6.0), Inches(5.2))
    image_fit(s, os.path.join(FIG_DIR, "05-student-activities.png"), Inches(6.75), Inches(1.55),
              Inches(6.0), Inches(5.2))
    notes(s, "学生端首页和活动中心，筛选、搜索、排序在前端完成参数拼装，后端分页。")

    # 11 界面：详情与签到
    s = blank(prs)
    title(s, "实现效果：报名与签到", "详情页一次返回报名、签到、评分与质量分信息")
    image_fit(s, os.path.join(FIG_DIR, "06-student-activity-detail.png"), Inches(0.55),
              Inches(1.55), Inches(6.0), Inches(5.2))
    image_fit(s, os.path.join(FIG_DIR, "21-student-checkin-success.png"), Inches(6.75),
              Inches(1.55), Inches(6.0), Inches(5.2))
    notes(s, "左侧是活动详情，右侧是签到成功后的状态，质量分随签到率实时变化。")

    # 12 界面：组织者
    s = blank(prs)
    title(s, "实现效果：组织者视角", "签到码、签到统计与报名名单集中于同一页面")
    image_fit(s, os.path.join(FIG_DIR, "12-organizer-checkin.png"), Inches(0.55),
              Inches(1.55), Inches(6.05), Inches(5.1))
    image_fit(s, os.path.join(FIG_DIR, "20-organizer-registrations.png"), Inches(6.75),
              Inches(1.55), Inches(6.05), Inches(5.1))
    notes(s, "左边是签到码和实时签到统计，右边是报名名单，可补签、可按状态和关键字筛选。")

    # 13 界面：管理员
    s = blank(prs)
    title(s, "实现效果：管理员视角", "审核、用户、分类、公告、平台统计、系统参数")
    image_fit(s, os.path.join(FIG_DIR, "15-admin-stats.png"), Inches(0.55), Inches(1.55),
              Inches(6.05), Inches(5.1))
    image_fit(s, os.path.join(FIG_DIR, "14-admin-audit.png"), Inches(6.75), Inches(1.55),
              Inches(6.05), Inches(5.1))
    notes(s, "平台统计是全局口径，活动审核按提交时间排序，驳回必须填理由。")

    # 14 测试
    s = blank(prs)
    title(s, "测试：接口级回归与并发实验", "六个脚本覆盖权限、边界、并发，共 98 条用例")
    kpi(s, [("98", "功能用例", PRIMARY), ("43", "管理员相关", PRIMARY),
            ("15", "接口契约", PRIMARY), ("6", "测试脚本", DARK)], Inches(1.75))
    bullets(s, ["权限：未登录 401、越权 403、伪造令牌拒绝",
                "边界：名额为 0、报名截止、重复报名、评价重复、非本人删除",
                "一致性：signed_count 与明细在每次操作后核对",
                "并发：50 并发抢 10 个名额，正式实现 10 成功 0 超卖"],
            Inches(3.6), Inches(0.7), Inches(11.6), 16, 0.62)
    notes(s, "测试不是走一遍页面，而是脚本化的接口断言，每次改完都能跑一遍。")

    # 15 部署
    s = blank(prs)
    title(s, "部署：一套代码，四种运行形态", "现场演示采用便携包，不依赖网络环境")
    forms = [("便携包", "jar 与内置 JRE\n解压后直接启动", "答辩现场"),
             ("本地 H2", "内存库自动建表并初始化数据\n重启即恢复", "开发调试"),
             ("本地 MySQL", "导入三段 SQL\n连接自有数据库", "课程验收"),
             ("Render + Vercel", "Docker 部署后端\n前端静态托管", "预留配置")]
    for i, (h, d, tag) in enumerate(forms):
        x = Inches(0.7) + i * Inches(3.05)
        rect(s, x, Inches(1.9), Inches(2.85), Inches(2.5), RGBColor(0xF6, 0xF9, 0xF8),
             RGBColor(0xDD, 0xE6, 0xE3))
        text(s, h, x + Inches(0.22), Inches(2.05), Inches(2.4), Inches(0.45), 18, True, PRIMARY)
        text(s, d, x + Inches(0.22), Inches(2.55), Inches(2.45), Inches(1.2), 13, False, GREY)
        text(s, tag, x + Inches(0.22), Inches(3.9), Inches(2.4), Inches(0.4), 13, True, ORANGE)
    bullets(s, ["Dockerfile 多阶段构建，镜像内含 JRE，内存限制 256MB",
                "数据库连接串与密钥全部走环境变量，仓库里不留明文"],
            Inches(4.9), Inches(0.7), Inches(11.6), 16, 0.6)
    notes(s, "现场以便携包演示；云端形态仅完成配置，未实际部署，详见论文 6.4 节。")

    # 15 不足与展望
    s = blank(prs)
    title(s, "不足与改进方向", "四点不足，都有明确的下一步")
    items = [("令牌无法主动失效", "JWT 无状态，退出登录仅在客户端清除；可加短有效期 + 刷新令牌"),
             ("统计实时计算", "数据量大后聚合变慢；可加物化表或定时预计算"),
             ("缺少操作审计", "后台操作无留痕；可加操作日志表与查询页"),
             ("签到仅码方式", "可扩展定位签到、二维码扫描与班级批量签到")]
    for i, (h, d) in enumerate(items):
        y = Inches(1.75) + i * Inches(1.22)
        rect(s, Inches(0.7), y + Inches(0.16), Inches(0.07), Inches(0.3), PRIMARY)
        text(s, h, Inches(1.0), y + Inches(0.1), Inches(4.0), Inches(0.45), 18, True, DARK)
        text(s, d, Inches(5.0), y + Inches(0.14), Inches(7.6), Inches(0.55), 15, False, GREY)
    notes(s, "不足与改进方向对应论文第 7 章，均有明确的下一步。")

    # 16 结束
    s = blank(prs)
    rect(s, 0, 0, prs.slide_width, prs.slide_height, RGBColor(0x1F, 0x3A, 0x34))
    rect(s, Inches(1.0), Inches(2.55), Inches(1.2), Inches(0.07), PRIMARY)
    text(s, "感谢各位老师", Inches(1.0), Inches(2.85), Inches(9), Inches(0.9), 40, True, WHITE)
    text(s, "请批评指正", Inches(1.0), Inches(3.75), Inches(9), Inches(0.6), 24, False,
         RGBColor(0xBF, 0xDA, 0xD1))
    text(s, "演示方式：便携包本地启动，访问 http://127.0.0.1:8080", Inches(1.0), Inches(4.75),
         Inches(11), Inches(0.4), 14, False, RGBColor(0x9A, 0xC8, 0xB8))
    notes(s, "汇报结束，请老师提问。")

    prs.save(OUT)
    return OUT


if __name__ == "__main__":
    print("ok", build())
