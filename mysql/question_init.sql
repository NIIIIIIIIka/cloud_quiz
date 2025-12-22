-- 建议使用 MySQL 8.0+（支持 CHECK 约束）
-- 字符集统一使用 utf8mb4，排序规则使用 utf8mb4_0900_ai_ci，能良好支持中文

-- 1) 建库与字符集
CREATE DATABASE IF NOT EXISTS quiz
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE quiz;

-- 2) 幂等清理（先删子表，再删父表）
DROP TABLE IF EXISTS questions;

-- 4) 题目表（保持四个选项结构；可按需后续做范式化为选项子表）
CREATE TABLE questions (
  question_id       INT NOT NULL AUTO_INCREMENT COMMENT '题目主键ID',
  question_text     VARCHAR(255) NOT NULL COMMENT '题目内容',
  answer1_text      VARCHAR(255) NOT NULL COMMENT '选项1内容',
  answer1_correct   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '选项1是否正确：0/1',
  answer2_text      VARCHAR(255) NOT NULL COMMENT '选项2内容',
  answer2_correct   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '选项2是否正确：0/1',
  answer3_text      VARCHAR(255) NOT NULL COMMENT '选项3内容',
  answer3_correct   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '选项3是否正确：0/1',
  answer4_text      VARCHAR(255) NOT NULL COMMENT '选项4内容',
  answer4_correct   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '选项4是否正确：0/1',
  is_deleted        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除（软删除标记）',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (question_id),
  KEY idx_questions_is_deleted (is_deleted),
  CONSTRAINT chk_questions_flags_01 CHECK (
    answer1_correct IN (0,1) AND
    answer2_correct IN (0,1) AND
    answer3_correct IN (0,1) AND
    answer4_correct IN (0,1)
  ),
  CONSTRAINT chk_questions_at_least_one_correct CHECK (
    (answer1_correct + answer2_correct + answer3_correct + answer4_correct) >= 1
  ),
  CONSTRAINT chk_questions_is_deleted CHECK (is_deleted IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='题目表';

-- 测试题目 1
INSERT INTO questions (
  question_text,
  answer1_text, answer1_correct,
  answer2_text, answer2_correct,
  answer3_text, answer3_correct,
  answer4_text, answer4_correct
) VALUES (
  '中国的首都是哪座城市？',
  '北京', 1,
  '上海', 0,
  '广州', 0,
  '深圳', 0
);

-- 测试题目 2
INSERT INTO questions (
  question_text,
  answer1_text, answer1_correct,
  answer2_text, answer2_correct,
  answer3_text, answer3_correct,
  answer4_text, answer4_correct
) VALUES (
  '以下哪一个是奇数？',
  '2', 0,
  '3', 1,
  '4', 0,
  '6', 0
);