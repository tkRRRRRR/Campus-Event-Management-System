# -*- coding: utf-8 -*-
"""Assemble doc/thesis/part*.md into a formatted Word document."""
import os
import re

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
THESIS_DIR = os.path.join(BASE, "doc", "thesis")
OUT = os.path.join(BASE, "doc", "毕业论文初稿.docx")

SONG = "宋体"
HEI = "黑体"
TITLE = "基于 SpringBoot 的校园活动管理系统设计与实现"


def set_font(run, name=SONG, size=12, bold=False, color=None):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:ascii"), "Times New Roman" if name == SONG else name)
    rfonts.set(qn("w:hAnsi"), "Times New Roman" if name == SONG else name)
    rfonts.set(qn("w:eastAsia"), name)
    if color:
        run.font.color.rgb = color


def add_rich(par, text, name=SONG, size=12, bold=False):
    """Handle **bold** and `inline code` segments."""
    for seg in re.split(r"(\*\*[^*]+\*\*|`[^`]+`)", text):
        if not seg:
            continue
        if seg.startswith("**") and seg.endswith("**"):
            r = par.add_run(seg[2:-2])
            set_font(r, name, size, True)
        elif seg.startswith("`") and seg.endswith("`") and len(seg) > 1:
            r = par.add_run(seg[1:-1])
            set_font(r, "Consolas", size - 1.5, bold)
        else:
            r = par.add_run(seg)
            set_font(r, name, size, bold)


def page_break(doc):
    doc.add_page_break()


def add_field(par, instr, placeholder=""):
    r = par.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    r._r.append(fld_begin)
    r2 = par.add_run()
    instr_el = OxmlElement("w:instrText")
    instr_el.set(qn("xml:space"), "preserve")
    instr_el.text = instr
    r2._r.append(instr_el)
    r3 = par.add_run()
    sep = OxmlElement("w:fldChar")
    sep.set(qn("w:fldCharType"), "separate")
    r3._r.append(sep)
    r4 = par.add_run(placeholder)
    set_font(r4, SONG, 12)
    r5 = par.add_run()
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    r5._r.append(end)


def setup_styles(doc):
    normal = doc.styles["Normal"]
    normal.font.name = SONG
    normal.font.size = Pt(12)
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), SONG)
    normal.paragraph_format.line_spacing = 1.5
    normal.paragraph_format.space_after = Pt(0)

    for lvl, size in ((1, 16), (2, 14), (3, 12)):
        st = doc.styles["Heading %d" % lvl]
        st.font.name = HEI
        st.font.size = Pt(size)
        st.font.bold = True
        st.font.color.rgb = RGBColor(0, 0, 0)
        rpr = st.element.get_or_add_rPr()
        rfonts = rpr.find(qn("w:rFonts"))
        if rfonts is None:
            rfonts = OxmlElement("w:rFonts")
            rpr.append(rfonts)
        rfonts.set(qn("w:eastAsia"), HEI)
        rfonts.set(qn("w:ascii"), HEI)
        rfonts.set(qn("w:hAnsi"), HEI)
        pf = st.paragraph_format
        pf.space_before = Pt(18 if lvl == 1 else 12)
        pf.space_after = Pt(12 if lvl == 1 else 6)
        pf.line_spacing = 1.5
        pf.keep_with_next = True

    st = doc.styles.add_style("CodeBlock", 1)  # paragraph style
    st.font.name = "Consolas"
    st.font.size = Pt(9)
    st.paragraph_format.line_spacing = 1.0
    st.paragraph_format.left_indent = Cm(0.75)
    st.paragraph_format.space_before = Pt(6)
    st.paragraph_format.space_after = Pt(6)


def add_cover(doc):
    def blank(n=1):
        for _ in range(n):
            p = doc.add_paragraph()
            p.paragraph_format.line_spacing = 1.5

    blank(3)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("本科毕业设计（论文）")
    set_font(r, HEI, 22, True)

    blank(2)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("基于 SpringBoot 的校园活动管理系统设计与实现")
    set_font(r, HEI, 26, True)

    blank(1)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Design and Implementation of a Campus Activity\nManagement System Based on Spring Boot")
    set_font(r, "Times New Roman", 14)

    blank(4)
    for label in ("学    院", "专    业", "班    级", "学    号", "姓    名", "指导教师"):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.line_spacing = 1.75
        r = p.add_run("%s：______________________" % label)
        set_font(r, SONG, 14)

    blank(4)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("2026 年 6 月")
    set_font(r, SONG, 14)


def add_toc(doc):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("目    录")
    set_font(r, HEI, 18, True)
    p.paragraph_format.space_after = Pt(18)

    p = doc.add_paragraph()
    add_field(p, 'TOC \\o "1-3" \\h \\z \\u', "（在 Word 中右键此处 → 更新域，即可生成目录）")
    p.paragraph_format.line_spacing = 1.5

    note = doc.add_paragraph()
    r = note.add_run("提示：目录由 Word 域自动生成。若未显示页码，请按 Ctrl+A 全选后按 F9，或在目录上右键选择“更新域”。")
    set_font(r, SONG, 10.5)
    note.paragraph_format.space_before = Pt(18)


def add_header_footer(section):
    # 封面不显示页眉页脚
    section.different_first_page_header_footer = True

    header = section.header
    p = header.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(TITLE)
    set_font(r, SONG, 9)

    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("— ")
    set_font(r, SONG, 10.5)
    add_field(p, "PAGE", "1")
    r = p.add_run(" —")
    set_font(r, SONG, 10.5)


def parse_table(lines, start):
    rows = []
    i = start
    while i < len(lines) and lines[i].strip().startswith("|"):
        raw = lines[i].strip().strip("|")
        cells = [c.strip() for c in raw.split("|")]
        if not re.fullmatch(r"[-: ]+", "".join(cells)):
            rows.append(cells)
        i += 1
    return rows, i


def add_table(doc, rows):
    if not rows:
        return
    cols = max(len(r) for r in rows)
    table = doc.add_table(rows=len(rows), cols=cols)
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    # 关闭"根据内容自动调整"，否则 Word 会忽略列宽设置
    try:
        from docx.oxml import OxmlElement
        from docx.oxml.ns import qn as _qn
        _tblPr = table._tbl.tblPr
        _layout = OxmlElement("w:tblLayout")
        _layout.set(_qn("w:type"), "fixed")
        _tblPr.append(_layout)
    except Exception:
        pass
    for ri, row in enumerate(rows):
        for ci in range(cols):
            cell = table.cell(ri, ci)
            cell.text = ""
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if (ri == 0 or ci > 0) else WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.line_spacing = 1.0
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            text = row[ci] if ci < len(row) else ""
            add_rich(p, text, SONG, 10.5, bold=(ri == 0))
    # small spacer
    sp = doc.add_paragraph()
    sp.paragraph_format.space_after = Pt(6)
    sp.paragraph_format.line_spacing = 1.0

    # 列宽：按表头语义给定固定比例，避免自动测量在固定布局下失真
    try:
        header = [c.strip() for c in rows[0]]
        preset = None
        if any("路径" in h for h in header) and cols == 4:
            preset = [2.2, 5.0, 2.7, 4.7]
        elif any("预期" in h for h in header) and cols == 4:
            preset = [1.7, 5.3, 3.9, 3.7]
        elif cols == 5:
            preset = [1.7, 3.0, 2.4, 3.8, 3.7]
        elif cols == 3:
            preset = [3.6, 5.2, 5.8]
        elif cols == 2:
            preset = [4.0, 10.6]
        if preset:
            for ci, w in enumerate(preset[:cols]):
                table.columns[ci].width = Cm(w)
                for ri in range(len(rows)):
                    table.cell(ri, ci).width = Cm(w)
    except Exception as exc:
        print("table width warn:", exc)


def add_figure(doc, path, caption):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run()
    run.add_picture(path, width=Inches(5.6))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_after = Pt(10)
    cap.paragraph_format.line_spacing = 1.0
    r = cap.add_run(caption)
    set_font(r, SONG, 10.5)


def build():
    parts = []
    for name in ("part1.md", "part2.md", "part3.md"):
        with open(os.path.join(THESIS_DIR, name), encoding="utf-8") as fh:
            parts.append(fh.read())
    md = "\n".join(parts)
    appendix = os.path.join(THESIS_DIR, "appendix.md")
    if os.path.exists(appendix):
        with open(appendix, encoding="utf-8") as fh:
            ap = fh.read()
        # 附录排在参考文献之后、致谢之前
        marker = "\n# 致谢"
        if marker in md:
            md = md.replace(marker, "\n" + ap.strip() + "\n" + marker, 1)
        else:
            md = md + "\n" + ap
    lines = md.split("\n")

    doc = Document()
    setup_styles(doc)
    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(3.17)
    add_header_footer(section)

    add_cover(doc)
    page_break(doc)
    add_toc(doc)
    page_break(doc)

    i = 0
    first_after_toc = True
    in_code = False
    code_buf = []
    while i < len(lines):
        line = lines[i].rstrip()
        stripped = line.strip()

        if stripped.startswith("```"):
            if in_code:
                p = doc.add_paragraph(style="CodeBlock")
                r = p.add_run("\n".join(code_buf))
                set_font(r, "Consolas", 9)
                code_buf = []
                in_code = False
            else:
                in_code = True
            i += 1
            continue
        if in_code:
            code_buf.append(line)
            i += 1
            continue

        if not stripped:
            i += 1
            continue

        if stripped.startswith("!["):
            m = re.match(r"!\[(.*?)\]\((.*?)\)", stripped)
            if m:
                caption, rel = m.group(1), m.group(2)
                img_path = os.path.join(BASE, "doc", rel)
                if os.path.exists(img_path):
                    add_figure(doc, img_path, caption)
                else:
                    p = doc.add_paragraph()
                    r = p.add_run("[缺失图片: %s]" % rel)
                    set_font(r, SONG, 10.5)
            i += 1
            continue

        if stripped.startswith("|"):
            rows, i = parse_table(lines, i)
            add_table(doc, rows)
            continue

        m = re.match(r"^(#{1,3})\s+(.*)$", stripped)
        if m:
            level = len(m.group(1))
            text = m.group(2).strip()
            if text in ("摘要", "Abstract", "参考文献", "致谢") or text.startswith("第") and "章" in text:
                if not first_after_toc:
                    page_break(doc)
                first_after_toc = False
                p = doc.add_paragraph(style="Heading 1")
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r = p.add_run(text)
                set_font(r, HEI, 16, True)
            elif level == 1:
                p = doc.add_paragraph(style="Heading 1")
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r = p.add_run(text)
                set_font(r, HEI, 16, True)
            elif level == 2:
                p = doc.add_paragraph(style="Heading 2")
                r = p.add_run(text)
                set_font(r, HEI, 14, True)
            else:
                p = doc.add_paragraph(style="Heading 3")
                r = p.add_run(text)
                set_font(r, HEI, 12, True)
            i += 1
            continue

        # list item
        if stripped.startswith("- "):
            p = doc.add_paragraph()
            p.paragraph_format.left_indent = Cm(1.2)
            p.paragraph_format.line_spacing = 1.5
            add_rich(p, "· " + stripped[2:])
            i += 1
            continue

        # 参考文献条目：悬挂缩进
        if re.match(r"^\[\d+\]", stripped):
            p = doc.add_paragraph()
            p.paragraph_format.left_indent = Cm(0.74)
            p.paragraph_format.first_line_indent = Cm(-0.74)
            p.paragraph_format.line_spacing = 1.5
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            add_rich(p, stripped)
            i += 1
            continue

        # 纯代码路径行：小号等宽、左对齐、不缩进
        if re.match(r"^`[^`]+`$", stripped):
            p = doc.add_paragraph()
            p.paragraph_format.left_indent = Cm(0.75)
            p.paragraph_format.line_spacing = 1.5
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            r = p.add_run(stripped[1:-1])
            set_font(r, "Consolas", 9.5)
            i += 1
            continue

        # normal paragraph
        p = doc.add_paragraph()
        p.paragraph_format.first_line_indent = Pt(24)
        p.paragraph_format.line_spacing = 1.5
        # 含超长不可断片段（如长路径）时用左对齐，避免两端对齐把字距拉散
        longest = max([len(t) for t in stripped.split()] or [0])
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT if longest > 45 else WD_ALIGN_PARAGRAPH.JUSTIFY
        add_rich(p, stripped)
        i += 1

    doc.save(OUT)
    print("saved:", OUT)


if __name__ == "__main__":
    build()
