# -*- coding: utf-8 -*-
"""Generate thesis diagrams (architecture, modules, ER) as PNG with PIL."""
import math
import os
from PIL import Image, ImageDraw, ImageFont

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "doc", "figures")
FONT = r"C:\Windows\Fonts\msyh.ttc"
FONT_B = r"C:\Windows\Fonts\msyhbd.ttc"

INK = (45, 55, 60)
LINE = (110, 130, 135)
PRIMARY = (47, 125, 107)
PRIMARY_L = (223, 240, 235)
GRAY_L = (246, 248, 248)
WHITE = (255, 255, 255)
ACCENT = (196, 138, 62)
ACCENT_L = (250, 240, 226)


def font(size, bold=False):
    return ImageFont.truetype(FONT_B if bold else FONT, size)


def fit(d, text, size, max_w, bold=False):
    """Return (font, size) shrunk until the text fits max_w."""
    s = size
    while s > 10:
        ft = font(s, bold)
        if d.textlength(text, font=ft) <= max_w:
            return ft, s
        s -= 1
    return font(10, bold), 10


def box(d, rect, title=None, lines=None, fill=WHITE, border=LINE, tsize=30, lsize=25,
        title_color=INK, radius=12, width=2):
    x1, y1, x2, y2 = rect
    d.rounded_rectangle([x1, y1, x2, y2], radius=radius, fill=fill, outline=border, width=width)
    inner_w = (x2 - x1) - 32
    gap = 10
    heights = []
    ftitle = flines = None
    if title:
        ftitle, s = fit(d, title, tsize, inner_w, bold=True)
        heights.append(s)
    fentries = []
    for ln in (lines or []):
        fnt, s = fit(d, ln, lsize, inner_w)
        fentries.append((fnt, s, ln))
        heights.append(s)
    total = sum(heights) + gap * (len(heights) - 1)
    cy = y1 + (y2 - y1 - total) / 2
    if title:
        w = d.textlength(title, font=ftitle)
        d.text((x1 + (x2 - x1 - w) / 2, cy), title, font=ftitle, fill=title_color)
        cy += heights[0] + gap
    for fnt, s, ln in fentries:
        w = d.textlength(ln, font=fnt)
        d.text((x1 + (x2 - x1 - w) / 2, cy), ln, font=fnt, fill=INK)
        cy += s + gap


def arrow(d, p1, p2, label=None, color=LINE, width=3):
    x1, y1 = p1
    x2, y2 = p2
    d.line([x1, y1, x2, y2], fill=color, width=width)
    ang = math.atan2(y2 - y1, x2 - x1)
    L = 16
    d.polygon([(x2, y2),
               (x2 - L * math.cos(ang - 0.4), y2 - L * math.sin(ang - 0.4)),
               (x2 - L * math.cos(ang + 0.4), y2 - L * math.sin(ang + 0.4))], fill=color)
    if label:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        tw = d.textlength(label, font=font(23))
        d.rectangle([mx - tw / 2 - 8, my - 20, mx + tw / 2 + 8, my + 20], fill=WHITE)
        d.text((mx - tw / 2, my - 15), label, font=font(23), fill=(80, 95, 100))


def arch():
    W, H = 1500, 1060
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)

    d.rounded_rectangle([40, 40, W - 40, 350], radius=16, fill=GRAY_L, outline=LINE, width=2)
    d.text((70, 58), "浏览器端", font=font(28, True), fill=PRIMARY)
    box(d, (80, 105, 430, 215), "学生端页面", ["活动浏览 / 报名", "签到 / 评价"], fill=PRIMARY_L, border=PRIMARY,
        tsize=27, lsize=22)
    box(d, (455, 105, 805, 215), "组织者页面", ["发布 / 名单", "签到 / 数据"], fill=PRIMARY_L, border=PRIMARY,
        tsize=27, lsize=22)
    box(d, (830, 105, 1180, 215), "管理员页面", ["审核 / 用户", "分类 / 参数"], fill=PRIMARY_L, border=PRIMARY,
        tsize=27, lsize=22)
    box(d, (80, 240, 1180, 325), "Vue 2.7 · Element UI · Vuex · vue-router(hash) · ECharts · axios",
        None, fill=WHITE, tsize=25)

    arrow(d, (650, 350), (650, 440), "HTTP / JSON    Authorization: Bearer JWT")

    d.rounded_rectangle([40, 440, W - 40, 820], radius=16, fill=GRAY_L, outline=LINE, width=2)
    d.text((70, 458), "Spring Boot 2.7 后端（单进程 · 端口 8080）", font=font(28, True), fill=PRIMARY)

    box(d, (80, 505, 380, 605), "JWT 拦截器", ["令牌校验 · 角色注解"], fill=WHITE, tsize=26, lsize=21)
    box(d, (80, 635, 380, 745), "全局异常 / 统一返回", ["{code, msg, data}"], fill=WHITE, tsize=24, lsize=21)
    box(d, (415, 505, 720, 605), "Controller 层", ["参数校验 · 结果包装"], fill=PRIMARY_L, border=PRIMARY,
        tsize=26, lsize=21)
    box(d, (755, 505, 1060, 605), "Service 层", ["业务校验 · 事务 · 状态流转"], fill=PRIMARY_L, border=PRIMARY,
        tsize=26, lsize=21)
    box(d, (1095, 505, 1420, 605), "Mapper 层", ["MyBatis-Plus · 自定义 SQL"], fill=PRIMARY_L, border=PRIMARY,
        tsize=26, lsize=21)
    box(d, (415, 635, 720, 745), "定时任务", ["每 5 分钟结束", "已过期的活动"], fill=WHITE, tsize=26, lsize=21)
    box(d, (755, 635, 1060, 745), "静态资源", ["前端构建产物 dist"], fill=WHITE, tsize=26, lsize=21)
    box(d, (1095, 635, 1420, 745), "文件上传", ["upload/** 映射"], fill=WHITE, tsize=26, lsize=21)

    arrow(d, (650, 820), (650, 895), "JDBC")

    d.rounded_rectangle([40, 895, W - 40, 1020], radius=16, fill=ACCENT_L, outline=ACCENT, width=2)
    d.text((70, 913), "数据层", font=font(28, True), fill=ACCENT)
    box(d, (210, 930, 700, 1005), "MySQL 8.0", ["生产 / 云端部署"], fill=WHITE, border=ACCENT, tsize=26, lsize=21)
    box(d, (810, 930, 1300, 1005), "H2 1.4.200 内存库", ["演示 / 便携包，启动重建"], fill=WHITE, border=ACCENT,
        tsize=26, lsize=21)

    img.save(os.path.join(OUT, "fig-arch.png"))
    print("arch ok")


def modules():
    W, H = 1400, 780
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)

    box(d, (450, 25, 950, 105), "校园活动管理系统", None, fill=PRIMARY, border=PRIMARY,
        title_color=WHITE, tsize=32)

    items = [
        ("用户与认证", ["注册 · 登录 · 资料", "密码 · 头像 · 用户管理"]),
        ("活动管理", ["发布 · 编辑 · 取消", "审核 · 驳回 · 重提"]),
        ("活动浏览", ["分类筛选 · 关键字", "排序 · 分页 · 详情"]),
        ("报名管理", ["名额扣减 · 重复校验", "取消 · 审核 · 名单"]),
        ("签到管理", ["签到码 · 补签", "爽约判定 · 签到率"]),
        ("评价管理", ["评分 · 文字评价", "匿名 · 删除 · 统计"]),
        ("数据统计", ["平台 / 组织者 / 分类", "趋势 · 排行 · 概览"]),
        ("通知与公告", ["审核结果 · 爽约提醒", "封禁通知 · 全站公告"]),
    ]
    cols, w, h = 4, 300, 200
    left, top = 55, 210
    gapx, gapy = 35, 55
    for i, (t, lines) in enumerate(items):
        r, c = divmod(i, cols)
        x = left + c * (w + gapx)
        y = top + r * (h + gapy)
        box(d, (x, y, x + w, y + h), t, lines, fill=PRIMARY_L, border=PRIMARY, tsize=27, lsize=22)
    # connectors
    d.line([700, 105, 700, 155], fill=LINE, width=2)
    d.line([left + w / 2, 155, left + cols * (w + gapx) - gapx - w / 2, 155], fill=LINE, width=2)
    for c in range(cols):
        x = left + c * (w + gapx) + w / 2
        d.line([x, 155, x, 210], fill=LINE, width=2)
    for c in range(cols):
        x = left + c * (w + gapx) + w / 2
        d.line([x, 410, x, 465], fill=LINE, width=2)

    img.save(os.path.join(OUT, "fig-modules.png"))
    print("modules ok")


def er():
    W, H = 1600, 1000
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)

    box(d, (60, 90, 380, 250), "活动分类", ["id · name · sort", "status · create_time"],
        fill=ACCENT_L, border=ACCENT, tsize=24, lsize=20)
    box(d, (560, 300, 1010, 560), "活动", ["id · title · category_id", "capacity · signed_count",
                                          "status · signup_deadline", "organizer_id"],
        fill=PRIMARY_L, border=PRIMARY, tsize=24, lsize=20)
    box(d, (60, 380, 400, 620), "用户", ["id · username", "role · status", "violation_count · ban_until"],
        fill=PRIMARY_L, border=PRIMARY, tsize=24, lsize=20)
    box(d, (1140, 190, 1540, 440), "报名", ["id · activity_id · user_id", "status · signup_time",
                                            "checkin_time · checkin_type"], fill=WHITE, tsize=24, lsize=20)
    box(d, (1140, 620, 1540, 850), "评价", ["id · activity_id · user_id", "score · content · anonymous"],
        fill=WHITE, tsize=24, lsize=20)
    box(d, (60, 700, 400, 930), "个人消息", ["id · user_id · type", "title · content · is_read"],
        fill=GRAY_L, tsize=24, lsize=20)
    box(d, (560, 700, 1010, 880), "平台公告", ["id · title · content", "publisher_id · status"],
        fill=GRAY_L, tsize=24, lsize=20)
    box(d, (1140, 40, 1540, 150), "系统参数", ["id · config_key · config_value"],
        fill=GRAY_L, tsize=24, lsize=20)

    arrow(d, (380, 175), (560, 380), "1 : N", color=PRIMARY)
    arrow(d, (400, 400), (560, 400), "1 : N", color=PRIMARY)
    # 用户 - 报名（从上方绕行）
    d.line([400, 470, 470, 470, 470, 230, 1140, 230], fill=PRIMARY, width=3)
    d.polygon([(1140, 230), (1124, 222), (1124, 238)], fill=PRIMARY)
    d.text((740, 204), "1 : N", font=font(22), fill=PRIMARY)
    arrow(d, (1010, 380), (1140, 320), "1 : N", color=PRIMARY)
    arrow(d, (1010, 530), (1140, 700), "1 : N", color=PRIMARY)
    # 用户 - 评价（从活动下方绕行）
    d.line([400, 600, 520, 600, 520, 660, 1100, 660, 1100, 700], fill=PRIMARY, width=3)
    d.polygon([(1100, 700), (1092, 684), (1108, 684)], fill=PRIMARY)
    d.text((790, 634), "1 : N", font=font(22), fill=PRIMARY)
    arrow(d, (230, 620), (230, 700), "1 : N", color=ACCENT)
    arrow(d, (400, 640), (560, 760), "1 : N", color=ACCENT)

    d.text((60, 30), "注：图中仅列出主要字段，完整字段见 db/schema.sql", font=font(22), fill=(110, 120, 125))

    img.save(os.path.join(OUT, "fig-er.png"))
    print("er ok")


if __name__ == "__main__":
    arch()
    modules()
    er()
