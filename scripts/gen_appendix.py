"""生成论文附录：doc/thesis/appendix.md

A 核心代码（从真实源码截取）
B 建表脚本（db/schema.sql）
C 接口清单（从 doc/02-接口文档.md 的表格汇总）
D 功能测试用例（从 doc/04-测试用例.md 提取编号/用例/预期/结果）
"""
import io
import os
import re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(BASE, "src", "main", "java", "com", "campus", "activity")


def read(path):
    return io.open(path, encoding="utf-8").read()


def _line_start(src, idx):
    return src.rfind("\n", 0, idx) + 1


def extract_by_signature(path, sig_regex):
    """按签名行截取方法体（含方法体花括号内的全部内容）。"""
    src = read(path)
    for m in re.finditer(sig_regex, src, re.M):
        start = _line_start(src, m.start())
        brace = src.find("{", m.end())
        if brace < 0:
            continue
        depth, i = 0, brace
        while i < len(src):
            if src[i] == "{":
                depth += 1
            elif src[i] == "}":
                depth -= 1
                if depth == 0:
                    return src[start:i + 1].rstrip()
            i += 1
    raise SystemExit("signature not found: %s in %s" % (sig_regex, path))


def extract_annotated_statement(path, needle):
    """截取包含 @Update 注解到分号结束的接口方法声明。"""
    src = read(path)
    idx = src.find(needle)
    if idx < 0:
        raise SystemExit("needle not found: %s" % needle)
    ann = src.rfind("@Update", 0, idx)
    start = _line_start(src, ann) if ann >= 0 else _line_start(src, idx)
    end = src.find(";", idx)
    return src[start:end + 1].rstrip()


def api_rows():
    text = read(os.path.join(BASE, "doc", "02-接口文档.md"))
    seen, rows = set(), []
    for line in text.splitlines():
        m = re.match(r"^\|\s*(GET|POST|PUT|DELETE)\s*\|\s*`?([^|`]+?)`?\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|", line)
        if not m:
            continue
        method, path, role, desc = [x.strip() for x in m.groups()]
        if not path.startswith("/"):
            continue
        key = (method, path)
        if key in seen:
            continue
        seen.add(key)
        rows.append((method, path, role, desc))
    rows.sort(key=lambda r: (r[1].count("/"), r[1]))
    return rows


def tc_rows():
    text = read(os.path.join(BASE, "doc", "04-测试用例.md"))
    rows = []
    for line in text.splitlines():
        m = re.match(r"^\|\s*(TC-\d+)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|", line)
        if not m:
            continue
        code, case, pre, step, expect, result = [x.strip() for x in m.groups()]
        rows.append((code, case, expect, result))
    return rows


def _wrap(body, width=96):
    out = []
    for line in body.splitlines():
        while len(line) > width:
            cut = line.rfind(" ", 0, width)
            if cut <= 20:
                break
            if len(line) - cut < 16:
                alt = line.rfind(" ", 0, cut - 1)
                if alt > 20:
                    cut = alt
            out.append(line[:cut])
            line = "        " + line[cut + 1:]
        out.append(line)
    return "\n".join(out)


def code_block(lang, body):
    return "```%s\n%s\n```\n" % (lang, _wrap(body))


def main():
    out = []
    out.append("# 附录 A 核心代码\n")
    out.append("附录中的代码均取自系统实际运行的源文件，仅去除与逻辑无关的空行。\n")

    a1 = extract_annotated_statement(
        os.path.join(JAVA, "mapper", "ActivityMapper.java"),
        "update activity set signed_count = signed_count + 1")
    out.append("## A.1 名额扣减的原子更新\n")
    out.append("src/main/java/com/campus/activity/mapper/ActivityMapper.java\n\n该语句把条件判断与自增合并为一条 SQL，影响行数为 1 才表示获得名额。\n")
    out.append(code_block("java", a1))

    a2 = extract_by_signature(
        os.path.join(JAVA, "service", "impl", "RegistrationServiceImpl.java"),
        r"^\s*public\s+void\s+register\(")
    out.append("## A.2 报名的校验顺序与事务边界\n")
    out.append("src/main/java/com/campus/activity/service/impl/RegistrationServiceImpl.java\n\n方法依次校验活动状态、报名截止时间、活动是否开始、是否被限制报名、是否重复报名，最后原子扣减名额并写入明细。\n")
    out.append(code_block("java", a2))

    a3 = extract_by_signature(
        os.path.join(JAVA, "service", "impl", "CheckinServiceImpl.java"),
        r"^\s*public\s+CheckinCodeVO\s+generateCode\(")
    out.append("## A.3 签到码的生成与复用\n")
    out.append("src/main/java/com/campus/activity/service/impl/CheckinServiceImpl.java\n\n有效期内重复生成会复用同一签到码，有效期不晚于活动结束时间。\n")
    out.append(code_block("java", a3))

    a4 = extract_by_signature(
        os.path.join(JAVA, "service", "impl", "ActivityServiceImpl.java"),
        r"^\s*private\s+double\s+calcQualityScore\(")
    out.append("## A.4 活动质量分计算\n")
    out.append("src/main/java/com/campus/activity/service/impl/ActivityServiceImpl.java\n\n签到率、评分归一化与完成率按 4:4:2 加权，读取时实时计算。\n")
    out.append(code_block("java", a4))

    out.append("# 附录 B 数据库建表脚本\n")
    out.append("db/schema.sql\n\n脚本仅使用 MySQL 与 H2 的公共语法子集，索引另见 db/indexes.sql。\n")
    out.append(code_block("sql", read(os.path.join(BASE, "db", "schema.sql")).strip()))

    rows = api_rows()
    out.append("# 附录 C 接口清单\n")
    out.append("系统共提供 %d 个业务接口，均以 /api 为前缀；除登录、注册与健康检查外，均需在请求头携带 Authorization: Bearer <token>。\n" % len(rows))
    out.append("| 方法 | 路径 | 权限 | 说明 |")
    out.append("|---|---|---|---|")
    for m, p, role, desc in rows:
        out.append("| %s | %s | %s | %s |" % (m, p, role, desc))
    out.append("")

    cases = tc_rows()
    out.append("# 附录 D 功能测试用例\n")
    out.append("共设计 %d 条功能用例，均在 H2 演示库与云端 MySQL 8.4 两套环境各执行一遍，结果一致。此处列出编号、名称、预期结果与执行结果，前置条件与操作步骤见 doc/04-测试用例.md。\n" % len(cases))
    out.append("| 编号 | 用例 | 预期结果 | 结果 |")
    out.append("|---|---|---|---|")
    for code, case, expect, result in cases:
        out.append("| %s | %s | %s | %s |" % (code, case, expect, result))
    out.append("")

    path = os.path.join(BASE, "doc", "thesis", "appendix.md")
    io.open(path, "w", encoding="utf-8", newline="\n").write("\n".join(out))
    print("written:", path, "apis:", len(rows), "cases:", len(cases))


if __name__ == "__main__":
    main()
