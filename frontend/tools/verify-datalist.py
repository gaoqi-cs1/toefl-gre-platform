import json, subprocess

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
payload = json.load(open("datalist.json", encoding="utf-8"))
rows = payload["sheets"][0]["data"]

names = [r[1] for r in rows]
q = " UNION ALL ".join(
    f"SELECT '{n}' AS t, (SELECT COUNT(*) FROM `{n}`) AS c, "
    f"(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='toefl_db' AND table_name='{n}') AS f"
    for n in names) + ";"

out = subprocess.run([MYSQL, "-u", "root", "--default-character-set=utf8mb4", "-N", "-B", "-e", q, "toefl_db"],
                     capture_output=True, text=True, encoding="utf-8", env={"MYSQL_PWD": "123456", "SystemRoot": r"C:\Windows"})
live = {}
for line in out.stdout.strip().splitlines():
    t, c, f = line.split("\t")
    live[t] = (int(c), int(f))
if out.returncode != 0:
    print("MYSQL_ERR", out.stderr[:400])

mismatch = []
for r in rows:
    name, sheet_rows, sheet_cols = r[1], int(r[4]), int(r[5])
    lr, lc = live.get(name, (None, None))
    if (lr, lc) != (sheet_rows, sheet_cols):
        mismatch.append((name, sheet_rows, sheet_cols, lr, lc))

# 空表集合（全库 32 张里 0 行的）
all_tables = subprocess.run(
    [MYSQL, "-u", "root", "--default-character-set=utf8mb4", "-N", "-B", "-e",
     "SELECT table_name FROM information_schema.tables WHERE table_schema='toefl_db';", "toefl_db"],
    capture_output=True, text=True, encoding="utf-8",
    env={"MYSQL_PWD": "123456", "SystemRoot": r"C:\Windows"}).stdout.split()
print("live tables:", len(all_tables), "listed:", len(names))

print("mismatch:", mismatch if mismatch else "NONE")
print("sheet total rows:", sum(int(r[4]) for r in rows), "live total:", sum(v[0] for v in live.values()))
print("sheet total fields:", sum(int(r[5]) for r in rows))
fams = {}
for r in rows:
    fams.setdefault(r[2], [0, 0])
    fams[r[2]][0] += 1
    fams[r[2]][1] += int(r[4])
print("families:", {k: v for k, v in fams.items()})
