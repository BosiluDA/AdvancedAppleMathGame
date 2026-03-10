-- ============================================================
--  Apple Math Puzzle Game — XAMPP MySQL Setup
--  Run this in phpMyAdmin or MySQL CLI:
--    mysql -u root -p < apple_math_game.sql
-- ============================================================

-- 1. Create & select the database
CREATE DATABASE IF NOT EXISTS apple_math_game
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE apple_math_game;

-- ============================================================
--  USERS  (Authentication)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt          VARCHAR(64)  NOT NULL,
    role          ENUM('player','admin') DEFAULT 'player',
    is_active     TINYINT(1)   DEFAULT 1,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP    NULL
);

-- ============================================================
--  SESSIONS  (Authorization tokens)
-- ============================================================
CREATE TABLE IF NOT EXISTS sessions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT          NOT NULL,
    token        VARCHAR(128) UNIQUE NOT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP    NOT NULL,
    ip_address   VARCHAR(45),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
--  SCORES  (Accounting — game results)
-- ============================================================
CREATE TABLE IF NOT EXISTS scores (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    score       INT          NOT NULL DEFAULT 0,
    mode        VARCHAR(30)  NOT NULL,
    level       INT          DEFAULT 0,
    time_taken  INT          DEFAULT 0,
    streak      INT          DEFAULT 0,
    played_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
--  LEVEL_PROGRESS  (Per-user level unlock tracking)
-- ============================================================
CREATE TABLE IF NOT EXISTS level_progress (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    level        INT NOT NULL,
    best_score   INT DEFAULT 0,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_level (user_id, level),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
--  AUDIT_LOG  (Accounting — full activity trail)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NULL,
    username   VARCHAR(50),
    action     VARCHAR(100) NOT NULL,
    detail     TEXT,
    ip_address VARCHAR(45),
    logged_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
--  INDEXES  (Performance)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_scores_user    ON scores(user_id);
CREATE INDEX IF NOT EXISTS idx_scores_score   ON scores(score DESC);
CREATE INDEX IF NOT EXISTS idx_scores_time    ON scores(time_taken ASC);
CREATE INDEX IF NOT EXISTS idx_audit_user     ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action   ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token);
CREATE INDEX IF NOT EXISTS idx_sessions_exp   ON sessions(expires_at);

-- ============================================================
--  SEED DATA  (Default admin account)
--  Username : admin
--  Password : admin123
--  NOTE: Change this password immediately after first login!
--  Hash below = SHA-256 x10000 of "SEED_SALT_HEREadmin123SEED_SALT_HERE"
-- ============================================================
INSERT IGNORE INTO users (username, password_hash, salt, role) VALUES (
    'admin',
    -- Re-generate via the app's registration flow for real deployments
    'CHANGE_ME_register_via_app',
    'SEED_SALT_HERE',
    'admin'
);

-- ============================================================
--  USEFUL VIEWS  (optional, for phpMyAdmin browsing)
-- ============================================================

CREATE OR REPLACE VIEW v_leaderboard_scores AS
    SELECT u.username, s.score, s.mode, s.level, s.played_at
    FROM scores s
    JOIN users u ON s.user_id = u.id
    ORDER BY s.score DESC
    LIMIT 100;

CREATE OR REPLACE VIEW v_leaderboard_times AS
    SELECT u.username, s.time_taken, s.level, s.score, s.played_at
    FROM scores s
    JOIN users u ON s.user_id = u.id
    WHERE s.mode LIKE 'Memory%'
    ORDER BY s.time_taken ASC
    LIMIT 100;

CREATE OR REPLACE VIEW v_user_progress AS
    SELECT u.username, lp.level, lp.best_score, lp.completed_at
    FROM level_progress lp
    JOIN users u ON lp.user_id = u.id
    ORDER BY u.username, lp.level;

CREATE OR REPLACE VIEW v_audit_recent AS
    SELECT id, username, action, detail, logged_at
    FROM audit_log
    ORDER BY logged_at DESC
    LIMIT 500;

-- ============================================================
--  CLEANUP JOB  (Remove expired sessions — run periodically)
--  You can set this up as a MySQL Event Scheduler job:
-- ============================================================
-- CREATE EVENT IF NOT EXISTS cleanup_expired_sessions
--     ON SCHEDULE EVERY 1 HOUR
--     DO DELETE FROM sessions WHERE expires_at < NOW();

-- ============================================================
--  DONE
-- ============================================================
SELECT 'apple_math_game database ready ✅' AS status;
