import json

SHEET = "有数据的表"

ROWS = [
    ("user", "账户", "账户", 2, 4, "—", "username(唯一) / password / create_time", "登录账号；密码当前为明文，生产环境需加盐哈希"),
    ("exam_record", "托福分块", "记录", 51, 15, "user_id", "模块得分 / 正确数 / 用时", "旧版分块模考记录（/api/examRecord）"),
    ("tpo", "托福分块", "题库", 1, 4, "—", "id / 套系名称", "TPO 套系主表，被 tpo_section.tpo_id 物理引用"),
    ("tpo_section", "托福分块", "题库", 13, 8, "tpo_id（★物理外键）", "type / title / audio_url / content_translation", "13 篇篇章；听力音频按 TPO 分目录"),
    ("tpo_question", "托福分块", "题库", 49, 21, "section_id（★物理外键）", "question_type / question_title / answer / analysis", "分块题目主表，含口语写作 prompt 与范文"),
    ("tpo_answer", "托福分块", "作答", 4, 9, "user_id / section_id / question_id", "user_answer / is_correct / audio_url", "单题作答记录，口语录音 URL 存在 audio_url"),
    ("tpo_wrong", "托福分块", "错题", 2, 5, "user_id / section_id / question_id", "create_time", "托福错题本（/wrongbook）主表"),
    ("tpo_wrong_question", "托福分块", "错题", 7, 6, "question_id", "question_title / 冗余题面", "与 tpo_wrong 并存的旧错题表，字段冗余"),
    ("suite_section", "托福旧套题", "套题", 32, 5, "suite_id", "title / content_text", "旧套题篇章表，路由 /tpo/suite 已下线"),
    ("tpo_suite", "托福旧套题", "套题", 8, 9, "—", "title / difficulty / total_minutes", "旧套题主表，页面已下线但数据保留"),
    ("tpo_exam_answer", "托福旧套题", "作答", 26, 7, "record_id", "user_answer / is_correct", "旧模考逐题作答，数据在但页面已下线"),
    ("full_suite", "真题套题", "套题", 3, 6, "—", "title / difficulty / total_minutes", "随机组卷生成；title 规则「随机套题 · MM-dd HH:mm」"),
    ("full_section", "真题套题", "套题", 12, 9, "suite_id", "module_type / sort_index / limit_second / audio_url", "从 tpo_section 整篇复制，改题库不影响已生成卷"),
    ("full_question", "真题套题", "题库", 41, 17, "section_id", "question_title / answer / analysis / prompt", "从 tpo_question 复制；口语写作素材拼进 prompt"),
    ("full_exam_record", "真题套题", "记录", 1, 19, "user_id / suite_id", "四项 band / total_band / legacy_score", "整卷成绩（/full/result），legacy_score 为 0–120 兼容分"),
    ("gre_section", "GRE 分块", "题库", 15, 9, "—", "module_type / limit_second / title", "15 个练习单元；限时 90/105 秒每题、写作 1800 秒封顶 3600"),
    ("gre_question", "GRE 分块", "题库", 56, 18, "section_id", "task_type / option_a…f / answer / analysis", "含六选二 6 个选项；写作题带 score_standard 与满分范文"),
    ("gre_answer", "GRE 分块", "作答", 7, 7, "user_id / question_id", "user_answer / is_correct / awa_score", "分块作答记录"),
    ("gre_suite", "GRE 套题", "套题", 7, 7, "—", "title / source / total_minutes", "7 套；source=seed 为初始卷，generated 为随机生成可删"),
    ("gre_suite_section", "GRE 套题", "套题", 55, 8, "suite_id", "module_type / sort_index / limit_second", "套题模块，计时同样按题量折算"),
    ("gre_suite_question", "GRE 套题", "题库", 179, 18, "section_id", "task_type / answer / analysis / sample_answer", "套题题目拷贝，字段与 gre_question 基本一致但不 JOIN"),
    ("user_answer", "通用遗留", "作答", 13, 9, "user_id / question_id", "user_answer / is_correct", "第一版通用作答表，未被新模块调用"),
    ("user_record", "通用遗留", "记录", 2, 7, "user_id", "得分 / 用时", "第一版通用练习记录"),
    ("user_wrong", "通用遗留", "错题", 3, 4, "user_id / question_id", "create_time", "第一版通用错题表"),
]

HEADERS = ["序号", "表名", "数据族", "用途", "行数", "字段数", "关键关联字段", "关键业务字段", "说明", "行数占比"]
FIRST_DATA, LAST_DATA = 3, 2 + len(ROWS)          # 3..26
TOTAL_ROW = LAST_DATA + 1                          # 27
SUM_HEAD = TOTAL_ROW + 2                           # 29
SUM_FIRST = SUM_HEAD + 1                           # 30
SUM_LAST = SUM_FIRST + 6                           # 36
SUM_TOTAL = SUM_LAST + 1                           # 37
NOTE_ROW = SUM_TOTAL + 2                           # 39

FAMILIES = ["账户", "托福分块", "托福旧套题", "真题套题", "GRE 分块", "GRE 套题", "通用遗留"]
EMPTY = ["question", "tpo_suite_section", "tpo_exam_record", "full_exam_answer",
         "gre_wrong", "gre_exam_record", "gre_exam_answer", "user_essay"]

# +table-put --sheets：从 A2 起写表头 + 24 行数据（行数占比留空，后面用公式填）
data = [[i + 1, r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], None] for i, r in enumerate(ROWS)]
payload = {"sheets": [{
    "name": SHEET,
    "start_cell": "A2",
    "header": True,
    "columns": HEADERS,
    "data": data,
    "dtypes": {"序号": "int64", "行数": "int64", "字段数": "int64", "行数占比": "float64"},
}]}
json.dump(payload, open("datalist.json", "w", encoding="utf-8"), ensure_ascii=False)

# +table-put --styles：表头底色、列宽、冻结
styles = {"styles": [{
    "name": SHEET,
    "cell_styles": [
        {"range": f"A2:J2", "background_color": "#E6F0EA", "font_color": "#14492F",
         "font_weight": "bold", "horizontal_alignment": "center", "vertical_alignment": "middle"},
    ],
    "col_sizes": [
        {"range": "A:A", "size": 52}, {"range": "B:B", "size": 180}, {"range": "C:C", "size": 104},
        {"range": "D:D", "size": 70}, {"range": "E:E", "size": 72}, {"range": "F:F", "size": 72},
        {"range": "G:G", "size": 210}, {"range": "H:H", "size": 265}, {"range": "I:I", "size": 345},
        {"range": "J:J", "size": 88},
    ],
    "row_sizes": [{"range": "1:1", "size": 40}, {"range": "2:2", "size": 34}],
    "freeze": {"rows": 2},
}]}
json.dump(styles, open("datalist-styles.json", "w", encoding="utf-8"), ensure_ascii=False)

# +cells-set --writes：标题、合计行、占比列、汇总块、空表说明
def cell(formula=None, value=None):
    if formula is not None:
        return {"formula": formula}
    if value is not None:
        return {"value": value}
    return {}

title = f"有数据的表 · {len(ROWS)} 张（{sum(r[3] for r in ROWS)} 行，2026-11-15 实测；已剔除 0 行的空表）"

tot_row = [
    cell(value="合计"),
    cell(formula=f"=COUNTA(B{FIRST_DATA}:B{LAST_DATA})"),
    cell(value=f"{len(ROWS)} 张有数据"),
    cell(value="表数 / 行数 / 字段数"),
    cell(formula=f"=SUM(E{FIRST_DATA}:E{LAST_DATA})"),
    cell(formula=f"=SUM(F{FIRST_DATA}:F{LAST_DATA})"),
    cell(), cell(), cell(),
    cell(formula=f"=SUM(J{FIRST_DATA}:J{LAST_DATA})"),
]

ratio_col = [[cell(formula=f"=IF($E${TOTAL_ROW}=0,\"\",E{r}/$E${TOTAL_ROW})")] for r in range(FIRST_DATA, LAST_DATA + 1)]

sum_head_row = [cell(value="数据族"), cell(value="表数量"), cell(value="行数合计"), cell(value="行数占比")]

fam_rows = []
for i, fam in enumerate(FAMILIES):
    r = SUM_FIRST + i
    fam_rows.append([
        cell(value=fam),
        cell(formula=f'=COUNTIF($C${FIRST_DATA}:$C${LAST_DATA},A{r})'),
        cell(formula=f'=SUMIF($C${FIRST_DATA}:$C${LAST_DATA},A{r},$E${FIRST_DATA}:$E${LAST_DATA})'),
        cell(formula=f"=IF($C${SUM_TOTAL}=0,\"\",C{r}/$C${SUM_TOTAL})"),
    ])

sum_total_row = [
    cell(value="合计"),
    cell(formula=f"=SUM(B{SUM_FIRST}:B{SUM_LAST})"),
    cell(formula=f"=SUM(C{SUM_FIRST}:C{SUM_LAST})"),
    cell(formula=f"=SUM(D{SUM_FIRST}:D{SUM_LAST})"),
]

note = ("未列入的 8 张空表（0 行）：" + "、".join(EMPTY) +
        "。它们只有表结构、没有任何记录，等有作答数据后自然回到本表。")

writes = [
    {"sheet_name": SHEET, "range": "A1", "cells": [[cell(value=title)]]},
    {"sheet_name": SHEET, "range": f"J{FIRST_DATA}:J{LAST_DATA}", "cells": ratio_col},
    {"sheet_name": SHEET, "range": f"A{TOTAL_ROW}:J{TOTAL_ROW}", "cells": [tot_row]},
    {"sheet_name": SHEET, "range": f"A{SUM_HEAD}:D{SUM_HEAD}", "cells": [sum_head_row]},
    {"sheet_name": SHEET, "range": f"A{SUM_FIRST}:D{SUM_LAST}", "cells": fam_rows},
    {"sheet_name": SHEET, "range": f"A{SUM_TOTAL}:D{SUM_TOTAL}", "cells": [sum_total_row]},
    {"sheet_name": SHEET, "range": f"A{NOTE_ROW}", "cells": [[cell(value=note)]]},
]
json.dump(writes, open("writes.json", "w", encoding="utf-8"), ensure_ascii=False)

# +styles-put：标题、合计行、汇总块、备注行的视觉
styles2 = {"styles": [{
    "name": SHEET,
    "cell_merges": [{"range": "A1:J1"}, {"range": f"A{NOTE_ROW}:J{NOTE_ROW}"}],
    "cell_styles": [
        {"range": "A1:J1", "background_color": "#1B5C3F", "font_color": "#FFFFFF",
         "font_weight": "bold", "horizontal_alignment": "left", "vertical_alignment": "middle"},
        {"range": f"J{FIRST_DATA}:J{LAST_DATA}", "number_format": "0.0%"},
        {"range": f"J{FIRST_DATA}:J{LAST_DATA}", "horizontal_alignment": "right"},
        {"range": f"A{TOTAL_ROW}:J{TOTAL_ROW}", "background_color": "#F0F6F2", "font_color": "#14492F",
         "font_weight": "bold", "number_format": "0.0%"},
        {"range": f"A{SUM_HEAD}:D{SUM_HEAD}", "background_color": "#E6F0EA", "font_color": "#14492F",
         "font_weight": "bold", "horizontal_alignment": "center"},
        {"range": f"D{SUM_FIRST}:D{SUM_TOTAL}", "number_format": "0.0%"},
        {"range": f"A{SUM_TOTAL}:D{SUM_TOTAL}", "background_color": "#F0F6F2", "font_color": "#14492F",
         "font_weight": "bold"},
        {"range": f"A{NOTE_ROW}:J{NOTE_ROW}", "font_color": "#5B6B62", "horizontal_alignment": "left",
         "vertical_alignment": "middle"},
    ],
    "row_sizes": [{f"range": f"{TOTAL_ROW}:{TOTAL_ROW}", "size": 30},
                  {"range": f"{NOTE_ROW}:{NOTE_ROW}", "size": 44}],
}]}
json.dump(styles2, open("styles2.json", "w", encoding="utf-8"), ensure_ascii=False)

print("rows", len(ROWS), "sum", sum(r[3] for r in ROWS),
      "rows:", FIRST_DATA, LAST_DATA, "total", TOTAL_ROW,
      "sumblk", SUM_HEAD, SUM_FIRST, SUM_LAST, SUM_TOTAL, "note", NOTE_ROW)
