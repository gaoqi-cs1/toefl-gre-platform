# GRE 题库扩充工具

把「题目数据文件（JSON）」写进 `gre_section` / `gre_question` 两张表，用来扩充 GRE 分块练习题库。
题库变大之后，套题页的「一键生成套题」才能抽出更多不同的组合。

## 文件清单

```
tools\gre-import\
  batch2-questions.json      第 2 批题目（7 个单元 / 25 道题，含解析、写作满分范例）
  batch2-questions.sql       由脚本生成的入库 SQL（可人工复核）
  import_gre_questions.py    导入脚本
  README.md                  本文件
```

## 怎么用

```powershell
cd "E:\cs\toefl-platform\tools\gre-import"

# 1) 校验 + 只生成 SQL（默认不动数据库）
python import_gre_questions.py batch2-questions.json

# 2) 复核生成的 .sql 后入库
python import_gre_questions.py batch2-questions.json --apply

# 3) 换库参数
python import_gre_questions.py batch2-questions.json --apply `
    --host 127.0.0.1 --port 3306 --user root --password 123456 --db toefl_db
```

`--mysql` 默认指向 `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`，换机器时改这个参数。

## 数据文件格式

```json
{
  "sections": [
    {
      "module_type": "verbal",              // verbal / quant / writing
      "sort_index": 5,                      // 该模块内的排序号，用来排单元顺序
      "title": "Text Completion · 单空专项 02",
      "subtitle": "4 题 · 单空逻辑",
      "hint": "先圈转折词与因果词",
      "difficulty": "中级",
      "limit_second": 480,
      "content_text": "阅读/听力类单元的文章正文（可选）",
      "questions": [
        {
          "task_type": "text_completion",   // 见下表
          "stem": "题干",
          "options": { "A": "...", "B": "..." },
          "answer": "B",
          "analysis": "中英结合的解析（必填）"
        }
      ]
    }
  ]
}
```

写作单元（`awa_issue` / `awa_argument`）没有选项，改用 `prompt` + `score_standard` + `sample_answer` 三件套：

```json
{
  "task_type": "awa_issue",
  "prompt": "题目要求 + 作答指令",
  "score_standard": "六分制评分要点",
  "sample_answer": "满分范例作文",
  "analysis": "这篇范例的思路拆解"
}
```

## 支持的 task_type

| task_type | 模块 | 说明 |
|---|---|---|
| `text_completion` | verbal | 单空填空 |
| `sentence_equivalence` | verbal | 六选一（沿用现有单字母口径，见下） |
| `reading` | verbal | 阅读，文章放单元级 `content_text` |
| `quant_compare` | quant | 数量比较，选项固定 4 个（A 大于 / B 大于 / 相等 / 无法确定） |
| `quant_problem` | quant | 问题求解，选项 A-E |
| `awa_issue` | writing | Issue 写作，AI 打分 + 范例 |
| `awa_argument` | writing | Argument 写作，AI 打分 + 范例 |

## 已知口径限制

1. **六选二目前是单选**。真实 GRE 的 Sentence Equivalence 要求「选两个」，但前端选项组件和
   `answer` 字段（`varchar(200)`，判分为整串比对）都按单选设计，库里现有 4 道 SE 也是单字母答案。
   改造成多选需要动 `GrePractice.vue` / `GreExam.vue` 的选项交互与 `answer` 的比对方式，
   不在本次范围内；新题沿用单选口径，保持与现有数据一致。
2. **双空 / 三空填空暂不支持**。同理，一个 `answer` 字段装不下多个空位，需要先改前端与判分。
   因此新扩充的填空全部是单空题。
3. 导入脚本只写 `gre_section` / `gre_question`，**不动套题表**。新题要进套题，去前端点「一键生成套题」，
   或调 `POST /api/greSuite/generate`。

## 幂等性

脚本按「单元标题」判断：已存在的标题整段跳过。所以重复执行不会产生重复题，
但如果要**追加**题到一个已有单元，需要换一个新标题（例如 `· 单空专项 03`）。

## 关于从第三方站点抓题（结论记录）

试过 `aplusgre.com`，抓不动，结论供以后参考：

- 站点是 Nuxt 前端应用，`/practice/` 的 HTML 只有约 2.5 KB 空壳，题目全部由前端渲染；
- `robots.txt` 为 `User-agent: *` / `Disallow:`（不禁抓），但 `/sitemap.xml` 404，也没有任何单题页面被搜索引擎收录；
- 真正的题目接口在 `api.aplusgre.com`，需要登录 token；
- 即便抓通，题目是 ETS 版权真题 + 该站的独家解析，整批复制进另一个平台存在版权风险。

所以本目录选择「自己写题 + 结构化入库」：题目、解析、范例答案都是自产的，可以持续扩充，也不受站点改版影响。
