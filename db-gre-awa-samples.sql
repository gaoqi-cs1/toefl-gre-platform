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
