import json, subprocess, sys

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
ENV = {"MYSQL_PWD": "123456", "SystemRoot": r"C:\Windows"}


def run(sql):
    r = subprocess.run([MYSQL, "-u", "root", "--default-character-set=utf8mb4", "-N", "-B", "-e", sql, "toefl_db"],
                       capture_output=True, text=True, encoding="utf-8", env=ENV)
    if r.returncode != 0:
        raise SystemExit("MYSQL_ERR " + r.stderr[:300])
    return r.stdout


dump = run("SHOW CREATE TABLE `" + "`; SHOW CREATE TABLE `".join(
    t for t in json.load(open("ddl.json", encoding="utf-8"))["sheets"][0]["data"] and
    [d[1] for d in json.load(open("ddl.json", encoding="utf-8"))["sheets"][0]["data"]]) + "`;")

fresh = {}
cur = None
for line in dump.split("\n"):
    if "CREATE TABLE" in line:
        body = line.split("\t", 1)[-1]
        cur = body.split("`")[1]
        fresh[cur] = [body]
    elif cur:
        fresh[cur].append(line)
    if cur and line.rstrip().endswith(";"):
        cur = None
for k in fresh:
    fresh[k] = "\n".join(fresh[k]).replace("\\n", "\n").strip()
    if not fresh[k].endswith(";"):
        fresh[k] += ";"

sheet = {}
d = json.load(open("ddl-read3.json", encoding="utf-8"))
import csv, io
rows = list(csv.reader(io.StringIO(d["csv"])))
for r in rows:
    if len(r) >= 6 and r[1].strip():
        sheet[r[1].strip()] = r[5].strip()

bad = []
for t, sql in fresh.items():
    s = sheet.get(t)
    if s is None:
        bad.append((t, "MISSING_IN_SHEET"))
    elif s.replace("\r\n", "\n") != sql:
        bad.append((t, "DIFF", len(s), len(sql)))
print("compared", len(fresh), "vs sheet", len(sheet))
print("bad:", bad if bad else "NONE")
