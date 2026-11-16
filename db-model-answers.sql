-- =====================================================================
-- 1) tpo_question / full_question 新增 sample_answer（口语、写作满分范例）
-- 2) 给 8 道口语写作题写入满分范例
-- 3) 顺手补齐 116-119 的题面（原来 prompt 是空的）
-- 4) 把范例同步进已经存在的套题（full_question）
--
-- 执行：
--   $env:MYSQL_PWD='123456'
--   Get-Content -Raw db-model-answers.sql | mysql -uroot --default-character-set=utf8mb4 toefl_db
-- 可重复执行（ALTER 用 information_schema 判断，不会报 duplicate column）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------- 加列（已存在就跳过） ----------
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'toefl_db' AND TABLE_NAME = 'tpo_question' AND COLUMN_NAME = 'sample_answer');
SET @s1 := IF(@c1 = 0, 'ALTER TABLE tpo_question ADD COLUMN sample_answer LONGTEXT NULL COMMENT ''口语/写作满分范例''', 'SELECT 1');
PREPARE p1 FROM @s1; EXECUTE p1; DEALLOCATE PREPARE p1;

SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'toefl_db' AND TABLE_NAME = 'full_question' AND COLUMN_NAME = 'sample_answer');
SET @s2 := IF(@c2 = 0, 'ALTER TABLE full_question ADD COLUMN sample_answer LONGTEXT NULL COMMENT ''口语/写作满分范例''', 'SELECT 1');
PREPARE p2 FROM @s2; EXECUTE p2; DEALLOCATE PREPARE p2;

-- ---------- 补齐 116-119 的题面 ----------
UPDATE tpo_question SET prompt = 'Some students take a year off between high school and university to work, travel, or prepare for exams. Do you think taking a gap year is a good idea? Give specific reasons and examples. Preparation 15s / Response 45s.'
WHERE id = 116;

UPDATE tpo_question SET prompt = 'The university plans to keep the main library open twenty-four hours a day during final exams. Read the announcement, then listen to a student response, and summarize the student opinion and the reasons he gives. Preparation 20s / Response 60s.'
WHERE id = 117;

UPDATE tpo_question SET prompt = 'Read a passage arguing that cities should ban single-use plastic bags, then listen to a lecture that challenges the passage. Write a response summarizing the lecture points and explaining how they cast doubt on the reading. 150-225 words, 20 minutes.'
WHERE id = 118;

UPDATE tpo_question SET prompt = 'Do you agree or disagree with the following statement? Universities should require every student to take a course in public speaking. Use specific reasons and examples to support your answer. 300+ words, 30 minutes.'
WHERE id = 119;

-- =====================================================================
-- 口语满分范例（Task 1 独立 / Task 2 综合 各约 110-130 词，45-60 秒口述量）
-- =====================================================================

-- 69 独立口语：study alone or in a group
UPDATE tpo_question SET sample_answer = 'I prefer studying in a group, mainly because explaining something out loud exposes what I only think I understand. Last semester four of us met twice a week for a statistics course. I was sure I understood confidence intervals until I tried to explain one to a classmate and realized I could not say what the ninety-five percent actually refers to. That ten-minute conversation saved me from losing points on the exam. A group also keeps me honest about time: when three people are waiting for me at the library at seven, I do not postpone studying. I still read alone first, so that I bring questions to the discussion instead of simply listening.'
WHERE id = 69;

-- 70 综合口语：campus library hours
UPDATE tpo_question SET sample_answer = 'The reading proposes changing the library opening hours, and the student in the conversation thinks the change is a waste of the library budget. He gives two reasons. First, he says the building is almost empty late at night. When he walked past the library at midnight last semester, most desks were unused, so keeping it open around the clock would mostly pay for empty rooms. Second, the money for extra night staff has to come from somewhere, and the library has already cancelled several journal subscriptions this year. He adds that what students really need is more group study rooms, because the ones that exist are booked weeks in advance.'
WHERE id = 70;

-- 116 独立口语：gap year
UPDATE tpo_question SET sample_answer = 'I think a gap year is a good idea if the student has a plan. My cousin took a year off before university and worked at a dental clinic. She saved enough money to cover her first year of tuition, and more importantly she saw what the job actually involves: cleaning instruments, talking to nervous patients, keeping records. Halfway through the year she realized she wanted to study public health rather than biology, which saved her from choosing the wrong major. A year without a plan can become twelve months of sleeping late, so I would only recommend it when the student can name a goal, whether that is saving money, improving a language, or testing a career.'
WHERE id = 116;

-- 117 综合口语：24 小时图书馆
UPDATE tpo_question SET sample_answer = 'The announcement says the main library will stay open twenty-four hours a day during final exams, with extra security and cleaning paid from the library budget. The student disagree with the plan for two reasons. First, he argues that almost nobody studies at four in the morning. When he passed the library at midnight last term, most desks were empty, so the university would be paying staff to watch an empty building. Second, that money has to come from somewhere, and the library has already cancelled several journal subscriptions this year. What students actually need, he says, is more group study rooms, because the current ones are reserved weeks in advance.'
WHERE id = 117;

-- =====================================================================
-- 写作满分范例（Integrated 约 180-220 词 / Independent 约 300-340 词）
-- =====================================================================

-- 71 综合写作：online courses
UPDATE tpo_question SET sample_answer = 'The reading argues that online courses benefit students because they are cheaper, more flexible, and more widely available. The lecture disputes each of these claims.
First, the reading says online study saves money. The lecturer responds that completion rates in online courses are far lower than in face-to-face classes, and when students withdraw, the tuition they paid is simply lost. Institutions must also hire more advisors and technical staff to keep students enrolled, so the real cost per completed course is higher, not lower.
Second, the reading presents flexibility as an advantage. The professor argues that flexibility is exactly the problem. Students who never enter a classroom lose the structure and the weekly deadlines that keep them working, and they receive very little feedback on their writing. Without that pressure, many students postpone the work until the semester is almost over.
Third, the reading claims that an online degree is valued the same way as a traditional one. The lecture points out that many employers and graduate programs still treat online credentials with suspicion, so graduates often need extra certificates or projects to prove what they know.
Taken together, the lecture suggests that the reading describes the convenience of online courses while ignoring their hidden costs and weaker outcomes.'
WHERE id = 71;

-- 72 独立写作：teamwork vs alone
UPDATE tpo_question SET sample_answer = 'Nobody who has watched a group project collapse in its final week would call teamwork effortless, yet I still believe that for most serious work a team outperforms an individual. Two reasons matter most: divided expertise and early correction of mistakes.
First, complex problems rarely fit one persons knowledge. In my undergraduate software course, our five-person team had six weeks to build a booking system. One member owned the database, another the interface, and I wrote the server logic. Working alone, I would have spent three weeks learning what my teammate already knew, and the result would have been shallow. Divided, each of us went deeper into one area, and the product was better than anything one of us could have produced in the same time.
Second, a team catches errors while they are still cheap to fix. When I work alone, a wrong assumption can survive for weeks because nothing contradicts it. In a group, somebody usually notices during the first meeting. That friction is uncomfortable, but it is precisely what prevents an expensive rewrite later.
I admit that teams start slowly, and that one strong writer or designer may do better alone on a small, well-defined task. Collaboration also fails when roles are unclear or when one member does all the thinking. Those failures, however, come from poor organization rather than from teamwork itself; a clear division of labour and a habit of reviewing each others work remove most of them.
For any task larger than one persons capacity, then, working in a team is better. It brings knowledge that no individual holds and catches mistakes before they become costly.'
WHERE id = 72;

-- 118 综合写作：禁塑令
UPDATE tpo_question SET sample_answer = 'The reading argues that cities should ban single-use plastic bags because they pollute the ocean, persist in landfills for centuries, and can easily be replaced. The lecture challenges each of these points.
First, the reading treats plastic bags as a major source of ocean pollution. The lecturer notes that studies of marine debris find bags to be only a small fraction of the total; discarded fishing gear and packaging account for far more. A ban on bags would therefore remove very little of the waste that actually harms animals.
Second, the reading claims that alternatives such as paper and cotton are ready substitutes. The professor shows that these bags carry their own environmental cost: producing paper requires more energy and water, and cotton farming uses large amounts of pesticide and land. Switching materials shifts the damage rather than removing it.
Third, the reading says a ban would not disrupt daily life. The lecture argues that it shifts a real cost onto low-income households and small shops, who must buy reusable bags and use each one dozens of times before it offsets the bag it replaced. For a family that shops daily, that is a noticeable new expense.
The lecture therefore does not deny that plastic waste is a problem, but it shows that a simple bag ban addresses only a small part of it while creating new burdens.'
WHERE id = 118;

-- 119 独立写作：public speaking 必修
UPDATE tpo_question SET sample_answer = 'Universities should require every student to take a course in public speaking. The skill is used far more often than the material in many advanced electives, and it can be taught systematically rather than left to chance.
The first reason is professional. Almost every career a graduate enters requires explaining something to people who are not specialists: a nurse describing a treatment plan, an engineer defending a design review, a teacher presenting a lesson. In a survey of employers that our career centre published last year, oral communication was listed among the top three skills expected of new hires, ahead of technical software knowledge. Yet most students never receive structured feedback on how they speak. A course that forces them to present, record, and revise gives them practice that an internship alone rarely provides.
The second reason is civic. Public speaking is not only about persuading an audience; it is about organizing an argument and answering questions under pressure. Students who learn to state a claim, support it with evidence, and respond to disagreement become harder to mislead and more willing to speak in meetings, community hearings, and job interviews. These habits outlast any single course.
Opponents argue that the requirement consumes credits that students could spend on their majors, and that a compulsory course produces bored audiences and formulaic speeches. That is a fair criticism of badly designed courses. It is not an argument against the subject itself. A well-run course limits enrolment, uses small tutorial groups, and grades recorded presentations against explicit criteria, so students practise rather than merely listen.
Requiring one public speaking course is therefore a cheap intervention with long-lasting returns. Universities already require writing; speaking deserves the same status.'
WHERE id = 119;

-- =====================================================================
-- 内置套题（TPO 01 / TPO 02）里那几道通用主观题的范例
-- 这些题目的标题是概括性的（Independent Speaking 等），按 task_type 分别给一版
-- =====================================================================

-- 14 speaking_independent：library vs home
UPDATE full_question SET sample_answer = 'I prefer studying in a library, mainly because a familiar room at home invites me to do everything except study. At home my desk is two steps from my bed, the kitchen, and a television, and I convince myself that folding laundry counts as a break. In the library the environment does the work for me: other people are reading, the room is quiet, and there is nothing else to do. I also finish more in a fixed block of time, because I go there with a list and leave when it is done. The one drawback is the walk across campus in winter, but that twenty minutes also separates study time from the rest of my day.'
WHERE id = 14;

-- 15 speaking_integrated：把小组自习室改成开放自习区
UPDATE full_question SET sample_answer = 'The notice says the university will replace the small study rooms on the second floor with one large open study area, arguing that more students can use the space at once. The student in the conversation disagree. First, he points out that the small rooms are the only place where project groups can talk without disturbing anyone; in an open area, four people discussing a presentation would be told to be quiet within minutes. Second, he argues that the open layout suits almost nobody: students who need silence will be distracted by keyboards and conversation, and groups will have nowhere to meet. He suggests that the money would be better spent adding more small rooms and a booking system, since demand for them is already higher than supply.'
WHERE id = 15;

-- 16 writing_integrated：拆小公园建高密度住宅
UPDATE full_question SET sample_answer = 'The reading argues that cities should replace small parks with high-density housing, because parks occupy valuable land, house too few people, and can be relocated to the outskirts. The lecture disputes all three claims.
First, the reading treats small parks as wasted space. The lecturer explains that these pocket parks cool the surrounding blocks in summer, and that neighbourhoods without them can be several degrees hotter, which raises cooling costs and health risks for elderly residents. Their value is not measured in residents per hectare.
Second, the reading claims that parks serve few people. The professor responds that small parks are used intensively precisely because they are close: children walk to them, office workers eat lunch there, and older people sit in them daily. A large park on the edge of the city requires a bus ride and is used far less often per square metre.
Third, the reading says housing demand justifies the loss. The lecture points out that cities already contain vacant and underused buildings that could be converted first, and that new towers on park land often become luxury units that do not house the families the reading describes.
The lecture therefore does not deny a housing shortage, but it shows that the reading counts only one kind of value and ignores the rest.'
WHERE id = 16;

-- 17 writing_discussion：统计学必修
UPDATE full_question SET sample_answer = 'Professor Lane asks whether every first-year student should be required to take statistics, and Anna supports the requirement because it helps students read research. I agree with Anna, though for a slightly different reason: the course is less about mathematics than about refusing claims that have no evidence behind them.
A required course reaches the students who would never choose it. Students in history, nursing, and journalism will meet misleading averages and badly designed surveys for the rest of their lives, and a single semester is enough to teach them what a sample is, why a correlation is not a cause, and how easily a graph can exaggerate a trend. Anna is right that this makes research readable, but it also makes advertising and political claims readable in a more sceptical way.
I would add one condition, however. The course should use the students own fields rather than abstract formulas, because a nursing student who analyses ward data will remember the method, while one who memorizes proofs will forget it in a year. If the requirement is taught that way, it is worth the credit.'
WHERE id = 17;

-- 28 speaking_independent：跨专业必修课
UPDATE full_question SET sample_answer = 'I agree that students should take at least one course outside their major. My own example comes from a computer science friend who took a course on urban planning. He expected an easy elective, but the readings changed how he approached his own projects: he started asking who would actually use the app he was building, and whether the people in the neighbourhood could reach the service. That question made his final project far more convincing to the judges. A course outside the major also gives students a second language for talking with people in other fields, which matters in almost every job. The risk is that the requirement becomes a filler course, so it should be chosen from a short list approved by advisors rather than picked at random.'
WHERE id = 28;

-- 29 writing_discussion：永久远程办公
UPDATE full_question SET sample_answer = 'Professor Hale asks whether companies should let employees work from home permanently. I would answer yes for experienced staff in well-defined roles, and no as a blanket policy.
The strongest argument for remote work is that it returns time. My aunt commuted ninety minutes each way for eleven years; working from home three days a week gave her back nine hours, which she used to finish a certification that led to a promotion. Companies also save on office space, and they can hire people who cannot move to an expensive city.
The counterargument that matters most is the one a classmate raised in our seminar: new employees learn by overhearing. A junior analyst who never sits near a senior colleague misses the informal corrections that turn a competent graduate into a reliable professional. That is why I would not make remote work universal. A workable policy keeps one or two shared days for training and planning, and lets experienced staff choose the rest.'
WHERE id = 29;

-- =====================================================================
-- 同步：把 tpo_question 的范例按题面标题写进已存在的套题明细
-- =====================================================================
UPDATE full_question fq
JOIN tpo_question tq ON tq.question_title = fq.question_title
SET fq.sample_answer = tq.sample_answer
WHERE fq.module_type IN ('speaking', 'writing')
  AND tq.sample_answer IS NOT NULL;

-- ---------- 核对 ----------
SELECT 'tpo 主观题范例' AS what, COUNT(*) AS cnt FROM tpo_question WHERE sample_answer IS NOT NULL;
SELECT 'full 主观题范例' AS what, COUNT(*) AS cnt FROM full_question WHERE sample_answer IS NOT NULL;
SELECT id, module_type, CHAR_LENGTH(sample_answer) AS len
FROM full_question WHERE module_type IN ('speaking','writing') ORDER BY module_type, id;
