-- 建议使用 MySQL 8.0+（支持 CHECK 约束）
-- 字符集统一使用 utf8mb4，排序规则使用 utf8mb4_0900_ai_ci，能良好支持中文

-- 1) 建库与字符集
CREATE DATABASE IF NOT EXISTS quiz
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE quiz;

-- 2) 幂等清理（先删子表，再删父表）
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

-- 6) 测试数据（使用变量保存自增ID，保证可重复执行）
-- 测试用户
INSERT INTO users (user_name, user_password, user_role)
VALUES ('test', '$2a$10$JSNNpBixuDCEQJGeZNy9V.7NMibUl5WLimdMDg8TRRpDMrOMoYMi2', 0); -- 仅用于测试，生产请存储加密后的密码