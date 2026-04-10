package applegame;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DataBaseManager {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/apple_math_game"
                                        + "?useSSL=false&serverTimezone=UTC"
                                        + "&allowPublicKeyRetrieval=true&autoReconnect=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES     = 1;

    private Connection connection;
    private boolean connected = false;

    public DataBaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connected  = true;
            System.out.println("✅ MySQL connected");
            createTablesIfNotExist();
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL driver not found");
        } catch (SQLException e) {
            System.out.println("❌ MySQL connection failed: " + e.getMessage());
        }
    }

    public boolean isConnected() { return connected && connection != null; }

    // ─── TABLE CREATION ───────────────────────────────────────────────────────

    private void createTablesIfNotExist() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id                  INT AUTO_INCREMENT PRIMARY KEY," +
                "  username            VARCHAR(50) UNIQUE NOT NULL," +
                "  email               VARCHAR(100) UNIQUE NOT NULL DEFAULT ''," +
                "  password_hash       VARCHAR(255) NOT NULL," +
                "  salt                VARCHAR(64) NOT NULL," +
                "  role                ENUM('player','admin') DEFAULT 'player'," +
                "  is_active           TINYINT(1) DEFAULT 1," +
                "  is_locked           TINYINT(1) DEFAULT 0," +
                "  email_verified      TINYINT(1) DEFAULT 0," +
                "  verify_token        VARCHAR(64) NULL," +
                "  failed_attempts     INT DEFAULT 0," +
                "  lockout_until       TIMESTAMP NULL DEFAULT NULL," +
                "  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  last_login          TIMESTAMP NULL DEFAULT NULL" +
                ")");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                "  id           INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id      INT NOT NULL," +
                "  token        VARCHAR(128) UNIQUE NOT NULL," +
                "  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  expires_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  ip_address   VARCHAR(45)," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS scores (" +
                "  id          INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id     INT NOT NULL," +
                "  username    VARCHAR(50) NOT NULL," +
                "  score       INT NOT NULL DEFAULT 0," +
                "  mode        VARCHAR(30) NOT NULL," +
                "  level       INT DEFAULT 0," +
                "  time_taken  INT DEFAULT 0," +
                "  streak      INT DEFAULT 0," +
                "  played_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS level_progress (" +
                "  id            INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id       INT NOT NULL," +
                "  level         INT NOT NULL," +
                "  best_score    INT DEFAULT 0," +
                "  attempt_count INT DEFAULT 0," +
                "  completed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uq_user_level (user_id, level)," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS audit_log (" +
                "  id         INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id    INT NULL," +
                "  username   VARCHAR(50)," +
                "  action     VARCHAR(100) NOT NULL," +
                "  detail     TEXT," +
                "  ip_address VARCHAR(45)," +
                "  logged_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            System.out.println("✅ Tables verified");
        }
    }

    // ─── PASSWORD ─────────────────────────────────────────────────────────────

    private String generateSalt() {
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            String salted = salt + password + salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = salted.getBytes("UTF-8");
            for (int i = 0; i < 10_000; i++) hash = md.digest(hash);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /** 0=weak 1=fair 2=good 3=strong */
    public static int passwordStrength(String pw) {
        if (pw == null || pw.length() < 6) return 0;
        int score = 0;
        if (pw.length() >= 8)  score++;
        if (pw.matches(".*[A-Z].*")) score++;
        if (pw.matches(".*[0-9].*")) score++;
        if (pw.matches(".*[^a-zA-Z0-9].*")) score++;
        return Math.min(score, 3);
    }

    public static String[] strengthLabel = {"Weak", "Fair", "Good", "Strong"};

    // ─── AUTHENTICATION ───────────────────────────────────────────────────────

    public LoginResult login(String username, String password) {
        if (!isConnected() || username == null || password == null)
            return LoginResult.error("No database connection.");
        username = username.trim();

        String sql = "SELECT id, username, password_hash, salt, role, is_active, " +
                     "is_locked, email_verified, failed_attempts, lockout_until " +
                     "FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                audit(null, username, "LOGIN_FAILED", "Unknown username");
                return LoginResult.error("Invalid username or password.");
            }
            int     uid            = rs.getInt("id");
            String  hash           = rs.getString("password_hash");
            String  salt           = rs.getString("salt");
            String  role           = rs.getString("role");
            boolean active         = rs.getBoolean("is_active");
            boolean locked         = rs.getBoolean("is_locked");
            boolean emailVerified  = rs.getBoolean("email_verified");
            int     failedAttempts = rs.getInt("failed_attempts");
            Timestamp lockoutUntil = rs.getTimestamp("lockout_until");

            // Check account disabled by admin
            if (!active) {
                audit(uid, username, "LOGIN_DENIED", "Account disabled by admin");
                return LoginResult.error("Your account has been disabled. Contact an admin.");
            }

            // Check lockout cooldown — compare entirely inside MySQL to avoid timezone issues
            if (lockoutUntil != null) {
                try (PreparedStatement chk = connection.prepareStatement(
                        "SELECT GREATEST(0, TIMESTAMPDIFF(SECOND, NOW(), lockout_until)) AS secs_left " +
                        "FROM users WHERE id=?")) {
                    chk.setInt(1, uid);
                    ResultSet lr = chk.executeQuery();
                    if (lr.next()) {
                        long secsLeft = lr.getLong("secs_left");
                        if (secsLeft > 0) {
                            audit(uid, username, "LOGIN_DENIED", "Account locked, " + secsLeft + "s remaining");
                            return LoginResult.locked("Too many attempts. Wait " + secsLeft + "s...");
                        } else {
                            // Expired — clear it in DB
                            try (PreparedStatement clr = connection.prepareStatement(
                                    "UPDATE users SET lockout_until=NULL, failed_attempts=0 WHERE id=?")) {
                                clr.setInt(1, uid);
                                clr.executeUpdate();
                            }
                            lockoutUntil = null;
                        }
                    }
                }
            }

            // Check email verified
            if (!emailVerified) {
                audit(uid, username, "LOGIN_DENIED", "Email not verified");
                return LoginResult.error("Please verify your email before logging in.");
            }

            // Check password
            String attempt = hashPassword(password, salt);
            if (!attempt.equals(hash)) {
                int newFails = failedAttempts + 1;
                if (newFails >= MAX_FAILED_ATTEMPTS) {
                    // Lock the account — reset counter to 0 so after cooldown expires
                    // the user gets a fresh set of attempts
                    try (PreparedStatement upd = connection.prepareStatement(
                            "UPDATE users SET failed_attempts=0, lockout_until=DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id=?")) {
                        upd.setInt(1, LOCKOUT_MINUTES);
                        upd.setInt(2, uid);
                        upd.executeUpdate();
                    }
                    audit(uid, username, "ACCOUNT_LOCKED",
                          "Locked for " + LOCKOUT_MINUTES + " min after " + newFails + " failed attempts");
                    return LoginResult.locked("Account locked for " + LOCKOUT_MINUTES +
                                              " minute after " + MAX_FAILED_ATTEMPTS + " failed attempts.");
                } else {
                    try (PreparedStatement upd = connection.prepareStatement(
                            "UPDATE users SET failed_attempts=? WHERE id=?")) {
                        upd.setInt(1, newFails);
                        upd.setInt(2, uid);
                        upd.executeUpdate();
                    }
                    int remaining = MAX_FAILED_ATTEMPTS - newFails;
                    audit(uid, username, "LOGIN_FAILED",
                          "Wrong password, attempt " + newFails + "/" + MAX_FAILED_ATTEMPTS);
                    return LoginResult.error("Invalid username or password. " + remaining + " attempt(s) left.");
                }
            }

            // Success — reset failed attempts
            try (PreparedStatement upd = connection.prepareStatement(
                    "UPDATE users SET failed_attempts=0, lockout_until=NULL, last_login=NOW() WHERE id=?")) {
                upd.setInt(1, uid);
                upd.executeUpdate();
            }

            String token = createSession(uid);
            audit(uid, username, "LOGIN_OK", "role=" + role);
            return LoginResult.success(new UserRecord(uid, username, role, token));

        } catch (SQLException e) {
            System.out.println("❌ Login error: " + e.getMessage());
            return LoginResult.error("Database error: " + e.getMessage());
        }
    }

    /** Returns null on success, error string on failure. */
    public String register(String username, String email, String password) {
        if (!isConnected()) return "No database connection.";

        // Username validation
        if (username == null || username.trim().length() < 3)
            return "Username must be at least 3 characters.";
        if (!username.matches("[a-zA-Z0-9_]+"))
            return "Username may only contain letters, numbers, and underscores.";
        if (username.length() > 20)
            return "Username must be 20 characters or fewer.";

        // Email validation
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return "Please enter a valid email address.";

        // Password validation
        if (password == null || password.length() < 6)
            return "Password must be at least 6 characters.";

        username = username.trim();
        email    = email.trim().toLowerCase();

        if (userExists(username)) return "Username '" + username + "' is already taken.";
        if (emailExists(email))   return "An account with that email already exists.";

        String salt        = generateSalt();
        String hash        = hashPassword(password, salt);
        String verifyToken = generateVerifyToken();

        String sql = "INSERT INTO users (username, email, password_hash, salt, role, email_verified, verify_token) " +
                     "VALUES (?,?,?,?,'player',0,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, salt);
            ps.setString(5, verifyToken);
            ps.executeUpdate();
            audit(null, username, "REGISTER_OK", "email=" + email);
            System.out.println("✅ Registered: " + username + " token=" + verifyToken);
            return null; // success
        } catch (SQLException e) {
            return "Registration failed: " + e.getMessage();
        }
    }

    /** Verifies email using the token. Returns null on success, error on failure. */
    public String verifyEmail(String token) {
        if (!isConnected()) return "No database connection.";
        String sql = "UPDATE users SET email_verified=1, verify_token=NULL WHERE verify_token=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            int rows = ps.executeUpdate();
            if (rows == 0) return "Invalid or already used verification token.";
            audit(null, null, "EMAIL_VERIFIED", "token=" + token);
            return null;
        } catch (SQLException e) {
            return "Verification error: " + e.getMessage();
        }
    }

    /** Gets the verify token for a just-registered user (for display in app). */
    public String getVerifyToken(String username) {
        String sql = "SELECT verify_token FROM users WHERE username=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("verify_token") : null;
        } catch (SQLException e) { return null; }
    }

    private String generateVerifyToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean userExists(String username) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM users WHERE username=?")) {
            ps.setString(1, username.trim());
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public boolean emailExists(String email) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM users WHERE email=?")) {
            ps.setString(1, email.trim().toLowerCase());
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    // ─── AUTHORIZATION ────────────────────────────────────────────────────────

    public boolean isSessionValid(String token) {
        if (!isConnected() || token == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM sessions WHERE token=? AND expires_at > NOW()")) {
            ps.setString(1, token);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public String getUserRole(int userId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT role FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("role") : null;
        } catch (SQLException e) { return null; }
    }

    public boolean hasPermission(int userId, String action) {
        String role = getUserRole(userId);
        if (role == null) return false;
        if ("admin".equals(role)) return true;
        return switch (action) {
            case "PLAY", "VIEW_LEADERBOARD", "SAVE_SCORE", "VIEW_OWN_SCORES" -> true;
            default -> false;
        };
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    public List<UserRecord> getAllUsers() {
        List<UserRecord> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT id, username, role, is_active, is_locked, email_verified, " +
                     "failed_attempts, created_at, last_login FROM users ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UserRecord r = new UserRecord(
                    rs.getInt("id"), rs.getString("username"),
                    rs.getString("role"), null);
                r.isActive       = rs.getBoolean("is_active");
                r.isLocked       = rs.getBoolean("is_locked");
                r.emailVerified  = rs.getBoolean("email_verified");
                r.failedAttempts = rs.getInt("failed_attempts");
                Timestamp ca = rs.getTimestamp("created_at");
                Timestamp ll = rs.getTimestamp("last_login");
                r.createdAt  = ca != null ? ca.toLocalDateTime() : null;
                r.lastLogin  = ll != null ? ll.toLocalDateTime() : null;
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("❌ getAllUsers: " + e.getMessage());
        }
        return list;
    }

    public void setUserActive(int adminId, int targetId, boolean active) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET is_active=? WHERE id=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, targetId);
            ps.executeUpdate();
            audit(adminId, null, active ? "ADMIN_UNLOCK_USER" : "ADMIN_LOCK_USER",
                  "targetId=" + targetId);
        } catch (SQLException e) {
            System.out.println("❌ setUserActive: " + e.getMessage());
        }
    }

    public void resetUserLockout(int adminId, int targetId) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET failed_attempts=0, lockout_until=NULL, is_locked=0 WHERE id=?")) {
            ps.setInt(1, targetId);
            ps.executeUpdate();
            audit(adminId, null, "ADMIN_RESET_LOCKOUT", "targetId=" + targetId);
        } catch (SQLException e) {
            System.out.println("❌ resetUserLockout: " + e.getMessage());
        }
    }

    // ─── ACCOUNTING ───────────────────────────────────────────────────────────

    private String createSession(int userId) {
        String token = Base64.getEncoder().encodeToString(
            (userId + ":" + System.currentTimeMillis() + ":" + Math.random()).getBytes());
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO sessions (user_id, token, expires_at) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 8 HOUR))")) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠ Could not create session: " + e.getMessage());
        }
        return token;
    }

    public void logout(int userId, String token) {
        audit(userId, null, "LOGOUT", "token invalidated");
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM sessions WHERE token=?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠ Logout error: " + e.getMessage());
        }
    }

    public void saveScore(int userId, String username, int score,
                          String mode, int level, int timeTaken, int streak) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO scores (user_id, username, score, mode, level, time_taken, streak) " +
                "VALUES (?,?,?,?,?,?,?)")) {
            ps.setInt(1, userId); ps.setString(2, username); ps.setInt(3, score);
            ps.setString(4, mode); ps.setInt(5, level);
            ps.setInt(6, timeTaken); ps.setInt(7, streak);
            ps.executeUpdate();
            audit(userId, username, "SCORE_SAVED",
                  "mode=" + mode + " score=" + score + " level=" + level);
        } catch (SQLException e) {
            System.out.println("❌ Save score: " + e.getMessage());
        }
    }

    public void saveLevelProgress(int userId, int level, int bestScore) {
        if (!isConnected()) return;
        String sql =
            "INSERT INTO level_progress (user_id, level, best_score, attempt_count) VALUES (?,?,?,1) " +
            "ON DUPLICATE KEY UPDATE " +
            "  best_score    = GREATEST(best_score, VALUES(best_score))," +
            "  attempt_count = attempt_count + 1," +
            "  completed_at  = IF(best_score < VALUES(best_score), NOW(), completed_at)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, level); ps.setInt(3, bestScore);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Save progress: " + e.getMessage());
        }
    }

    public java.util.Set<Integer> getCompletedLevels(int userId) {
        java.util.Set<Integer> done = new java.util.HashSet<>();
        if (!isConnected()) return done;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT level FROM level_progress WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) done.add(rs.getInt("level"));
        } catch (SQLException e) {
            System.out.println("❌ Get progress: " + e.getMessage());
        }
        return done;
    }

    public List<ScoreEntry> getTopScores() {
        return queryScores("SELECT username, score, mode, level, played_at " +
                           "FROM scores ORDER BY score DESC LIMIT 10", false);
    }

    public List<ScoreEntry> getTopTimes() {
        return queryScores("SELECT username, score, mode, level, time_taken, played_at " +
                           "FROM scores WHERE mode LIKE 'Memory%' ORDER BY time_taken ASC LIMIT 10", true);
    }

    public List<ScoreEntry> getUserScores(int userId) {
        List<ScoreEntry> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT username, score, mode, level, played_at FROM scores " +
                "WHERE user_id=? ORDER BY played_at DESC LIMIT 30")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rowToEntry(rs, false));
        } catch (SQLException e) { System.out.println("❌ getUserScores: " + e.getMessage()); }
        return list;
    }

    public List<LevelStat> getUserLevelStats(int userId) {
        List<LevelStat> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT level, best_score, attempt_count, completed_at " +
                "FROM level_progress WHERE user_id=? ORDER BY level")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new LevelStat(
                    rs.getInt("level"), rs.getInt("best_score"),
                    rs.getInt("attempt_count"),
                    rs.getTimestamp("completed_at").toLocalDateTime()));
            }
        } catch (SQLException e) { System.out.println("❌ getLevelStats: " + e.getMessage()); }
        return list;
    }

    public List<AuditEntry> getUserLoginHistory(int userId) {
        List<AuditEntry> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT action, detail, logged_at FROM audit_log " +
                "WHERE user_id=? AND action IN ('LOGIN_OK','LOGIN_FAILED','ACCOUNT_LOCKED','LOGIN_DENIED') " +
                "ORDER BY logged_at DESC LIMIT 20")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AuditEntry(
                    rs.getString("action"), rs.getString("detail"),
                    rs.getTimestamp("logged_at").toLocalDateTime()));
            }
        } catch (SQLException e) { System.out.println("❌ getLoginHistory: " + e.getMessage()); }
        return list;
    }

    public void audit(Integer userId, String username, String action, String detail) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO audit_log (user_id, username, action, detail) VALUES (?,?,?,?)")) {
            if (userId != null) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, detail);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠ Audit: " + e.getMessage());
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private List<ScoreEntry> queryScores(String sql, boolean includeTime) {
        List<ScoreEntry> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rowToEntry(rs, includeTime));
        } catch (SQLException e) { System.out.println("❌ queryScores: " + e.getMessage()); }
        return list;
    }

    private ScoreEntry rowToEntry(ResultSet rs, boolean includeTime) throws SQLException {
        ScoreEntry e = new ScoreEntry(
            rs.getString("username"), rs.getInt("score"),
            rs.getString("mode"), rs.getInt("level"),
            rs.getTimestamp("played_at").toLocalDateTime());
        if (includeTime) e.timeTaken = rs.getInt("time_taken");
        return e;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ─── DATA MODELS ──────────────────────────────────────────────────────────

    public static class LoginResult {
        public enum Status { SUCCESS, ERROR, LOCKED }
        public final Status      status;
        public final String      message;
        public final UserRecord  user;

        private LoginResult(Status s, String msg, UserRecord u) {
            status = s; message = msg; user = u;
        }
        public static LoginResult success(UserRecord u) { return new LoginResult(Status.SUCCESS, null, u); }
        public static LoginResult error(String msg)     { return new LoginResult(Status.ERROR,   msg,  null); }
        public static LoginResult locked(String msg)    { return new LoginResult(Status.LOCKED,  msg,  null); }
        public boolean isSuccess() { return status == Status.SUCCESS; }
    }

    public static class UserRecord {
        public final int    id;
        public final String username;
        public final String role;
        public final String sessionToken;
        public boolean isActive       = true;
        public boolean isLocked       = false;
        public boolean emailVerified  = false;
        public int     failedAttempts = 0;
        public LocalDateTime createdAt;
        public LocalDateTime lastLogin;

        public UserRecord(int id, String username, String role, String sessionToken) {
            this.id = id; this.username = username;
            this.role = role; this.sessionToken = sessionToken;
        }
        public boolean isAdmin() { return "admin".equals(role); }
    }

    public static class ScoreEntry {
        public final String username;
        public final int score;
        public final String mode;
        public final int level;
        public final LocalDateTime playedAt;
        public int timeTaken = 0;

        public ScoreEntry(String username, int score, String mode,
                          int level, LocalDateTime playedAt) {
            this.username = username; this.score = score;
            this.mode = mode; this.level = level; this.playedAt = playedAt;
        }
    }

    public static class LevelStat {
        public final int level, bestScore, attempts;
        public final LocalDateTime completedAt;
        public LevelStat(int level, int bestScore, int attempts, LocalDateTime completedAt) {
            this.level = level; this.bestScore = bestScore;
            this.attempts = attempts; this.completedAt = completedAt;
        }
    }

    public static class AuditEntry {
        public final String action, detail;
        public final LocalDateTime time;
        public AuditEntry(String action, String detail, LocalDateTime time) {
            this.action = action; this.detail = detail; this.time = time;
        }
    }
}
