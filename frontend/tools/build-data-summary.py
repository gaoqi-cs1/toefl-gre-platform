import unicodedata
from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

# 表名, 数据族, 用途, 行数, 字段数, 关键关联字段, 关键业务字段, 说明
ROWS = [
    ("user", "账户", "账户", 2, 4, "—", "username(唯一) / password / create_time", "登录账号；密码当前为明文，生产环境需加盐哈希"),
    ("exam_record", "托福分块", "记录", 51, 15, "user_id", "模块得分 / 正确数 / 用时", "旧版分块模考记录（/api/examRecord），与 tpo_answer 并行"),
    ("question", "通用遗留", "题库", 0, 7, "section_id", "question_title / answer", "空表；实际题目都在 tpo_question，此表为历史结构"),
    ("suite_section", "托福旧套题", "套题", 32, 5, "suite_id", "title / content_text", "旧套题篇章表，路由 /tpo/suite 已下线"),
    ("tpo", "托福分块", "题库", 1, 4, "—", "id / 套系名称", "TPO 套系主表，被 tpo_section.tpo_id 物理引用"),
    ("tpo_section", "托福分块", "题库", 13, 8, "tpo_id（★物理外键）", "type / title / audio_url / content_translation", "13 篇篇章；听力音频按 TPO 分目录，译文只部分篇有"),
    ("tpo_question", "托福分块", "题库", 49, 21, "section_id（★物理外键）", "question_type / question_title / answer / analysis", "分块题目主表，含口语写作 prompt 与范文"),
    ("tpo_answer", "托福分块", "作答", 4, 9, "user_id / section_id / question_id", "user_answer / is_correct / audio_url", "单题作答记录，口语录音 URL 存在 audio_url"),
    ("tpo_wrong", "托福分块", "错题", 2, 5, "user_id / section_id / question_id", "create_time", "托福错题本（/wrongbook）主表"),
    ("tpo_wrong_question", "托福分块", "错题", 7, 6, "question_id", "question_title / 冗余题面", "与 tpo_wrong 并存的旧错题表，字段冗余"),
    ("tpo_suite", "托福旧套题", "套题", 8, 9, "—", "title / difficulty / total_minutes", "旧套题主表，页面已下线但数据保留"),
    ("tpo_suite_section", "托福旧套题", "套题", 0, 5, "suite_id", "title / sort_index", "空表，旧套题篇章映射"),
    ("tpo_exam_record", "托福旧套题", "记录", 0, 13, "user_id", "成绩字段 / 用时", "旧套题模考记录，未使用"),
    ("tpo_exam_answer", "托福旧套题", "作答", 26, 7, "record_id", "user_answer / is_correct", "旧模考逐题作答，数据在但页面已下线"),
    ("user_answer", "通用遗留", "作答", 13, 9, "user_id / question_id", "user_answer / is_correct", "第一版通用作答表，未被新模块调用"),
    ("user_record", "通用遗留", "记录", 2, 7, "user_id", "得分 / 用时", "第一版通用练习记录"),
    ("user_essay", "通用遗留", "作文", 0, 5, "user_id", "content / score", "旧作文表，空表"),
    ("user_wrong", "通用遗留", "错题", 3, 4, "user_id / question_id", "create_time", "第一版通用错题表"),
    ("full_suite", "真题套题", "套题", 3, 6, "—", "title / difficulty / total_minutes", "随机组卷生成；title 规则「随机套题 · MM-dd HH:mm」"),
    ("full_section", "真题套题", "套题", 12, 9, "suite_id", "module_type / sort_index / limit_second / audio_url", "从 tpo_section 整篇复制，改题库不影响已生成卷"),
    ("full_question", "真题套题", "题库", 41, 17, "section_id", "question_title / answer / analysis / prompt", "从 tpo_question 复制；口语写作素材拼进 prompt"),
    ("full_exam_record", "真题套题", "记录", 1, 19, "user_id / suite_id", "四项 band / total_band / legacy_score", "整卷成绩（/full/result），legacy_score 为 0–120 兼容分"),
    ("full_exam_answer", "真题套题", "作答", 0, 11, "record_id", "user_answer / correct_answer / band / ai_feedback", "逐题作答与评分反馈；尚未产生模考数据"),
    ("gre_section", "GRE 分块", "题库", 15, 9, "—", "module_type / limit_second / title", "15 个练习单元；限时 90/105 秒每题、写作 1800 秒封顶 3600"),
    ("gre_question", "GRE 分块", "题库", 56, 18, "section_id", "task_type / option_a…f / answer / analysis", "含六选二 6 个选项；写作题带 score_standard 与满分范文"),
    ("gre_answer", "GRE 分块", "作答", 7, 7, "user_id / question_id", "user_answer / is_correct / awa_score", "分块作答记录"),
    ("gre_wrong", "GRE 分块", "错题", 0, 22, "user_id / question_id", "question_scope / mastered / wrong_count", "独立错题本（/gre/wrong），不并入托福错题"),
    ("gre_suite", "GRE 套题", "套题", 7, 7, "—", "title / source / total_minutes", "7 套；source=seed 为初始卷，generated 为随机生成可删"),
    ("gre_suite_section", "GRE 套题", "套题", 55, 8, "suite_id", "module_type / sort_index / limit_second", "套题模块，计时同样按题量折算"),
    ("gre_suite_question", "GRE 套题", "题库", 179, 18, "section_id", "task_type / answer / analysis / sample_answer", "套题题目拷贝，字段与 gre_question 基本一致但不 JOIN"),
    ("gre_exam_record", "GRE 套题", "记录", 0, 17, "user_id / suite_id", "verbal_scaled / quant_scaled / awa_score / total_score", "整卷成绩，尚未产生模考"),
    ("gre_exam_answer", "GRE 套题", "作答", 0, 10, "record_id", "user_answer / is_correct / awa_score / ai_feedback", "逐题作答与评分反馈"),
]

HEADERS = ["序号", "表名", "数据族", "用途", "行数", "字段数", "关键关联字段", "关键业务字段", "说明"]

FAMILY_ORDER = ["账户", "托福分块", "托福旧套题", "真题套题", "GRE 分块", "GRE 套题", "通用遗留"]

wb = Workbook()
ws = wb.active
ws.title = "数据总表"

TITLE_FILL = PatternFill("solid", fgColor="1B5C3F")
HEAD_FILL = PatternFill("solid", fgColor="E6F0EA")
TOTAL_FILL = PatternFill("solid", fgColor="F0F6F2")
THIN = Side(style="thin", color="B8C7D9")

ws.merge_cells("A1:I1")
ws["A1"] = "toefl_db 数据总表 · 32 张表（行数与字段数为实际查询结果，2026-11-15）"
ws["A1"].font = Font(name="微软雅黑", size=13, bold=True, color="FFFFFF")
ws["A1"].fill = TITLE_FILL
ws["A1"].alignment = Alignment(horizontal="left", vertical="center", indent=1)
ws.row_dimensions[1].height = 34

for col, name in enumerate(HEADERS, start=1):
    c = ws.cell(row=2, column=col, value=name)
    c.font = Font(name="微软雅黑", size=11, bold=True, color="14492F")
    c.fill = HEAD_FILL
    c.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
    c.border = Border(bottom=THIN)
ws.row_dimensions[2].height = 26

ordered = sorted(ROWS, key=lambda r: (FAMILY_ORDER.index(r[1]), r[0]))
row = 3
for idx, rec in enumerate(ordered, start=1):
    ws.cell(row=row, column=1, value=idx).alignment = Alignment(horizontal="center", vertical="center")
    ws.cell(row=row, column=2, value=rec[0]).alignment = Alignment(horizontal="left", vertical="center")
    ws.cell(row=row, column=3, value=rec[1]).alignment = Alignment(horizontal="center", vertical="center")
    ws.cell(row=row, column=4, value=rec[2]).alignment = Alignment(horizontal="center", vertical="center")
    ws.cell(row=row, column=5, value=rec[3]).alignment = Alignment(horizontal="right", vertical="center")
    ws.cell(row=row, column=6, value=rec[4]).alignment = Alignment(horizontal="right", vertical="center")
    ws.cell(row=row, column=7, value=rec[5]).alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
    ws.cell(row=row, column=8, value=rec[6]).alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
    ws.cell(row=row, column=9, value=rec[7]).alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
    for col in range(1, 10):
        ws.cell(row=row, column=col).font = Font(name="微软雅黑", size=10)
    row += 1

first_data, last_data = 3, row - 1
total_row = row
ws.cell(row=total_row, column=1, value="合计")
ws.cell(row=total_row, column=2, value=f"=COUNTA(B{first_data}:B{last_data})")
ws.cell(row=total_row, column=5, value=f"=SUM(E{first_data}:E{last_data})")
ws.cell(row=total_row, column=6, value=f"=SUM(F{first_data}:F{last_data})")
for col in range(1, 10):
    c = ws.cell(row=total_row, column=col)
    c.font = Font(name="微软雅黑", size=11, bold=True, color="14492F")
    c.fill = TOTAL_FILL
    c.border = Border(top=THIN)
    if col in (1, 2):
        c.alignment = Alignment(horizontal="center", vertical="center")
    elif col in (5, 6):
        c.alignment = Alignment(horizontal="right", vertical="center")
ws.cell(row=total_row, column=3, value="32 张表").alignment = Alignment(horizontal="center", vertical="center")
ws.cell(row=total_row, column=4, value="表数 / 行数 / 字段数").font = Font(name="微软雅黑", size=10, color="5B6B62")

def display_width(value):
    return sum(2 if unicodedata.east_asian_width(ch) in ("F", "W") else 1 for ch in str(value or ""))

FIXED_W = {"A": 6, "B": 24, "C": 13, "D": 8, "E": 9, "F": 9, "G": 28, "H": 36, "I": 48}
for col_letter, width in FIXED_W.items():
    ws.column_dimensions[col_letter].width = width

ws.freeze_panes = "A3"
ws.sheet_view.showGridLines = False

# ---------------- 汇总 ----------------
ws2 = wb.create_sheet("汇总")
ws2.merge_cells("A1:D1")
ws2["A1"] = "按数据族汇总"
ws2["A1"].font = Font(name="微软雅黑", size=13, bold=True, color="FFFFFF")
ws2["A1"].fill = TITLE_FILL
ws2["A1"].alignment = Alignment(horizontal="left", vertical="center", indent=1)
ws2.row_dimensions[1].height = 30

head2 = ["数据族", "表数量", "行数合计", "口径说明"]
for col, name in enumerate(head2, start=1):
    c = ws2.cell(row=2, column=col, value=name)
    c.font = Font(name="微软雅黑", size=11, bold=True, color="14492F")
    c.fill = HEAD_FILL
    c.alignment = Alignment(horizontal="center", vertical="center")
    c.border = Border(bottom=THIN)

notes = {
    "账户": "登录账号，全站唯一 user 表",
    "托福分块": "tpo 链接 + 篇章 + 题目 + 作答 + 错题，含 2 处物理外键",
    "托福旧套题": "旧套题体系，路由已下线、数据与页面文件均保留",
    "真题套题": "tpo_* 整篇复制而来，随机组卷，与题库解耦",
    "GRE 分块": "独立于托福的分块练习与错题表",
    "GRE 套题": "独立于 GRE 分块的套题链路，一键生成可删",
    "通用遗留": "第一版通用表与空表，现无页面引用",
}
r2 = 3
for fam in FAMILY_ORDER:
    ws2.cell(row=r2, column=1, value=fam).alignment = Alignment(horizontal="left", vertical="center")
    ws2.cell(row=r2, column=2, value=f'=COUNTIF(数据总表!$C${first_data}:$C${last_data},A{r2})').alignment = Alignment(horizontal="right", vertical="center")
    ws2.cell(row=r2, column=3, value=f'=SUMIF(数据总表!$C${first_data}:$C${last_data},A{r2},数据总表!$E${first_data}:$E${last_data})').alignment = Alignment(horizontal="right", vertical="center")
    ws2.cell(row=r2, column=4, value=notes[fam]).alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
    for col in range(1, 5):
        ws2.cell(row=r2, column=col).font = Font(name="微软雅黑", size=10)
    r2 += 1

ws2.cell(row=r2, column=1, value="合计")
ws2.cell(row=r2, column=2, value=f"=SUM(B3:B{r2 - 1})")
ws2.cell(row=r2, column=3, value=f"=SUM(C3:C{r2 - 1})")
for col in range(1, 5):
    c = ws2.cell(row=r2, column=col)
    c.font = Font(name="微软雅黑", size=11, bold=True, color="14492F")
    c.fill = TOTAL_FILL
    c.border = Border(top=THIN)
    c.alignment = Alignment(horizontal="right" if col in (2, 3) else "left", vertical="center")

r2 += 2
ws2.cell(row=r2, column=1, value="关键结论")
ws2.cell(row=r2, column=1).font = Font(name="微软雅黑", size=12, bold=True, color="14492F")
r2 += 1
facts = [
    ("表数量与字段量", f'=COUNTA(数据总表!$B${first_data}:$B${last_data})&" 张表 / "&SUM(数据总表!$F${first_data}:$F${last_data})&" 个字段"'),
    ("数据行数最大的表", '=INDEX(数据总表!$B$3:$B$34,MATCH(MAX(数据总表!$E$3:$E$34),数据总表!$E$3:$E$34,0))&"（"&MAX(数据总表!$E$3:$E$34)&" 行）"'),
    ("空表数量", '=COUNTIF(数据总表!$E$3:$E$34,0)&" 张空表：尚未产生作答/记录数据"'),
    ("物理外键", "仅 2 处：tpo_section.tpo_id → tpo，tpo_question.section_id → tpo_section；其余均为逻辑外键"),
    ("数据隔离", "tpo_* / full_* / gre_* / user_* 四组互不调用，套题子表为整篇复制而非引用"),
]
for label, formula in facts:
    ws2.cell(row=r2, column=1, value=label).font = Font(name="微软雅黑", size=10, bold=True)
    ws2.cell(row=r2, column=1).alignment = Alignment(horizontal="left", vertical="center")
    c = ws2.cell(row=r2, column=2, value=formula)
    c.font = Font(name="微软雅黑", size=10)
    c.alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
    ws2.merge_cells(start_row=r2, start_column=2, end_row=r2, end_column=4)
    ws2.row_dimensions[r2].height = 30
    r2 += 1

ws2.column_dimensions["A"].width = 22
ws2.column_dimensions["B"].width = 26
ws2.column_dimensions["C"].width = 14
ws2.column_dimensions["D"].width = 46
ws2.sheet_view.showGridLines = False

wb.save(r"E:\cs\toefl-platform\数据总表.xlsx")
print("saved", len(ordered), "rows; data rows", first_data, last_data, "total row", total_row)
