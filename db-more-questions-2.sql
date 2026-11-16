-- =============================================================
-- 题库再加两篇阅读 + 一篇听力（给「一键生成套题」提供更多随机组合）
--
-- 新增：
--   section 12  reading  The Printing Press（3 段 + 5 题）
--   section 13  reading  Coral Bleaching（3 段 + 5 题）
--   section 14  listening  Research Paper Topic（音频 /audio/research-paper.wav，5 题）
-- 所有新题解析都是「【中文解析】+【English】」，与前一版保持一致。
--
-- 执行：
--   $env:MYSQL_PWD='123456'
--   Get-Content -Raw db-more-questions-2.sql | mysql -uroot --default-character-set=utf8mb4 toefl_db
-- 可重复执行：只重建 12/13/14 三个篇章和 120-134 号题目。
-- ⚠ 如果你以后手工占用了 12/13/14 号或 120+ 号 id，跑之前先改脚本里的数字。
-- =============================================================

SET NAMES utf8mb4;

DELETE FROM tpo_question WHERE section_id IN (12, 13, 14);
DELETE FROM tpo_section WHERE id IN (12, 13, 14);

-- ============================ section 12 阅读：印刷术 ============================
INSERT INTO tpo_section (id, tpo_id, type, content_text, audio_url, create_time) VALUES
(12, 1, 'reading', 'Before the middle of the fifteenth century, every book in Europe was written out by hand. A single copy of a long text could take a scribe months to produce, and the parchment alone was expensive enough that only wealthy institutions could own many volumes. Libraries were therefore small, and most people never read a book at all. The technology that changed this was movable type. Its decisive feature was not the press itself but the letters: each character was a separate piece of metal that could be rearranged, cleaned and used again for an entirely different page.

The consequences appeared within two generations. Because copies could be produced faster, the price of a printed page fell, and the number of editions rose sharply. Books on medicine, law and geography began to circulate outside the universities and monasteries that had controlled copying. Printed sheets also standardized forms that had previously varied by region, from spelling to the layout of mathematical tables, and a scholar in one city could finally check a calculation against the same text a scholar in another city owned. Yet the change was not simply liberating. Printers needed capital for metal, paper and presses, so publishing concentrated in a few wealthy towns. Authorities quickly learned to license and censor what came off the presses, and the majority of people still could not read.

Historians now argue about how much of this was caused by printing and how much was already under way. Commercial expansion, urban growth and the spread of schools all pushed in the same direction, and print amplified those forces rather than creating them from nothing. What printing did was remove a bottleneck: once the cost of reproducing a text dropped, the limit on how many people could hold the same idea at the same time largely disappeared. That is why its effects were gradual in themselves but cumulative in the long run, and why the sixteenth-century wars over religious texts are so often traced back to the press.', NULL, NOW());

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(120, 12, 'FACTUAL', 'According to paragraph 1, what was the main limitation on books before movable type?',
 'Printing presses were illegal', 'Each copy had to be written by hand and was expensive', 'Readers were not interested in long texts', 'Paper could not be produced in Europe', 'B',
 '【中文解析】第一段开门见山：十五世纪中叶以前，欧洲每本书都是手抄的（"every book in Europe was written out by hand"），一部长篇要抄几个月，光羊皮纸就很贵。因此 B 正确。文中从未说印刷机违法、读者没兴趣或欧洲不能造纸，A、C、D 都是凭空添加。
【English】Analysis: Paragraph 1 states that before the mid-fifteenth century every book in Europe was written out by hand, that a long text could take a scribe months, and that the parchment alone was expensive. Option B restates this. A, C and D introduce claims the passage never makes.', NOW()),
(121, 12, 'VOCABULARY', 'The phrase "movable type" in paragraph 1 refers to',
 'a press that could be moved between towns', 'ink that dried faster than handwriting', 'individual metal letters that could be rearranged and reused', 'a method of binding pages into a single volume', 'C',
 '【中文解析】原文紧接着就解释了：重点不在印刷机本身，而在"字母"——每个字符是一块单独的金属，可以重排、清洗、再用于完全不同的页面（"each character was a separate piece of metal that could be rearranged"）。movable 在这里指"可移动、可重排"的字模，不是"整套设备能搬走"，B、D 也都不符合。所以选 C。
【English】Analysis: The sentence immediately after the phrase explains it: each character was a separate piece of metal that could be rearranged, cleaned and used again for a different page. Movable refers to the reusable letters, not to a press that travels between towns, so C is correct.', NOW()),
(122, 12, 'INFERENCE', 'What can be inferred from paragraph 2 about the effect of printing on ideas?',
 'Ideas could spread faster because copies became cheaper and more numerous', 'Ideas stopped spreading because governments censored all books', 'Only universities were allowed to own printed books', 'Printed books were more expensive than hand-copied ones', 'A',
 '【中文解析】第二段说：印刷让单页成本下降、版本数量大增，医学、法律、地理类书籍开始走出大学和修道院；学者还能对照同一份文本核对计算。由此可推出：复制成本降低后，观念传播变快变广（A）。B 的"全部被审查"与原文"authorities learned to license and censor"（开始学会审查）程度不符；C 与"走出大学"相反；D 与"价格下降"相反。
【English】Analysis: Paragraph 2 explains that printed pages became cheaper, editions multiplied, and books began to circulate beyond universities and monasteries, letting scholars compare the same text. Option A follows. B exaggerates censorship into a total stop, C reverses the movement out of universities, and D contradicts the falling price.', NOW()),
(123, 12, 'RHETORICAL PURPOSE', 'Why does the author mention that authorities learned to license and censor printed material?',
 'To show that printing was technically imperfect', 'To explain why scribes lost their jobs', 'To argue that censorship caused the price of books to rise', 'To show that the effects of printing were not simply liberating', 'D',
 '【中文解析】修辞目的题。该句出现在"Yet the change was not simply liberating"（然而这一变化并非单纯的解放）之后，和"印刷商需要资本、出版集中到少数富裕城镇"并列，都是用来平衡上一段的乐观叙述。因此作者提审查是为了说明印刷术的后果是双面的，选 D。文中没有说印刷技术不完善（A）、没有说抄写员失业（B）、也没有把审查和书价联系起来（C）。
【English】Analysis: The clue is the sentence that introduces this detail: "Yet the change was not simply liberating." The licensing and censorship point sits alongside the need for capital and the concentration of publishing, all balancing the optimistic account in the previous paragraph. Its purpose is to show the effects were two-sided; D is correct.', NOW()),
(124, 12, 'NEGATIVE FACTUAL', 'All of the following are mentioned as consequences of printing EXCEPT',
 'A fall in the price of a printed page', 'A sharp rise in the number of editions', 'Standardized spelling and mathematical tables', 'Immediate literacy for most of the population', 'D',
 '【中文解析】否定事实题。第二段列出的后果有：单页价格下降、版本数激增、拼写与数学表格等格式标准化、书籍走出大学与修道院。同时明确说 "the majority of people still could not read"（大多数人仍然不识字），所以 D 不但没被提及，还与原文相反。做否定题必须逐项回原文定位。
【English】Analysis: Paragraph 2 lists falling page prices, a sharp rise in editions, standardized spelling and tables, and circulation beyond universities. It also states that the majority of people still could not read, so D is both unmentioned and contradicted by the passage.', NOW());

-- ============================ section 13 阅读：珊瑚白化 ============================
INSERT INTO tpo_section (id, tpo_id, type, content_text, audio_url, create_time) VALUES
(13, 1, 'reading', 'A coral reef looks like stone, but the animals that build it are tiny and soft-bodied. Each coral polyp lives with single-celled algae inside its tissue. The partnership is a trade: the algae produce sugars by photosynthesis and supply most of the coral energy budget, while the coral gives the algae shelter and nutrients. The same algae are also what gives healthy coral its color, which is why a living reef is yellow, pink or brown rather than grey.

The partnership breaks down when the water gets too warm. Above a certain temperature the algae produce compounds that damage their host, and the coral expels them. What remains is the white limestone skeleton seen through transparent tissue, an effect called bleaching. Bleaching is not automatically death. If the water cools within a few weeks, corals can take up new algae and recover. But each bleaching event costs energy reserves, and a reef that bleaches in two consecutive years often cannot rebuild its algae population before the next heat wave arrives. Repeated events therefore convert a temporary stress into mortality.

Local pressures make the outcome worse. Sediment washed off cleared land blocks the light the algae need, fertilizer runoff feeds seaweed that overgrows young coral, and overfishing removes the fish that graze the seaweed back. Managers can act on these local factors: replanting mangrove forests that trap sediment, restricting fertilizer use near the coast, and closing reefs to fishing long enough for fish stocks to rebuild. None of that changes the ocean temperature. This is why reef scientists distinguish between what can be managed locally and what depends on global emissions, and why a well-protected reef can still bleach in an exceptionally hot year.', NULL, NOW());

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(125, 13, 'FACTUAL', 'According to paragraph 1, what gives healthy coral its color?',
 'Algae living inside the coral tissue', 'Minerals dissolved in the surrounding seawater', 'The limestone skeleton beneath the tissue', 'Bacteria on the surface of the reef', 'A',
 '【中文解析】第一段说明：每个珊瑚虫体内都住着单细胞藻类，这些藻类通过光合作用提供糖分，也正是它们带来颜色（"The same algae are also what gives healthy coral its color"）。因此 A 正确。B 的溶解矿物、C 的石灰岩骨架、D 的表面细菌都没有被提及；白色骨架恰恰是藻类被排出后才暴露出来的。
【English】Analysis: Paragraph 1 says each polyp lives with single-celled algae, which supply most of the coral energy and are what gives healthy coral its color. A is correct. Dissolved minerals, the limestone skeleton and surface bacteria are not mentioned, and the white skeleton is what becomes visible after the algae are expelled.', NOW()),
(126, 13, 'VOCABULARY', 'The word "bleaching" in paragraph 2 is closest in meaning to',
 'growing a protective outer layer', 'losing color because the algae are expelled', 'being covered by sediment', 'breaking into smaller pieces', 'B',
 '【中文解析】第二段先讲：水温过高时藻类产生伤害宿主的化合物，珊瑚把藻类排出体外；接着说明剩下的就是透过透明组织看到的白色石灰骨架，"an effect called bleaching"。所以 bleaching 指"因藻类被排出而失去颜色"，选 B。A 与生长无关，C 是第三段的沉积物问题，D 是物理破碎。
【English】Analysis: Paragraph 2 explains that the coral expels the algae and what remains is the white limestone skeleton seen through transparent tissue, an effect called bleaching. The word therefore means losing color because the algae are expelled; B is correct. Sediment belongs to paragraph 3, and the other options describe different processes.', NOW()),
(127, 13, 'INFERENCE', 'What can be inferred from paragraph 2 about reefs that bleach in two consecutive years?',
 'They will always recover within a few weeks.', 'They are immune to further heat stress.', 'They may not have time to rebuild their algae before the next heat wave.', 'They stop producing limestone permanently.', 'C',
 '【中文解析】原文说单次白化不等于死亡，若几周内水温降下来还能恢复；但每次白化都消耗能量储备，"a reef that bleaches in two consecutive years often cannot rebuild its algae population before the next heat wave arrives"（连续两年白化的礁往往来不及重建藻类群落），接着总结"反复事件把暂时压力变成死亡"。因此可推出 C。A、B 与"energy reserves 被消耗"矛盾，D 的"永久停止造礁"没有提到。
【English】Analysis: Paragraph 2 says a single bleaching is not automatically death, but each event costs energy reserves, and a reef that bleaches in two consecutive years often cannot rebuild its algae before the next heat wave. C follows. A and B contradict the point about depleted reserves, and permanence of limestone production is never discussed.', NOW()),
(128, 13, 'RHETORICAL PURPOSE', 'Why does the author discuss sediment, fertilizer runoff and overfishing?',
 'To prove that bleaching is caused only by human activity', 'To separate local pressures that can be managed from ocean warming that cannot', 'To suggest that reefs should be closed to all research', 'To explain why coral grows more slowly than seaweed', 'B',
 '【中文解析】修辞目的题。第三段讲完三种本地压力后立即指出：管理者可以处理这些问题（种红树林拦泥沙、限制化肥、休渔），"None of that changes the ocean temperature"，并说明科学家因此把"可本地治理的"与"取决于全球排放的"区分开。作者提这三点的目的正是划出这条界线，选 B。A 与"bleaching 由高温触发"矛盾；C 曲解了"closing reefs to fishing"；D 只是细节，不是目的。
【English】Analysis: After listing the three local pressures, the author says managers can address them but adds that none of it changes the ocean temperature, which is why scientists separate locally manageable factors from those tied to global emissions. The purpose is to draw that line; B is correct. A contradicts the heat-driven mechanism, and C misreads the fishing closure.', NOW()),
(129, 13, 'NEGATIVE FACTUAL', 'All of the following are mentioned as actions managers can take EXCEPT',
 'Replanting mangrove forests that trap sediment', 'Restricting fertilizer use near the coast', 'Closing reefs to fishing for a period', 'Adding nutrients to reefs to feed the algae', 'D',
 '【中文解析】否定事实题。第三段明确列出的本地措施是：重植拦截泥沙的红树林、限制近岸化肥使用、休渔一段时间让鱼群恢复。D 的"向礁区添加营养盐喂藻类"全文没有出现——注意营养盐过多恰恰是原文提到的问题（化肥径流滋生海藻），方向相反。所以选 D。
【English】Analysis: Paragraph 3 lists replanting mangroves, restricting coastal fertilizer use and closing reefs to fishing. Adding nutrients to feed the algae is never proposed, and excess nutrients are in fact described as a problem; D is correct.', NOW());

-- ============================ section 14 听力：论文选题 ============================
INSERT INTO tpo_section (id, tpo_id, type, content_text, audio_url, create_time) VALUES
(14, 1, 'listening', 'Student: Professor Lee, do you have a minute? I want to ask about the topic for my research paper.
Professor: Of course. Come in and sit down. What are you thinking about?
Student: I was going to write about urban farming, but the sources I found are mostly news articles. I need something with data.
Professor: That is a common problem. News coverage tells you what happened, but not how researchers measured it. Have you looked at the city agriculture survey from three years ago?
Student: I saw a reference to it, but the full report is not in our library.
Professor: The geography department keeps a copy, and I can request it through interlibrary loan. It usually takes about a week.
Student: A week is fine. The paper is due in five weeks, so I still have time.
Professor: Then here is my suggestion. Use the survey for your main evidence, and treat the news articles as background. Also narrow your topic: describe how one neighborhood changed its food supply, not the whole city.
Student: That makes sense. Should I send you an outline before I start writing?
Professor: Yes, send me one page by Friday. I will comment on the structure, not the grammar, so keep it simple.', '/audio/research-paper.wav', NOW());

INSERT INTO tpo_question (id, section_id, question_type, question_title, option_a, option_b, option_c, option_d, answer, analysis, create_time) VALUES
(130, 14, 'PURPOSE', 'Why does the student go to see the professor?',
 'To ask about a topic for a research paper', 'To complain about the library collection', 'To request a recommendation letter', 'To change the date of an exam', 'A',
 '【中文解析】目的题，学生在第一句就说清了来意："I want to ask about the topic for my research paper"（想请教论文选题）。B 的图书馆抱怨、C 的推荐信、D 的改考期都是学生向教授求助的常见事由，属于场景干扰项。目的题优先抓开头 1-2 句。
【English】Analysis: A purpose question. The student states his reason in his first sentence: he wants to ask about the topic for his research paper. B, C and D are other common reasons students visit a professor, a typical distractor pattern. Listen to the opening lines.', NOW()),
(131, 14, 'DETAIL', 'What problem does the student have with the sources on urban farming?',
 'They are written in a foreign language', 'They are more than ten years old', 'They are mostly news articles without data', 'They are all about a single neighborhood', 'C',
 '【中文解析】细节题。学生说 "the sources I found are mostly news articles. I need something with data"（找到的多是新闻稿，而自己需要带数据的材料）。教授也补充：新闻报道告诉你发生了什么，却不告诉你研究者是怎么测量的。因此选 C。A、B、D 的时间、语言、范围都是改动过的细节。
【English】Analysis: A detail question. The student says the sources he found are mostly news articles and that he needs something with data, and the professor adds that news coverage does not show how researchers measured anything. C is correct; the other options alter language, age or scope.', NOW()),
(132, 14, 'DETAIL', 'Where can the full survey report be obtained?',
 'From the campus bookstore', 'Through interlibrary loan from the geography department', 'From the professor personal collection', 'By downloading it from the news article', 'B',
 '【中文解析】细节题。学生说完整报告不在本校图书馆，教授回应："The geography department keeps a copy, and I can request it through interlibrary loan"（地理系有纸质本，可以通过馆际互借申请），并提到大概需要一周。因此选 B。注意 A、C、D 都换了来源，D 尤其错在"从新闻稿下载"。
【English】Analysis: A detail question. The student says the full report is not in the library, and the professor replies that the geography department keeps a copy and can request it through interlibrary loan, which takes about a week. B is correct; the other options substitute a different source.', NOW()),
(133, 14, 'DETAIL', 'What does the professor suggest the student do about the topic?',
 'Choose a different country to study', 'Write about the whole city instead of one area', 'Interview farmers instead of using surveys', 'Describe how one neighborhood changed its food supply', 'D',
 '【中文解析】细节题。教授的建议是 "narrow your topic: describe how one neighborhood changed its food supply, not the whole city"（把题目缩小，写一个街区而不是整座城市）。因此 D 正确。B 恰好与建议相反，A、C 的方法替换都没有出现。
【English】Analysis: A detail question. The professor advises the student to narrow the topic and describe how one neighborhood changed its food supply rather than the whole city. D is correct; B states the opposite of the advice.', NOW()),
(134, 14, 'NEXT ACTION', 'What should the student do by Friday?',
 'Send the professor a one-page outline', 'Finish the entire paper', 'Return the survey to the geography department', 'Interview three neighborhood residents', 'A',
 '【中文解析】推断下一步行动。教授说 "send me one page by Friday. I will comment on the structure, not the grammar"（周五前发一页提纲，只看结构不看语法）。因此 A 正确。B 的"完成全文"与五周后交稿的时间线矛盾；C 的归还报告、D 的访谈居民都没有被要求。
【English】Analysis: The professor asks for a one-page outline by Friday and says she will comment on structure rather than grammar, so A is correct. Finishing the whole paper conflicts with the five-week deadline, and returning the report or interviewing residents is never requested.', NOW());

-- 核对
SELECT s.id AS section_id, s.type, COUNT(q.id) AS question_count
FROM tpo_section s LEFT JOIN tpo_question q ON q.section_id = s.id
GROUP BY s.id, s.type ORDER BY s.type, s.id;
