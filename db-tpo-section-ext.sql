-- 为 tpo_section 增加「篇章标题」和「中文译文」两列
-- 目的：
--   1) 列表页不再用正文前 40 字当标题（现在主页显示的是 "The origins of theater are a ma..." 这种半截句子）
--   2) 阅读/听力提交答案后，可以点按钮切换「中文译文」
-- 说明：content_translation 为空时，前端按钮会提示「本篇暂无译文」，不报错。

ALTER TABLE tpo_section
    ADD COLUMN title VARCHAR(120) NULL COMMENT '篇章标题（中英结合，列表页展示）' AFTER type,
    ADD COLUMN content_translation LONGTEXT NULL COMMENT '正文/听力原文的中文译文（可空）' AFTER content_text;

-- 回填现有 13 个篇章的标题（按各篇实际内容人工命名，不再截取正文）
UPDATE tpo_section SET title = '阅读 Passage 1 · 戏剧的起源'            WHERE id = 1;
UPDATE tpo_section SET title = '口语 Task 1–2 · 独立口语与综合口语'      WHERE id = 4;
UPDATE tpo_section SET title = '写作 Task 1–2 · 综合写作与独立写作'      WHERE id = 5;
UPDATE tpo_section SET title = '阅读 Passage 2 · 农业的出现'              WHERE id = 6;
UPDATE tpo_section SET title = '听力 Conversation 1 · 图书馆借书'          WHERE id = 7;
UPDATE tpo_section SET title = '阅读 Passage 3 · 城市热岛效应'            WHERE id = 8;
UPDATE tpo_section SET title = '听力 Conversation 2 · 校园兼职面试'        WHERE id = 9;
UPDATE tpo_section SET title = '口语 Set 2 · 校园话题'                    WHERE id = 10;
UPDATE tpo_section SET title = '写作 Set 2 · 垃圾分类与大学课程'          WHERE id = 11;
UPDATE tpo_section SET title = '阅读 Passage 4 · 活字印刷术'              WHERE id = 12;
UPDATE tpo_section SET title = '阅读 Passage 5 · 珊瑚礁'                  WHERE id = 13;
UPDATE tpo_section SET title = '听力 Conversation 3 · 论文咨询'            WHERE id = 14;
UPDATE tpo_section SET title = '阅读 Passage 6 · 城市夜间照明（示例）'    WHERE id = 15;

-- 示例篇（section 15）自带中文译文，用来验证「提交后切换译文」这条链路
UPDATE tpo_section SET content_translation = '在人类历史的大部分时间里，夜晚是真的黑。直到最近一百年，人造光才便宜到足以在日落后照亮街道、住宅和工作场所。这一变化带来了显而易见的好处：更安全的道路、更长的工作时间，以及更热闹的公共空间。然而研究者开始注意到一种不那么明显的代价。许多昆虫依靠微弱的天体线索导航，而一盏明亮的街灯可能把它们卷进无谓的绕圈，直到力竭而死。夜间迁徙的鸟类面临类似的问题：它们绕着被照亮的塔楼盘旋，而不是沿着自己的路线飞行。

有些城市的应对办法是：在车流稀少的时段调暗路灯，或者选用吸引昆虫较少的波长。这类措施很少能省下多少钱，但确实减少了生态干扰，而且若干研究报告说，居民几乎察觉不到可见度上的差别。'
WHERE id = 15;

-- 校验
SELECT id, type, title, CHAR_LENGTH(content_translation) AS tr_len FROM tpo_section ORDER BY id;
