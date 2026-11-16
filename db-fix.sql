-- ============================================================
-- toefl_db 数据修复脚本（2026-11-13）
-- 目标库：toefl_db     执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 toefl_db < db-fix.sql
-- 说明：只做“修复 + 补示例数据”，不删改原有题库正文。
-- ============================================================

USE toefl_db;

-- ------------------------------------------------------------
-- 修复 1：suite_section 指向了不存在的篇章
--   原数据：每个套题都关联 section_id = 1/2/3/4
--   实际 tpo_section 里只有 1(reading)、4(speaking)、5(writing)、6(reading)、7(listening)
--   2 和 3 根本不存在 → 套题详情查不到篇章、整套模考对应模块空白
-- ------------------------------------------------------------
UPDATE suite_section SET section_id = 7 WHERE type = 'listening' AND section_id = 2;
UPDATE suite_section SET section_id = 4 WHERE type = 'speaking'  AND section_id = 3;
UPDATE suite_section SET section_id = 5 WHERE type = 'writing'   AND section_id = 4;

-- ------------------------------------------------------------
-- 修复 2：听力篇章引用的音频文件不存在
--   原值 /audio/tpo01-conversation1.mp3 在 frontend/public/audio 下没有对应文件，
--   页面点播放会 404。改成仓库里真实存在的 cave.mp3。
-- ------------------------------------------------------------
UPDATE tpo_section
   SET audio_url = '/audio/cave.mp3'
 WHERE id = 7 AND audio_url = '/audio/tpo01-conversation1.mp3';

-- 为保证脚本可重复执行，先清掉本脚本自己插入过的示例题
-- （用 analysis 前缀 "示例题。" 精确识别，不会误删真实题目）
DELETE FROM tpo_question WHERE section_id IN (4,5,7) AND analysis LIKE '示例题。%';

-- ------------------------------------------------------------
-- 补数据：听力篇章（section 7）没有题目 → 补 3 道单选题
--   题目依据该篇章自身的内容摘要（学生找历史论文参考书、不能外借只能在馆内阅读复印、
--   管理员告知阅览室开放时间），答案均可由原文推出，属于示例题，可随时替换为真实 TPO 题。
-- ------------------------------------------------------------
INSERT INTO tpo_question
  (section_id, module_type, question_title, option_a, option_b, option_c, option_d,
   answer, analysis, limit_second, create_time)
VALUES
  (7, 'listening',
   'Why does the student go to the library?',
   'To return an overdue book',
   'To borrow a reference book for his history paper',
   'To reserve a study room for the weekend',
   'To ask about a part-time job in the library',
   'B',
   '示例题。原文说学生想借一本历史论文用的参考书（reference book for his history paper）。',
   0, NOW()),

  (7, 'listening',
   'What does the librarian tell the student about the book he wants?',
   'It can be checked out for one week',
   'It is already lost and cannot be found',
   'It cannot be checked out, but he can read it and copy pages in the library',
   'It is available only through the online database',
   'C',
   '示例题。原文明确：书不能外借（cannot be checked out），但可以在馆内阅读并复印。',
   0, NOW()),

  (7, 'listening',
   'What information does the librarian give the student at the end?',
   'The opening time of the reading room',
   'The phone number of his history professor',
   'The price of a new copy of the book',
   'The deadline for his history paper',
   'A',
   '示例题。原文最后一句：管理员告诉他阅览室的开放时间。',
   0, NOW());

-- ------------------------------------------------------------
-- 补数据：口语篇章（section 4）没有题目 → 补 2 道口语任务
-- ------------------------------------------------------------
INSERT INTO tpo_question
  (section_id, module_type, question_title, prompt, answer, analysis,
   limit_second, score_standard, create_time)
VALUES
  (4, 'speaking',
   'Task 1 - Independent Speaking: Study alone or in a group?',
   'Some students prefer to study alone, while others prefer to study in groups. Which do you prefer? Use specific reasons and examples to support your answer.',
   NULL,
   '示例题。建议结构：观点(1句) → 理由1 + 例子 → 理由2 + 例子 → 结论(1句)。准备15秒，作答45秒。',
   45,
   'Delivery 清晰流畅；Language Use 语法与词汇；Topic Development 论点展开与细节。',
   NOW()),

  (4, 'speaking',
   'Task 2 - Integrated Speaking: Campus library hours',
   'The reading passage proposes changing the library opening hours. Listen to the conversation and then summarize the opinion of the student and the reasons he gives for it.',
   NULL,
   '示例题。Integrated 任务先读再听：先概括阅读中的提议，再复述学生态度与两条理由，不需要加入自己的观点。准备20秒，作答60秒。',
   60,
   '内容完整性（覆盖阅读提议 + 听力两条理由）；流利度；语言准确性。',
   NOW());

-- ------------------------------------------------------------
-- 补数据：写作篇章（section 5）没有题目 → 补 2 道写作任务
-- ------------------------------------------------------------
INSERT INTO tpo_question
  (section_id, module_type, question_title, prompt, answer, analysis,
   limit_second, score_standard, create_time)
VALUES
  (5, 'writing',
   'Integrated Writing: Online courses',
   'Read the passage about online courses and listen to the lecture. Summarize the points made in the lecture and explain how they cast doubt on the points made in the reading. 150-225 words, 20 minutes.',
   NULL,
   '示例题。Integrated Writing 不写自己的观点，重点是准确转述听力如何反驳阅读的三点。建议 4 段：引言 + 3 组对应反驳。',
   1200,
   '内容准确（不遗漏听力要点、不与阅读混淆）；组织连贯；语言准确多样。',
   NOW()),

  (5, 'writing',
   'Independent Writing: Teamwork vs working alone',
   'Do you agree or disagree with the following statement? It is better to work in a team than to work alone. Use specific reasons and examples to support your answer. 300+ words, 30 minutes.',
   NULL,
   '示例题。独立写作建议 5 段：引言表明立场 → 理由1 + 具体例子 → 理由2 + 具体例子 → 让步段(可选) → 结论。',
   1800,
   '论点明确；论证充分有细节；结构清晰；句式与词汇多样；语法拼写准确。',
   NOW());

-- ------------------------------------------------------------
-- 校验：执行后应满足
--   1) suite_section 的 section_id 全部存在于 tpo_section
--   2) 四个模块的篇章都有题目
-- ------------------------------------------------------------
-- SELECT s.suite_id, s.type, s.section_id, t.type AS real_type
--   FROM suite_section s LEFT JOIN tpo_section t ON t.id = s.section_id;
-- SELECT type, COUNT(*) FROM tpo_section GROUP BY type;
-- SELECT section_id, COUNT(*) FROM tpo_question GROUP BY section_id;
