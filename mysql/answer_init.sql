-- 建议使用 MySQL 8.0+（支持 CHECK 约束）
-- 字符集统一使用 utf8mb4，排序规则使用 utf8mb4_0900_ai_ci，能良好支持中文

-- 1) 建库与字符集
CREATE DATABASE IF NOT EXISTS quiz
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE quiz;

-- 2) 幂等清理（先删子表，再删父表）
DROP TABLE IF EXISTS answer_history;


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
  CONSTRAINT chk_answer_history_selected_option CHECK (selected_option BETWEEN 1 AND 4),
  CONSTRAINT chk_answer_history_is_correct CHECK (is_correct IN (0, 1)),
  CONSTRAINT chk_answer_history_is_deleted CHECK (is_deleted IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='答题记录表';

-- 测试答题记录：一次正确、一次错误
INSERT INTO answer_history (user_id, question_id, selected_option, is_correct, answer_time)
VALUES
(1, 1, 1, 1, NOW()),  -- 选择北京，正确
(1, 2, 1, 0, NOW());  -- 选择2，错误（正确答案应为选项2：3）
