#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
GRE 题库扩充导入工具
====================

用途
----
把 batch2-questions.json 这类「题目数据文件」写进 MySQL 的 gre_section / gre_question 两张表，
用于扩充分块练习题库。扩充之后，套题页的「一键生成套题」就能抽到更多题。

为什么单独做一个导入脚本
------------------------
1. 题目是长文本（含引号、换行、中文解析），直接手写 SQL 极易被引号转义搞崩；
2. 反复导入同一批题时不能重复入库，脚本按「单元标题」做幂等判断，已存在就整段跳过；
3. 先出 SQL 再执行，出问题可以人工复核，不会直接污染数据库。

用法
----
    # 1) 只生成 SQL，不执行（默认，安全）
    python import_gre_questions.py batch2-questions.json

    # 2) 确认 SQL 无误后入库
    python import_gre_questions.py batch2-questions.json --apply

    # 3) 换数据库参数
    python import_gre_questions.py batch2-questions.json --apply ^
        --host 127.0.0.1 --port 3306 --user root --password 123456 --db toefl_db

数据文件格式（每道题）
----------------------
    {
      "task_type": "text_completion",       # 已有取值：text_completion / sentence_equivalence /
                                            # reading / quant_compare / quant_problem / awa_issue / awa_argument
      "stem": "题干",                        # 客观题用；写作题可留空
      "options": {"A": "...", "B": "..."},  # 客观题必填；写作题留空 {}
      "answer": "B",                        # 客观题必填，单字母（六选二沿用现有单字母口径）
      "analysis": "中英结合的解析",           # 必填
      "prompt": "写作题的任务描述",           # 写作题用
      "score_standard": "评分要点",           # 写作题用
      "sample_answer": "满分范例答案"         # 写作题用
    }

注意
----
- 本工具只写 gre_section / gre_question（分块练习题库），不会动 gre_suite / gre_suite_question；
  想把这些新题变成套题，去前端 GRE 套题页点「一键生成套题」，或调 POST /api/greSuite/generate。
- 已存在的单元（按 title 判定）会整段跳过，所以重复跑不会产生重复题。
"""

import argparse
import json
import os
import subprocess
import sys

MYSQL_DEFAULT = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

# 客观题允许的题型
OBJECTIVE_TYPES = {
    "text_completion",
    "sentence_equivalence",
    "reading",
    "quant_compare",
    "quant_problem",
}
# 写作题允许的题型（没有选项、没有标准答案，靠 AI 打分 + 范例答案）
WRITING_TYPES = {"awa_issue", "awa_argument"}

# gre_question 表里选项列的顺序
OPTION_KEYS = ["A", "B", "C", "D", "E", "F"]


def sql_str(value):
    """把 Python 字符串转成 SQL 单引号字面量：空值 -> NULL，其余做引号转义。"""
    if value is None:
        return "NULL"
    text = str(value)
    if text == "":
        return "''"
    return "'" + text.replace("'", "''") + "'"


def sql_int(value, default=0):
    try:
        return str(int(value))
    except (TypeError, ValueError):
        return str(default)


def validate_section(section, index):
    """校验一个单元的数据完整性，返回错误列表（空列表表示通过）。"""
    errors = []
    where = "第 %d 个单元(%s)" % (index + 1, section.get("title", "?"))
    if not section.get("title"):
        errors.append(where + "：缺 title")
    if section.get("module_type") not in ("verbal", "quant", "writing"):
        errors.append(where + "：module_type 必须是 verbal / quant / writing")

    questions = section.get("questions") or []
    if not questions:
        errors.append(where + "：没有题目")

    for qi, question in enumerate(questions):
        qwhere = "%s 第 %d 题" % (where, qi + 1)
        task_type = (question.get("task_type") or "").strip()
        if task_type in OBJECTIVE_TYPES:
            if not question.get("stem"):
                errors.append(qwhere + "：缺 stem")
            options = question.get("options") or {}
            if len(options) < 2:
                errors.append(qwhere + "：选项少于 2 个")
            extra = [key for key in options if key not in OPTION_KEYS]
            if extra:
                errors.append(qwhere + "：选项键只能是 A-F，出现 " + ",".join(extra))
            answer = (question.get("answer") or "").strip()
            if not answer:
                errors.append(qwhere + "：客观题缺 answer")
            elif answer not in options:
                errors.append(qwhere + "：answer=%s 不在选项里" % answer)
            if not question.get("analysis"):
                errors.append(qwhere + "：缺 analysis（用户要求每题必须带解析）")
        elif task_type in WRITING_TYPES:
            if not question.get("prompt"):
                errors.append(qwhere + "：写作题缺 prompt")
            if not question.get("sample_answer"):
                errors.append(qwhere + "：写作题缺 sample_answer（用户要求配满分范例）")
        else:
            errors.append(qwhere + "：未知 task_type=" + (task_type or "空"))
    return errors


def build_sql(sections, database):
    """把数据渲染成一段可直接喂给 mysql.exe 的 SQL 脚本。"""
    lines = []
    lines.append("-- GRE 题库扩充脚本（由 import_gre_questions.py 自动生成）")
    lines.append("-- 幂等策略：本脚本假定对应单元标题在库里尚不存在；重复导入前请先确认")
    lines.append("SET NAMES utf8mb4;")
    lines.append("")

    for section in sections:
        module_type = section["module_type"]
        title = section["title"]
        subtitle = section.get("subtitle")
        content_text = section.get("content_text")
        hint = section.get("hint")
        difficulty = section.get("difficulty", "中级")
        limit_second = section.get("limit_second", 0)
        sort_index = section.get("sort_index", 0)

        lines.append("-- ============================================================")
        lines.append("-- 单元：%s（%s）" % (title, module_type))
        lines.append("-- ============================================================")
        lines.append(
            "INSERT INTO gre_section (module_type, sort_index, title, subtitle, content_text, hint, difficulty, limit_second) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s);"
            % (
                sql_str(module_type),
                sql_int(sort_index),
                sql_str(title),
                sql_str(subtitle),
                sql_str(content_text),
                sql_str(hint),
                sql_str(difficulty),
                sql_int(limit_second),
            )
        )
        lines.append("SET @sid = LAST_INSERT_ID();")
        lines.append("")

        for qi, question in enumerate(section["questions"], start=1):
            options = question.get("options") or {}
            option_values = [sql_str(options.get(key)) for key in OPTION_KEYS]
            task_type = question["task_type"]
            is_writing = task_type in WRITING_TYPES

            # 写作题没有题干时，用 prompt 首句当列表里的短标题，方便套题页显示
            question_title = question.get("stem") or (
                (question.get("prompt") or "").strip().split("\n")[0][:80]
            )
            answer = None if is_writing else (question.get("answer") or "").strip()

            lines.append("-- 第 %d 题：%s" % (qi, (question_title or "")[:60].replace("\n", " ")))
            lines.append(
                "INSERT INTO gre_question (section_id, module_type, sort_index, question_title, "
                "option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, "
                "prompt, task_type, score_standard, sample_answer, difficulty) VALUES ("
                "@sid, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);"
                % (
                    sql_str(module_type),
                    sql_int(qi),
                    sql_str(question_title),
                    option_values[0],
                    option_values[1],
                    option_values[2],
                    option_values[3],
                    option_values[4],
                    option_values[5],
                    sql_str(answer),
                    sql_str(question.get("analysis")),
                    sql_str(question.get("prompt")),
                    sql_str(task_type),
                    sql_str(question.get("score_standard")),
                    sql_str(question.get("sample_answer")),
                    sql_str(question.get("difficulty", "中级")),
                )
            )
            lines.append("")

    lines.append("-- 导入完成")
    return "\n".join(lines)


def run_mysql(mysql_bin, args, sql_text):
    """通过 mysql.exe 执行 SQL（用 stdin 传入，避免命令行长度限制与转义问题）。"""
    command = [
        mysql_bin,
        "-h",
        args.host,
        "-P",
        str(args.port),
        "-u",
        args.user,
        "--default-character-set=utf8mb4",
        args.db,
    ]
    env = dict(os.environ)
    env["MYSQL_PWD"] = args.password
    result = subprocess.run(
        command,
        input=sql_text,
        encoding="utf-8",
        env=env,
        capture_output=True,
    )
    return result


def fetch_existing_titles(mysql_bin, args):
    """查出库里已有的单元标题，用于幂等跳过。

    用 -N -B（不输出列名、tab 分隔）拿最干净的纯文本，避免解析表格边框。
    """
    command = [
        mysql_bin,
        "-h",
        args.host,
        "-P",
        str(args.port),
        "-u",
        args.user,
        "--default-character-set=utf8mb4",
        "-N",
        "-B",
        args.db,
        "-e",
        "SELECT title FROM gre_section;",
    ]
    env = dict(os.environ)
    env["MYSQL_PWD"] = args.password
    result = subprocess.run(command, encoding="utf-8", env=env, capture_output=True)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "查询 gre_section 失败")
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def main():
    parser = argparse.ArgumentParser(description="把 GRE 扩充题库写入 gre_section / gre_question")
    parser.add_argument("data_file", help="题目数据文件（JSON）")
    parser.add_argument("--out", default="", help="生成的 SQL 输出路径，默认与数据文件同名 .sql")
    parser.add_argument("--apply", action="store_true", help="生成 SQL 后立即执行入库")
    parser.add_argument("--force", action="store_true", help="忽略「标题已存在」的跳过规则，强制插入（会产生重复单元，慎用）")
    parser.add_argument("--mysql", default=MYSQL_DEFAULT, help="mysql.exe 路径")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", default=3306, type=int)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--db", default="toefl_db")
    args = parser.parse_args()

    with open(args.data_file, "r", encoding="utf-8") as handle:
        data = json.load(handle)

    sections = data.get("sections") or []
    if not sections:
        print("数据文件里没有 sections，退出")
        return 1

    # 0) 幂等过滤：库里已经有的单元标题整段跳过，避免重复入库
    if args.apply and not args.force:
        try:
            existing = fetch_existing_titles(args.mysql, args)
        except RuntimeError as error:
            print("无法查询已有单元标题：" + str(error))
            return 1
        kept = []
        skipped = []
        for section in sections:
            if section.get("title") in existing:
                skipped.append(section)
            else:
                kept.append(section)
        if skipped:
            print("以下单元已存在，整段跳过（要强制覆盖请先删掉库里同标题的单元）：")
            for section in skipped:
                print("  - " + section["title"])
        sections = kept
        if not sections:
            print("没有需要导入的新单元，退出")
            return 0

    # 1) 校验
    errors = []
    for index, section in enumerate(sections):
        errors.extend(validate_section(section, index))
    if errors:
        print("数据校验未通过：")
        for error in errors:
            print("  - " + error)
        return 1

    total_questions = sum(len(section["questions"]) for section in sections)
    print("校验通过：%d 个单元、%d 道题" % (len(sections), total_questions))
    for section in sections:
        print("  - [%s] %s：%d 题" % (section["module_type"], section["title"], len(section["questions"])))

    # 2) 生成 SQL
    sql_text = build_sql(sections, args.db)
    out_path = args.out or (os.path.splitext(args.data_file)[0] + ".sql")
    with open(out_path, "w", encoding="utf-8") as handle:
        handle.write(sql_text)
    print("SQL 已生成：" + out_path)

    if not args.apply:
        print("未执行入库。复核后加 --apply 再跑一次。")
        return 0

    # 3) 执行
    result = run_mysql(args.mysql, args, sql_text)
    if result.returncode != 0:
        print("入库失败，mysql 返回 " + str(result.returncode))
        if result.stderr:
            print(result.stderr.strip())
        return 1
    print("入库完成。建议执行下面的核对 SQL：")
    print(
        "  SELECT s.id, s.module_type, s.title, COUNT(q.id) "
        "FROM gre_section s LEFT JOIN gre_question q ON q.section_id = s.id "
        "GROUP BY s.id ORDER BY s.module_type, s.sort_index;"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
