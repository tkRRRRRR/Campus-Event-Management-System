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


def extract_method(path, name):
    src = read(path)
    idx = src.find(name + "(")
    if idx < 0:
        raise SystemExit("method not found: %s in %s" % (name, path))
    start = src.rfind("\n", 0, idx)
    # 向上包含紧邻的注解与注释
    lines = src[:idx].split("\n")
    keep = []
    for line in reversed(lines):
        s = line.strip()
        if s.startswith("@") or s.startswith("*") or s.startswith("/*") or s.startswith("//"):
            keep.append(line)
        else:
            break
    head = "\n".join(reversed(keep))
    brace = src.find("{", idx)
    depth = 0
    i = brace
    while i < len(src):
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    body = src[(idx - len(head)) if head else idx:i + 1]
    # 去掉前置注解行里的缩进异常
    snippet = (head + "\n" + src[idx:i + 1]) if head else src[idx:i + 1]
    return snippet.strip("\n")


def api_rows():
    text = read(os.path.join(BASE, "doc", "02-接口文档.md"))
    seen, rows = set(), []
    for line in text.splitlines():
        m = re.match(r"^\|\s*(GET|POST|PUT|DELETE)\s*\|\s*`?([^|`]+?)`?\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|", line)
        if not m:
            continue
        method, path, role, desc = [x.strip() for x in m.groups()]
        if path.startswith("/api") is False and not path.startswith("/"):
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


def main():
    out = []
    out.append("# 附录 A 核心代码\n")
    out.append("附录中的代码均取自系统实际运行的源文件，未作简化或改写。\n")

    a = extract_method(os.path.join(JAVA, "mapper", "ActivityMapper.java"), "increaseSignedCount")
    out.append("## A.1 名额扣减的原子更新\n")
    out.append("文件：`src/main/java/com/campus/activity/mapper/ActivityMapper.java`。条件与自增位于同一条语句，影响行数为 1 才表示获得名额。\n")
    out.append("```java\n" + a + "\n```\n")

    b = extract_method(os.path.join(JAVA, "service", "impl", "RegistrationServiceImpl.java"), "register")
    out.append("## A.2 报名的校验顺序与事务边界\n")
    out.append("文件：`src/main/java/com/campus/activity/service/impl/RegistrationServiceImpl.java`。方法依次校验活动状态、报名截止、活动是否开始、是否被限制报名、是否重复报名，最后执行名额扣减并写入明细。\n")
    out.append("```java\n" + b + "\n```\n")

    c = extract_method(os.path.join(JAVA, "service", "impl", "CheckinServiceImpl.java"), "generateCode")
    out.append("## A.3 签到码的生成与复用\n")
    out.append("文件：`src/main/java/com/campus/activity/service/impl/CheckinServiceImpl.java`。有效期内重复生成会复用同一签到码，有效期不晚于活动结束时间。\n")
    out.append("```java\n" + c + "\n```\n")

    d = extract_method(os.path.join(JAVA, "service", "impl", "ActivityServiceImpl.java"), "calcQualityScore")
    out.append("## A.4 活动质量分计算\n")
    out.append("文件：`src/main/java/com/campus/activity/service/impl/ActivityServiceImpl.java`。签到率、评分归一化与完成率按 4:4:2 加权，读取时实时计算。\n")
    out.append("```java\n" + d + "\n```\n")

    out.append("# 附录 B 数据库建表脚本\n")
    out.append("文件：`db/schema.sql`。脚本仅使用 MySQL 与 H2 的公共语法子集，索引另见 `db/indexes.sql`。\n")
    out.append("```sql\n" + read(os.path.join(BASE, "db", "schema.sql")).strip() + "\n```\n")

    rows = api_rows()
    out.append("# 附录 C 接口清单\n")
    out.append("系统共提供 %d 个业务接口，均以 `/api` 为前缀；除登录、注册与健康检查外，均需在请求头携带 `Authorization: Bearer <token>`。\n" % len(rows))
    out.append("| 方法 | 路径 | 权限 | 说明 |")
    out.append("|---|---|---|---|")
    for m, p, role, desc in rows:
        out.append("| %s | `%s` | %s | %s |" % (m, p, role, desc))
    out.append("")

    cases = tc_rows()
    out.append("# 附录 D 功能测试用例\n")
    out.append("共设计 %d 条功能用例，均在 H2 演示库与云端 MySQL 8.4 两套环境各执行一遍，结果一致。此处列出用例编号、名称、预期结果与执行结果，前置条件与操作步骤见 `doc/04-测试用例.md`。\n" % len(cases))
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
