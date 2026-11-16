import json, subprocess

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
ENV = {"MYSQL_PWD": "123456", "SystemRoot": r"C:\Windows"}
SHEET = "建表语句（DDL）"

base = json.load(open("datalist.json", encoding="utf-8"))
rows = base["sheets"][0]["data"]          # [序号, 表名, 数据族, 用途, 行数, 字段数, ...]


def run(sql):
    r = subprocess.run([MYSQL, "-u", "root", "--default-character-set=utf8mb4", "-N", "-B", "-e", sql, "toefl_db"],
                       capture_output=True, text=True, encoding="utf-8", env=ENV)
    if r.returncode != 0:
        raise SystemExit("MYSQL_ERR " + r.stderr[:300])
    return r.stdout


def ddl(table):
    out = run(f"SHOW CREATE TABLE `{table}`;")
    body = out.split("\t", 1)[1] if "\t" in out else out
    sql = body.strip().replace("\\n", "\n")     # batch 模式把换行转义成 \n，还原
    if not sql.endswith(";"):
        sql += ";"
    return sql


data = []
for i, r in enumerate(rows):
    data.append([i + 1, r[1], r[2], int(r[4]), int(r[5]), ddl(r[1])])

FIRST, LAST = 3, 2 + len(data)              # 3..26
TOTAL = LAST + 1                            # 27
SUM_HEAD = TOTAL + 2                        # 29
SUM_FIRST = SUM_HEAD + 1                    # 30
SUM_LAST = SUM_FIRST + 6                    # 36
SUM_TOTAL = SUM_LAST + 1                    # 37
NOTE = SUM_TOTAL + 2                        # 39
FAMILIES = ["账户", "托福分块", "托福旧套题", "真题套题", "GRE 分块", "GRE 套题", "通用遗留"]
EMPTY = ["question", "tpo_suite_section", "tpo_exam_record", "full_exam_answer",
         "gre_wrong", "gre_exam_record", "gre_exam_answer", "user_essay"]

# 1) 表体：A2 起 表头 + 24 条 DDL
json.dump({"sheets": [{
    "name": SHEET,
    "start_cell": "A2",
    "header": True,
    "columns": ["序号", "表名", "数据族", "行数", "字段数", "建表语句（SHOW CREATE TABLE 原文）"],
    "data": data,
    "dtypes": {"序号": "int64", "行数": "int64", "字段数": "int64"},
}]}, open("ddl.json", "w", encoding="utf-8"), ensure_ascii=False)

# 2) 表体样式
json.dump({"styles": [{
    "name": SHEET,
    "cell_styles": [
        {"range": "A2:F2", "background_color": "#E6F0EA", "font_color": "#14492F",
         "font_weight": "bold", "horizontal_alignment": "center", "vertical_alignment": "middle"},
        {"range": f"F{FIRST}:F{LAST}", "font_family": "Consolas", "font_size": 10,
         "horizontal_alignment": "left", "vertical_alignment": "top", "word_wrap": "auto-wrap"},
        {"range": f"D{FIRST}:E{TOTAL}", "horizontal_alignment": "right"},
    ],
    "col_sizes": [{"range": "A:A", "size": 52}, {"range": "B:B", "size": 170},
                  {"range": "C:C", "size": 104}, {"range": "D:D", "size": 72},
                  {"range": "E:E", "size": 72}, {"range": "F:F", "size": 720}],
    "row_sizes": [{"range": "1:1", "size": 40}, {"range": "2:2", "size": 34}],
    "freeze": {"rows": 2},
}]}, open("ddl-styles.json", "w", encoding="utf-8"), ensure_ascii=False)

def c(formula=None, value=None):
    if formula is not None:
        return {"formula": formula}
    if value is not None:
        return {"value": value}
    return {}

title = (f"toefl_db 建表语句（DDL）· {len(data)} 张有数据的表 · "
         f"{sum(d[3] for d in data)} 行 / {sum(d[4] for d in data)} 字段 · 取自 SHOW CREATE TABLE 原文，2026-11-15")

writes = [
    {"sheet_name": SHEET, "range": "A1", "cells": [[c(value=title)]]},
    {"sheet_name": SHEET, "range": f"A{TOTAL}:F{TOTAL}", "cells": [[
        c(value="合计"), c(formula=f"=COUNTA(B{FIRST}:B{LAST})"), c(value="7 个数据族"),
        c(formula=f"=SUM(D{FIRST}:D{LAST})"), c(formula=f"=SUM(E{FIRST}:E{LAST})"),
        c(value="本表只列 COUNT(*)>0 的表；8 张 0 行表见下方备注"),
    ]]},
    {"sheet_name": SHEET, "range": f"A{SUM_HEAD}:D{SUM_HEAD}", "cells": [[
        c(value="数据族"), c(value="表数量"), c(value="行数合计"), c(value="字段数合计")]]},
    {"sheet_name": SHEET, "range": f"A{SUM_FIRST}:D{SUM_LAST}", "cells": [[
        c(value=fam),
        c(formula=f'=COUNTIF($C${FIRST}:$C${LAST},A{SUM_FIRST + i})'),
        c(formula=f'=SUMIF($C${FIRST}:$C${LAST},A{SUM_FIRST + i},$D${FIRST}:$D${LAST})'),
        c(formula=f'=SUMIF($C${FIRST}:$C${LAST},A{SUM_FIRST + i},$E${FIRST}:$E${LAST})'),
    ] for i, fam in enumerate(FAMILIES)]},
    {"sheet_name": SHEET, "range": f"A{SUM_TOTAL}:D{SUM_TOTAL}", "cells": [[
        c(value="合计"),
        c(formula=f"=SUM(B{SUM_FIRST}:B{SUM_LAST})"),
        c(formula=f"=SUM(C{SUM_FIRST}:C{SUM_LAST})"),
        c(formula=f"=SUM(D{SUM_FIRST}:D{SUM_LAST})")]]},
    {"sheet_name": SHEET, "range": f"A{NOTE}", "cells": [[c(value=
        "另有 8 张 0 行的空表只有结构、没有记录，未列入：" + "、".join(EMPTY) +
        "。需要时同样可 SHOW CREATE TABLE 取语句。")]]},
]
json.dump(writes, open("ddl-writes.json", "w", encoding="utf-8"), ensure_ascii=False)

json.dump({"styles": [{
    "name": SHEET,
    "cell_merges": [{"range": "A1:F1"}, {"range": f"A{NOTE}:F{NOTE}"}],
    "cell_styles": [
        {"range": "A1:F1", "background_color": "#1B5C3F", "font_color": "#FFFFFF",
         "font_weight": "bold", "horizontal_alignment": "left", "vertical_alignment": "middle"},
        {"range": f"A{TOTAL}:F{TOTAL}", "background_color": "#F0F6F2", "font_color": "#14492F",
         "font_weight": "bold"},
        {"range": f"A{SUM_HEAD}:D{SUM_HEAD}", "background_color": "#E6F0EA", "font_color": "#14492F",
         "font_weight": "bold", "horizontal_alignment": "center"},
        {"range": f"A{SUM_TOTAL}:D{SUM_TOTAL}", "background_color": "#F0F6F2", "font_color": "#14492F",
         "font_weight": "bold"},
        {"range": f"B{SUM_FIRST}:D{SUM_TOTAL}", "horizontal_alignment": "right"},
        {"range": f"A{NOTE}:F{NOTE}", "font_color": "#5B6B62", "horizontal_alignment": "left",
         "vertical_alignment": "middle"},
    ],
    "row_sizes": [{"range": f"{TOTAL}:{TOTAL}", "size": 30}, {"range": f"{NOTE}:{NOTE}", "size": 44}],
}]}, open("ddl-styles2.json", "w", encoding="utf-8"), ensure_ascii=False)

print("rows", len(data), "rows_sum", sum(d[3] for d in data), "cols_sum", sum(d[4] for d in data))
print("grid:", FIRST, LAST, TOTAL, SUM_HEAD, SUM_FIRST, SUM_LAST, SUM_TOTAL, NOTE)
print(data[0][5][:300])
