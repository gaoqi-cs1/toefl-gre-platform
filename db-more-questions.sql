-- =============================================================
-- 单模块练习题库扩充 + 解析中英双语
--
-- ① 把现有题目的解析改成「中文 + English」双语；
-- ② 补满 section 6（阅读：Agriculture）的 5 道题，并把正文扩成 3 段；
-- ③ 新增 section 8（阅读：Urban Heat Islands）+ 5 道题；
-- ④ 新增 section 9（听力：Campus Bookstore Job）+ 5 道题，音频 /audio/campus-job.wav。
--    这套音频是按题目对话现生成的（TTS），内容与题目对应。
--
-- 执行：$env:MYSQL_PWD='123456'; Get-Content -Raw db-more-questions.sql | mysql -uroot --default-character-set=utf8mb4 toefl_db
-- 可重复执行：只改 60-72 的解析、重写 section 6/8/9 的题目与章节 8/9 本身，
-- 不动 section 1/4/5/7 的题目和其它篇章。
-- ⚠ 注意：脚本会先删除 section 8、9（如果以后你手工加了 8/9 号篇章，请先改脚本里的 id）。
-- =============================================================

SET NAMES utf8mb4;

-- =============================================================
-- 一、现有题目解析：中英双语
-- =============================================================

-- ---------- section 1 阅读（6 题） ----------
UPDATE tpo_question SET analysis = '【中文解析】ceremony 指"正式的仪式活动"，对应选项 A 的 ritual activity（仪式性活动）。其余三项都与该词在本句中的含义无关：B 说"现代娱乐方式"，C 说"困难的体力劳动"，D 说"私人聚会"。
【English】Analysis: A ceremony is a formal ritual activity, which matches option A. The other choices do not fit the meaning of the word in this context: B refers to modern entertainment, C to hard physical labor, and D to a private gathering.'
WHERE id = 60;

UPDATE tpo_question SET analysis = '【中文解析】原文明确说古人举行仪式是为了祈求好天气或捕猎顺利（pray for favorable weather or successful hunting）。B 是对这句的同义改写："他们在捕猎前寻求帮助"。A 说"庆祝丰收之后"，C 说"为了让观众开心"，D 说"为了记录部落历史"，均与原文不符。
【English】Analysis: The passage states that early humans held ceremonies to pray for favorable weather or successful hunting. Option B restates this as seeking help before a hunt, while A, C and D add purposes the passage never mentions.'
WHERE id = 61;

UPDATE tpo_question SET analysis = '【中文解析】答题依据是 "Gradually, these rituals became less religious and more entertaining"（仪式逐渐宗教色彩变淡、娱乐色彩变浓）。由此可推出：仪式随时间脱离了纯宗教功能。A 说"完全消失"、C 说"被政府禁止"、D 说"从不改变"，都与原文相反或过度。
【English】Analysis: The sentence "Gradually, these rituals became less religious and more entertaining" implies that rituals moved away from a purely religious function over time. A, C and D either contradict the passage or go beyond it.'
WHERE id = 62;

UPDATE tpo_question SET analysis = '【中文解析】考查修辞目的。整段都在论证"戏剧起源于古代仪式"这一理论，作者提到 early human rituals 是为了给这个理论提供证据，因此选 B（"为了支持戏剧从仪式发展而来的说法"）。
【English】Analysis: This is a rhetorical-purpose question. The whole paragraph argues the theory that theater developed from ancient rituals, so the mention of early human rituals serves as evidence for that claim. Option B is correct.'
WHERE id = 63;

UPDATE tpo_question SET analysis = '【中文解析】否定事实题：原文提到了仪式的时间、参与者和目的，但从未把"戏剧"和"农业"作比较，也没有说戏剧比农业更重要。因此 D 是"未提及"项。
【English】Analysis: A negative factual question. The passage discusses when and why rituals were held, but it never compares theater with farming or claims that theater mattered more. D is therefore NOT mentioned.'
WHERE id = 64;

UPDATE tpo_question SET analysis = '【中文解析】句意简化题。Gradually 意为"逐渐地"（slowly over time）；less religious and more entertaining 即"宗教色彩减少、娱乐性增加"。A 保留了这两层意思且语法完整，是最佳简化。
【English】Analysis: A sentence-simplification question. "Gradually" means slowly over time, and "less religious and more entertaining" means the religious element declined while entertainment increased. Option A keeps both ideas with correct grammar.'
WHERE id = 65;

-- ---------- section 7 听力（3 题） ----------
UPDATE tpo_question SET analysis = '【中文解析】学生一开口就说明来意：他要找一本写历史论文用的参考书（a reference book for his history paper）。因此选 B。A 的"借小说消遣"、C 的"投诉开放时间"、D 的"办借书证"都不是他的目的。
【English】Analysis: The student states his purpose at the start: he needs a reference book for his history paper. Option B matches. A, C and D are never mentioned as his reason for coming.'
WHERE id = 66;

UPDATE tpo_question SET analysis = '【中文解析】管理员说这本书是"预留用书（on reserve）"，不能带出馆外（cannot be checked out），但可以在馆内阅读并复印需要的页面。因此选 C。
【English】Analysis: The librarian explains that the book is on reserve, so it cannot be checked out; students may read it inside and copy the pages they need. Option C is correct.'
WHERE id = 67;

UPDATE tpo_question SET analysis = '【中文解析】对话结尾管理员告知阅览室开放时间：工作日到晚上十点，周五到六点。因此 A 正确。B、C、D 都是被改动过的时间，属于典型的数字干扰项。
【English】Analysis: At the end of the conversation the librarian gives the reading-room hours: ten at night on weekdays and six on Friday. Option A is correct; B, C and D are altered numbers, a common distractor pattern in TOEFL Listening.'
WHERE id = 68;

-- ---------- section 4 口语（2 题，解析=作答提示） ----------
UPDATE tpo_question SET analysis = '【中文解析】独立口语结构建议：① 一句明确表态（I prefer... / I would rather...）；② 理由一 + 具体例子（不要只给抽象理由，说清"什么时候、发生了什么"）；③ 理由二 + 例子；④ 一句话收尾。准备 15 秒时只写 3 个关键词，作答 45 秒内说完，不要留白。
【English】Analysis: A workable structure for the independent task: state your preference in one sentence, give two reasons, support each reason with a concrete example that names a time and an event, and close with one sentence. In the 15-second preparation, write down three keywords only, and keep speaking for the full 45 seconds.'
WHERE id = 69;

UPDATE tpo_question SET analysis = '【中文解析】综合口语（读+听）不要加自己的观点：先用一句话概括阅读材料里的提议或变化，再说明说话人的态度（同意/反对），然后按顺序复述两条理由及其细节。60 秒里阅读内容控制在 10 秒以内，其余时间给听力的两个理由。
【English】Analysis: For the integrated task, do not add your own opinion. Summarize the reading in one sentence, state the speaker attitude, then report the two reasons with their details in order. Keep the reading summary to about ten seconds and spend the rest of the 60 seconds on the two listening points.'
WHERE id = 70;

-- ---------- section 5 写作（2 题，解析=作答提示） ----------
UPDATE tpo_question SET analysis = '【中文解析】综合写作不写自己的立场，任务是"证明听力如何反驳阅读"。建议四段：引言（阅读提出三点 + 讲座逐一反驳）→ 反驳点一 → 反驳点二 → 反驳点三。每段先写听力观点，再写它与阅读哪一点冲突，不要逐句翻译原文。
【English】Analysis: In integrated writing you do not give your own opinion; your job is to show how the lecture challenges the reading. Use four paragraphs: an introduction, then one paragraph for each of the three counterpoints. In each paragraph, state the lecture point first and then explain which reading claim it undermines. Do not translate the source sentence by sentence.'
WHERE id = 71;

UPDATE tpo_question SET analysis = '【中文解析】独立写作建议五段：引言（背景 + 明确立场）→ 理由一 + 具体例子 → 理由二 + 具体例子 → 让步段（承认对方一点合理性，再说明为什么不足以推翻你的立场）→ 结论。例子要具体到"谁、在哪、做了什么、结果如何"。
【English】Analysis: A reliable five-paragraph plan for independent writing: introduction with a clear position, reason one plus a concrete example, reason two plus a concrete example, a short concession paragraph that admits one merit of the other side and explains why it does not overturn your view, and a conclusion. Make examples specific: who, where, what happened, and what result it produced.'
WHERE id = 72;

-- =============================================================
-- 二、section 6（阅读 Agriculture）：扩写正文 + 补 5 题
-- =============================================================
UPDATE tpo_section SET content_text = 'The development of agriculture transformed human society. Before farming, people relied on hunting and gathering. They moved frequently to find food, and they carried few possessions. Once plants were domesticated, humans could settle down in villages. Settled life led to population growth, specialized jobs and the invention of writing. Scholars argue that agriculture was the foundation of civilization.

The change, however, was not a single event. Farming spread unevenly, and many communities mixed cultivation with hunting for generations. Early farmers worked longer hours than hunter-gatherers, and their diet was narrower. Relying on a few crops made harvest failures more dangerous, while living close to animals and to other people made it easier for disease to spread. Some historians therefore describe the shift as a trade: more food and more people, in exchange for worse health and harder work.

The debate continues because the evidence is uneven. Archaeological records show changes in tools and seeds, but they say little about how people felt about those changes. What is clear is the direction: from small groups that moved to large groups that stayed. Whether that direction should be called progress depends on which costs and benefits a scholar decides to count.'
WHERE id = 6;

DELETE FROM tpo_question WHERE section_id = 6;

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(101, 6, 'FACTUAL', 'According to paragraph 1, what allowed people to settle in villages?',
 'Finding better hunting grounds', 'Domesticating plants', 'Inventing writing', 'Building permanent roads', 'B',
 '【中文解析】第一段说 "Once plants were domesticated, humans could settle down in villages"，即植物被驯化之后人才可以定居。注意写作的出现是定居生活的"结果"（led to the invention of writing），不是原因，C 是典型因果倒置干扰项。
【English】Analysis: Paragraph 1 says that once plants were domesticated, humans could settle down in villages. Writing is presented as a result of settled life, not its cause, so C is a classic cause-and-effect reversal.', NOW()),
(102, 6, 'VOCABULARY', 'The word "domesticated" in paragraph 1 is closest in meaning to',
 'discovered by accident', 'protected from bad weather', 'brought under human control', 'carried over long distances', 'C',
 '【中文解析】domesticate 指"驯化、使受人类控制并用于生产"。放在本句（植物被驯化后人类定居）语境里只能是 C。A "偶然发现"、B "防天气"、D "长途运输"都不能支撑"定居"这一结果。
【English】Analysis: To domesticate a plant means to bring it under human control so that it can be cultivated. That meaning is required by the rest of the sentence, which explains that settlement followed. A, B and D do not account for that result.', NOW()),
(103, 6, 'INFERENCE', 'What can be inferred from paragraph 2 about the shift to farming?',
 'It happened quickly in most regions.', 'It brought costs as well as benefits.', 'It ended hunting everywhere.', 'It improved health immediately.', 'B',
 '【中文解析】第二段列举了农业的好处（更多食物、更多人口）和代价（工时更长、饮食更单一、疾病更易传播），并说历史学家把它看作一场"交易"。因此可推出：这一转变既有收益也有代价（B）。A 与 "was not a single event" 矛盾；C 与 "mixed cultivation with hunting" 矛盾；D 与 "made it easier for disease to spread" 矛盾。
【English】Analysis: Paragraph 2 lists both gains (more food, more people) and costs (longer hours, a narrower diet, easier spread of disease), and historians describe the shift as a trade. Option B follows. A contradicts the statement that the change was not a single event, C contradicts the mixing of cultivation with hunting, and D contradicts the point about disease.', NOW()),
(104, 6, 'NEGATIVE FACTUAL', 'All of the following are mentioned as costs of farming EXCEPT',
 'Longer working hours', 'A narrower diet', 'Greater exposure to disease', 'Loss of the ability to write', 'D',
 '【中文解析】否定事实题。第二段列出的代价是：工时更长、饮食更窄、疾病更易传播。写作能力恰恰是第一段提到的文明成果（the invention of writing），不是农业的代价，所以 D 是"未提及/相反"项。做这类题要逐项回原文定位，不要凭常识判断。
【English】Analysis: The costs named in paragraph 2 are longer hours, a narrower diet and greater exposure to disease. Writing is described in paragraph 1 as an achievement of settled life, not a cost of farming, so D is the correct answer. For negative factual questions, locate each option in the text instead of relying on general knowledge.', NOW()),
(105, 6, 'RHETORICAL PURPOSE', 'Why does the author mention that the evidence is uneven?',
 'To explain why the debate about the shift continues', 'To prove that farming was a mistake', 'To criticize archaeologists for careless work', 'To argue that writing appeared before farming', 'A',
 '【中文解析】修辞目的题。第三段第一句给出原因："The debate continues because the evidence is uneven."——正因为考古证据不完整，历史学家才继续争论。后文补充"考古记录显示工具和种子的变化，却看不出人们的感受"。所以选 A。
【English】Analysis: This is a rhetorical-purpose question. Paragraph 3 opens with "The debate continues because the evidence is uneven," and then explains that archaeological records show changes in tools and seeds but reveal little about how people felt. The mention of uneven evidence therefore explains the persistence of the debate; A is correct.', NOW());

-- =============================================================
-- 三、新增 section 8（阅读 Urban Heat Islands）+ 5 题
-- =============================================================
DELETE FROM tpo_question WHERE section_id = 8;
DELETE FROM tpo_section WHERE id = 8;

INSERT INTO tpo_section (id, tpo_id, type, content_text, audio_url, create_time) VALUES
(8, 1, 'reading', 'Anyone who walks from a park into a city street on a summer afternoon feels a change: the air is warmer, and the warmth seems to come out of the pavement. The effect has a name. An urban heat island is a city area that is measurably warmer than the countryside around it. Two features of the built environment produce it. First, dark surfaces such as asphalt and concrete absorb solar energy during the day and release it slowly at night. Second, the trees and soil that once cooled the air through evaporation have been replaced by surfaces that cannot hold water.

The temperature difference is largest after sunset. In a rural field, the ground cools quickly once the sun is down; in a dense district, walls and roofs keep releasing the heat they stored, so the city may stay several degrees warmer through the entire night. The consequences are practical. Warmer nights raise the demand for electricity, because air conditioners run longer. Sustained heat also stresses the bodies of older residents and outdoor workers, and it can lower the quality of water in rivers when warm runoff reaches them.

Cities have therefore begun to test countermeasures. One approach is to restore vegetation: street trees, parks and green roofs cool the air directly and shade the surfaces beneath them. Another is to change the surfaces themselves. Light-colored roofs and pavements reflect more sunlight, and permeable materials let rain soak in instead of running off. Measurements from pilot projects suggest that a well-planted block can be two or three degrees cooler than a bare one on a hot afternoon. The limits are mostly institutional rather than technical. Trees need years to grow, and the districts that most need shade are often the ones with the smallest budgets for it.', NULL, NOW());

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(106, 8, 'FACTUAL', 'According to paragraph 1, what causes the urban heat island effect?',
 'The large number of people living in cities', 'Absorbent dark surfaces and the loss of trees and soil', 'The height of modern city buildings', 'The noise produced by traffic', 'B',
 '【中文解析】第一段明确给出两个成因：① 深色表面（沥青、混凝土）白天吸热、夜间缓慢释放；② 原本靠蒸发降温的树木和土壤被"不能保水"的表面取代。B 正好概括这两点。人口数量、楼高、交通噪音在原文中都没有被列为成因，属于"听起来合理但文中无据"的干扰项。
【English】Analysis: Paragraph 1 names two causes: dark surfaces such as asphalt and concrete absorb solar energy and release it slowly at night, and the trees and soil that once cooled the air through evaporation have been replaced by surfaces that cannot hold water. B summarizes both. Population size, building height and traffic noise are plausible but not stated as causes.', NOW()),
(107, 8, 'VOCABULARY', 'The word "countermeasures" in paragraph 3 is closest in meaning to',
 'measuring instruments', 'government laws', 'actions taken to reduce a problem', 'arguments against a theory', 'C',
 '【中文解析】counter- 表示"反、对"，measure 在此指"措施"，countermeasure 即"应对措施"：第三段紧接着列出的植树、浅色屋顶、透水材料，正是为降低热岛效应采取的行动，所以 C 正确。A 把 measure 误解为"测量工具"，B 过度具体成"法律"，D 与语境无关。
【English】Analysis: The prefix counter- means against, and a countermeasure is an action taken against a problem. The paragraph then lists restoring vegetation, light-colored surfaces and permeable materials, all actions aimed at reducing the heat island effect. C is correct; A misreads measure as an instrument, B is too specific, and D does not fit the context.', NOW()),
(108, 8, 'INFERENCE', 'What can be inferred about city temperatures at night?',
 'They stay higher than rural temperatures because stored heat is released.', 'They fall faster than rural temperatures.', 'They are unaffected by the materials used in buildings.', 'They vary only with the number of cars on the road.', 'A',
 '【中文解析】第二段对比了夜间情况：乡村地面"太阳落山后很快变凉"，而密集城区的墙体和屋顶"持续释放白天储存的热量"，因此城市整晚都可能高出好几度。可推出 A。B 与原文相反；C 与第一段"材料吸热"矛盾；D 原文未提。
【English】Analysis: Paragraph 2 contrasts the two settings: rural ground cools quickly after sunset, while walls and roofs in a dense district keep releasing stored heat, so the city may stay several degrees warmer through the night. A follows. B reverses the comparison, C contradicts the point about materials, and D is not discussed.', NOW()),
(109, 8, 'RHETORICAL PURPOSE', 'Why does the author mention measurements from pilot projects?',
 'To argue that the problem cannot be solved', 'To show evidence that planting and lighter surfaces actually cool streets', 'To criticize cities for wasting money', 'To explain how thermometers are manufactured', 'B',
 '【中文解析】修辞目的题。作者在提出两类应对措施（恢复植被、改变表面材料）之后，用"试点项目的实测数据"说明这些措施确实有效（种植良好的街区在炎热下午可低 2-3 度）。因此目的是提供实证支持，选 B。A 与"limits are mostly institutional"的表述方向不同：作者承认限制存在，但不是说无解。
【English】Analysis: After introducing two kinds of countermeasures, the author cites measurements from pilot projects showing that a well-planted block can be two or three degrees cooler on a hot afternoon. The purpose is to provide evidence that the measures work. B is correct; the passage acknowledges institutional limits but does not claim the problem is unsolvable.', NOW()),
(110, 8, 'NEGATIVE FACTUAL', 'All of the following are mentioned as countermeasures EXCEPT',
 'Planting street trees and green roofs', 'Using light-colored roofs and pavements', 'Using materials that let rain soak in', 'Requiring every household to buy an air conditioner', 'D',
 '【中文解析】否定事实题。第三段列出的措施是：街道树木/公园/绿色屋顶、浅色屋顶与路面、透水材料。买空调是"应对高温的被动适应"，不是文中提出的城市措施（文中反而说夜间更热会让空调运行更久、用电需求上升）。因此 D 是未提及项。
【English】Analysis: Paragraph 3 lists street trees, parks, green roofs, light-colored roofs and pavements, and permeable materials. Buying air conditioners is never proposed as a countermeasure; the passage instead notes that warmer nights make air conditioners run longer. D is therefore NOT mentioned.', NOW());

-- =============================================================
-- 四、新增 section 9（听力 Campus Bookstore Job）+ 5 题
-- 音频 /audio/campus-job.wav 是按下面这段对话生成的，内容与题目一致
-- =============================================================
DELETE FROM tpo_question WHERE section_id = 9;
DELETE FROM tpo_section WHERE id = 9;

INSERT INTO tpo_section (id, tpo_id, type, content_text, audio_url, create_time) VALUES
(9, 1, 'listening', 'Student: Excuse me, is this where I ask about the part-time job at the campus bookstore?
Staff: Yes, you are in the right place. We still have a few openings for the evening shift.
Student: Great. How many hours would I work each week?
Staff: About ten hours, usually between five and nine in the evening, three days a week.
Student: That fits my class schedule. What do I need to do to apply?
Staff: Fill out this application form first, and then we will set up a short interview with the store manager.
Student: Should I bring anything to the interview?
Staff: Bring your student ID and a copy of your class schedule, so we can confirm that the hours do not conflict with your classes.
Student: Understood. When will I know the result?
Staff: The manager usually calls within two days after the interview. If you do not hear from us by Friday, come back and ask for me.
Student: Thank you very much. I will fill out the form right away.', '/audio/campus-job.wav', NOW());

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(111, 9, 'PURPOSE', 'Why does the student speak to the staff member?',
 'To ask about a part-time job at the bookstore', 'To return a book he borrowed', 'To complain about the store hours', 'To buy course materials for a class', 'A',
 '【中文解析】主旨/目的题，听力第一句就给出答案："is this where I ask about the part-time job at the campus bookstore?"（这里是不是咨询校园书店兼职的地方）。B、C、D 都是把 bookstore 这个场景换成别的常见事务，属于场景干扰项。做目的题要抓对话开头 1-2 句。
【English】Analysis: A purpose question. The student answers it in his first sentence: he asks whether this is the place to ask about the part-time job at the campus bookstore. B, C and D reuse the bookstore setting for other errands, a common distractor pattern. For purpose questions, listen closely to the first one or two lines.', NOW()),
(112, 9, 'DETAIL', 'What does the staff member say about the work schedule?',
 'Twelve hours a week, morning shifts', 'About ten hours a week, evening shifts', 'Six hours a week, weekend shifts only', 'Twenty hours a week, any shift the student prefers', 'B',
 '【中文解析】细节题。职员说 "About ten hours, usually between five and nine in the evening, three days a week"——每周约 10 小时、晚班。A/C/D 的时长或时段都被改动过。听力细节题常靠"数字 + 时间词"制造干扰，要边听边记。
【English】Analysis: A detail question. The staff member says the student would work about ten hours, usually between five and nine in the evening, three days a week. A, C and D change either the number of hours or the time of day. TOEFL detail questions often rely on numbers plus time expressions, so note them while listening.', NOW()),
(113, 9, 'DETAIL', 'What must the student do first in order to apply?',
 'Take a written test', 'Meet the store manager immediately', 'Fill out an application form', 'Ask his professor for a recommendation', 'C',
 '【中文解析】细节题。职员说 "Fill out this application form first, and then we will set up a short interview with the store manager"（先填申请表，再安排面试）。B 把第二步说成第一步，属于顺序干扰；A、D 原文都没有提到。
【English】Analysis: A detail question. The staff member says to fill out the application form first, and then an interview with the store manager will be arranged. B reverses the order, a common sequencing distractor, while A and D are never mentioned.', NOW()),
(114, 9, 'DETAIL', 'What should the student bring to the interview?',
 'A letter of recommendation', 'A student ID and a class schedule', 'A copy of his transcript', 'A resume with previous jobs listed', 'B',
 '【中文解析】细节题。职员明确说 "Bring your student ID and a copy of your class schedule"，目的是确认工作时间不与课程冲突。C 的"成绩单"和 D 的"简历"是同类招聘场景的常见材料，但文中没有出现，属于"合理但无据"的干扰项。
【English】Analysis: A detail question. The staff member asks the student to bring a student ID and a copy of the class schedule so that the hours can be checked against his classes. A transcript (C) and a resume (D) belong to the same hiring context but are never mentioned.', NOW()),
(115, 9, 'NEXT ACTION', 'What should the student do if he does not hear back by Friday?',
 'Call the store manager at home', 'Apply to a different department instead', 'Wait until the next semester', 'Come back and ask for the staff member', 'D',
 '【中文解析】推断下一步行动。职员说 "The manager usually calls within two days after the interview. If you do not hear from us by Friday, come back and ask for me."——若周五前没有消息，就回来找他。D 与原文一致。A 的"打家里电话"、B 的"换部门"、C 的"等下学期"都是常见但未提及的处理方式。
【English】Analysis: The staff member says the manager usually calls within two days after the interview, and adds that if the student does not hear by Friday, he should come back and ask for the staff member. D matches. Calling the manager at home, applying elsewhere and waiting a semester are common but unmentioned options.', NOW());

-- =============================================================
-- 四之二、新增 section 10（口语第二套）+ section 11（写作第二套）
-- 让口语、写作也各有第二篇，四个模块都能换着练
-- =============================================================
DELETE FROM tpo_question WHERE section_id IN (10, 11);
DELETE FROM tpo_section WHERE id IN (10, 11);

INSERT INTO tpo_section (id, tpo_id, type, content_text, audio_url, create_time) VALUES
(10, 1, 'speaking', 'Speaking Tasks (Set 2)
Task 1 - Independent: Some students take a year off between high school and university to work, travel or prepare for exams. Do you think taking a gap year is a good idea? Give specific reasons and examples. Preparation 15s / Response 45s.
Task 2 - Integrated: The university plans to keep the main library open twenty-four hours a day during final exams. Read the announcement, then listen to a student response, and summarize the student opinion and its reasons. Preparation 20s / Response 60s.
Reading (announcement): To support students during final exams, the main library will stay open around the clock for the last three weeks of the semester. Security staff and cleaning services will be added at night, and the cost will be covered by the library budget.
Listening (student): I do not think this is a good use of money. First, hardly anyone studies at four in the morning; when I walked past the library at midnight last term, most desks were empty. Second, the budget for night staff has to come from somewhere, and the library already cancelled several journal subscriptions this year. What students actually need is more group study rooms, because the ones we have are booked weeks in advance.', NULL, NOW()),
(11, 1, 'writing', 'Writing Tasks (Set 2)
Task 1 - Integrated Writing: Read a passage arguing that cities should ban single-use plastic bags, then listen to a lecture that challenges the passage. Write a response summarizing the lecture points and explaining how they cast doubt on the reading. 150-225 words, 20 minutes.
Reading (three reasons): 1) Plastic bags are a major source of ocean pollution and harm marine animals. 2) They take centuries to break down and fill landfills. 3) Cheap, durable alternatives such as paper or cotton bags already exist, so a ban would not disrupt daily life.
Listening (lecture): 1) Studies of ocean debris find that plastic bags are a small fraction of the total; discarded fishing gear and packaging dominate. 2) Paper and cotton bags require more energy and water to produce, and cotton farming uses large amounts of pesticide, so the alternatives carry their own environmental cost. 3) A ban shifts a real cost onto low-income households and small shops, because reusable bags must be used dozens of times before they offset the impact of the bag they replace.
Task 2 - Independent Writing: Do you agree or disagree with the following statement? Universities should require every student to take a course in public speaking. Use specific reasons and examples to support your answer. 300+ words, 30 minutes.', NULL, NOW());

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(116, 10, NULL, 'Task 1 - Independent Speaking: Is a gap year a good idea?', NULL, NULL, NULL, NULL, NULL,
'【中文解析】表态要快：先一句 "I think taking a gap year is a good idea / is not a good idea"，再给两条理由。可行的理由方向：① 更清楚自己要学什么（举例：在实习中发现自己真正喜欢数据分析，于是改了专业）；② 更成熟、更能管理时间。若反方：① 容易失去学习状态（举例：同学一年没碰数学，回来第一学期很吃力）；② 成本高，晚一年进入就业市场。准备 15 秒只写三个关键词，示例要具体到人、地点、结果。
【English】Analysis: Commit to a position in your first sentence, then give two reasons. If you support a gap year, useful reasons are clearer academic goals, supported by an example such as changing majors after an internship, and better time management. If you oppose it, point to lost study habits, illustrated by a classmate who struggled after a year away, and the cost of entering the job market a year later. In the 15-second preparation write three keywords only, and make every example concrete about who, where and what happened.', NOW()),
(117, 10, NULL, 'Task 2 - Integrated Speaking: Campus library hours', NULL, NULL, NULL, NULL, NULL,
'【中文解析】综合口语只转述、不加个人观点。流程：① 一句话概括阅读提议及两个理由（图书馆期末 24 小时开放，理由是支持学生备考、由图书馆预算承担）；② 学生反对，反对理由一是凌晨几乎没人来（他午夜路过看到大部分座位是空的），二是夜间人力成本会挤掉期刊订阅，学生真正需要的是更多小组自习室（现在要提前几周预约）。60 秒里阅读概括不超过 10 秒。
【English】Analysis: Restate, never argue. Open with one sentence covering the proposal and its grounds: the library will stay open around the clock during finals to support students, with the cost paid from the library budget. Then report the student disagreement in order: almost nobody studies at four in the morning, based on his own observation that most desks were empty at midnight, and night staffing costs money that has already been taken from journal subscriptions, while what students actually need is more group study rooms, which are booked weeks ahead. Keep the reading summary under ten seconds.', NOW()),
(118, 11, NULL, 'Integrated Writing: Ban on single-use plastic bags', NULL, NULL, NULL, NULL, NULL,
'【中文解析】综合写作按"一点对一点"写，共四段。第一段：阅读主张禁止一次性塑料袋，理由是海洋污染、难降解、替代品已成熟；讲座从三个方面质疑。第二段：讲座指出海洋垃圾研究显示塑料袋只占很小比例，主要成分是废弃渔具和包装——削弱阅读的第一个理由。第三段：讲座说纸袋和棉袋生产耗能耗水更多、棉花种植大量使用农药，替代品本身也有环境代价——削弱"替代品成熟无代价"。第四段：讲座认为禁令把成本转嫁给低收入家庭和小店，因为可重复使用的袋子要使用几十次才能抵消影响。注意：不要抄原文句子，用 "The lecture points out that..." 这类转述句式。
【English】Analysis: Write four paragraphs and match each lecture point to the reading claim it undermines. Paragraph one sets up the conflict: the reading favors a ban because of ocean pollution, slow breakdown and ready alternatives. Paragraph two shows that ocean-debris studies attribute only a small share of the total to plastic bags, with discarded fishing gear and packaging dominating. Paragraph three explains that paper and cotton bags demand more energy and water, and cotton farming uses large amounts of pesticide. Paragraph four argues that a ban shifts a real cost onto low-income households and small shops, since reusable bags must be used dozens of times to offset their own impact. Paraphrase the sources instead of copying their sentences.', NOW()),
(119, 11, NULL, 'Independent Writing: Should universities require a public speaking course?', NULL, NULL, NULL, NULL, NULL,
'【中文解析】独立写作建议五段，且例子要具体。支持方理由方向：① 表达能力直接影响求职与晋升（举例：同组两位实习生，技术相当，做汇报更清楚的那位拿到 return offer）；② 结构化表达能减少误解（举例：小组项目里把分工讲清楚，返工减少）。反方理由方向：① 强制课程占用专业课时，兴趣不足的学生收益低；② 表达可以在专业课内训练（举例：把答辩、组内汇报纳入评分）。让步段承认对方一点合理性，再说明为什么不足以推翻立场。字数 300+，用具体人名/场景替代 "many people think"。
【English】Analysis: Use five paragraphs and concrete examples. If you support the requirement, argue that speaking ability affects hiring and promotion, illustrated by two interns with similar technical skills where the clearer presenter received the offer, and that structured speech reduces misunderstandings, illustrated by a group project where clear task assignments cut rework. If you oppose it, argue that a compulsory course displaces major coursework and yields little for uninterested students, and that speaking can be trained inside major courses by grading presentations and defenses. Include a short concession, then explain why it does not overturn your position. Keep the essay above 300 words and replace generalizations such as many people think with specific people and situations.', NOW());

-- =============================================================
-- 五、执行后核对
-- =============================================================
SELECT s.id AS section_id, s.type, COUNT(q.id) AS question_count
FROM tpo_section s LEFT JOIN tpo_question q ON q.section_id = s.id
GROUP BY s.id, s.type ORDER BY s.type, s.id;
