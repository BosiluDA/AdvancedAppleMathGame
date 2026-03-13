-- ===========================================================
-- Apple Math Puzzle Game - Full AAA Database Setup
-- Compatible with: MySQL 5.7+ / MariaDB 10.3+ (XAMPP)
--
-- HOW TO RUN:
--   phpMyAdmin -> Import tab -> Choose File -> Go
--   OR: mysql -u root < apple_math_game.sql
-- ===========================================================

CREATE DATABASE IF NOT EXISTS apple_math_game
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE apple_math_game;


-- ===========================================================
-- TABLE: users
-- Authentication + Authorization
-- ===========================================================

CREATE TABLE IF NOT EXISTS users (
    id              INT           NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)   NOT NULL,
    email           VARCHAR(100)  NOT NULL DEFAULT '',
    password_hash   VARCHAR(255)  NOT NULL,
    salt            VARCHAR(64)   NOT NULL,
    role            VARCHAR(10)   NOT NULL DEFAULT 'player',
    is_active       TINYINT(1)    NOT NULL DEFAULT 1,
    is_locked       TINYINT(1)    NOT NULL DEFAULT 0,
    email_verified  TINYINT(1)    NOT NULL DEFAULT 0,
    verify_token    VARCHAR(64)   NULL DEFAULT NULL,
    failed_attempts INT           NOT NULL DEFAULT 0,
    lockout_until   TIMESTAMP     NULL DEFAULT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login      TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email)
);


-- ===========================================================
-- TABLE: sessions
-- Authorization tokens (8-hour expiry)
-- ===========================================================

CREATE TABLE IF NOT EXISTS sessions (
    id           INT          NOT NULL AUTO_INCREMENT,
    user_id      INT          NOT NULL,
    token        VARCHAR(128) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address   VARCHAR(45)  NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_token (token),
    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- ===========================================================
-- TABLE: scores
-- Accounting - every game result
-- ===========================================================

CREATE TABLE IF NOT EXISTS scores (
    id          INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    score       INT          NOT NULL DEFAULT 0,
    mode        VARCHAR(30)  NOT NULL,
    level       INT          NOT NULL DEFAULT 0,
    time_taken  INT          NOT NULL DEFAULT 0,
    streak      INT          NOT NULL DEFAULT 0,
    played_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_scores_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- ===========================================================
-- TABLE: level_progress
-- Accounting - per-user level tracking with attempt count
-- ===========================================================

CREATE TABLE IF NOT EXISTS level_progress (
    id            INT       NOT NULL AUTO_INCREMENT,
    user_id       INT       NOT NULL,
    level         INT       NOT NULL,
    best_score    INT       NOT NULL DEFAULT 0,
    attempt_count INT       NOT NULL DEFAULT 0,
    completed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_level (user_id, level),
    CONSTRAINT fk_progress_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- ===========================================================
-- TABLE: audit_log
-- Accounting - full activity trail
-- Tracks: logins, failures, lockouts, score saves, admin actions
-- ===========================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id         INT          NOT NULL AUTO_INCREMENT,
    user_id    INT          NULL,
    username   VARCHAR(50)  NULL,
    action     VARCHAR(100) NOT NULL,
    detail     TEXT         NULL,
    ip_address VARCHAR(45)  NULL DEFAULT NULL,
    logged_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);


-- ===========================================================
-- INDEXES
-- ===========================================================

ALTER TABLE scores
    ADD INDEX idx_scores_user  (user_id),
    ADD INDEX idx_scores_score (score),
    ADD INDEX idx_scores_time  (time_taken);

ALTER TABLE audit_log
    ADD INDEX idx_audit_user   (user_id),
    ADD INDEX idx_audit_action (action),
    ADD INDEX idx_audit_time   (logged_at);

ALTER TABLE sessions
    ADD INDEX idx_sessions_exp (expires_at);

ALTER TABLE users
    ADD INDEX idx_users_lockout (lockout_until);


-- ===========================================================
-- VIEWS
-- ===========================================================

CREATE OR REPLACE VIEW v_top_scores AS
    SELECT u.username, s.score, s.mode, s.level, s.streak, s.played_at
    FROM scores s
    INNER JOIN users u ON s.user_id = u.id
    ORDER BY s.score DESC
    LIMIT 100;

CREATE OR REPLACE VIEW v_fastest_times AS
    SELECT u.username, s.time_taken, s.level, s.score, s.played_at
    FROM scores s
    INNER JOIN users u ON s.user_id = u.id
    WHERE s.mode LIKE 'Memory%'
    ORDER BY s.time_taken ASC
    LIMIT 100;

CREATE OR REPLACE VIEW v_user_progress AS
    SELECT u.username, lp.level, lp.best_score, lp.attempt_count, lp.completed_at
    FROM level_progress lp
    INNER JOIN users u ON lp.user_id = u.id
    ORDER BY u.username ASC, lp.level ASC;

CREATE OR REPLACE VIEW v_audit_recent AS
    SELECT a.id, a.username, a.action, a.detail, a.logged_at
    FROM audit_log a
    ORDER BY a.logged_at DESC
    LIMIT 500;

CREATE OR REPLACE VIEW v_failed_logins AS
    SELECT username, COUNT(*) AS attempts, MAX(logged_at) AS last_attempt
    FROM audit_log
    WHERE action = 'LOGIN_FAILED'
    GROUP BY username
    ORDER BY attempts DESC;

CREATE OR REPLACE VIEW v_player_summary AS
    SELECT
        u.id,
        u.username,
        u.role,
        u.is_active,
        u.email_verified,
        u.failed_attempts,
        u.created_at,
        u.last_login,
        COUNT(DISTINCT s.id)     AS total_games,
        MAX(s.score)             AS best_score,
        COUNT(DISTINCT lp.level) AS levels_completed
    FROM users u
    LEFT JOIN scores s          ON s.user_id  = u.id
    LEFT JOIN level_progress lp ON lp.user_id = u.id
    GROUP BY u.id, u.username, u.role, u.is_active,
             u.email_verified, u.failed_attempts,
             u.created_at, u.last_login;


-- ===========================================================
-- VERIFY
-- ===========================================================

SELECT
    table_name  AS 'Table',
    table_rows  AS 'Rows (approx)',
    create_time AS 'Created'
FROM information_schema.tables
WHERE table_schema = 'apple_math_game'
ORDER BY table_name;
