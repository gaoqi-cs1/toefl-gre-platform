-- =====================================================================
-- GRE 模块独立数据库（与 TOEFL 的 tpo_* / full_* 完全隔离，互不调用）
-- 文件：db-gre-module.sql        执行：mysql -uroot -p123456 toefl_db < db-gre-module.sql
--
-- 表结构分层：
--   1) 分块练习：gre_section + gre_question + gre_answer
--   2) 套题：    gre_suite + gre_suite_section + gre_suite_question
--   3) 模考记录：gre_exam_record + gre_exam_answer
--
-- 说明：
--   * gre_question 是"分块练习题库"，gre_suite_question 是"套题内的题目"，
--     两者结构相似但各自独立存储；一键生成套题是把练习题库的题「复制」一份进套题表，
--     之后改练习库不会影响已生成的套题（单向复制，不是互相引用）。
--   * 所有表都用 CREATE TABLE IF NOT EXISTS，重复执行不会丢数据；
--     只有练习题库与套题内容在文末被 DELETE 后重新插入（这两部分是本脚本自带的种子数据）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 分块练习单元（一个 section = 一组同类型题，RC 的正文放在 content_text）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_section (
  id            bigint       NOT NULL AUTO_INCREMENT,
  module_type   varchar(20)  NOT NULL COMMENT 'writing / verbal / quant',
  sort_index    int          DEFAULT 0 COMMENT '同模块内顺序，决定页面上的卡片次序',
  title         varchar(200) NOT NULL COMMENT '练习单元名称',
  subtitle      varchar(255) DEFAULT NULL COMMENT '一句话说明考查重点',
  content_text  longtext     COMMENT '阅读文章正文（只有 RC 才有）',
  hint          varchar(255) DEFAULT NULL COMMENT '给考生的提示',
  difficulty    varchar(20)  DEFAULT '中级' COMMENT '入门 / 中级 / 进阶',
  limit_second  int          DEFAULT 0 COMMENT '建议限时（秒），0 表示不限',
  PRIMARY KEY (id),
  KEY idx_gre_section_module (module_type, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 分块练习单元（独立于套题）';

-- ---------------------------------------------------------------------
-- 2. 分块练习题库
--    选项最多 6 个（Sentence Equivalence 是六选一），Quant 只用 A-E
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_question (
  id            bigint        NOT NULL AUTO_INCREMENT,
  section_id    bigint        NOT NULL COMMENT '所属练习单元 gre_section.id',
  module_type   varchar(20)   NOT NULL COMMENT 'writing / verbal / quant',
  sort_index    int           DEFAULT 0 COMMENT '单元内序号，页面显示 Q1/Q2',
  question_title varchar(2000) DEFAULT NULL COMMENT '题干',
  option_a      varchar(1000) DEFAULT NULL,
  option_b      varchar(1000) DEFAULT NULL,
  option_c      varchar(1000) DEFAULT NULL,
  option_d      varchar(1000) DEFAULT NULL,
  option_e      varchar(1000) DEFAULT NULL,
  option_f      varchar(1000) DEFAULT NULL,
  answer        varchar(200)  DEFAULT NULL COMMENT '客观题答案字母；写作题留空',
  analysis      text          COMMENT '解析（做题页提交后才下发）',
  prompt        longtext      COMMENT '写作题面与写作要求',
  task_type     varchar(40)   DEFAULT NULL COMMENT 'text_completion / sentence_equivalence / reading / quant_compare / quant_problem / awa_issue / awa_argument',
  score_standard longtext     COMMENT '写作评分说明（AWA 打分时给考生看）',
  sample_answer longtext      COMMENT '写作满分范例（提交后下发）',
  difficulty    varchar(20)   DEFAULT '中级',
  PRIMARY KEY (id),
  KEY idx_gre_question_section (section_id, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 分块练习题库';

-- ---------------------------------------------------------------------
-- 3. 分块练习做题记录（每题一条，重复作答覆盖）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_answer (
  id          bigint      NOT NULL AUTO_INCREMENT,
  user_id     bigint      NOT NULL,
  question_id bigint      NOT NULL,
  user_answer longtext    COMMENT '客观题是选项字母，写作是全文',
  is_correct  tinyint     DEFAULT NULL COMMENT '客观题 1/0；写作题留空',
  awa_score   decimal(3,1) DEFAULT NULL COMMENT '写作 AWA 0-6 档',
  create_time datetime    DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gre_answer_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 分块练习作答记录';

-- ---------------------------------------------------------------------
-- 4. 套题目录
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_suite (
  id            bigint       NOT NULL AUTO_INCREMENT,
  title         varchar(100) NOT NULL COMMENT '套题名称',
  description   varchar(255) DEFAULT NULL,
  difficulty    varchar(20)  DEFAULT '中级',
  total_minutes int          DEFAULT 130 COMMENT '建议总时长（分钟），GRE 全套约 2 小时',
  source        varchar(20)  DEFAULT 'seed' COMMENT 'seed = 预置，generated = 一键生成',
  create_time   datetime     DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 套题目录（独立数据）';

-- ---------------------------------------------------------------------
-- 5. 套题内的模块
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_suite_section (
  id           bigint       NOT NULL AUTO_INCREMENT,
  suite_id     bigint       NOT NULL,
  module_type  varchar(20)  NOT NULL COMMENT 'writing / verbal / quant',
  sort_index   int          DEFAULT 0 COMMENT '同模块内顺序',
  title        varchar(255) DEFAULT NULL,
  content_text longtext     COMMENT '阅读文章正文',
  hint         varchar(255) DEFAULT NULL,
  limit_second int          DEFAULT 0 COMMENT '本模块限时（秒）',
  PRIMARY KEY (id),
  KEY idx_gre_suite_section (suite_id, module_type, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 套题内的模块';

-- ---------------------------------------------------------------------
-- 6. 套题内的题目
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_suite_question (
  id             bigint        NOT NULL AUTO_INCREMENT,
  section_id     bigint        NOT NULL COMMENT '所属 gre_suite_section.id',
  module_type    varchar(20)   NOT NULL,
  sort_index     int           DEFAULT 0,
  question_title varchar(2000) DEFAULT NULL,
  option_a       varchar(1000) DEFAULT NULL,
  option_b       varchar(1000) DEFAULT NULL,
  option_c       varchar(1000) DEFAULT NULL,
  option_d       varchar(1000) DEFAULT NULL,
  option_e       varchar(1000) DEFAULT NULL,
  option_f       varchar(1000) DEFAULT NULL,
  answer         varchar(200)  DEFAULT NULL,
  analysis       text,
  prompt         longtext,
  task_type      varchar(40)   DEFAULT NULL,
  score_standard longtext,
  sample_answer  longtext,
  difficulty     varchar(20)   DEFAULT '中级',
  PRIMARY KEY (id),
  KEY idx_gre_suite_question (section_id, sort_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 套题内的题目';

-- ---------------------------------------------------------------------
-- 7. 模考记录
--    GRE 计分：Verbal / Quant 各 130-170（1 分档），总分 260-340；AWA 0-6（0.5 档）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_exam_record (
  id             bigint      NOT NULL AUTO_INCREMENT,
  user_id        bigint      NOT NULL,
  suite_id       bigint      NOT NULL,
  suite_title    varchar(100) DEFAULT NULL,
  status         varchar(20) DEFAULT 'doing' COMMENT 'doing / finished',
  start_time     datetime    DEFAULT NULL,
  end_time       datetime    DEFAULT NULL,
  cost_seconds   int         DEFAULT 0,
  verbal_correct int         DEFAULT 0,
  verbal_total   int         DEFAULT 0,
  quant_correct  int         DEFAULT 0,
  quant_total    int         DEFAULT 0,
  verbal_scaled  int         DEFAULT NULL COMMENT 'Verbal 130-170',
  quant_scaled   int         DEFAULT NULL COMMENT 'Quant 130-170',
  awa_score      decimal(3,1) DEFAULT NULL COMMENT 'AWA 0-6',
  total_score    int         DEFAULT NULL COMMENT 'verbal + quant，260-340',
  create_time    datetime    DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_gre_record_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 套题模考记录';

-- ---------------------------------------------------------------------
-- 8. 模考作答明细（整卷每题一条，未作答也写，成绩页才能全量展示）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gre_exam_answer (
  id             bigint      NOT NULL AUTO_INCREMENT,
  record_id      bigint      NOT NULL,
  question_id    bigint      NOT NULL,
  module_type    varchar(20) NOT NULL,
  user_answer    longtext    COMMENT '客观题选项字母；写作为全文',
  correct_answer varchar(200) DEFAULT NULL,
  is_correct     tinyint     DEFAULT NULL COMMENT '客观题 1/0；写作为 NULL',
  awa_score      decimal(3,1) DEFAULT NULL,
  ai_feedback    longtext    COMMENT 'AWA 评分反馈 JSON',
  create_time    datetime    DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_gre_answer_record (record_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GRE 套题作答明细';

-- =====================================================================
-- 以下为种子数据：先清空本脚本负责的表，再插入（不动用户作答记录）
-- =====================================================================
DELETE FROM gre_question;
DELETE FROM gre_section;
DELETE FROM gre_suite_question;
DELETE FROM gre_suite_section;
DELETE FROM gre_suite;

-- ---------------------------------------------------------------------
-- 分块练习单元
-- ---------------------------------------------------------------------
INSERT INTO gre_section (id, module_type, sort_index, title, subtitle, content_text, hint, difficulty, limit_second) VALUES
(1, 'verbal', 1, 'Text Completion · 单空专项', '先看转折/因果信号词，再定空格的情感方向', NULL, '每题只有一个空格，五个选项只有一个同时满足语义与语气', '入门', 0),
(2, 'verbal', 2, 'Sentence Equivalence · 六选一', '找与句子里已有逻辑一致的那一个词', NULL, '六个选项里有近义干扰项，判断依据是句子本身的逻辑关系', '中级', 0),
(3, 'verbal', 3, 'Reading Comprehension · 短篇 01', '珊瑚礁与植食性鱼类：结论取决于"怎么吃"', 'Marine ecologists have long argued that herbivorous reef fish protect corals by cropping algae. The logic is intuitive: algae compete with coral for light and space, so more grazers should mean healthier reefs. Yet a decade-long survey of forty reefs in the Indo-Pacific found that reefs with the highest grazer biomass were not consistently the healthiest. Reef health instead tracked the density of a single genus of grazing fish that removes algae without scraping the coral skeleton. The authors therefore argue that the protective effect depends less on how much algae is eaten than on how it is eaten. That argument implies, further, that stocking a reef with fast-growing grazers could do more harm than good, since such fish tend to graze indiscriminately and leave wounds that corals must spend energy repairing.', '文章不长，但要抓住"however / instead / therefore"三个转向词', '中级', 480),
(4, 'verbal', 4, 'Reading Comprehension · 短篇 02', '印刷术到底"民主化"了什么', 'For historians of the book, the printing press is often described as having democratized knowledge. That description is not wrong, but it compresses several distinct changes into one word. Cheap print increased the number of texts in circulation; it did not, by itself, increase the number of people able to read them. Literacy in most of early modern Europe expanded slowly, over generations, and largely through institutions such as schools, churches and guilds that had little to do with the press. What the press did change quickly was the economics of error: a manuscript mistake stayed local, while a typesetting mistake could be reproduced in thousands of copies before anyone noticed. Contemporaries were acutely aware of this, which is why errata lists, correctors and privilege notices appear so early in printed books.', '注意作者是"修正一个流行说法"，不是全盘否定', '中级', 480),
(5, 'quant', 1, 'Quantitative Comparison · 数量比较', 'A、B 两栏比大小，先化简再判断是否需要分类讨论', NULL, '四个选项含义固定：A 大 / B 大 / 相等 / 无法确定', '入门', 0),
(6, 'quant', 2, 'Problem Solving · 问题求解', '五选一，覆盖算术、代数、几何、概率', NULL, 'GRE 的数量题不追求复杂计算，追求把条件翻译成式子', '中级', 0),
(7, 'writing', 1, 'Analytical Writing · Issue 题库', '30 分钟立论文，立场 + 两个理由 + 具体例子', NULL, 'AWA 满分 6 分，评分看论证深度、例证具体性、组织与语言控制', '进阶', 1800),
(8, 'writing', 2, 'Analytical Writing · Argument 题库', '30 分钟驳论文，只评"论证是否成立"，不需要自己站队', NULL, 'AWA Argument 的核心是找出假设、替代解释与证据缺口', '进阶', 1800);

-- ---------------------------------------------------------------------
-- 分块练习题目（Verbal：Text Completion 101-104）
-- ---------------------------------------------------------------------
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(101, 1, 'verbal', 1, 'Far from being ______, the director''s notes were so hedged that the cast could infer almost nothing about the performance she wanted.',
 'candid', 'cautious', 'verbose', 'generous', 'exacting', NULL, 'A',
 '中：far from 表否定，"notes 并非____"，后半句说她含糊其辞（hedged），因此空格要与 hedged 相反，填 candid（直率）。
EN: Hedged means deliberately unclear, so the blank must be its opposite: candid. Choice B (cautious) actually agrees with hedged, which is the trap.',
 NULL, 'text_completion', NULL, NULL, '入门'),
(102, 1, 'verbal', 2, 'The philosopher''s early essays, once dismissed as ______, are now admired for precisely the economy that critics mistook for emptiness.',
 'pretentious', 'slight', 'obscure', 'rigorous', 'derivative', NULL, 'B',
 '中："economy"（简洁）曾被误当成 emptiness（空洞），所以当年被判为 slight（单薄、不重要）。
EN: The clue is economy mistaken for emptiness, which points to slight. Pretentious and derivative describe attitude or origin, not thinness of substance.',
 NULL, 'text_completion', NULL, NULL, '中级'),
(103, 1, 'verbal', 3, 'Because her conclusion rested on a single fragmentary inscription, her colleagues urged ______ until further excavation could confirm it.',
 'circumspection', 'enthusiasm', 'replication', 'improvisation', 'leniency', NULL, 'A',
 '中：证据只有一块残碑，同行自然劝她"谨慎"（circumspection），等更多证据再下结论。
EN: A single fragmentary inscription is weak evidence, so the advice is caution, i.e. circumspection. Enthusiasm and improvisation push in the wrong direction.',
 NULL, 'text_completion', NULL, NULL, '中级'),
(104, 1, 'verbal', 4, 'The lectures were ______: even listeners who rejected the professor''s conclusions conceded that no better-informed case could be made.',
 'cogent', 'dogmatic', 'desultory', 'intemperate', 'perfunctory', NULL, 'A',
 '中：反对者也承认"没有更有依据的论证"，说明讲座本身严密有力（cogent）。
EN: Even opponents concede the case is as well informed as possible, which describes cogent argument. Dogmatic and intemperate are about attitude, which the sentence does not discuss.',
 NULL, 'text_completion', NULL, NULL, '中级');

-- Verbal：Sentence Equivalence 111-114（六选一）
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(111, 2, 'verbal', 1, 'The committee''s report was criticized as ______: it catalogued the problem in elaborate detail yet proposed nothing that could be acted on.',
 'sterile', 'exhaustive', 'pragmatic', 'evasive', 'candid', 'meticulous', 'A',
 '中：冒号后说"列尽细节却提不出可执行的建议"，批评点在"无产出"，所以是 sterile（不结果实的）。exhaustive 与 meticulous 只表示详尽，属于中性偏褒。
EN: The colon defines the criticism: elaborate detail with nothing actionable. That is sterile. Exhaustive and meticulous describe thoroughness, not lack of result.',
 NULL, 'sentence_equivalence', NULL, NULL, '中级'),
(112, 2, 'verbal', 2, 'Although the hypothesis once seemed ______, a decade of experiments has made it a respectable, if still contested, option.',
 'untenable', 'novel', 'orthodox', 'marginal', 'promising', 'durable', 'A',
 '中：although 表让步，后半句"如今成了 respectable（可敬的）选项"，反推当年是"站不住脚"（untenable）。
EN: The although contrast means the earlier judgment was negative and strong: untenable. Marginal is weaker and does not contrast with respectable as sharply.',
 NULL, 'sentence_equivalence', NULL, NULL, '中级'),
(113, 2, 'verbal', 3, 'Her prose is ______: every clause carries information, and nothing is repeated for effect.',
 'economical', 'ornate', 'diffuse', 'elliptical', 'prolix', 'discursive', 'A',
 '中：冒号后解释"每个从句都带信息、没有为效果重复"，正是 economical（经济、不浪费）。ornate / prolix / discursive 都是冗长方向。
EN: Every clause carries information and nothing is padding: that is economical prose. Ornate, prolix and discursive all mean the opposite.',
 NULL, 'sentence_equivalence', NULL, NULL, '中级'),
(114, 2, 'verbal', 4, 'The negotiations were ______: each side restated its opening position and no concession was even discussed.',
 'stagnant', 'amicable', 'brisk', 'confidential', 'constructive', 'ceremonial', 'A',
 '中：双方只是重复开场立场、连让步都没谈，进程完全停滞（stagnant）。
EN: Restating positions without discussing concessions means no movement, so stagnant. Constructive and brisk describe movement and pace, which the sentence denies.',
 NULL, 'sentence_equivalence', NULL, NULL, '中级');

-- Verbal：Reading Comprehension 121-123（对应 section 3 的珊瑚礁短文）
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(121, 3, 'verbal', 1, 'Which of the following best states the main point of the passage?',
 'High grazer biomass is the single best predictor of reef health.', 'Whether grazing protects corals depends on the manner of grazing rather than the volume of algae removed.', 'Algae, not grazing fish, are the primary cause of coral decline.', 'Fast-growing grazers should be introduced to reefs to control algae.', 'Survey data from the Indo-Pacific cannot be generalized to other oceans.', NULL, 'B',
 '中：文章先给出流行观点（吃得多=礁健康），再用 however 引入调查结果（礁健康只跟着"某一种啃食方式"走），therefore 落到结论：保护作用取决于"怎么吃"。
EN: The pivot is however plus therefore. The passage explicitly says the effect depends less on how much algae is eaten than on how it is eaten, which is exactly choice B.',
 NULL, 'reading', NULL, NULL, '中级'),
(122, 3, 'verbal', 2, 'The passage suggests that adding fast-growing grazers to a reef would most likely',
 'speed the removal of algae and quickly restore coral cover.', 'reduce algal competition without any measurable cost to corals.', 'produce short-term algae control while slowing coral recovery over time.', 'have no effect, because grazing intensity is already at a maximum.', 'increase the number of grazing genera on the reef.', NULL, 'C',
 '中：末句说这类鱼" indiscriminate grazing 会留下伤口，珊瑚要花能量修复"，所以短期能清藻、长期拖慢恢复。
EN: The final sentence supplies the mechanism: indiscriminate grazing leaves wounds that cost the coral energy to repair. That is short-term control with a long-term cost.',
 NULL, 'reading', NULL, NULL, '进阶'),
(123, 3, 'verbal', 3, 'The author mentions wounds primarily in order to',
 'describe the physical appearance of damaged corals.', 'explain why one genus of grazer is more beneficial than others.', 'question whether algae compete with coral for light.', 'introduce evidence that grazing can harm corals directly.', 'suggest that coral repair is independent of energy budgets.', NULL, 'D',
 '中：伤口是"啃食可以直接伤到珊瑚"的具体机制，用来说明 indiscriminate grazing 的代价。
EN: Wounds give a concrete mechanism by which grazing itself can damage corals, supporting the claim that how the algae is removed matters.',
 NULL, 'reading', NULL, NULL, '进阶');

-- Verbal：Reading Comprehension 131-133（对应 section 4 的印刷术短文）
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(131, 4, 'verbal', 1, 'The primary purpose of the passage is to',
 'argue that the printing press had little effect on literacy.', 'complicate a familiar characterization of the printing press.', 'compare manuscript and print cultures in terms of cost.', 'describe the training of early modern correctors.', 'defend the claim that cheap print democratized knowledge.', NULL, 'B',
 '中：首句给出流行说法（democratized knowledge），紧接着"not wrong, but compresses several distinct changes into one word"，全篇在拆解这个说法。
EN: The passage never denies the popular claim outright; it says the claim compresses distinct changes, then separates them. That is complicating, not refuting.',
 NULL, 'reading', NULL, NULL, '中级'),
(132, 4, 'verbal', 2, 'The passage implies that a manuscript error was less consequential than a typesetting error because',
 'manuscripts were proofread more carefully than printed books.', 'fewer copies of a manuscript were in circulation.', 'manuscript errors were usually corrected by the author in person.', 'printed books were sold to readers who could not read Latin.', 'printers were less educated than scribes.', NULL, 'B',
 '中："a manuscript mistake stayed local" 的原因就是抄本数量少，扩散范围有限。
EN: The contrast is local versus thousands of copies. The implied reason is simply the number of copies, not the skill of scribes or printers.',
 NULL, 'reading', NULL, NULL, '中级'),
(133, 4, 'verbal', 3, 'The appearance of errata lists early in printed books is presented as evidence that',
 'printers were required by law to disclose mistakes.', 'early printers feared mass reproduction of a single error.', 'readers preferred corrected editions to cheap ones.', 'manuscript culture had no mechanism for fixing errors.', 'the economics of printing favored short books.', NULL, 'B',
 '中：文中说"同代人清楚这一点（错一次会被复制上千份），所以勘误表很早就出现"，即对错误被批量复制的担忧。
EN: The contemporaries were acutely aware that a mistake could be reproduced in thousands of copies; errata lists are the institutional response to that risk.',
 NULL, 'reading', NULL, NULL, '进阶');

-- Quant：Quantitative Comparison 141-146
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(141, 5, 'quant', 1, 'x > 0
Quantity A: x² + 1
Quantity B: 2x',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'D',
 '中：x² + 1 − 2x = (x − 1)²，当 x = 1 时两栏相等（都为 2），当 x = 2 时 A = 5 > B = 4，两种情况都符合 x > 0，所以关系无法确定，选 D。
EN: The difference is (x - 1)², which is zero at x = 1 but positive elsewhere. A single counterexample in each direction forces choice D. A common mistake is to notice the perfect square and answer A.',
 NULL, 'quant_compare', NULL, NULL, '进阶'),
(142, 5, 'quant', 2, 'Quantity A: 3/8
Quantity B: 0.375',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'C',
 '中：3/8 = 0.375，两栏完全相等。
EN: Divide 3 by 8 to get 0.375; the quantities are identical.',
 NULL, 'quant_compare', NULL, NULL, '入门'),
(143, 5, 'quant', 3, 'Quantity A: The number of prime numbers between 10 and 20
Quantity B: 4',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'C',
 '中：10 到 20 之间的质数是 11、13、17、19，共 4 个，与 B 相等。
EN: The primes between 10 and 20 are 11, 13, 17 and 19, exactly four.',
 NULL, 'quant_compare', NULL, NULL, '入门'),
(144, 5, 'quant', 4, 'Quantity A: The area of a circle with radius 3
Quantity B: The area of a square with perimeter 24',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'B',
 '中：圆面积 9π ≈ 28.3；正方形边长 6，面积 36，所以 B 大。
EN: Circle area is 9π about 28.3; square side is 6, area 36. B is greater.',
 NULL, 'quant_compare', NULL, NULL, '中级'),
(145, 5, 'quant', 5, 'x < 0 < y
Quantity A: x + y
Quantity B: y − x',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'B',
 '中：B − A = (y − x) − (x + y) = −2x，因为 x < 0，所以 −2x > 0，B 恒大于 A。
EN: Subtract A from B: (y - x) - (x + y) = -2x, which is positive because x is negative. B is always greater.',
 NULL, 'quant_compare', NULL, NULL, '进阶'),
(146, 5, 'quant', 6, 'Quantity A: 20% of 60
Quantity B: 60% of 20',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'C',
 '中：两栏都是 12，相等。
EN: Both equal 12; multiplication is commutative.',
 NULL, 'quant_compare', NULL, NULL, '入门');

-- Quant：Problem Solving 151-156
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(151, 6, 'quant', 1, 'If 2(x − 3) = 14, what is the value of x?',
 '8', '9', '10', '11', '12', NULL, 'C',
 '中：两边除以 2 得 x − 3 = 7，所以 x = 10。
EN: Divide both sides by 2 to get x - 3 = 7, so x = 10.',
 NULL, 'quant_problem', NULL, NULL, '入门'),
(152, 6, 'quant', 2, 'A machine produces 240 parts in 4 hours. At the same constant rate, how many parts does it produce in 7 hours?',
 '400', '420', '440', '460', '480', NULL, 'B',
 '中：速率 = 240/4 = 60 件/小时，7 小时 = 420 件。
EN: The rate is 60 parts per hour; in 7 hours that is 420.',
 NULL, 'quant_problem', NULL, NULL, '入门'),
(153, 6, 'quant', 3, 'The average of five numbers is 12. If one number is removed, the average of the remaining four numbers is 11. What number was removed?',
 '14', '16', '18', '20', '22', NULL, 'B',
 '中：五个数总和 60，剩下四个总和 44，被拿掉的数是 16。
EN: Total is 5 x 12 = 60; the remaining four sum to 44; the removed number is 16.',
 NULL, 'quant_problem', NULL, NULL, '中级'),
(154, 6, 'quant', 4, 'A bag contains 3 red and 5 blue marbles. If two marbles are drawn without replacement, what is the probability that both are red?',
 '3/28', '3/20', '1/7', '5/28', '1/4', NULL, 'A',
 '中：(3/8)×(2/7) = 6/56 = 3/28。
EN: Multiply successive probabilities: 3/8 x 2/7 = 3/28.',
 NULL, 'quant_problem', NULL, NULL, '中级'),
(155, 6, 'quant', 5, 'A rectangle has perimeter 30 and area 50. What is the length of its diagonal?',
 '5√2', '5√5', '10', '2√30', '15', NULL, 'B',
 '中：长宽之和 15，乘积 50，对角线² = 15² − 2×50 = 125，对角线 = 5√5。
EN: With l + w = 15 and lw = 50, the diagonal squared is 225 - 100 = 125, so the diagonal is 5√5.',
 NULL, 'quant_problem', NULL, NULL, '进阶'),
(156, 6, 'quant', 6, 'How many positive integers less than 100 are divisible by 3 but not by 5?',
 '25', '26', '27', '28', '30', NULL, 'C',
 '中：3 的倍数共 33 个（3…99），其中同时被 15 整除的有 15、30、45、60、75、90 共 6 个，33 − 6 = 27。
EN: There are 33 multiples of 3 below 100 and 6 multiples of 15, so 27 qualify.',
 NULL, 'quant_problem', NULL, NULL, '中级');

-- Writing：Issue 题库 161-163（161 带满分范例）
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(161, 7, 'writing', 1, 'Issue 01 · 科研限制', NULL, NULL,
 'Governments should place few, if any, restrictions on scientific research and the development of new technologies.

Write a response in which you discuss the extent to which you agree or disagree with the recommendation and explain your reasoning. In developing and supporting your position, describe specific circumstances in which adopting the recommendation would or would not be advantageous.',
 'awa_issue',
 'AWA 6 分要求：立场明确、论证有深度（不只是罗列理由）、例证具体、段落之间有推进、语言准确且有变化。4 分是"论证尚可但展开不足"，3 分是"立场清楚但论证薄弱"。',
 '【6 分范例 · 约 260 词】

The claim that governments should place few restrictions on research is attractive because the costs of restriction are easy to name and the benefits are hard to see. A vaccine delayed by a safety review is a visible cost; a technology deployed before its risks are understood is a cost that arrives years later, spread across people who never chose it. That asymmetry, not any distrust of science, is why I would agree only in part.

Consider a case where restriction clearly helps. When researchers proposed releasing a gene drive that would spread through wild mosquito populations, the relevant question was not whether the science was sound but who would bear the consequences of being wrong. A gene drive does not stay in one laboratory; it reproduces. Here a slow, public approval process is not bureaucracy but the only way to make the people affected parties to the decision.

Yet most research does not have this structure, and treating it as if it did would be costly. The development of a new statistical method, a better battery chemistry, or a faster diagnostic involves no irreversible release into a shared environment. Where harms are reversible, small and confined to consenting participants, prior review mainly delays benefits: medicines reach patients later, and small laboratories cede the field to institutions that can afford compliance.

The sensible line, then, is not more or fewer restrictions in general but restrictions proportional to reversibility. Research whose errors can be undone deserves light oversight; research whose errors propagate deserves the opposite. A government that applies one rule to both is not being principled, only uniform.',
 '进阶'),
(162, 7, 'writing', 2, 'Issue 02 · 理解一个社会', NULL, NULL,
 'The best way to understand a society is to examine the everyday lives of ordinary people rather than the achievements of its leaders.

Write a response in which you discuss the extent to which you agree or disagree with the statement and explain your reasoning. In developing and supporting your position, consider ways in which the statement might or might not hold true.',
 'awa_issue',
 'AWA 6 分要求：立场明确、论证有层次、例证具体、能处理反方视角、语言准确。',
 NULL, '进阶'),
(163, 7, 'writing', 3, 'Issue 03 · 大学跨学科课程', NULL, NULL,
 'Universities should require all students to take courses outside their major field of study.

Write a response in which you discuss the extent to which you agree or disagree with the recommendation and explain your reasoning. In developing and supporting your position, describe specific circumstances in which adopting the recommendation would or would not be advantageous.',
 'awa_issue',
 'AWA 6 分要求：立场明确、论证有深度、例证具体、语言准确且有变化。',
 NULL, '中级');

-- Writing：Argument 题库 171-172（171 带满分范例）
INSERT INTO gre_question (id, section_id, module_type, sort_index, question_title, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(171, 8, 'writing', 1, 'Argument 01 · Bellavista 减税政策', NULL, NULL,
 'The following appeared in a memo from the mayor of the town of Bellavista.

Two years ago, our town adopted a policy of offering tax breaks to companies that relocate here. In that period, our unemployment rate fell from 8 percent to 5 percent and retail sales rose by 12 percent. Clearly, the tax-break policy has strengthened our economy.

Write a response in which you examine the stated and/or unstated assumptions of the argument. Be sure to explain how the argument depends on these assumptions and what the implications are for the argument if the assumptions prove unwarranted.',
 'awa_argument',
 'AWA Argument 6 分要求：准确识别论证缺口（相关不等于因果、遗漏替代解释、样本与时间范围问题）、逐条解释"如果假设不成立会怎样"、组织清晰。',
 '【6 分范例 · 约 250 词】

The memo reads two improvements as the product of one policy, but three questions have to be answered before that inference holds.

First, is the comparison sound? The memo compares unemployment and retail sales before and after the policy, but says nothing about the rest of the region or the country. If the surrounding counties also saw unemployment fall and sales rise, the tax breaks cannot be credited with either change; a national recovery would produce the same numbers. The argument needs a comparison with towns that did not adopt tax breaks, not only with its own past.

Second, who actually moved in? Tax breaks attract firms that expect to profit from them, which may mean firms that were already planning to relocate, or firms with low wage bills. If the new employers hired few local workers, or hired them part-time, the drop from 8 to 5 percent could reflect people leaving the labor force rather than people finding work. The memo treats a falling unemployment rate as unambiguously good news, which is exactly the kind of assumption that should be tested.

Third, what did the policy cost? Retail sales may have risen by 12 percent while the town gave up revenue that would otherwise have funded schools or roads. A policy that raises sales but leaves public services worse off has not strengthened the economy; it has moved activity from one column to another.

None of this proves the tax breaks failed. It proves the memo has not shown that they worked. Until the comparison group, the composition of new employment, and the fiscal cost are addressed, the recommendation rests on correlation dressed as cause.',
 '进阶'),
(172, 8, 'writing', 2, 'Argument 02 · 健身房与缺勤率', NULL, NULL,
 'The following appeared in a letter to the editor of a business magazine.

A recent study of 200 employees at a large firm found that those who exercised regularly reported fewer sick days than those who did not. Therefore, any company that wants to reduce absenteeism should provide free gym memberships to all employees.

Write a response in which you discuss what questions would need to be answered in order to decide whether the recommendation is likely to be correct. Be sure to explain how the answers to these questions would help to evaluate the recommendation.',
 'awa_argument',
 'AWA Argument 6 分要求：识别相关与因果混淆、样本偏差、替代解释，并解释每个问题如何影响结论。',
 NULL, '进阶');

-- ---------------------------------------------------------------------
-- 套题 1：GRE 全真模考 01（Verbal 2 个 section、Quant 2 个 section、Writing 2 篇）
-- ---------------------------------------------------------------------
INSERT INTO gre_suite (id, title, description, difficulty, total_minutes, source, create_time) VALUES
(1, 'GRE 全真模考 01 · 基础卷', 'Analytical Writing 2 篇 + Verbal 2 个 section + Quant 2 个 section，按真实考试顺序作答，最后一次性提交判分。', '中级', 130, 'seed', NOW());

INSERT INTO gre_suite_section (id, suite_id, module_type, sort_index, title, content_text, hint, limit_second) VALUES
(1, 1, 'writing', 1, 'Analytical Writing · Issue + Argument', NULL, '两篇各 30 分钟：先花 3 分钟列提纲，再写正文', 3600),
(2, 1, 'verbal', 1, 'Verbal · Section 1', NULL, '填空与六选一，先定逻辑方向再选词', 1080),
(3, 1, 'verbal', 2, 'Verbal · Section 2 · Reading', 'Standardized time is usually presented as a technical achievement, the natural outcome of railways and telegraphs. Yet the adoption of time zones in the United States was as much a political and commercial negotiation as a scientific one. Railways needed common schedules, but local observatories, city governments and farmers each had reasons to resist. What finally carried the day was not superior accuracy, since several proposed systems were equally precise, but the willingness of a few large carriers to adopt a single standard and the resulting pressure on everyone who connected to their networks. Historians who describe the change as inevitable therefore mistake a contingent settlement for a foregone conclusion. The same technical need could have produced a patchwork of regional standards, and indeed it did, briefly, in parts of Europe.', '这一篇考"作者如何反驳必然论"，别把细节当主旨', 1080),
(4, 1, 'quant', 1, 'Quant · Section 1', NULL, '数量比较优先化简；不能确定时选 D', 1260),
(5, 1, 'quant', 2, 'Quant · Section 2', NULL, '问题求解注意单位与"小于 100"这类边界条件', 1260);

-- 套题 1 的题目：Writing
INSERT INTO gre_suite_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(201, 1, 'writing', 1, 'Issue · 科研限制', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 'Governments should place few, if any, restrictions on scientific research and the development of new technologies.

Write a response in which you discuss the extent to which you agree or disagree with the recommendation and explain your reasoning. In developing and supporting your position, describe specific circumstances in which adopting the recommendation would or would not be advantageous.',
 'awa_issue',
 '6 分：立场明确、论证有深度、例证具体、段落有推进。4 分：论证尚可但展开不足。3 分：立场清楚但论证薄弱。',
 '【6 分范例 · 约 260 词】

The claim that governments should place few restrictions on research is attractive because the costs of restriction are easy to name and the benefits are hard to see. A vaccine delayed by a safety review is a visible cost; a technology deployed before its risks are understood is a cost that arrives years later, spread across people who never chose it. That asymmetry, not any distrust of science, is why I would agree only in part.

Consider a case where restriction clearly helps. When researchers proposed releasing a gene drive that would spread through wild mosquito populations, the relevant question was not whether the science was sound but who would bear the consequences of being wrong. A gene drive does not stay in one laboratory; it reproduces. Here a slow, public approval process is not bureaucracy but the only way to make the people affected parties to the decision.

Yet most research does not have this structure, and treating it as if it did would be costly. The development of a new statistical method, a better battery chemistry, or a faster diagnostic involves no irreversible release into a shared environment. Where harms are reversible, small and confined to consenting participants, prior review mainly delays benefits: medicines reach patients later, and small laboratories cede the field to institutions that can afford compliance.

The sensible line, then, is not more or fewer restrictions in general but restrictions proportional to reversibility. Research whose errors can be undone deserves light oversight; research whose errors propagate deserves the opposite. A government that applies one rule to both is not being principled, only uniform.',
 '进阶'),
(202, 1, 'writing', 2, 'Argument · Bellavista 减税政策', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 'The following appeared in a memo from the mayor of the town of Bellavista.

Two years ago, our town adopted a policy of offering tax breaks to companies that relocate here. In that period, our unemployment rate fell from 8 percent to 5 percent and retail sales rose by 12 percent. Clearly, the tax-break policy has strengthened our economy.

Write a response in which you examine the stated and/or unstated assumptions of the argument. Be sure to explain how the argument depends on these assumptions and what the implications are for the argument if the assumptions prove unwarranted.',
 'awa_argument',
 '6 分：准确识别相关与因果混淆、遗漏替代解释，逐条说明假设不成立会怎样。4 分：识别部分缺口但展开不足。',
 '【6 分范例 · 约 250 词】

The memo reads two improvements as the product of one policy, but three questions have to be answered before that inference holds.

First, is the comparison sound? The memo compares unemployment and retail sales before and after the policy, but says nothing about the rest of the region or the country. If the surrounding counties also saw unemployment fall and sales rise, the tax breaks cannot be credited with either change; a national recovery would produce the same numbers. The argument needs a comparison with towns that did not adopt tax breaks, not only with its own past.

Second, who actually moved in? Tax breaks attract firms that expect to profit from them, which may mean firms that were already planning to relocate, or firms with low wage bills. If the new employers hired few local workers, or hired them part-time, the drop from 8 to 5 percent could reflect people leaving the labor force rather than people finding work. The memo treats a falling unemployment rate as unambiguously good news, which is exactly the kind of assumption that should be tested.

Third, what did the policy cost? Retail sales may have risen by 12 percent while the town gave up revenue that would otherwise have funded schools or roads. A policy that raises sales but leaves public services worse off has not strengthened the economy; it has moved activity from one column to another.

None of this proves the tax breaks failed. It proves the memo has not shown that they worked. Until the comparison group, the composition of new employment, and the fiscal cost are addressed, the recommendation rests on correlation dressed as cause.',
 '进阶');

-- 套题 1 的题目：Verbal Section 1（填空 + 六选一）
INSERT INTO gre_suite_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(203, 2, 'verbal', 1, 'The committee''s early drafts were so ______ that reviewers could not tell which recommendations the town was actually prepared to fund.',
 'explicit', 'guarded', 'lengthy', 'technical', 'sincere', NULL, 'B',
 '中：审阅者看不出真正要资助哪条建议，说明草稿含糊保留（guarded）。
EN: Reviewers cannot tell what is really being proposed, so the drafts are guarded rather than explicit.',
 NULL, 'text_completion', NULL, NULL, '中级'),
(204, 2, 'verbal', 2, 'Although the evidence for the new treatment is ______, the demand for it is already so strong that regulators have had to issue guidance on off-label use.',
 'scant', 'conclusive', 'contested but growing', 'widely publicized', 'expensive to produce', NULL, 'A',
 '中：although 让步 + demand 已经很强，说明证据其实"不足"（scant）。
EN: The although contrast sets weak evidence against strong demand, so scant is the fit.',
 NULL, 'text_completion', NULL, NULL, '中级'),
(205, 2, 'verbal', 3, 'The reviewer called the biography ______: it follows every documented incident but never asks why any of them mattered.',
 'exhaustive', 'pedantic', 'evasive', 'partisan', 'lyrical', 'searching', 'A',
 '中：冒号后是"每一件事都写到，但从不问意义"，这是详尽的（exhaustive），批评点在"只堆材料"。
EN: Following every documented incident points to exhaustive; the flaw is detail without argument. Pedantic is close but describes manner, not completeness.',
 NULL, 'sentence_equivalence', NULL, NULL, '中级'),
(206, 2, 'verbal', 4, 'Rather than ______ the criticism, the author engages it directly in the introduction and answers each point in turn.',
 'anticipating', 'deflecting', 'rehearsing', 'inviting', 'amplifying', 'documenting', 'B',
 '中：rather than 后面要与 engages it directly 相反，即"回避"（deflecting）。
EN: The contrast is with engaging directly, so the blank means avoiding, i.e. deflecting.',
 NULL, 'sentence_equivalence', NULL, NULL, '中级');

-- 套题 1 的题目：Verbal Section 2（Reading，对应 section 3 的正文）
INSERT INTO gre_suite_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(207, 3, 'verbal', 1, 'The primary purpose of the passage is to',
 'trace the technical development of timekeeping instruments.', 'challenge the view that standardized time was an inevitable technical outcome.', 'argue that local observatories were more accurate than railways.', 'describe how European cities resisted time zones.', 'explain why farmers supported standard time.', NULL, 'B',
 '中：文章反复强调"不只是技术"，末句直接说把变化视为必然的历史学家把"偶然的妥协"当成了"注定的结果"。
EN: The final sentence states the author''s target: historians who call the change inevitable mistake a contingent settlement for a foregone conclusion.',
 NULL, 'reading', NULL, NULL, '中级'),
(208, 3, 'verbal', 2, 'The passage suggests that smaller railway companies adopted a single standard mainly because',
 'they were convinced by the accuracy of the observatories.', 'they were connected to networks operated by the largest carriers.', 'the federal government required them to do so.', 'their customers demanded uniform local time.', 'they had invested in new telegraph equipment.', NULL, 'B',
 '中：carried the day 的原因是"少数大公司采用单一时制，随后给所有接入其网络的人带来压力"。
EN: The passage names the pressure on everyone connected to those networks as the decisive force.',
 NULL, 'reading', NULL, NULL, '进阶'),
(209, 3, 'verbal', 3, 'The statement that the same technical need could have produced a patchwork of regional standards serves to',
 'concede that the adopted system was technically inferior.', 'show that the outcome was one of several possible results.', 'argue that Europe adopted standards earlier than the United States.', 'explain why observatories opposed time zones.', 'suggest that railways did not need common schedules.', NULL, 'B',
 '中：这句话用于证明"技术需要"也可以有别的结果，从而否定必然论。
EN: The sentence illustrates an alternative outcome, which undercuts the inevitability claim.',
 NULL, 'reading', NULL, NULL, '进阶'),
(210, 3, 'verbal', 4, 'The author''s attitude toward accounts that treat standard time as a purely technical achievement is best described as',
 'fully supportive.', 'mildly amused.', 'skeptical.', 'openly hostile.', 'indifferent.', NULL, 'C',
 '中：作者说"通常被描述为技术成就"，随后用 yet 展开反驳，并批评必然论——属于怀疑但克制的语气。
EN: The author calls those accounts inaccurate in emphasis, not absurd; skeptical is the measured choice.',
 NULL, 'reading', NULL, NULL, '进阶');

-- 套题 1 的题目：Quant Section 1（数量比较 2 + 问题求解 2）
INSERT INTO gre_suite_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(211, 4, 'quant', 1, '0 < x < 1
Quantity A: x²
Quantity B: x',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'B',
 '中：0 < x < 1 时 x² < x，例如 x = 0.5 得 0.25 < 0.5，所以 B 大。
EN: For a positive fraction, squaring makes it smaller, so B is greater.',
 NULL, 'quant_compare', NULL, NULL, '入门'),
(212, 4, 'quant', 2, 'Quantity A: 15% of 80
Quantity B: 80% of 15',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'C',
 '中：两栏都是 12。
EN: Both equal 12.',
 NULL, 'quant_compare', NULL, NULL, '入门'),
(213, 4, 'quant', 3, 'If 5 more than twice a number equals 27, what is the number?',
 '9', '10', '11', '12', '13', NULL, 'C',
 '中：2n + 5 = 27 → n = 11。
EN: Solve 2n + 5 = 27 to get n = 11.',
 NULL, 'quant_problem', NULL, NULL, '入门'),
(214, 4, 'quant', 4, 'A car travels 150 miles at 50 miles per hour and returns along the same route at 60 miles per hour. What is the average speed for the round trip?',
 '54', '54.5', '55', '55.5', '56', NULL, 'B',
 '中：平均速度 = 总路程/总时间 = 300 / (150/50 + 150/60) = 300 / 5.5 ≈ 54.5。
EN: Total distance 300 miles; total time 3 + 2.5 = 5.5 hours; average speed is about 54.5, not the arithmetic mean 55.',
 NULL, 'quant_problem', NULL, NULL, '进阶');

-- 套题 1 的题目：Quant Section 2
INSERT INTO gre_suite_question (id, section_id, module_type, sort_index, question_title, option_a, option_b, option_c, option_d, option_e, option_f, answer, analysis, prompt, task_type, score_standard, sample_answer, difficulty) VALUES
(215, 5, 'quant', 1, 'n > 1 is an integer
Quantity A: The number of distinct positive divisors of 12
Quantity B: 5',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'A',
 '中：12 的正因数有 1、2、3、4、6、12，共 6 个，所以 A 大。
EN: 12 has six positive divisors, so A exceeds B. Note that the condition on n is a red herring.',
 NULL, 'quant_compare', NULL, NULL, '中级'),
(216, 5, 'quant', 2, 'Quantity A: √50
Quantity B: 7',
 'Quantity A is greater.', 'Quantity B is greater.', 'The two quantities are equal.', 'The relationship cannot be determined from the information given.', NULL, NULL, 'A',
 '中：7² = 49 < 50，所以 √50 > 7。
EN: Since 49 is less than 50, the square root of 50 is slightly greater than 7.',
 NULL, 'quant_compare', NULL, NULL, '入门'),
(217, 5, 'quant', 3, 'Two fair dice are rolled. What is the probability that the sum is 7?',
 '1/12', '1/9', '1/6', '5/36', '1/4', NULL, 'C',
 '中：和为 7 的组合有 (1,6)(2,5)(3,4)(4,3)(5,2)(6,1) 共 6 种，6/36 = 1/6。
EN: Six of the 36 outcomes sum to 7, giving 1/6.',
 NULL, 'quant_problem', NULL, NULL, '中级'),
(218, 5, 'quant', 4, 'If 20 percent of x is 12, what is 30 percent of x?',
 '15', '18', '20', '24', '30', NULL, 'B',
 '中：0.2x = 12 → x = 60；0.3x = 18。
EN: x equals 60, so 30 percent of x is 18.',
 NULL, 'quant_problem', NULL, NULL, '入门');


-- ============================================================
-- 以下为写作满分范例补全 + 换行修复（与 db-gre-awa-samples.sql 内容一致，
-- 目的是让本文件可以整体重复执行而不丢范例）
-- ============================================================
-- ============================================================
-- GRE AWA 满分范例补全 + 题干/解析换行修复（可重复执行）
-- 用途：gre_question 里 5 道写作题全部补上 6 分档英文范例，
--       并把早先误存成字面量 "\n" 的换行符改成真实换行，
--       避免前端显示出一堆 "\n" 而不是分段。
-- 执行：mysql -u root -p toefl_db < db-gre-awa-samples.sql
-- ============================================================

USE toefl_db;

-- ---------- 1. 换行修复：把字面量 \n 换成真实换行 ----------
UPDATE gre_question
SET prompt = REPLACE(prompt, '\\n', CHAR(10))
WHERE prompt LIKE '%\\n%';

UPDATE gre_question
SET analysis = REPLACE(analysis, '\\n', CHAR(10))
WHERE analysis LIKE '%\\n%';

UPDATE gre_suite_question
SET prompt = REPLACE(prompt, '\\n', CHAR(10))
WHERE prompt LIKE '%\\n%';

UPDATE gre_suite_question
SET analysis = REPLACE(analysis, '\\n', CHAR(10))
WHERE analysis LIKE '%\\n%';

-- ---------- 2. 补全 162 / 163 / 172 的 6 分档范例 ----------
-- 162 · Issue：了解一个社会最好的方式是看普通人的日常生活，而不是领导者的成就
UPDATE gre_question
SET sample_answer = 'I largely agree that a society is best understood through the everyday lives of ordinary people, though I would not dismiss the achievements of its leaders entirely. Daily routines reveal the structures that shape most lives: how people eat, travel, work, raise children, and care for the elderly. These patterns expose the distribution of time, money, and dignity far more honestly than monuments do.

Consider housing. A country may celebrate stunning architecture while millions spend hours commuting because affordable flats are scarce. Only by watching ordinary mornings—crowded buses, shared kitchens, early markets—does the true cost of urban policy become visible. Similarly, the position of women is better measured by who does the unpaid work at home than by how many women hold cabinet posts.

Yet leaders'' achievements are not irrelevant. Public investments in sanitation, schooling, or vaccination change ordinary life precisely because someone made a decision. A railway built by a government alters what a village can sell at market. The difficulty is that official accounts tend to describe intentions, while daily life records results.

I would also refine the claim: the "ordinary" observer must be careful. Tourist impressions and comfortable anecdotes can be as misleading as official propaganda. Reliable understanding requires data—household surveys, time-use studies, employment records—combined with patient observation.

In sum, everyday life is the more reliable starting point, but leadership matters as a cause whose effects appear, or fail to appear, in kitchens, buses, and clinics. The best reading of a society uses both, and trusts the second only where the first confirms it.'
WHERE id = 162;

-- 163 · Issue：大学是否应该要求所有学生修本专业以外的课程
UPDATE gre_question
SET sample_answer = 'This recommendation is broadly reasonable, but "all students" and "require" need qualification. A university is not only a training centre; it is the place where citizens learn to think across boundaries. Requiring some breadth protects students from the narrowness of a single department and from the assumption that their field''s methods are the only legitimate ones.

The advantages are concrete. An engineering student who takes a course in ethics will meet problems her technical training cannot solve, such as how to weigh privacy against safety. A literature student who studies statistics learns to interrogate claims rather than merely interpret them. Cross-disciplinary contact also reduces the tribalism that lets experts dismiss criticism from outside.

Circumstances where the requirement hurts are equally real. Students who must work part-time, commute, or support families have little room in a schedule; mandatory courses can push them past the point of passing their major. In tightly sequenced degrees such as nursing or architecture, extra requirements can delay graduation and increase debt. And a compulsory "appreciation" course that a hundred students take in a lecture hall may deliver no breadth at all.

A better policy would require a modest number of credits with meaningful choice, protect time-to-degree, and let departments approve options that genuinely engage other disciplines. Students should be able to substitute a rigorous alternative for a shallow survey.

So I support the recommendation''s aim, with the design constraint that breadth must be real, affordable, and flexible rather than a bureaucratic box. Under those conditions the policy serves both the individual and the society that depends on informed judgment.'
WHERE id = 163;

-- 172 · Argument：员工锻炼与病假，作者据此建议公司提供免费健身房会员
UPDATE gre_question
SET sample_answer = 'The letter moves quickly from a correlation to a policy, and several questions must be answered before that jump is safe.

First, is the relationship causal, or do healthier people simply exercise? If employees who already sleep well, eat well, and face less stress are both more likely to exercise and less likely to fall ill, the gym membership may change nothing. The study''s design cannot separate these explanations. We would need a trial that assigns memberships randomly and then compares sick days.

Second, how were sick days measured? Self-reported absence is unreliable: employees who fear appearing unreliable may under-report, and those whose exercise programme is company-sponsored may feel pressure to appear healthy. Payroll or attendance records over several years would settle this.

Third, what is the sample? Two hundred employees at one large firm tell us little about a small company, a warehouse, or a firm where shifts make gym visits impossible. Questions of industry, job type, and schedule must be answered before generalising.

Fourth, does the recommendation''s mechanism work? A free membership is not the same as attendance. We should ask how many employees would actually use it, whether the gym is near the workplace, and whether time is available. If usage is low, the benefit is a gesture.

Finally, is the intervention worth its cost and risk? A company might reduce absence more cheaply through predictable schedules, paid sick leave that does not punish honesty, or on-site exercise space. The letter should compare options rather than assume the first plausible one.

None of these answers is available, and each of them changes the conclusion. Until they are, the advice to every company is premature.'
WHERE id = 172;

-- ---------- 3. 自检 ----------
SELECT id, task_type, CHAR_LENGTH(sample_answer) AS sample_len
FROM gre_question
WHERE module_type = 'writing'
ORDER BY id;
