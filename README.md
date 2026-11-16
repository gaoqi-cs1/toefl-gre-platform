# TOEFL / GRE 刷题平台

一个自建的全栈刷题平台：TOEFL 分块练习 + 真题套题模考，GRE 分块练习 + 全真套题，两条线**数据完全独立**，同一套前后端骨架。个人项目，用于托福 / GRE 备考，也作为作品集。



* 后端：Spring Boot 2.7.15 + MyBatis-Plus 3.5.3.1 + MySQL 8（Java 11，130 个文件 / 6498 行）

* 前端：Vue 3.3.4 + Vue Router 4 + Pinia + Element Plus 2.3.8 + Vite 4（29 个 `.vue` / 8045 行）

* 规模：58 个 REST 接口・32 张表（24 张有数据，共 589 行 / 231 字段）



***

## 1. 功能地图



| 路由                                  | 页面              | 说明                                       |
| ----------------------------------- | --------------- | ---------------------------------------- |
| `/` → `/tpo`                        | 首页              | 只放四板块入口：阅读 / 听力 / 口语 / 写作                |
| `/tpo/module/:type`                 | `TpoModuleList` | 某个板块下的篇章 / 单元列表（首页不直接列题目）                |
| `/tpo/ReadingExam/:sectionId`       | 阅读答题            | 进页自动计时、不可暂停                              |
| `/tpo/listening/:sectionId`         | 听力答题            | 音频按 TPO 分目录；提交后可切换原文中文译文                 |
| `/tpo/speaking/:sectionId`          | 口语答题            | 录音上传后由本地 rubric 引擎评分                     |
| `/tpo/writing/:sectionId`           | 写作答题            | 题目 + 满分范文                                |
| `/wrongbook`                        | TOEFL 错题本       | 数据在 `tpo_wrong` / `tpo_wrong_question`   |
| `/full` `/full/exam` `/full/result` | 真题套题            | 随机组卷，四模块顺序作答，整卷一次性提交，结果按 1–6 档给分         |
| `/gre`                              | GRE 入口          | 分块练习目录 + 套题目录 + 模考记录                     |
| `/gre/practice/:sectionId`          | GRE 分块练习        | 逐题提交判分                                   |
| `/gre/suite`                        | GRE 全真套题        | 与 TOEFL `/full` 平行的一级入口                  |
| `/gre/exam` `/gre/result`           | GRE 套题作答 / 成绩   | Writing → Verbal → Quant 整卷提交，总分 260–340 |
| `/gre/wrong`                        | GRE 错题本         | 独立表 `gre_wrong`，不并入托福错题本                 |
| `/login` `/user` `/home`            | 登录 / 用户管理 / 仪表  | JWT 鉴权，路由守卫读 `userInfo.token`            |

旧套题路由 `/tpo/suite/*`、`/suite/*` 已整体下线（对应 `.vue` 文件原样保留在磁盘，只是不再挂路由）。



***

## 2. 目录结构



```
toefl-platform/

├─ backend/                       # Spring Boot

│  ├─ pom.xml                     # 依赖与打包

│  ├─ src/main/java/com/demo/toefl/

│  │  ├─ controller/              # 20 个 \*Controller.java，共 58 个接口

│  │  ├─ service/ serviceImpl/    # 业务层（含 AiScoringService / GreAwaScoringService）

│  │  ├─ mapper/                  # MyBatis-Plus Mapper

│  │  ├─ entity/                  # 31 个实体（与业务表一一对应，空表只留结构）

│  │  ├─ util/                    # JwtUtil / AuthHelper / 三个评分工具

│  │  └─ config/                  # WebConfig（拦截器、CORS、/upload 映射）

│  ├─ src/main/resources/         # application.yml、mapper/\*.xml

│  └─ upload/audio/               # 口语录音落盘目录

├─ frontend/                      # Vue 3 + Vite

│  └─ src/{views,components,api,router,stores,utils,styles}

├─ tools/                         # 数据/文档生成的辅助脚本（Python）

├─ \*.sql                          # 建库、增量修复、题库导入脚本

├─ 项目说明-知识点与架构.txt        # 知识点 + 架构长文（含 ER 附录）

├─ er-diagram.html / er图.png      # ER 图（可视化 / 静态图）

└─ 数据总表.xlsx                   # 表级数据快照（行数 / 字段数）
```



***

## 3. 快速开始

### 3.1 数据库



```
\-- MySQL 8，库名 toefl\_db，字符集 utf8mb4

CREATE DATABASE toefl\_db DEFAULT CHARSET utf8mb4;
```



```
\# 基础结构：tpo / tpo\_section / tpo\_question / user 等

mysql -u root -p toefl\_db < toefl\_db.sql

\# 题库与模块增量（按需依次导入，均为可重复执行的 upsert 或带判重的 insert）

mysql -u root -p toefl\_db < db-tpo-section-ext.sql

mysql -u root -p toefl\_db < db-more-questions.sql

mysql -u root -p toefl\_db < db-more-questions-2.sql

mysql -u root -p toefl\_db < db-gre-module.sql

mysql -u root -p toefl\_db < db-gre-suite-series.sql

mysql -u root -p toefl\_db < db-gre-timing.sql

mysql -u root -p toefl\_db < db-gre-awa-samples.sql

mysql -u root -p toefl\_db < full-suite-db.sql
```

`db-fix.sql` / `db-backup-before-fix.sql` 是一次历史修复的脚本与回滚快照，正常初始化不需要执行。

### 3.2 后端



```
cd backend

mvn spring-boot:run          # 默认 8080

\# 或 mvn clean package -DskipTests && java -jar target/toefl-backend-0.0.1-SNAPSHOT.jar
```

`src/main/resources/application.yml` 里改自己的库账号：



```
server:

&#x20; port: 8080

spring:

&#x20; datasource:

&#x20;   url: jdbc:mysql://localhost:3306/toefl\_db?useUnicode=true\&characterEncoding=utf8\&serverTimezone=Asia/Shanghai

&#x20;   username: root

&#x20;   password: 123456          # ← 改成你的

&#x20; servlet:

&#x20;   multipart:

&#x20;     max-file-size: 50MB     # 听力音频 / 口语录音

jwt:

&#x20; secret: toefl-platform-secret-key-202611

&#x20; expire: 7200000             # 2 小时（毫秒）
```

### 3.3 前端



```
cd frontend

npm install

npm run dev                  # Vite 默认 5173，/api 与 /upload 已代理到 8080

npm run build                # 产物在 frontend/dist
```

`vite.config.js` 已配置 `@` 别名与两条代理（`/api`、`/upload`），**不要重写路径**—— 后端接口本身就是 `/api/` 打头。



***

## 4. 数据库结构

库名 `toefl_db`，32 张表。**24 张有数据（589 行 / 231 字段）**，8 张只有结构没有记录。

### 4.1 按数据族汇总（非空表）



| 数据族    | 表数     | 行数      | 字段数     | 主要表                                                                                            |
| ------ | ------ | ------- | ------- | ---------------------------------------------------------------------------------------------- |
| GRE 套题 | 3      | 241     | 33      | `gre_suite` `gre_suite_section` `gre_suite_question`                                           |
| 托福分块   | 7      | 127     | 68      | `tpo` `tpo_section` `tpo_question` `tpo_answer` `tpo_wrong` `tpo_wrong_question` `exam_record` |
| GRE 分块 | 3      | 78      | 34      | `gre_section` `gre_question` `gre_answer`                                                      |
| 托福旧套题  | 3      | 66      | 21      | `tpo_suite` `suite_section` `tpo_exam_answer`                                                  |
| 真题套题   | 4      | 57      | 51      | `full_suite` `full_section` `full_question` `full_exam_record`                                 |
| 通用遗留   | 3      | 18      | 20      | `user_answer` `user_record` `user_wrong`                                                       |
| 账户     | 1      | 2       | 4       | `user`                                                                                         |
| **合计** | **24** | **589** | **231** |                                                                                                |

最大表是 `gre_suite_question`（179 行，占全库 30.4%）；`tpo_question` 49 行、`full_question` 41 行、`gre_question` 56 行。

### 4.2 空表（0 行，只有结构）

`question`、`tpo_suite_section`、`tpo_exam_record`、`full_exam_answer`、`gre_wrong`、`gre_exam_record`、`gre_exam_answer`、`user_essay`

这些是旧版通用表或尚未产生作答数据的表（如 GRE 错题本 `gre_wrong` 要等答错才落记录），建表语句仍完整保留。

### 4.3 关键约束



* 全部 32 张主键均为 `id bigint AUTO_INCREMENT`；`user.username` 唯一索引。

* **物理外键只有 2 条**：`tpo_section.tpo_id → tpo`、`tpo_question.section_id → tpo_section`，其余关联（`suite_id`、`record_id`、`user_id`…）都是应用层逻辑外键。

* TOEFL 与 GRE 各建一套表，互不 JOIN；GRE 套题题目字段几乎复制 `gre_question`，换取 "改题库不影响已生成卷"。

* 完整建表语句（`SHOW CREATE TABLE` 原文，24 张）：见飞书表 **TOEFL-GRE 平台数据总表** 的「建表语句（DDL）」子表。

### 4.4 ER 图



* `er-diagram.html` —— 4 页 A4 横向、四组数据族的 SVG 关系图，浏览器直接打开。

* `er图.png` —— 静态图。文字版关系说明见 `项目说明-知识点与架构.txt` 附录 A。



***

## 5. 评分与计时口径



| 项          | 规则                                                                                                    | 代码                                          |
| ---------- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| TOEFL 分块   | 阅读按题数映射 0–30（13 题 = 26，≥14 题 = 30）；听力按 3/6/9…30 阶梯                                                    | `ToeflScoreUtil`                            |
| TOEFL 真题套题 | 正确率 → 1–6 档（0.5 步进，阈值 0.95/0.85/0.75/0.65/0.55/0.45/0.35/0.25/0.15）；兼容分 `legacy_score = band/6 * 120` | `FullScoreUtil`                             |
| GRE        | Verbal / Quant 线性映射 130–170，总分 260–340，AWA 按 0.5 档                                                    | `GreScoreUtil`                              |
| 口语 / 写作评分  | 本地 rubric 引擎（关键词覆盖 + 结构 + 长度），**不调用外部大模型**                                                            | `AiScoringService` / `GreAwaScoringService` |
| 计时         | 一律「进页自动开始、不可暂停」；GRE verbal 90 秒 / 题、quant 105 秒 / 题、writing `LEAST(3600, 题数×1800)`、下限 300 秒           | 前端计时 + `db-gre-timing.sql`                  |

未采用 "任一部分超时即整卷立即提交"，超时后由 `advanceAfterTimeout` 推进到下一部分。



***

## 6. 接口速查（58 个）



| 前缀                                                             | 数量            | 职责                                       |
| -------------------------------------------------------------- | ------------- | ---------------------------------------- |
| `/api/examRecord`                                              | 7             | TOEFL 分块模考记录                             |
| `/api/fullExam`                                                | 5             | 真题套题答题与提交                                |
| `/api/greExam`                                                 | 5             | GRE 套题作答 / 提交 / 成绩                       |
| `/api/user`                                                    | 4             | 登录、注册、用户列表、当前用户                          |
| `/api/wrong`                                                   | 4             | TOEFL 错题本                                |
| `/api/greWrong`                                                | 4             | GRE 错题本                                  |
| `/api/greSuite`                                                | 4             | GRE 套题目录 / 详情                            |
| `/api/answer` `/api/gre/practice` `/api/suite` `/api/tposuite` | 3 各           | 分块作答提交、GRE 分块练习、旧套题、TPO 套系               |
| `/api/fullSuite`                                               | 2+1           | 真题套题列表 + `SuiteGeneratorController` 随机组卷 |
| `/api`                                                         | 2             | `SectionController` 通用 section 查询（无子前缀）  |
| `/api/question` `/api/tpo-section`                             | 2 / 2         | 题目、篇章（TPO 专用前缀，避免与 `/api/section` 冲突）    |
| `/api/section` `/api/essay` `/api/file` `/api/record`          | 1 / 1 / 1 / 1 | 通用 section、写作提交、文件上传、通用记录                |

鉴权：自研 `JwtInterceptor` + `WebConfig`（**未引入 spring-boot-starter-security**），拦截 `/api/**`，登录等公开路径在 `excludePathPatterns` 中放行。令牌解析出的用户身份由 `AuthHelper` 注入业务层。



***

## 7. 已知限制



1. **JWT 密钥两处不一致**：`application.yml` 配 `toefl-platform-secret-key-202611` / 7200000ms，而 `JwtUtil` 里硬编码 `gap-rbac-demo-secret-key-20261105abcd` / 2h，实际以硬编码为准。应改为 `@Value` 注入，否则改 yml 不生效。

2. **密码明文存储**：`user.password` 注释写明 "先明文调试"，生产需改 BCrypt。

3. 口语评分不是真实 ASR：目前按文本 / 录音时长做 rubric 打分，接入语音识别才有真实口语能力。

4. `question`、`tpo_suite_section` 等 8 张空表是历史遗留结构，代码里仍保留对应实体与 Mapper。

5. 旧套题（`tpo_suite` / `suite_section` / `tpo_exam_answer`，66 行）数据在、页面已下线，属并行模块遗留。

6. 无自动化测试与 CI，验证靠 `verify-api.ps1` / `verify-full-suite.ps1` 这类脚本与手工冒烟。

## 8. 文档索引



| 文件                                 | 内容                                                       |
| ---------------------------------- | -------------------------------------------------------- |
| `项目说明-知识点与架构.txt`                  | 十一章：技术栈、模块、知识点、简历 bullet、10 个面试追问、已知限制、术语、附录 A（四组数据族 ER） |
| `真题套题说明.md`                        | `/full` 组卷、作答、评分、成绩页设计                                   |
| `GRE 模块交付说明.md`                    | GRE 分块与套题的表结构、接口、计时与 AWA 评分                              |
| `随机套题与答题体验说明.md`                   | 随机组卷规则与答题交互取舍                                            |
| `单模块题库扩充说明.md`                     | 25 道 GRE 自写题（31→56 题、8→15 单元）                            |
| `修复说明.md`                          | 导入幂等、题干挤压等历史修复记录                                         |
| `数据总表.xlsx` / 飞书「TOEFL-GRE 平台数据总表」 | 表级行数 / 字段数、分族汇总、24 条 DDL                                 |
| `er-diagram.html` / `er图.png`      | ER 可视化                                                   |



***

*统计口径：代码行数取&#x20;*`backend/src/main/java`*&#x20;下 130 个&#x20;*`.java`*&#x20;与&#x20;*`frontend/src`*&#x20;下 29 个&#x20;*`.vue`*&#x20;的实际行数；表数据为&#x20;*`toefl_db`*&#x20;实测&#x20;*`COUNT(*)`*&#x20;与&#x20;*`information_schema.columns`*，24 张非空表合计 589 行 / 231 字段。*