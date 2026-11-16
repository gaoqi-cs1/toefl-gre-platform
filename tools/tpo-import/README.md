# 从 FM-TOEFL 整理题目导入本平台（阅读模块）

> 一句话：FM-TOEFL 的题库是**加密 SQLite**（`%APPDATA%\FM-TOEFL\dds\udr92b`，SQLCipher 加密，`app.asar` 内无明文题目），所以不要指望直接拷文件；能走的是「你在软件里看到的题 → 按模板粘贴 → 脚本入库」。

## 目录

```
E:\cs\toefl-platform\tools\tpo-import\
├─ README.md                  本说明
├─ template-reading.txt       阅读题模板（照着填，可一个文件放多套）
└─ import_tpo.py              解析模板 → 生成 SQL（可选直接执行）
```

## 第一步：从 FM-TOEFL 取内容（手工，合规）

1. 打开 FM-TOEFL，进「阅读」模块，选一套（比如 Official 01 Passage 1）。
2. 左栏文章：从第一句到结尾，全选复制。
3. 右栏题目：逐题复制题干 + 四个选项；做完后软件会给正确答案与解析，也一起复制。
4. 粘贴到 `template-reading.txt` 里，按下面的格式摆好。一套一个 `[SET]` 块，一题一个 `[Q]` 块。
5. 一次可以攒很多套，全放同一个文件（或分多个 txt，脚本支持多文件）。

## 第二步：模板格式

```
[SET]
title: Official 01 - Passage 1 - 戏剧的起源
sectionTitle: 阅读 Passage 1

[PASSAGE]
（文章正文，保留分段，空行分段）
[/PASSAGE]

[Q]
type: VOCABULARY
stem: The word "ceremonies" in the passage is closest in meaning to
A) rituals
B) buildings
C) costumes
D) audiences
answer: A
analysis: 原文说这些活动是 periodically repeated 的集体行为，与 rituals（仪式）最接近。B/C/D 都是原文提到的具体物件，不是该词含义。
[/Q]

[Q]
type: FACTUAL
stem: According to the passage, why did early performances take place outdoors?
A) ...
B) ...
C) ...
D) ...
answer: B
analysis: ...
[/Q]
```

规则：

- `type` 建议用官方题型名：`VOCABULARY` / `FACTUAL` / `NEGATIVE FACTUAL` / `INFERENCE` / `RHETORICAL PURPOSE` / `SENTENCE SIMPLIFICATION` / `INSERT TEXT` / `PROSE SUMMARY` / `TABLE COMPLETION`。
- 选项四个，分别以 `A)` `B)` `C)` `D)` 开头；只有一个词也可以写成 `A. word`。
- `answer` 写 `A`/`B`/`C`/`D`（不区分大小写）。
- `analysis` 支持中英混写，前台会原样显示（你在做错后看的就是这段）。
- 文章里如果有一句被高亮（考词汇题的那个词），把整句照抄即可，不需要保留高亮格式。

## 第三步：生成 SQL 并入库

```powershell
cd E:\cs\toefl-platform\tools\tpo-import
python import_tpo.py template-reading.txt --tpo-no TPO01 --title "TPO 01 模拟套题"
```

默认只生成 `db-import-<时间>.sql`，不写库；确认无误后：

```powershell
python import_tpo.py template-reading.txt --tpo-no TPO01 --title "TPO 01 模拟套题" --apply
```

`--apply` 会用 `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`（root/123456，库 `toefl_db`）执行。

其他参数：

| 参数 | 作用 |
|---|---|
| `--tpo-no TPO02` | 归到哪个 TPO 编号（不存在会自动建 `tpo` 记录） |
| `--title "..."` | 新建 tpo 记录时的标题 |
| `--dry-run` | 只打印将要插入的条数，不生成文件 |
| `--apply` | 生成后立即执行入库 |
| `--skip-existing` | 题干完全相同（同 passage_title + 题干）时跳过，便于重复跑同一份模板 |

## 入库后的效果

- 阅读分块练习页（`/tpo`）会多出对应的练习单元；
- 套题页（`/full`）「一键生成套题」会把这些新题参与随机组卷；
- 每道题带 `analysis`，做题提交后才显示正确答案与解析（符合你之前要求的行为）。

## 为什么不去破解加密库

`dds\udr92b` 是 SQLCipher 加密的商业数据库，硬解需要从 `main.jsc` 里提取密钥，属于破坏商业软件的技术保护措施；而且里面的阅读文章本身是 ETS 版权材料。你自己购买的授权允许你**自己学习使用**，但整库导出再发布到自己的网站上属于再分发，风险在你这边。所以这里只做「你手上已有的内容，按你自己的节奏搬进自己的练习库」这一条路。

## 听力模块（已接入，2026-11-14）

听力在自己的数据约定上和阅读不同：**音频挂在 section 上，不在题目上**。

- 前端 `views/tpo/ListeningExam.vue` 用 `sectionInfo.audioUrl` 播放（即 `tpo_section.audio_url`），
  `content_text` 作为可展开的「听力原文」；
- 现有项目的音频放在 `frontend\public\audio\`，URL 写 `/audio/xxx.mp3`，打包后进 `dist/audio`；
- FM-TOEFL 的 `mres\`、`mres000\rev\` 里的 mp3 **是明文的**，可以直接引用，不用解密。

### 模板格式（template-listening.txt）

```
[SET]
title: 图书馆预约
sectionTitle: 听力 Conversation 1
audio: mres\AU-xxxx.mp3          ← 相对 E:\飞马\FM-TOEFL，或写绝对路径

[TRANSCRIPT]
（听力原文，可选；不写也能导入，只是习题页不显示「听力原文」）
[/TRANSCRIPT]

[Q]
type: PURPOSE
stem: ...
A) ... B) ... C) ... D) ...
answer: C
analysis: ...
[/Q]
```

### 命令

```powershell
cd E:\cs\toefl-platform\tools\tpo-import
python import_tpo.py template-listening.txt --tpo-no TPO01 --apply
```

脚本会做三件事：

1. 把 `audio:` 指向的文件按**原文件名**复制到 `frontend\public\audio\`（同名同大小就跳过，不重复占空间）；
2. `tpo_section` 写入 `type='listening'`、`audio_url='/audio/<文件名>'`、`content_text=听力原文`；
3. 题目写入 `tpo_question`，`module_type='listening'`。

新增参数：

| 参数 | 默认 | 作用 |
|---|---|---|
| `--audio-root` | `E:\飞马\FM-TOEFL` | `audio:` 写相对路径时的基准目录 |
| `--audio-dir` | `E:\cs\toefl-platform\frontend\public\audio` | 音频复制目标 |

### 已验证

用一段 2 秒测试音做过端到端验证：section 17 生成 `type=listening`、`audio_url=/audio/test-silence.wav`、
文件确实落到 `public\audio\`、题目 `module_type=listening` 正确；验证后测试数据已删除，库里回到
`tpo_question 49 / tpo_section 13`（reading 6、listening 3、speaking 2、writing 2）。

### 听力比阅读多一步的地方

- 一组听力通常 1 段音频配 3~5 题，模板里一个 `[SET]` 只对应**一段音频**，所以同讲座的题要放在同一个 `[SET]` 里；
- 音频文件建议保持原名（自带 hash 那段），不要改名，以后回查「这题音频是哪来的」还能对上；
- 如果是 Lecture + 多个问题，`sectionTitle` 写成 `听力 Lecture 1 - 生物学`，方便 `/tpo` 列表分组。

## 中文译文（[TRANSLATION]）与页面分层改造（2026-11-14 晚）

这一轮把 tpo_section 扩了两列，导入工具同步跟上了：

| 列 | 作用 | 空值时页面表现 |
|---|---|---|
| `title` | 列表页标题，例如「阅读 Passage 1 · 戏剧的起源」 | 后端回退成正文首句前 40 字（老逻辑） |
| `content_translation` | 正文/听力原文的中文译文 | 阅读/听力页按钮提示「本篇暂无中文译文」 |

译文写在 `[TRANSLATION]` 块里，位置放在 `[PASSAGE]`（或 `[TRANSCRIPT]`）之后、第一个 `[Q]` 之前：

```
[SET]
title: ...
sectionTitle: 阅读 Passage 1

[PASSAGE]
...英文原文...
[/PASSAGE]

[TRANSLATION]
...中文译文，段落之间空一行...
[/TRANSLATION]

[Q]
...
```

前端行为：**提交答案前**译文按钮不存在；提交过至少一题后才出现「查看中文译文 / 隐藏中文译文」；
听力页更严——换到下一题会自动收起译文。这条规则来自"做题前不得提前泄露答案"的既有约束。

### 页面分层

- `/tpo`：只有四张板块入口卡片（阅读/听力/口语/写作），不再列出具体篇章。
- `/tpo/module/reading|listening|speaking|writing`：该板块的篇章列表，显示 `title`、题数、是否已有译文。
- 点进去才是具体练习页（`/tpo/ReadingExam/:id` 等）。

把译文补进库里的两条路：
1. 走导入工具重新导入（带 `[TRANSLATION]` 块）；
2. 直接更新 `tpo_section.content_translation`（`UPDATE tpo_section SET content_translation = '...' WHERE id = N;`）。
目前只有 section 15（示例篇）自带译文，其余 12 个单元为空。

## 音频按 TPO 分目录（2026-11-14）

原来所有听力音频平铺在 `frontend\public\audio\`，篇数一多就分不清哪段属于哪套。现在改成：

```
frontend\public\audio\
  tpo01\cave.mp3
  tpo01\campus-job.wav
  tpo01\research-paper.wav
  tpo02\...
```

- 导入时 `--tpo-no TPO01` 会自动把子目录推成 `tpo01`，音频复制到 `audio\tpo01\`，库里的 `audio_url` 写成 `/audio/tpo01/xxx.mp3`；
- 想换目录名用 `--audio-subdir`（例如 `--audio-subdir tpo02`）；
- 现有 3 条记录的 `audio_url` 已经批量改到 `/audio/tpo01/…`，文件也已移动到位。

迁移 SQL 就一行（以后别的套号照抄）：

```sql
UPDATE tpo_section SET audio_url = REPLACE(audio_url, '/audio/', '/audio/tpo01/')
 WHERE audio_url LIKE '/audio/%' AND audio_url NOT LIKE '/audio/tpo01/%';
```