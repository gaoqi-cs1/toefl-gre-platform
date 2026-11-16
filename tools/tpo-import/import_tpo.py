#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
解析 template-reading.txt 这类手工整理的题库文本，生成 MySQL 入库 SQL（可选直接执行）。

用法：
    python import_tpo.py template-reading.txt --tpo-no TPO01 --title "TPO 01 模拟套题"
    python import_tpo.py template-reading.txt --tpo-no TPO01 --apply
    python import_tpo.py template-reading.txt --dry-run

目标表（沿用现有阅读结构，不改表）：
    tpo            : id, tpo_no, title
    tpo_section    : id, tpo_id, type='reading', content_text=文章
    tpo_question   : id, section_id, module_type='reading', type/question_type=题型,
                     question_title=题干, option_a..d, answer, analysis,
                     passage_title, passage_content
"""
import argparse
import datetime
import os
import re
import shutil
import subprocess
import sys

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
DB = "toefl_db"
DB_USER = "root"
DB_PASS = "123456"
USABLE_TYPES = {
    "VOCABULARY", "FACTUAL", "NEGATIVE FACTUAL", "INFERENCE",
    "RHETORICAL PURPOSE", "SENTENCE SIMPLIFICATION", "INSERT TEXT",
    "PROSE SUMMARY", "TABLE COMPLETION",
}


def sq(text):
    """转义为 MySQL 单引号字符串。"""
    if text is None:
        return "NULL"
    text = str(text).replace("\\", "\\\\").replace("'", "''")
    text = text.replace("\r\n", "\n")
    return "'" + text + "'"


def parse_file(path):
    raw = open(path, encoding="utf-8-sig").read()
    raw = raw.replace("\r\n", "\n")

    sets = []
    for block in re.split(r"\[SET\]", raw):
        if not block.strip():
            continue
        # 阅读用 [PASSAGE] 包文章；听力可以用 [TRANSCRIPT] 包原文，或干脆不写原文
        m = re.search(r"\[(PASSAGE|TRANSCRIPT)\]", block)
        if m:
            head = block[:m.start()]
            passage_body = block[m.end():].split("[/" + m.group(1) + "]", 1)[0].strip()
        else:
            head = block.split("[Q]", 1)[0]
            passage_body = ""
        if "[Q]" not in block:
            continue

        # 可选的中文译文块：挂在 section 上，提交答案后由前端按钮切换显示
        tm = re.search(r"\[TRANSLATION\](.*?)\[/TRANSLATION\]", block, re.S)
        translation = tm.group(1).strip() if tm else ""

        meta = {}
        for line in head.strip().split("\n"):
            if ":" in line:
                k, v = line.split(":", 1)
                meta[k.strip().lower()] = v.strip()

        questions = []
        for qb in re.findall(r"\[Q\](.*?)\[\/Q\]", block, re.S):
            qmeta, stem, opts, ans, ana = {}, "", {}, "", ""
            cur = None
            for line in qb.strip().split("\n"):
                s = line.strip()
                if not s:
                    if cur == "stem" and stem:
                        stem += "\n"
                    continue
                m = re.match(r"^(type|stem|answer|analysis)\s*:\s*(.*)$", s, re.I)
                if m:
                    k = m.group(1).lower()
                    qmeta[k] = m.group(2).strip()
                    cur = k
                    if k == "stem":
                        stem = m.group(2).strip()
                    elif k == "analysis":
                        ana = m.group(2).strip()
                    elif k == "answer":
                        ans = m.group(2).strip()
                    continue
                mo = re.match(r"^([A-Da-d])\s*[\)\.、]\s*(.*)$", s)
                if mo:
                    opts[mo.group(1).upper()] = mo.group(2).strip()
                    cur = "opt"
                    continue
                # 续行：题干/解析写了两行
                if cur == "stem":
                    stem += "\n" + s
                elif cur == "analysis":
                    ana += "\n" + s
                elif cur == "type" and s.upper() in USABLE_TYPES:
                    qmeta["type"] = s.upper()

            if stem and len(opts) >= 2:
                questions.append({
                    "type": (qmeta.get("type") or "FACTUAL").upper(),
                    "stem": stem.strip(),
                    "options": opts,
                    "answer": ans.upper()[:1],
                    "analysis": ana.strip(),
                })

        sets.append({
            "title": meta.get("title", "未命名文章"),
            "section_title": meta.get("sectiontitle", meta.get("title", "未命名文章")),
            "passage": passage_body,
            "translation": translation,
            "audio": meta.get("audio", "").strip(),
            "audio_url": None,
            "questions": questions,
        })
    return sets


def build_sql(sets, tpo_no, tpo_title):
    """生成 SQL。调用前应先按 skip-existing 过滤掉重复题，这里只负责拼语句。"""
    out = []
    out.append("SET NAMES utf8mb4;")
    out.append("-- 1) 确保 tpo 记录存在")
    out.append(
        "INSERT INTO tpo (tpo_no, title, create_time) "
        f"SELECT {sq(tpo_no)}, {sq(tpo_title)}, NOW() FROM DUAL "
        f"WHERE NOT EXISTS (SELECT 1 FROM tpo WHERE tpo_no = {sq(tpo_no)});"
    )
    out.append(f"SET @tpo_id = (SELECT id FROM tpo WHERE tpo_no = {sq(tpo_no)} LIMIT 1);")
    out.append("")

    n_q = 0
    for s in sets:
        module = "listening" if s.get("audio_url") else "reading"
        audio_url = s.get("audio_url")
        out.append(f"-- {'听力' if module == 'listening' else '阅读'}：{s['title']}")
        out.append(
            "INSERT INTO tpo_section (tpo_id, type, title, content_text, content_translation, audio_url, create_time) VALUES "
            f"(@tpo_id, '{module}', {sq(s['section_title'])}, {sq(s['passage'])}, {sq(s.get('translation', ''))}, {sq(audio_url)}, NOW());"
        )
        out.append("SET @section_id = LAST_INSERT_ID();")
        for q in s["questions"]:
            opts = q["options"]
            out.append(
                "INSERT INTO tpo_question (section_id, module_type, type, question_type, question_title, "
                "option_a, option_b, option_c, option_d, answer, analysis, create_time, "
                "passage_title, passage_content) VALUES "
                f"(@section_id, '{module}', {sq(q['type'])}, {sq(q['type'])}, {sq(q['stem'])}, "
                f"{sq(opts.get('A', ''))}, {sq(opts.get('B', ''))}, {sq(opts.get('C', ''))}, {sq(opts.get('D', ''))}, "
                f"{sq(q['answer'])}, {sq(q['analysis'])}, NOW(), "
                f"{sq(s['section_title'])}, {sq(s['passage'])});"
            )
            n_q += 1
        out.append("")
    return "\n".join(out), n_q


def fetch_existing():
    """读出库里已有的 (passage_title, question_title)，用于 --skip-existing。"""
    env = dict(os.environ, MYSQL_PWD=DB_PASS)
    p = subprocess.run(
        [MYSQL, "-u", DB_USER, "-N", "-B", "--default-character-set=utf8mb4", "-e",
         f"SELECT passage_title, question_title FROM {DB}.tpo_question "
         "WHERE passage_title IS NOT NULL AND question_title IS NOT NULL"],
        env=env, capture_output=True,
    )
    keys = set()
    for line in (p.stdout or b"").decode("utf-8", "replace").splitlines():
        parts = line.split("\t")
        if len(parts) == 2:
            keys.add((parts[0].strip(), parts[1].strip()))
    return keys


def copy_audio(sets, audio_root, audio_dir, subdir="tpo01"):
    """把模板里 audio: 指向的音频复制到 frontend/public/audio/<套号>/，并回填 /audio/<套号>/xxx 形式 URL。

    subdir 用套号小写（tpo01 / tpo02 …），避免所有 TPO 的音频平铺在一个目录里。
    """
    target_dir = os.path.join(audio_dir, subdir)
    os.makedirs(target_dir, exist_ok=True)
    done, missing = 0, []
    for s in sets:
        src = s.get("audio") or ""
        if not src:
            continue
        if not os.path.isabs(src):
            src = os.path.join(audio_root, src)
        if not os.path.exists(src):
            missing.append(src)
            continue
        name = os.path.basename(src)
        dst = os.path.join(target_dir, name)
        if not os.path.exists(dst) or os.path.getsize(dst) != os.path.getsize(src):
            shutil.copy2(src, dst)
        s["audio_url"] = "/audio/" + subdir + "/" + name
        done += 1
    print(f"音频处理：{done} 个已就位，{len(missing)} 个找不到源文件")
    for m in missing:
        print(f"    ✗ 缺少音频：{m}")
    return sets


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="+", help="模板 txt 文件，可多个")
    ap.add_argument("--tpo-no", default="TPO01")
    ap.add_argument("--title", default=None)
    ap.add_argument("--out", default=None, help="输出的 sql 路径")
    ap.add_argument("--apply", action="store_true", help="生成后立即用 mysql 执行")
    ap.add_argument("--dry-run", action="store_true", help="只统计，不落文件")
    ap.add_argument("--skip-existing", action="store_true", help="题干已存在则跳过")
    ap.add_argument("--audio-root", default=r"E:\飞马\FM-TOEFL", help="audio: 相对路径的基准目录")
    ap.add_argument("--audio-dir", default=r"E:\cs\toefl-platform\frontend\public\audio", help="音频复制的根目录，脚本会自动再加一层套号子目录")
    ap.add_argument("--audio-subdir", default="", help="音频子目录（默认由 --tpo-no 推导，如 TPO01 -> tpo01）")
    a = ap.parse_args()

    all_sets = []
    for f in a.files:
        if not os.path.exists(f):
            print(f"[跳过] 找不到文件：{f}")
            continue
        all_sets.extend(parse_file(f))

    # 有 audio: 的套先把音频复制到 frontend/public/audio，并回填 /audio/xxx
    if any(s.get("audio") for s in all_sets):
        subdir = (a.audio_subdir or a.tpo_no).strip().lower()
        copy_audio(all_sets, a.audio_root, a.audio_dir, subdir)

    total_q = sum(len(s["questions"]) for s in all_sets)

    # --skip-existing：先在本地剔除库里已有的题，避免重复入库、也避免留下空 section
    if a.skip_existing:
        existing = fetch_existing()
        kept = []
        skipped = 0
        for s in all_sets:
            remain = [q for q in s["questions"] if (s["section_title"].strip(), q["stem"].strip()) not in existing]
            skipped += len(s["questions"]) - len(remain)
            if remain:
                s = dict(s, questions=remain)
                kept.append(s)
        all_sets = kept
        if skipped:
            print(f"跳过已存在的题 {skipped} 道")
        total_q = sum(len(s["questions"]) for s in all_sets)

    total_q = sum(len(s["questions"]) for s in all_sets)
    print(f"解析到 {len(all_sets)} 篇文章、{total_q} 道题")
    for s in all_sets:
        print(f"  - {s['title']}：{len(s['questions'])} 题")
        if not s["questions"]:
            print("    ⚠ 这篇没有识别到题目，检查 [Q] 块格式")
        for q in s["questions"]:
            if len(q["options"]) < 4:
                print(f"    ⚠ 题目选项不足 4 个：{q['stem'][:40]}")
            if q["answer"] not in ("A", "B", "C", "D"):
                print(f"    ⚠ 答案未识别：{q['stem'][:40]}")

    if a.dry_run or not all_sets:
        return

    title = a.title or f"{a.tpo_no} 导入题库"
    sql, n_q = build_sql(all_sets, a.tpo_no, title)

    out_path = a.out or ("db-import-%s.sql" % datetime.datetime.now().strftime("%Y%m%d-%H%M%S"))
    with open(out_path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(sql + "\n")
    print(f"已生成 {out_path}（{n_q} 道题）")

    if a.apply:
        # 直接把 sql 内容喂给 mysql 的 stdin（比 cmd 重定向稳，不受引号影响）
        env = dict(os.environ, MYSQL_PWD=DB_PASS)
        with open(out_path, "rb") as fh:
            p = subprocess.run(
                [MYSQL, "-u", DB_USER, "--default-character-set=utf8mb4", DB],
                stdin=fh, env=env, capture_output=True,
            )
        print("mysql exit:", p.returncode)
        out = (p.stdout or b"").decode("utf-8", "replace").strip()
        err = (p.stderr or b"").decode("utf-8", "replace").strip()
        if out:
            print(out[:2000])
        if err:
            print("STDERR:", err[:2000])
        else:
            print("入库完成：可用下面的 SQL 抽查")
            print(f'  "{MYSQL}" -u root --default-character-set=utf8mb4 -e '
                  f'"SELECT COUNT(*) FROM {DB}.tpo_question;"')
    else:
        print("未执行入库。确认无误后加 --apply，或手动：")
        print(f'  "{MYSQL}" -u root --default-character-set=utf8mb4 {DB} < "{out_path}"')


if __name__ == "__main__":
    sys.exit(main())
