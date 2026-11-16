# GRE 模块交付说明（分块练习 + 全真套题 · 两套独立数据）

本轮在 `E:\cs\toefl-platform` 里新增了完整的 GRE 模块，并把「GRE 备考」接进了顶部导航。
GRE 的分块练习与全真套题**各自独立建表、互不调用**：练习只读写 `gre_section / gre_question / gre_answer`，
套题只读写 `gre_suite / gre_suite_section / gre_suite_question / gre_exam_record / gre_exam_answer`，
和 TOEFL 的 `tpo_* / full_*` 也没有任何交叉。

---

## 一、本轮新增 / 修改的文件

### 数据库
| 文件 | 说明 |
| --- | --- |
| `db-gre-module.sql` | 新建并已执行成功：8 张 `gre_*` 表 + 种子数据（8 个练习单元、31 道练习题、1 套 18 题预置套题）。可重复执行（内容表先 DELETE 再插入，不动用户作答表）。 |
| `db-gre-awa-samples.sql` | 新建并已执行：给 5 道写作题补齐 6 分档英文范例，并把题干里误存的字面量 `\n` 换成真实换行。这段内容也已追加到 `db-gre-module.sql` 末尾，重新播种不会丢范例。 |

### 后端（`backend\src\main\java\com\demo\toefl\`）
| 目录 / 文件 | 作用 |
| --- | --- |
| `entity\GreSection / GreQuestion / GreAnswer` | 分块练习三张表 |
| `entity\GreSuite / GreSuiteSection / GreSuiteQuestion` | 套题三张表（`GreSuite` 带 writing/verbal/quant/questionCount 四个统计字段） |
| `entity\GreExamRecord / GreExamAnswer` | 模考记录与逐题作答 |
| `mapper\Gre*Mapper`（8 个） | MyBatis-Plus BaseMapper |
| `util\GreScoreUtil` | 量表分换算（130-170）、总分、0.5 档量化、AWA 档位评语 |
| `service\GreAwaScoringService` | AWA 本地 rubric 评分（v2，0-6） |
| `controller\GrePracticeController` | `/api/gre/practice/*` 分块目录、单节题目、单题提交 |
| `controller\GreSuiteController` | `/api/greSuite/*` 套题目录、详情、一键随机生成、删除生成套题 |
| `controller\GreExamController` | `/api/greExam/*` 开始、整卷提交判分、记录、成绩详情、删除 |
| `config\WebConfig.java` | 放行名单追加 `/api/gre/practice/**`、`/api/greSuite/**`、`/api/greExam/**` |

### 前端（`frontend\src\`）
| 文件 | 作用 |
| --- | --- |
| `views\gre\GreHome.vue` | GRE 入口：三个模块的分块练习目录 + 套题卡片 + 我的模考记录 + 一键生成套题 |
| `views\gre\GrePractice.vue` | 分块练习做题页：文章卡 + 选项（未作答无选中态）+ 逐题提交后显示对错/正确答案/中英解析；写作题给 AWA 维度分与范文 |
| `views\gre\GreExam.vue` | 套题作答页：Writing → Verbal → Quant 顺序，答案存本机缓存，最后一次性提交 |
| `views\gre\GreResult.vue` | 成绩页：总分 260-340、V/Q 量表分、AWA 0-6、逐题回顾、未作答显示「我的答案：空」、写作给维度反馈与满分范例 |
| `router\index.js` | 新增 `/gre`、`/gre/practice/:sectionId`、`/gre/exam`、`/gre/result` 四条路由 |
| `components\AppShell.vue` | 顶部导航新增「GRE 备考」，`isActive` 让 `/gre/*` 都算激活；品牌与页脚文案改为 TOEFL & GRE |

---

## 二、数据库结构（8 张表）

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `gre_section` | module_type / title / content_text / limit_second | 8 个单元：TC 单空、SE 六选一、RC 珊瑚礁、RC 印刷术、QC、PS、Issue 题库、Argument 题库 |
| `gre_question` | section_id / option_a…option_f / answer / analysis / prompt / task_type / score_standard / sample_answer | 31 题；写作题 `task_type` 为 `awa_issue` / `awa_argument`，`sample_answer` 存 6 分范例 |
| `gre_answer` | user_id + question_id 唯一键 | 分块练习作答（upsert） |
| `gre_suite` | title / source（preset 或 generated）/ 统计字段在实体里（非表列） | 1 套预置「GRE 全真模考 01 · 基础卷」 |
| `gre_suite_section` | suite_id / module_type / sort_index / content_text | 5 个：writing 1、verbal 2、quant 2 |
| `gre_suite_question` | section_id / option_a…f / answer / analysis / prompt / sample_answer | 18 题 |
| `gre_exam_record` | verbal_scaled / quant_scaled / awa_score / total_score / status | 模考记录，量表分 130-170，总分 = V + Q |
| `gre_exam_answer` | record_id / question_id / user_answer / correct_answer / is_correct / awa_score / ai_feedback | 逐题明细，**未作答也写一行**（user_answer 空） |

题目量分布：`verbal 14`、`quant 12`、`writing 5`（共 31）。套题 18 题：写作 2、Verbal 8、Quant 8。

---

## 三、接口清单（前端只调这三组，不碰 TOEFL 接口）

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/gre/practice/catalog` | 三模块 + 每单元题量、难度、是否含文章 |
| GET | `/api/gre/practice/section/{id}` | 单节题目（**脱敏**：不下发 answer / analysis / sampleAnswer，只给 `options[{key,text}]`） |
| POST | `/api/gre/practice/answer` | 单题提交：客观题返回 correct/correctAnswer/analysis；写作返回 awaScore/feedback/sampleAnswer |
| GET | `/api/greSuite/list` | 套题目录（带三模块题量与总题量） |
| GET | `/api/greSuite/{id}` | 套题三层结构（module → section → question），题目脱敏 |
| POST | `/api/greSuite/generate` | 一键生成：`{writing:2, verbal:10, quant:10}`，随机抽题，按来源练习单元分组复制（RC 正文跟着题走） |
| DELETE | `/api/greSuite/{id}` | 只允许删 `source='generated'` 的套题 |
| POST | `/api/greExam/start` | `{suiteId, userId}` 建一条 doing 记录 |
| POST | `/api/greExam/submit` | `{recordId, costSeconds, answers:[{questionId,userAnswer}]}` 整卷判分 |
| GET | `/api/greExam/records?userId=` | 我的模考列表 |
| GET | `/api/greExam/record/{id}` | 成绩详情：整卷每题都返回（含未作答），写作带 AWA 反馈与范文 |
| DELETE | `/api/greExam/record/{id}` | 删除记录（连带明细） |

判分口径：
- Verbal / Quant：`scaled(correct,total) = 130 + round(正确率 × 40)`，裁剪到 130-170（教学近似换算，非官方预测分）。
- AWA：`GreAwaScoringService` 本地 rubric，四维合计 6.0（论证深度 1.5 / 例证 1.5 / 结构 1.5 / 语言 1.0），0.5 一档。
- 总分 = Verbal 量表分 + Quant 量表分，AWA 单列。

AWA rubric v2 的校准结果（用库里的范例作文实测）：

| 作文 | 词数 | 段数 | 得分 |
| --- | --- | --- | --- |
| 161 Issue 范例 | 238 | 5 | 6.0 |
| 162 Issue 范例 | 212 | 5 | 5.5 |
| 163 Issue 范例 | 232 | 5 | 5.0 |
| 171 Argument 范例 | 245 | 6 | 5.5 |
| 172 Argument 范例 | 234 | 7 | 5.0 |
| 19 词的碎片 | 19 | 1 | 0.5 |
| 72 词单段套话 | 72 | 1 | 2.5 |

---

## 四、本轮修掉的 5 个真实 bug（都在编译/启动/评分环节）

1. **`selectCount` 强转报错**：`Long cannot be converted to int`，改成 `(int) (long) selectCount(...)`。
2. **启动直接失败**：`GreSuiteController` 用 `@Resource` 注入 `suiteSectionMapper` 等字段时，字段名和 TOEFL 已有的 `suiteSectionMapper` bean 同名，Spring 先按名字命中，类型不符 → `Expected a bean of type GreSuiteSectionMapper`。三个 GRE controller 的注入全部改成 `@Autowired`（按类型注入）后启动正常。
3. **AWA 分数虚低**：`countWords` 用 `" " + word + " "` 匹配，遇到 `however,` 这种带标点的写法全部漏数，导致结构分 0、范例作文只有 4 分。已改为先把标点归一成空格再匹配，并重写 rubric v2（放宽词表、加入隐含分段、具体信息分档）。
4. **AWA 范例不全**：原来只有 161 / 171 两篇有范文，`162 / 163 / 172` 是 NULL。已补 3 篇原创英文 6 分范例（约 250-260 词），现在 5 道写作题 100% 有范文。
5. **前端颜色变量不存在**：GRE 三个页面用了 `var(--success)` / `var(--danger)`，但 `theme.css` 里没有这两个变量，对错配色会失效。已换成字面色值 `#17936a` / `#c0392b`。

另外顺手修了题干里的字面量 `\n`（之前会导致页面上出现一堆反斜杠而不是分段），现在是真实换行。

---

## 五、验证结果（都已经跑过，不是"应该能跑"）

| 验证 | 结果 |
| --- | --- |
| javac 全量编译（含 21 个新 GRE 文件） | exit 0，只有 lombok 的 Unsafe 警告 |
| 8081 测试实例启动 | `Started ToeflApplication in 3.879 seconds` |
| GRE 接口自检 13 项 | 全 PASS：目录 3 模块/31 题/8 单元、单节脱敏、客观题判对错、写作 AWA 评分、套题目录、一键生成 2/10/10、生成后详情 22 题、开始模考、整卷判分（总分 284 / V 150 / Q 134 / AWA 1.5）、成绩详情 22 行且未作答 1 行、带范文 2 篇、记录列表、清理删除 |
| 前端 `npm run build` | exit 0，`✓ built in 8.06s`，产出 `GreHome / GrePractice / GreExam / GreResult` 四个 chunk |

---

## 六、你现在需要做的 3 步

1. **重启后端**：监听着 8080 的那个进程跑的还是旧代码，新加的 `Gre*` controller 不在里面。停掉 8080 的 Spring Boot，重新启动（IDE 里 Run `ToeflApplication` 即可）。
2. **重启前端**：`cd E:\cs\toefl-platform\frontend && npm run dev`（如果 dev 一直在跑，HMR 会自动加载新页面，刷新一次浏览器即可）。
3. **打开页面**：登录后点顶部导航「GRE 备考」→ 左侧任一单元进入分块练习；右侧套题卡点「开始模考」；成绩页从「我的 GRE 模考记录」里的「查看成绩」进入。

数据库不用重复执行（已经执行过）；如果哪天把库清了重来，按顺序执行 `db-gre-module.sql` 即可（里面已含 AWA 范例补丁）。

---

## 七、已知限制（先讲清楚，避免你踩）

1. AWA 是**本地 rubric 自查分**，不是 ETS 官方评分，页面上也写了这句提示。它看的是结构、例证、词汇与篇幅，不能判断论证是否真的成立。要接大模型评分，只需替换 `GreAwaScoringService` 里 `scoreIssue / scoreArgument` 的实现，接口与页面都不用动。
2. 分块练习目前是**逐题提交、提交后锁定**（符合你"提交后才显示正确答案"的要求）；套题是**整卷一次性提交**。如果你希望分块也支持"整节一次提交"，需要改 `GrePractice.vue` 与后端加一个批量判分接口，这一轮没做。
3. ~~GRE 没有错题本~~ → **已补**（第八节）：独立表 `gre_wrong`、独立页面 `/gre/wrong`、独立接口 `/api/greWrong/**`，不并入托福 `wrong_book`。
4. Quant 目前全部是选择题（QC 四选一、PS 五选一），没有做数值填空题。
5. ~~TOEFL 旧页面还没逐个重做视觉~~ → **已用低风险方式补上**：只改 `styles/theme.css`（追加"全站视觉统一层"），模板与业务逻辑零改动，旧页面统一获得新底色、页头、卡片、表格、表单、按钮、弹窗、焦点态与过渡。逐页重做模板仍可作为后续打磨项。

---

## 八、本轮补做的两件事 + GRE 主视觉板块

### 8.1 GRE 错题本（独立表，没并进托福 `wrong_book`）

**建表文件**：`E:\cs\toefl-platform\db-gre-wrong.sql`（已执行成功）

| 字段 | 说明 |
| --- | --- |
| `user_id` / `question_id` | 谁、哪道题 |
| `question_scope` | `practice`=分块练习题 / `suite`=套题里的题。**两套题库的题目 id 各自编号，靠这个字段区分，不会互相覆盖** |
| `module_type` / `task_type` | verbal / quant / writing 与具体题型 |
| `question_title` / `option_a..f` / `correct_answer` / `analysis` | 答错那一刻从题库复制的**快照**，题库以后改题，回看仍是当时那道题 |
| `user_answer` | 最近一次的错误作答 |
| `wrong_count` | 累计错几次 |
| `last_source` | `practice` / `exam` |
| `mastered` | 1=已标记掌握，列表默认过滤掉 |
| 唯一键 | `uk_gre_wrong_user_q (user_id, question_id, question_scope)` |

**写入逻辑**（`service\GreWrongService.java`，分块练习和套题共用）：

1. 分块练习答错 → `recordPracticeWrong`，套题提交判错 → `recordSuiteWrong`；
2. 同一用户同一题再错 → `wrong_count + 1`、`mastered` 重置为 0，不新增行；
3. **之后答对一次 → 直接从错题本删除**（`releasePractice` / `releaseSuite`）；
4. 写作题（`awa*`）没有唯一正确答案，不进错题本；套题里**没作答**的题也不进（避免整卷空白把错题本灌满）。

**接口**（`controller\GreWrongController.java`，`WebConfig` 已放行 `/api/greWrong/**`）：

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/greWrong/list?userId=&moduleType=&includeMastered=` | 错题列表 + 三模块待复习统计 |
| POST | `/api/greWrong/master` | `{id, mastered}` 标记掌握 / 恢复 |
| DELETE | `/api/greWrong/{id}` | 移出错题本 |
| DELETE | `/api/greWrong/all` | 清空当前用户未掌握的错题 |

**页面**：`frontend\src\views\gre\GreWrong.vue`（路由 `/gre/wrong`）
- 顶部四个筛选页签（全部 / Verbal / Quant / Writing），带各自待复习数量；
- 每道错题一张卡：题干、选项（**正确答案绿框 + 「正确答案」标签，我的错误答案红框 + 「我的答案」标签**）、错几次、来源（分块练习 / 套题）、中英解析、时间；
- 操作：「标记为已掌握」「去重做这道题」「移出错题本」。
- 导航改动：原来的「错题本」改成 **「托福错题本」**，新增 **「GRE 错题本」**，避免两个错题本混淆；`isActive` 里 `/gre/wrong` 不再被 `GRE 备考` 抢走高亮。

**接口自检（本机 8081 实测，全部通过）**：

| 步骤 | 结果 |
| --- | --- |
| 初始列表 | `total=0`，stats 全 0 |
| 分块练习 q101 答错（提交 `Z`） | 列表 1 条，`scope=practice`、`wrongCount=1`、`correct_answer=A` |
| 同题再错一次 | 仍 1 条，`wrongCount=2`，`user_answer` 更新为最新一次 |
| 答对 q101 | 列表回到 0 条（自动移出） |
| 套题提交（1 题答错 + 其余空） | 列表 1 条，`scope=suite`、`last_source=exam`、`correct_answer=B`；未作答的题**没有**进错题本 |
| 标记掌握 | 默认列表隐藏，`includeMastered=true` 仍能看到 |
| 删除 | 列表与库都清空 |

### 8.2 全站视觉统一（不动模板，只加一层 CSS）

改的文件只有一个：`frontend\src\styles\theme.css`（末尾追加「全站视觉统一层」一段）。这样做的好处是**老页面的模板、逻辑、接口一行都没动**，风险最低，但 `TpoList / ReadingExam / ListeningExam / SpeakingExam / WritingExam / FullList / FullExam / FullResult / WrongBook` 会自动跟着变：

- 页面底色：极淡的蓝/琥珀双径向渐变（原来是大面积纯灰，显脏）；
- 页头：标题 27px、副标题限宽 780px、动作区对齐方式从 `flex-end` 改 `flex-start`；
- 卡片：统一圆角边框 + 悬停描边过渡；
- 表格：表头改成浅色底 + 加粗、行悬停高亮、圆角裁切、行高统一；
- 表单：输入框 / 文本域 / 下拉统一圆角与**聚焦外发光环**；
- 按钮：统一胶囊圆角、按下位移 1px、主按钮带投影；
- 弹窗 / 消息框 / 全局提示：圆角统一；
- 键盘可达性：`:focus-visible` 统一焦点环；
- 尊重系统「减少动态效果」设置；
- 补充 `.pill--bad` / `.pill--warn` 两个状态标签样式（GRE 错题本用到）；
- 滚动条已是细样式（原有），保持。

### 8.3 GRE 的「明显板块」

`GreHome.vue` 顶部把原来的普通页头换成一个**主视觉 hero**（`gre-hero`）：

- 深靛蓝渐变横幅 + 右上角光晕，整块 26px 大圆角、带投影，一眼能看出这是 GRE 入口；
- 英文小标题 `GRADUATE RECORD EXAMINATIONS` + 大标题「GRE 备考中心」 + 两行说明（两个入口、两套独立数据、错题自动进入错题本）；
- 三个动作按钮：**开始分块练习**（平滑滚到分块区块）、**一键生成套题**、**GRE 错题本**（带琥珀色待复习数量角标）；
- 右侧四个数据块：练习题总数 / 练习单元数 / 可用套题数 / 待复习错题数（数据来自 `catalog`、`greSuite/list`、`greWrong/list` 统计）；
- 窄屏（<960px）自动变单列。

### 8.4 本轮验证

| 验证 | 结果 |
| --- | --- |
| javac 全量编译（新增 4 个文件：`GreWrong` / `GreWrongMapper` / `GreWrongService` / `GreWrongController`） | exit 0 |
| 8081 测试实例启动 | `Started ToeflApplication in 6.728 seconds` |
| 错题本接口自检（上表 7 项） | 全 PASS |
| 原有 13 项 GRE 接口自检 | 未出现回归（catalog / section / answer / suite / exam 均正常） |
| 前端 `npm run build` | exit 0，`✓ built in 20.38s`，新增 `GreWrong` chunk（5.37 kB），`GreHome` 6.64 kB CSS / 9.71 kB JS |

### 8.5 你这边只需要

1. **重启 8080 后端**（新 `GreWrongController` 和两个写入钩子都要重启才生效）；
2. 刷新浏览器（`npm run dev` 在跑就会自动热更新）；
3. 打开 `/gre` 看 hero 板块 → 做错一题 → 打开 `/gre/wrong` 确认错题进来 → 再答对一次 → 确认自动移出。

## 九、TOEFL 与 GRE 做题入口"平行化"改造（本轮）

### 9.1 改造前的问题
TOEFL 的做题入口是一级并列的两个页面（`/tpo` 分块练习、`/full` 全真套题），而 GRE 把"分块练习 + 全真套题"塞在 `/gre` 一个页面里，导航也只有「TPO 练习 / 真题套题 / GRE 备考」三项——两个考试在信息架构上不对等。

### 9.2 改造后的四个一级入口（平行、数据各自独立）

| 入口 | 路由 | 页面文件 | 数据来源 | 后端接口前缀 |
|---|---|---|---|---|
| TOEFL 分块练习 | `/tpo` | `views/tpo/TpoList.vue` | TPO 分块题库 | `/api/tpo*` |
| TOEFL 全真套题 | `/full` | `views/full/FullList.vue` | `full_suite` 等套题表 | `/api/fullSuite`、`/api/fullExam` |
| GRE 分块练习 | `/gre` | `views/gre/GreHome.vue` | `gre_section`、`gre_question` | `/api/gre/practice/*` |
| GRE 全真套题 | `/gre/suite` | `views/gre/GreSuiteHome.vue` | `gre_suite`、`gre_suite_question`、`gre_exam_record`、`gre_exam_answer` | `/api/greSuite`、`/api/greExam` |

- 错题本仍各一套，挂在四个入口之下但不属于做题入口：`/wrongbook`（`wrong_book`）与 `/gre/wrong`（`gre_wrong`）。
- GRE 分块与 GRE 套题**互不读表**：`GreHome.vue` 只调 `gre/practice/catalog`，`GreSuiteHome.vue` 只调 `greSuite/*` 与 `greExam/*`；两者唯一的交集是主页 hero 上显示"可用套题数量"（只做展示，不参与做题）。

### 9.3 本轮改动清单

1. **新建 `frontend/src/views/gre/GreSuiteHome.vue`**：GRE 全真套题独立页。套题卡片（难度 / 时长 / 写作·Verbal·Quant 题量）、「一键生成套题」（随机组卷）、「开始模考」「继续作答」「查看成绩」「删除记录」。本地答案缓存前缀 `greExam:*`，与 TOEFL 的 `fullExam:*` 完全隔离。
2. **重写 `frontend/src/views/gre/GreHome.vue`**：只留 GRE 分块练习。hero 标题改为「GRE 分块练习」，数据块 = 练习题 / 练习单元 / 可用套题 / 待复习错题；动作 = 开始练习、GRE 全真套题、GRE 错题本。旧版完整备份在同目录 `GreHome.vue.bak`（不删）。
3. **新建 `frontend/src/components/ModeSwitch.vue`**：四个入口的胶囊切换条，四 tab = TOEFL 分块 `/tpo`、TOEFL 全真套题 `/full`、GRE 分块 `/gre`、GRE 全真套题 `/gre/suite`，当前页高亮。已挂到 `/tpo`、`/full`、`/gre`、`/gre/suite` 四个页面顶部。
4. **`frontend/src/router/index.js`**：新增 `/gre/suite`（name `GreSuiteHome`，`meta.requireAuth`）。
5. **`frontend/src/components/AppShell.vue`**：导航从 3 项改为 6 项并列——TOEFL 分块 / TOEFL 套题 / GRE 分块 / GRE 套题 / 托福错题本 / GRE 错题本；`isActive` 同步：`/tpo`、`/wrongbook` 前缀命中，`/gre` 排除 `/gre/suite`、`/gre/exam`、`/gre/result`、`/gre/wrong`，`/gre/suite` 额外接管 `/gre/exam`、`/gre/result`。
6. **`frontend/src/views/full/FullList.vue`、`views/tpo/TpoList.vue`**：页头下方插入 `ModeSwitch`，保证从托福页面也能一眼看到四个入口。

### 9.4 验证
- `npm run build`：exit 0，`✓ built in 7.53s`，产物含 `GreSuiteHome-013a0e84.js`（6.47 kB）与更新后的 `GreHome-d6db84d5.js`（4.24 kB，已减去套题部分）、`FullList`、`GreWrong`、`GrePractice`、`GreExam`、`GreResult` 全部 chunk 正常生成。
- 路由表核对：`/gre`、`/gre/practice/:sectionId`、`/gre/exam`、`/gre/result`、`/gre/suite`、`/gre/wrong` 六条齐全，`/gre/suite` 位于 `/gre/wrong` 之前不影响匹配（均为静态路径）。

### 9.5 使用说明（本轮新增行为）
- 想看 GRE 分块练习 → 顶部导航「GRE 分块」或直接 `/gre`。
- 想组卷做整卷模考 → 顶部导航「GRE 套题」或 `/gre/suite`，点「一键生成套题」，系统按练习单元分组随机抽题（阅读正文随题一起进套题，保证同一篇文章下的题在一套里）。
- 做套题时答案先存浏览器本地，四个板块（Writing → Verbal → Quant）全部做完再提交，后端一次性判分；中途刷新可继续，记录页有「继续作答」。