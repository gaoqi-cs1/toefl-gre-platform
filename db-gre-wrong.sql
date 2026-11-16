-- ============================================================
-- GRE 错题本（独立表，不并入托福错题本 wrong_book）
-- 说明：分块练习答错、套题提交时判错的题都会写进这张表，
--       同一用户同一题只留一条，wrong_count 累加，掌握后用 mastered=1 标记。
-- 执行：mysql -u root -p toefl_db < db-gre-wrong.sql
-- ============================================================

USE toefl_db;

CREATE TABLE IF NOT EXISTS gre_wrong (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户id',
    question_id BIGINT NOT NULL COMMENT '题目id（scope=practice 时是 gre_question.id，scope=suite 时是 gre_suite_question.id）',
    question_scope VARCHAR(20) DEFAULT 'practice' COMMENT 'practice=分块练习题 / suite=套题里的题（两套题库 id 独立，靠这个区分）',
    section_id BIGINT DEFAULT NULL COMMENT '来源练习单元',
    module_type VARCHAR(20) DEFAULT NULL COMMENT 'writing / verbal / quant',
    task_type VARCHAR(40) DEFAULT NULL COMMENT 'text_completion / reading / quant_compare ...',
    question_title VARCHAR(2000) DEFAULT NULL COMMENT '题干快照，题库改了也不影响回看',
    option_a VARCHAR(1000) DEFAULT NULL,
    option_b VARCHAR(1000) DEFAULT NULL,
    option_c VARCHAR(1000) DEFAULT NULL,
    option_d VARCHAR(1000) DEFAULT NULL,
    option_e VARCHAR(1000) DEFAULT NULL,
    option_f VARCHAR(1000) DEFAULT NULL,
    correct_answer VARCHAR(200) DEFAULT NULL COMMENT '正确答案快照',
    user_answer VARCHAR(4000) DEFAULT NULL COMMENT '最近一次的错误作答',
    analysis TEXT COMMENT '解析快照（中英双语）',
    wrong_count INT DEFAULT 1 COMMENT '累计错几次',
    last_source VARCHAR(20) DEFAULT 'practice' COMMENT '最近一次来自 practice 还是 exam',
    mastered TINYINT DEFAULT 0 COMMENT '1=我标记已掌握，列表默认不显示',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gre_wrong_user_q (user_id, question_id, question_scope),
    KEY idx_gre_wrong_module (module_type, mastered)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'GRE 错题本';

SELECT COUNT(*) AS gre_wrong_rows FROM gre_wrong;
