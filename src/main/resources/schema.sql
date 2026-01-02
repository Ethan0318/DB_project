CREATE DATABASE IF NOT EXISTS collab_edit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE collab_edit;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(128) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  status TINYINT DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  last_login_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  nickname VARCHAR(64),
  avatar_url VARCHAR(255),
  phone VARCHAR(32),
  bio VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_user_profile_phone ON user_profile(phone);

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64),
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  created_by BIGINT,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  owner_id BIGINT NOT NULL,
  tag_id BIGINT,
  status VARCHAR(32),
  search_text TEXT,
  deleted TINYINT DEFAULT 0,
  created_at DATETIME,
  updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_content (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL UNIQUE,
  content LONGTEXT,
  updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  content LONGTEXT,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_acl (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  perm VARCHAR(16) NOT NULL,
  created_at DATETIME,
  UNIQUE KEY uk_doc_user (doc_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  anchor TEXT,
  parent_id BIGINT,
  at_user_id BIGINT,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  payload TEXT,
  read_flag TINYINT DEFAULT 0,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS op_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  action VARCHAR(64) NOT NULL,
  detail TEXT,
  ip VARCHAR(64),
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_reset_codes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  code VARCHAR(16) NOT NULL,
  expires_at DATETIME,
  used TINYINT DEFAULT 0,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS doc_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  content LONGTEXT NOT NULL,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS doc_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description TEXT,
  assignee_id BIGINT,
  status VARCHAR(16) NOT NULL,
  due_date DATETIME,
  created_by BIGINT,
  created_at DATETIME,
  updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  edit_enabled TINYINT DEFAULT 1,
  comment_enabled TINYINT DEFAULT 1,
  task_enabled TINYINT DEFAULT 1,
  share_enabled TINYINT DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS survey_responses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  rating TINYINT,
  feedback TEXT,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_documents_tag ON documents(tag_id);
CREATE INDEX idx_documents_updated ON documents(updated_at);
CREATE FULLTEXT INDEX idx_documents_search ON documents(title, search_text);
CREATE INDEX idx_comments_doc ON comments(doc_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_chat_doc ON chat_messages(doc_id);
CREATE INDEX idx_tasks_doc ON doc_tasks(doc_id);
