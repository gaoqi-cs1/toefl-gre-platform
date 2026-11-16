-- GRE 计时口径对齐真实考试（2026-11-14）
-- ---------------------------------------------------------------
-- 真实 GRE 的节奏：Verbal 每个 section 30 分钟 / 20 题 ≈ 90 秒一题；
-- Quant 每个 section 35 分钟 / 20 题 ≈ 105 秒一题；AWA 两篇共 60 分钟，每篇 30 分钟。
-- 本项目的一个 section 题量远小于真实 section，所以按「每题秒数 × 题量」折算成时限，
-- 而不是直接给 30/35 分钟，否则练一个 4 题的填空单元会有半小时，完全不像考试。
--
-- 下限 300 秒（5 分钟），避免 1 题的小单元时限短到无法操作；
-- 写作封顶 3600 秒（60 分钟），与真实 AWA 总时长一致。

-- ============ 1) 分块练习单元 ============
UPDATE gre_section s
SET s.limit_second = GREATEST(
    300,
    CASE s.module_type
        WHEN 'verbal'  THEN (SELECT COUNT(*) FROM gre_question q WHERE q.section_id = s.id) * 90
        WHEN 'quant'   THEN (SELECT COUNT(*) FROM gre_question q WHERE q.section_id = s.id) * 105
        WHEN 'writing' THEN LEAST(3600, (SELECT COUNT(*) FROM gre_question q WHERE q.section_id = s.id) * 1800)
        ELSE s.limit_second
    END
);

-- ============ 2) 套题内的 section ============
-- 套题的题存在 gre_suite_question，用 section_id = gre_suite_section.id 关联
UPDATE gre_suite_section ss
SET ss.limit_second = GREATEST(
    300,
    CASE ss.module_type
        WHEN 'verbal'  THEN (SELECT COUNT(*) FROM gre_suite_question q WHERE q.section_id = ss.id) * 90
        WHEN 'quant'   THEN (SELECT COUNT(*) FROM gre_suite_question q WHERE q.section_id = ss.id) * 105
        WHEN 'writing' THEN LEAST(3600, (SELECT COUNT(*) FROM gre_suite_question q WHERE q.section_id = ss.id) * 1800)
        ELSE ss.limit_second
    END
);

-- 核对：分块练习
SELECT id, module_type, title, limit_second,
       (SELECT COUNT(*) FROM gre_question q WHERE q.section_id = gre_section.id) AS qn
FROM gre_section ORDER BY module_type, sort_index;

-- 核对：套题各 section
SELECT gs.id AS suite, ss.module_type, ss.limit_second,
       (SELECT COUNT(*) FROM gre_suite_question q WHERE q.section_id = ss.id) AS qn
FROM gre_suite gs JOIN gre_suite_section ss ON ss.suite_id = gs.id
ORDER BY gs.id, ss.sort_index;
