-- 创建数据库，不存在才创建，字符集utf8mb4（支持全部emoji、生僻字）
CREATE DATABASE IF NOT EXISTS toefl_db DEFAULT CHARACTER SET utf8mb4;
USE toefl_db;

-- 用户表：保存平台账号密码
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键id',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，唯一不重复',
  `password` VARCHAR(100) NOT NULL COMMENT '密码，先明文调试，后续可改成BCrypt加密',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间'
);

-- TPO套题表：TPO1、TPO2、TPO3...
CREATE TABLE `tpo` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `tpo_no` VARCHAR(20) NOT NULL COMMENT 'TPO编号，例如 TPO01',
  `title` VARCHAR(100) COMMENT '套题备注名称',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间'
);

-- 模块表：一套TPO分为阅读/听力/口语/写作4个section
CREATE TABLE `tpo_section` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `tpo_id` BIGINT NOT NULL COMMENT '关联所属TPO套题id',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：reading / listening / speaking / writing',
  `content_text` LONGTEXT COMMENT '阅读文章 / 听力原文 / 口语题干 / 写作题干',
  `audio_url` VARCHAR(255) COMMENT '听力音频文件访问地址，阅读写作此字段为空',
  FOREIGN KEY (tpo_id) REFERENCES tpo(id)
);

-- 题目表：每个section下的小题
CREATE TABLE `tpo_question` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题目主键',
  `section_id` BIGINT NOT NULL COMMENT '关联所属模块section id',
  `question_type` VARCHAR(20) COMMENT 'single单选 / multi多选',
  `question_title` LONGTEXT COMMENT '题干文字',
  `option_a` LONGTEXT COMMENT 'A选项',
  `option_b` LONGTEXT COMMENT 'B选项',
  `option_c` LONGTEXT COMMENT 'C选项',
  `option_d` LONGTEXT COMMENT 'D选项',
  `answer` VARCHAR(50) COMMENT '正确答案，单选A/B/C/D，多选例如AC',
  `analysis` LONGTEXT COMMENT '答案解析',
  FOREIGN KEY (section_id) REFERENCES tpo_section(id)
);

-- 用户做题记录表：记录每一道做过的题，统计正确率
CREATE TABLE `user_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户id',
  `question_id` BIGINT NOT NULL COMMENT '题目id',
  `user_answer` VARCHAR(50) COMMENT '用户选择的答案',
  `is_right` TINYINT COMMENT '0错误 1正确',
  `cost_time` INT COMMENT '做题耗时，单位秒',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '做题时间'
);

-- 错题本表：用户手动加入错题的题目，唯一约束防止重复添加同一错题
CREATE TABLE `user_wrong` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户id',
  `question_id` BIGINT NOT NULL COMMENT '题目id',
  `add_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入错题本时间',
  UNIQUE KEY uk_user_question(user_id,question_id)
);
-- 1. 套题主表
CREATE TABLE IF NOT EXISTS tpo_suite (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '套题id',
  title VARCHAR(100) NOT NULL COMMENT '套题名称，如 TPO 1',
  description VARCHAR(255) NULL COMMENT '描述',
  reading_minutes INT NOT NULL DEFAULT 60 COMMENT '阅读限时分钟',
  listening_minutes INT NOT NULL DEFAULT 35 COMMENT '听力限时分钟',
  speaking_minutes INT NOT NULL DEFAULT 20 COMMENT '口语限时分钟',
  writing_minutes INT NOT NULL DEFAULT 50 COMMENT '写作限时分钟',
  create_time DATETIME NULL,
  create_by VARCHAR(50) NULL COMMENT '创建人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='真题套题表';

-- 2. 套题-篇章关联表（决定套题里每个模块用哪几篇）
CREATE TABLE IF NOT EXISTS tpo_suite_section (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  suite_id BIGINT NOT NULL COMMENT '套题id',
  module_type VARCHAR(20) NOT NULL COMMENT 'reading/listening/speaking/writing',
  section_id BIGINT NOT NULL COMMENT 'tpo_section.id',
  sort_index INT NOT NULL DEFAULT 0 COMMENT '模块内排序',
  KEY idx_suite (suite_id),
  KEY idx_module (suite_id, module_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套题篇章关联表';

-- 3. 整卷作答记录表
CREATE TABLE IF NOT EXISTS tpo_exam_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  suite_id BIGINT NOT NULL COMMENT '套题id',
  user_id BIGINT NOT NULL COMMENT '用户id',
  correct_count INT NOT NULL DEFAULT 0 COMMENT '客观题答对数',
  total_count INT NOT NULL DEFAULT 0 COMMENT '客观题总数',
  reading_correct INT NOT NULL DEFAULT 0,
  reading_total INT NOT NULL DEFAULT 0,
  listening_correct INT NOT NULL DEFAULT 0,
  listening_total INT NOT NULL DEFAULT 0,
  speaking_count INT NOT NULL DEFAULT 0 COMMENT '口语已答题数',
  writing_count INT NOT NULL DEFAULT 0 COMMENT '写作已答题数',
  cost_seconds INT NOT NULL DEFAULT 0 COMMENT '整卷用时秒',
  create_time DATETIME NULL,
  KEY idx_user (user_id),
  KEY idx_suite (suite_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整卷作答记录表';

-- 4. 整卷答题明细表
CREATE TABLE IF NOT EXISTS tpo_exam_answer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id BIGINT NOT NULL COMMENT '作答记录id',
  question_id BIGINT NOT NULL,
  module_type VARCHAR(20) NULL,
  user_answer TEXT NULL COMMENT '我的答案，空表示未作答',
  correct_answer VARCHAR(255) NULL COMMENT '正确答案/参考答案',
  is_correct TINYINT NOT NULL DEFAULT 0 COMMENT '1对 0错',
  KEY idx_record (record_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整卷答题明细表';
CREATE TABLE exam_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户id',
  suite_id BIGINT NOT NULL COMMENT '套题id',
  score INT DEFAULT 0 COMMENT '得分',
  exam_status VARCHAR(20) DEFAULT 'doing' COMMENT 'doing进行中 / finish已完成',
  start_time DATETIME COMMENT '开始时间',
  end_time DATETIME COMMENT '结束时间',
  create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
