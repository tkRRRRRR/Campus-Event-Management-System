"""把 doc/06-答辩讲稿.md、07-答辩问答.md、08-演示脚本.md 合并生成 doc/答辩材料.docx"""
import os
import re

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOC = os.path.join(BASE, "doc")
SONG = "宋体"
HEI = "黑体"
TITLE = "基于 SpringBoot 的校园活动管理系统设计与实现"


def set_font(run, name, size, bold=False):
    run.font.name = name
    run.font.size = Pt(size)
    run.bold = bold
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:eastAsia"), name)


def setup(doc):
    st = doc.styles["Normal"]
    st.font.name = SONG
    st.font.size = Pt(12)
    st.element.rPr.rFonts.set(qn("w:eastAsia"), SONG)
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21.0), Cm(29.7)
    sec.top_margin = Cm(2.54)
    sec.bottom_margin = Cm(2.54)
    sec.left_margin = Cm(3.17)
    sec.right_margin = Cm(3.17)


def para(doc, text, size=12, bold=False, font=SONG, indent=True, space_after=0,
         align=WD_ALIGN_PARAGRAPH.JUSTIFY, line=1.5):
    p = doc.add_paragraph()
    p.alignment = align
    pf = p.paragraph_format
    pf.line_spacing = line
    pf.space_before = Pt(0)
    pf.space_after = Pt(space_after)
    if indent:
        pf.first_line_indent = Pt(24)
    r = p.add_run(text)
    set_font(r, font, size, bold)
    return p


def heading(doc, text, level):
    sizes = {1: 16, 2: 14, 3: 12}
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    pf = p.paragraph_format
    pf.space_before = Pt(12 if level == 1 else 8)
    pf.space_after = Pt(6)
    pf.line_spacing = 1.5
    r = p.add_run(text)
    set_font(r, HEI, sizes[level], True)
    return p


def add_table(doc, rows):
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, row in enumerate(rows):
        for j, cell in enumerate(row):
            c = t.cell(i, j)
            c.text = ""
            p = c.paragraphs[0]
            p.paragraph_format.line_spacing = 1.2
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            r = p.add_run(cell)
            set_font(r, SONG, 10.5, i == 0)


def parse_md(doc, path):
    lines = open(path, encoding="utf-8").read().splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].rstrip()
        if not line.strip():
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                raw = lines[i].strip().strip("|")
                cells = [c.strip() for c in raw.split("|")]
                if not re.fullmatch(r"[-: ]+", "".join(cells)):
                    rows.append(cells)
                i += 1
            if rows:
                add_table(doc, rows)
                doc.add_paragraph()
            continue
        if line.startswith("### "):
            heading(doc, line[4:].strip(), 3)
        elif line.startswith("## "):
            heading(doc, line[3:].strip(), 2)
        elif line.startswith("# "):
            heading(doc, line[2:].strip(), 1)
        elif line.startswith("> "):
            p = para(doc, line[2:].strip(), size=10.5, indent=False)
            p.paragraph_format.left_indent = Cm(0.5)
        elif line.startswith("- [ ] "):
            para(doc, "□ " + line[6:].strip(), size=12, indent=False)
        elif re.match(r"^\d+\.\s+", line.strip()):
            para(doc, line.strip(), size=12, indent=False)
        elif line.strip() == "---":
            pass
        else:
            text = re.sub(r"\*\*(.+?)\*\*", r"\1", line.strip())
            para(doc, text)
        i += 1


def main():
    doc = Document()
    setup(doc)

    for _ in range(4):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("本科毕业设计（论文）答辩材料")
    set_font(r, HEI, 22, True)
    doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("答辩讲稿 · 预判问答 · 现场演示脚本")
    set_font(r, HEI, 16, False)
    for _ in range(3):
        doc.add_paragraph()
    for text in (TITLE, "答辩人：____________    学号：____________", "指导教师：____________", "2026 年 6 月"):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(text)
        set_font(r, SONG, 14, False)
    doc.add_page_break()

    for idx, (name, path) in enumerate([
        ("一、答辩讲稿", os.path.join(DOC, "06-答辩讲稿.md")),
        ("二、预判问答", os.path.join(DOC, "07-答辩问答.md")),
        ("三、现场演示脚本", os.path.join(DOC, "08-演示脚本.md")),
    ]):
        if idx:
            doc.add_page_break()
        parse_md(doc, path)

    out = os.path.join(DOC, "答辩材料.docx")
    doc.save(out)
    print("saved:", out)


if __name__ == "__main__":
    main()
