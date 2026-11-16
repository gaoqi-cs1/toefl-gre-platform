-- =============================================================
-- 真题套题（Full Suite）独立数据模型
-- 说明：这套表与单模块练习用的 tpo_section / tpo_question 完全独立，
--       真题套题的题目、篇章、音频、答案都只存在 full_* 表里，互不干扰。
-- 执行：$env:MYSQL_PWD='123456'; Get-Content full-suite-db.sql -Raw | mysql -uroot --default-character-set=utf8mb4 toefl_db
-- 注意：脚本可重复执行（先 DROP 再 CREATE，只影响 full_* 这 5 张表）
-- =============================================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS full_exam_answer;
DROP TABLE IF EXISTS full_exam_record;
DROP TABLE IF EXISTS full_question;
DROP TABLE IF EXISTS full_section;
DROP TABLE IF EXISTS full_suite;

-- ---------------------------------------------------------
-- 套题目录
-- ---------------------------------------------------------
CREATE TABLE full_suite (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '套题id',
  title         VARCHAR(100) NOT NULL                COMMENT '套题名称，例如 TPO 01 全真模考',
  description   VARCHAR(255)          DEFAULT NULL   COMMENT '一句话介绍',
  difficulty    VARCHAR(20)           DEFAULT '中级' COMMENT '难度：入门/中级/进阶',
  total_minutes INT                   DEFAULT 120    COMMENT '建议总时长（分钟）',
  create_time   DATETIME              DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='真题套题目录（独立数据）';

-- ---------------------------------------------------------
-- 套题内的模块（阅读篇章 / 听力音频片段 / 口语任务组 / 写作任务组）
-- ---------------------------------------------------------
CREATE TABLE full_section (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  suite_id     BIGINT       NOT NULL                COMMENT '所属套题',
  module_type  VARCHAR(20)  NOT NULL                COMMENT 'reading/listening/speaking/writing',
  sort_index   INT          DEFAULT 0               COMMENT '同模块内的顺序',
  title        VARCHAR(255)          DEFAULT NULL   COMMENT '模块标题/篇章标题',
  content_text LONGTEXT              DEFAULT NULL   COMMENT '阅读正文（听力可放听力原文，仅成绩页展示）',
  audio_url    VARCHAR(500)          DEFAULT NULL   COMMENT '听力音频地址',
  limit_second INT                   DEFAULT 0      COMMENT '本模块限时（秒），0 表示不限',
  hint         VARCHAR(255)          DEFAULT NULL   COMMENT '给考生的提示语',
  PRIMARY KEY (id),
  KEY idx_full_section_suite (suite_id, module_type, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套题内的模块与篇章';

-- ---------------------------------------------------------
-- 套题内的题目（客观题有 option_*/answer，主观题有 prompt/score_standard）
-- ---------------------------------------------------------
CREATE TABLE full_question (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  section_id     BIGINT        NOT NULL,
  module_type    VARCHAR(20)   NOT NULL              COMMENT 'reading/listening/speaking/writing',
  sort_index     INT           DEFAULT 0             COMMENT '模块内序号，页面显示 Q1/Q2',
  question_title VARCHAR(2000)          DEFAULT NULL COMMENT '题干',
  option_a       VARCHAR(1000)          DEFAULT NULL,
  option_b       VARCHAR(1000)          DEFAULT NULL,
  option_c       VARCHAR(1000)          DEFAULT NULL,
  option_d       VARCHAR(1000)          DEFAULT NULL,
  answer         VARCHAR(200)           DEFAULT NULL COMMENT '客观题标准答案；主观题为空',
  analysis       TEXT                   DEFAULT NULL COMMENT '解析（成绩页展示）',
  prompt         LONGTEXT               DEFAULT NULL COMMENT '口语/写作题面与要求',
  task_type      VARCHAR(40)            DEFAULT NULL COMMENT 'speaking_independent / speaking_integrated / writing_integrated / writing_discussion / writing_email',
  limit_second   INT           DEFAULT 0             COMMENT '作答限时（秒）',
  prepare_second INT           DEFAULT 0             COMMENT '准备时间（秒）',
  score_standard LONGTEXT               DEFAULT NULL COMMENT '本题评分标准（AI 评分会读它）',
  PRIMARY KEY (id),
  KEY idx_full_question_section (section_id, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套题内的题目';

-- ---------------------------------------------------------
-- 一次整卷作答记录
-- 分数口径：新托福按 1-6 分档（0.5 步进），总分 = 四项平均；
--           legacy_score 是兼容旧页面的 0-120 换算值
-- ---------------------------------------------------------
CREATE TABLE full_exam_record (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  user_id           BIGINT      NOT NULL,
  suite_id          BIGINT      NOT NULL,
  suite_title       VARCHAR(100)         DEFAULT NULL,
  status            VARCHAR(20)          DEFAULT 'doing' COMMENT 'doing/finished',
  start_time        DATETIME             DEFAULT NULL,
  end_time          DATETIME             DEFAULT NULL,
  cost_seconds      INT                  DEFAULT 0,
  reading_correct   INT                  DEFAULT 0,
  reading_total     INT                  DEFAULT 0,
  listening_correct INT                  DEFAULT 0,
  listening_total   INT                  DEFAULT 0,
  reading_band      DECIMAL(3,1)         DEFAULT NULL COMMENT '阅读 1-6 档',
  listening_band    DECIMAL(3,1)         DEFAULT NULL,
  speaking_band     DECIMAL(3,1)         DEFAULT NULL,
  writing_band      DECIMAL(3,1)         DEFAULT NULL,
  total_band        DECIMAL(3,1)         DEFAULT NULL COMMENT '四项平均，1-6',
  legacy_score      INT                  DEFAULT 0    COMMENT '0-120 兼容分',
  create_time       DATETIME             DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_full_record_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='真题套题模考记录';

-- ---------------------------------------------------------
-- 逐题作答明细（含 AI 评分反馈 JSON）
-- ---------------------------------------------------------
CREATE TABLE full_exam_answer (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  record_id      BIGINT       NOT NULL,
  question_id    BIGINT       NOT NULL,
  module_type    VARCHAR(20)  NOT NULL,
  user_answer    LONGTEXT              DEFAULT NULL COMMENT '客观题是选项字母，主观题是文本',
  correct_answer VARCHAR(200)          DEFAULT NULL,
  is_correct     TINYINT               DEFAULT NULL COMMENT '客观题 1/0，主观题为 NULL',
  band           DECIMAL(3,1)          DEFAULT NULL COMMENT '主观题 AI 给的分档',
  ai_feedback    LONGTEXT              DEFAULT NULL COMMENT 'AI 反馈 JSON',
  create_time    DATETIME              DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_full_answer_record (record_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套题作答明细';


-- =============================================================
-- 示例内容 ①：TPO 01 全真模考（阅读 8 题 / 听力 5 题 / 口语 2 题 / 写作 2 题）
-- =============================================================
INSERT INTO full_suite (id, title, description, difficulty, total_minutes, create_time) VALUES
(1, 'TPO 01 全真模考', '四个模块一次性完成，中途答案本地缓存，最后统一提交判分', '中级', 120, NOW()),
(2, 'TPO 02 进阶模考', '题量略少，适合第一次完整走模考流程', '进阶', 90, NOW());

-- ---------- 阅读（suite 1） ----------
INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(1, 1, 'reading', 1, 'The Origins of Theater',
 'Long before written drama, human communities already performed stories in public. In many river valleys, seasonal festivals combined music, dance, and spoken narrative into a single event that lasted several days. These gatherings were not merely entertainment; they marked planting, harvest, and the passage from one year to the next.\n\nScholars once argued that theater grew out of religious ritual. The evidence is suggestive but incomplete. Ritual and drama share a deliberate separation from ordinary time: performers wear special clothing, speak in heightened language, and act within a bounded space. Yet ritual aims at repetition, while drama increasingly depended on novelty, on surprise, and on the pleasure of seeing a familiar story told in an unfamiliar way.\n\nA second explanation points to the storyteller. In communities where most people could not read, a single skilled narrator could hold a crowd for hours, shifting voice and posture to represent many characters. Over time, assistants began to answer the narrator, and the exchange between two voices made conflict visible on stage. This is the moment when narrative becomes drama: the audience no longer hears about an event, it watches one happen.\n\nNeither explanation accounts for the whole picture. What theater required was a stable surplus of time and food, a place where a crowd could gather, and a culture willing to accept that a fiction may carry truth. Where these conditions met, theater appeared, and it appeared independently in societies that had no contact with one another.',
 NULL, 0, '本篇为学术阅读，8 道选择题，平均每题 2 分钟');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(1, 'reading', 1,
 'What does the first paragraph suggest about seasonal festivals?',
 'They were held mainly for political purposes.',
 'They combined several forms of performance into one event.',
 'They were performed by professional actors only.',
 'They replaced religious ceremonies entirely.',
 'B',
 '第一段明确说节日把音乐、舞蹈、口头叙事结合成一个持续数天的事件，B 是对这句话的改写。A、D 文中没有依据；C 与"human communities already performed stories in public"矛盾。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 2,
 'The word "suggestive" in paragraph 2 is closest in meaning to',
 'certain',
 'misleading',
 'worth considering',
 'outdated',
 'C',
 'suggestive 指"能提示某种结论、值得参考"，即 worth considering。它不是"确定"（certain），也不是"误导"或"过时"。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 3,
 'According to paragraph 2, how do ritual and drama differ?',
 'Ritual is performed outdoors, drama indoors.',
 'Ritual seeks repetition, drama seeks novelty.',
 'Ritual uses music, drama does not.',
 'Ritual is performed by one person, drama by many.',
 'B',
 '原文 "ritual aims at repetition, while drama increasingly depended on novelty"，直接对应 B。其余三项都是无依据的对比。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 4,
 'What role does the storyteller play in the second explanation?',
 'The storyteller wrote the first plays.',
 'The storyteller performed many characters alone.',
 'The storyteller trained professional actors.',
 'The storyteller refused to repeat familiar stories.',
 'B',
 '第二段（第三个段落）说叙述者"shifting voice and posture to represent many characters"，即一人演绎多个角色，B 正确。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 5,
 'The phrase "narrative becomes drama" is explained in the passage by',
 'the audience hearing about an event instead of seeing it',
 'the audience watching an event happen instead of hearing about it',
 'the narrator writing down the story',
 'the performers abandoning music',
 'B',
 '原文紧接一句 "the audience no longer hears about an event, it watches one happen"，正是 B 的表述。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 6,
 'Which of the following is NOT mentioned as a condition for theater to appear?',
 'a surplus of time and food',
 'a place where crowds could gather',
 'a culture willing to accept fiction as a carrier of truth',
 'a system of formal written scripts',
 'D',
 '最后一段列了三个条件：时间与食物的盈余、可聚集人群的场所、愿意接受虚构承载真实的文化。文中没有提到成文脚本，且前文强调当时多数人不识字，D 与原文冲突。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 7,
 'What does the author conclude about the two explanations?',
 'The ritual explanation is fully correct.',
 'The storyteller explanation is fully correct.',
 'Neither explanation is complete on its own.',
 'Both explanations have been disproved.',
 'C',
 '最后一段首句 "Neither explanation accounts for the whole picture"，作者认为两种解释都不足以独立解释全部现象，C 正确。',
 NULL, NULL, 0, 0, NULL),
(1, 'reading', 8,
 'Why does the author mention societies that had no contact with one another?',
 'To show that theater developed independently in different places.',
 'To prove that all societies borrowed theater from one region.',
 'To argue that theater is a modern invention.',
 'To explain why theater disappeared in some regions.',
 'A',
 '结尾 "it appeared independently in societies that had no contact with one another" 的目的就是说明戏剧在不同地区独立起源，A 正确。',
 NULL, NULL, 0, 0, NULL);

-- ---------- 听力（suite 1，音频复用 frontend/public/audio/cave.mp3） ----------
INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(2, 1, 'listening', 1, 'Conversation: A Student in the Library',
 'Student: Excuse me, I am looking for a book on the history of paper making for my term paper.
Librarian: That title is on reserve, so it cannot leave the building. You can read it here and copy the pages you need.
Student: How late can I stay?
Librarian: The reading room closes at ten on weekdays and at six on Friday. Photocopiers are on the second floor and take cards, not coins.',
 '/audio/cave.mp3', 0, '听力音频在页面上可直接播放，本模块 5 道选择题');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(2, 'listening', 1,
 'Why does the student speak to the librarian?',
 'To borrow a book for a class presentation',
 'To find a book for a term paper',
 'To complain about the closing time',
 'To ask for a library card',
 'B',
 '学生开口就说明 "looking for a book on the history of paper making for my term paper"，目的是为学期论文找书。',
 NULL, NULL, 0, 0, NULL),
(2, 'listening', 2,
 'What does the librarian say about the book?',
 'It is available for two weeks.',
 'It must be read inside the library.',
 'It has already been checked out.',
 'It is kept on the second floor.',
 'B',
 '图书管理员说该书 "on reserve, so it cannot leave the building"，即只能在馆内阅读。',
 NULL, NULL, 0, 0, NULL),
(2, 'listening', 3,
 'What are students allowed to do with the book?',
 'Take it home for one night',
 'Copy the pages they need',
 'Write notes directly on the pages',
 'Lend it to a classmate',
 'B',
 '管理员明确说 "You can read it here and copy the pages you need"。',
 NULL, NULL, 0, 0, NULL),
(2, 'listening', 4,
 'When does the reading room close on Friday?',
 'At six',
 'At eight',
 'At nine',
 'At ten',
 'A',
 '管理员说 "at ten on weekdays and at six on Friday"，周五是六点。',
 NULL, NULL, 0, 0, NULL),
(2, 'listening', 5,
 'What does the librarian say about the photocopiers?',
 'They are free of charge.',
 'They accept cards rather than coins.',
 'They are on the first floor.',
 'They close before the reading room.',
 'B',
 '最后一句说明复印机 "take cards, not coins"，即刷卡不收硬币。',
 NULL, NULL, 0, 0, NULL);

-- ---------- 口语（suite 1） ----------
INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(3, 1, 'speaking', 1, 'Speaking Tasks', NULL, NULL, 0, '口语共 2 题：独立题 45 秒，综合题 60 秒；录音后把口述内容转写进文本框，AI 才能评分');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(3, 'speaking', 1,
 'Independent Speaking',
 NULL, NULL, NULL, NULL, NULL, NULL,
 'Some students prefer to study in a library, while others prefer to study at home. Which do you prefer and why? Use specific reasons and details to support your answer.',
 'speaking_independent', 45, 15,
 '评分维度（1-6 档）：① 内容完整度（是否明确表态并给出理由与细节）；② 语言组织（是否有清晰的开头-理由-例子结构、连接词使用）；③ 语言质量（词汇多样性、句子长度变化）；④ 与题目的相关度（是否回应题面核心词）。'),
(3, 'speaking', 2,
 'Integrated Speaking',
 NULL, NULL, NULL, NULL, NULL, NULL,
 'Read the notice: the university plans to replace the small study rooms on the second floor with a larger open study area. Then listen to a student who disagrees. Summarize the notice and explain why the student disagrees.',
 'speaking_integrated', 60, 20,
 '评分维度（1-6 档）：① 是否准确概括材料中的两个要点；② 是否说清学生的反对理由；③ 结构（notice → reason → detail）；④ 语言质量（词汇与句式的丰富度）。');

-- ---------- 写作（suite 1） ----------
INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(4, 1, 'writing', 1, 'Writing Tasks', NULL, NULL, 0, '写作共 2 题：综合写作 20 分钟，学术讨论 10 分钟；AI 按新托福 1-6 档 rubric 评分');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(4, 'writing', 1,
 'Integrated Writing',
 NULL, NULL, NULL, NULL, NULL, NULL,
 'Read the passage arguing that cities should replace small parks with high-density housing, then listen to a lecture that challenges these points. Write a response that summarizes the lecture and explains how it casts doubt on the reading. Target 150-225 words.',
 'writing_integrated', 1200, 0,
 '评分维度（1-6 档）：① 是否完整覆盖听力中的反驳要点；② 与阅读观点的对应关系是否清楚；③ 词数是否在 150-225 区间；④ 语言质量（连接词、词汇多样性、句子长度变化、语法风险）。'),
(4, 'writing', 2,
 'Academic Discussion',
 NULL, NULL, NULL, NULL, NULL, NULL,
 'Professor Lane: Some universities now require every first-year student to take a course in statistics. Do you think this requirement is a good idea? Why or why not?
Anna: It helps students read research critically, so I support it.
Ben: Not everyone needs statistics; students should choose.
Write a post of at least 120 words: state your position, respond to one classmate, and support your view with a concrete example.',
 'writing_discussion', 600, 0,
 '评分维度（1-6 档）：① 是否明确表态并回应某位同学的观点；② 论证是否有具体例子支撑；③ 词数是否达到 120 以上；④ 语言质量（连接词、词汇多样性、句子长度变化、语法风险）。');


-- =============================================================
-- 示例内容 ②：TPO 02 进阶模考（阅读 6 / 听力 4 / 口语 1 / 写作 1）
-- =============================================================
INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(5, 2, 'reading', 1, 'The Rise of Coffeehouses',
 'In the seventeenth century, coffee reached European cities faster than almost any other imported good. The drink was cheap enough for a clerk and strong enough to keep a worker awake through a long evening, and the places that served it became something the tavern had never been: a room where people of different trades could talk without drinking.\n\nOwners quickly learned that a coffeehouse sold more than a beverage. They provided newspapers, warm light, and tables that encouraged strangers to sit together. Customers paid for a cup and stayed for hours, and the conversation they joined often turned to prices, ships, and politics. Merchants who had never met began to trust each other because both were seen in the same room.\n\nGovernments were uneasy. A ruler who disliked public argument could close a coffeehouse more easily than a street. Some cities did close them, and the drink simply moved into private homes, where the conversation continued without witnesses. The habit survived because it was never only about coffee.',
 NULL, 0, '本篇 6 道选择题，建议 12 分钟');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(5, 'reading', 1,
 'What made coffee attractive to ordinary workers?',
 'It was expensive and rare.',
 'It was cheap and kept people awake.',
 'It tasted better than tea.',
 'It was served only in private homes.',
 'B',
 '第一段 "cheap enough for a clerk and strong enough to keep a worker awake"，B 为同义改写。',
 NULL, NULL, 0, 0, NULL),
(5, 'reading', 2,
 'According to the passage, coffeehouses differed from taverns mainly because',
 'they served food as well as drinks',
 'people could talk without drinking',
 'they were open only at night',
 'they were owned by governments',
 'B',
 '原文 "a room where people of different trades could talk without drinking"，这正是与酒馆的关键差别。',
 NULL, NULL, 0, 0, NULL),
(5, 'reading', 3,
 'What did coffeehouse owners provide besides coffee?',
 'Newspapers, warm light, and shared tables',
 'Free meals and beds',
 'Ships and cargo lists',
 'Private rooms for merchants',
 'A',
 '第二段直接列出 "newspapers, warm light, and tables that encouraged strangers to sit together"。',
 NULL, NULL, 0, 0, NULL),
(5, 'reading', 4,
 'Why did merchants begin to trust one another?',
 'They signed formal contracts in the coffeehouse.',
 'They were seen in the same room.',
 'They shared the cost of coffee.',
 'They all worked for the government.',
 'B',
 '原文 "began to trust each other because both were seen in the same room"，信任来自共同在场的可见性。',
 NULL, NULL, 0, 0, NULL),
(5, 'reading', 5,
 'What does the passage say about governments and coffeehouses?',
 'Governments encouraged them as places of learning.',
 'Governments could close them more easily than a street.',
 'Governments required them to sell newspapers.',
 'Governments built them for merchants.',
 'B',
 '第三段 "could close a coffeehouse more easily than a street"，说明政府更容易关掉咖啡馆。',
 NULL, NULL, 0, 0, NULL),
(5, 'reading', 6,
 'Why did the habit survive even after some coffeehouses were closed?',
 'Coffee was replaced by tea.',
 'The conversation moved into private homes.',
 'Merchants stopped meeting in public.',
 'Governments later reopened the shops.',
 'B',
 '最后一段说明店铺关掉后，"the drink simply moved into private homes"，习惯因此延续。"',
 NULL, NULL, 0, 0, NULL);

INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(6, 2, 'listening', 1, 'Lecture: Animal Migration',
 'Professor: Many birds migrate thousands of kilometers, yet they do not carry a map. Experiments show that some species read the position of the sun, and others sense the earth magnetic field. Young birds often learn the route from adults, which means migration is partly inherited and partly taught.
Researchers once assumed that every bird followed the same route. Tracking devices now show individual differences: some birds pause for weeks, others fly directly.',
 '/audio/cave.mp3', 0, '本模块 4 道选择题');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(6, 'listening', 1,
 'What is the lecture mainly about?',
 'How birds learn to fly',
 'How birds find their way during migration',
 'Why birds migrate in groups',
 'How tracking devices are built',
 'B',
 '开头即提出鸟类迁徙却不带地图，随后讲定位方式，主旨是"迁徙中如何辨别方向"。',
 NULL, NULL, 0, 0, NULL),
(6, 'listening', 2,
 'According to the professor, what do some birds use to navigate?',
 'The position of the sun',
 'The sound of the ocean',
 'The shape of coastlines',
 'The smell of plants',
 'A',
 '教授提到 "some species read the position of the sun"（还有地磁场），A 正确。',
 NULL, NULL, 0, 0, NULL),
(6, 'listening', 3,
 'Why is migration described as partly taught?',
 'Young birds learn the route from adults.',
 'Birds copy other species.',
 'Researchers teach birds in laboratories.',
 'Birds memorize maps from books.',
 'A',
 '原文 "Young birds often learn the route from adults"，因此迁徙一部分是学来的。',
 NULL, NULL, 0, 0, NULL),
(6, 'listening', 4,
 'What did tracking devices reveal?',
 'All birds follow exactly the same route.',
 'Individual birds differ in timing and path.',
 'Birds cannot migrate without adults.',
 'Birds migrate only at night.',
 'B',
 '设备显示个体差异：有的停留数周、有的直飞，B 正确。',
 NULL, NULL, 0, 0, NULL);

INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(7, 2, 'speaking', 1, 'Speaking Task', NULL, NULL, 0, '1 道独立口语题，45 秒');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(7, 'speaking', 1,
 'Independent Speaking',
 NULL, NULL, NULL, NULL, NULL, NULL,
 'Do you agree or disagree: students should be required to take at least one course outside their major? Give reasons and examples.',
 'speaking_independent', 45, 15,
 '评分维度（1-6 档）：① 是否明确立场；② 理由是否具体；③ 结构是否清楚；④ 语言质量（词汇与句式）。');

INSERT INTO full_section (id, suite_id, module_type, sort_index, title, content_text, audio_url, limit_second, hint) VALUES
(8, 2, 'writing', 1, 'Writing Task', NULL, NULL, 0, '1 道学术讨论写作，10 分钟');

INSERT INTO full_question (section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, answer, analysis, prompt, task_type, limit_second, prepare_second, score_standard) VALUES
(8, 'writing', 1,
 'Academic Discussion',
 NULL, NULL, NULL, NULL, NULL, NULL,
 'Professor Hale: Should companies allow employees to work from home permanently? Support your answer with an example.
Write at least 100 words and respond to at least one other student.',
 'writing_discussion', 600, 0,
 '评分维度（1-6 档）：① 观点是否明确；② 是否回应他人；③ 例子是否具体；④ 语言质量（连接词、词汇、句式、语法风险）。');
