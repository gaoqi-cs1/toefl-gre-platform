-- =====================================================================
-- 口语录音落库：full_exam_answer 新增 audio_url
-- 作用：套题模考里口语题录音后，成绩页能回放这段录音。
-- 可重复执行（已存在列就跳过）
-- =====================================================================

SET NAMES utf8mb4;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'toefl_db' AND TABLE_NAME = 'full_exam_answer' AND COLUMN_NAME = 'audio_url');
SET @s := IF(@c = 0, 'ALTER TABLE full_exam_answer ADD COLUMN audio_url VARCHAR(255) NULL COMMENT ''口语录音地址'' AFTER user_answer', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SHOW COLUMNS FROM full_exam_answer LIKE 'audio_url';
