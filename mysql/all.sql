-- 建议使用 MySQL 8.0+（支持 CHECK 约束）
-- 字符集统一使用 utf8mb4，排序规则使用 utf8mb4_0900_ai_ci，能良好支持中文

-- 1) 建库与字符集
CREATE DATABASE IF NOT EXISTS quiz
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE quiz;

-- 2) 幂等清理（先删子表，再删父表）
DROP TABLE IF EXISTS answer_history;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS users;

-- 3) 用户表（避免使用保留字 user，改为 users）
CREATE TABLE users (
  user_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  user_name     VARCHAR(256) NOT NULL COMMENT '用户名，唯一',
  user_password VARCHAR(512) NOT NULL COMMENT '登录密码（建议加密存储，如bcrypt）',
  user_role     TINYINT NOT NULL DEFAULT 0 COMMENT '用户角色：0-普通用户，1-管理员',
  is_deleted    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除（软删除标记）',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_user_name (user_name),
  KEY idx_users_is_deleted (is_deleted),
  CONSTRAINT chk_users_user_role CHECK (user_role IN (0, 1)),
  CONSTRAINT chk_users_is_deleted CHECK (is_deleted IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户表';

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

-- 5) 答题记录表（外键：users、questions）
CREATE TABLE answer_history (
  answer_history_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '答题记录主键ID',
  user_id           BIGINT NOT NULL COMMENT '关联的用户ID（外键）',
  question_id       INT NOT NULL COMMENT '关联的题目ID（外键）',
  selected_option   TINYINT NOT NULL COMMENT '用户选择的选项：1-4',
  is_correct        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否答对：0-错误，1-正确',
  answer_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  is_deleted        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除（软删除标记）',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (answer_history_id),
  KEY idx_answer_history_user_id (user_id),
  KEY idx_answer_history_question_id (question_id),
  CONSTRAINT fk_answer_history_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_answer_history_question
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_answer_history_selected_option CHECK (selected_option BETWEEN 1 AND 4),
  CONSTRAINT chk_answer_history_is_correct CHECK (is_correct IN (0, 1)),
  CONSTRAINT chk_answer_history_is_deleted CHECK (is_deleted IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='答题记录表';

-- 6) 测试数据（使用变量保存自增ID，保证可重复执行）
-- 测试用户
INSERT INTO users (user_name, user_password, user_role)
VALUES ('test_user', '123456', 0); -- 仅用于测试，生产请存储加密后的密码
SET @u1 := LAST_INSERT_ID();

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
SET @q1 := LAST_INSERT_ID();

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
SET @q2 := LAST_INSERT_ID();

-- 测试答题记录：一次正确、一次错误
INSERT INTO answer_history (user_id, question_id, selected_option, is_correct, answer_time)
VALUES
(@u1, @q1, 1, 1, NOW()),  -- 选择北京，正确
(@u1, @q2, 1, 0, NOW());  -- 选择2，错误（正确答案应为选项2：3）
