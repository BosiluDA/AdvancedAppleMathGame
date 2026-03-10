package applegame;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Manages all MySQL database operations.
 * Tables: users, sessions, scores, level_progress, audit_log
 */
public class DataBaseManager {

    // ── Connection config ─────────────────────────────────────────────────────
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/apple_math_game"
                                        + "?useSSL=false&serverTimezone=UTC"
                                        + "&allowPublicKeyRetrieval=true"
                                        + "&autoReconnect=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";   // XAMPP default

    private Connection connection;
    private boolean connected = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public DataBaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connected  = true;
            System.out.println("✅ MySQL connected");
            createTablesIfNotExist();
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL driver not found — add mysql-connector-j JAR to libraries");
        } catch (SQLException e) {
            System.out.println("❌ MySQL connection failed: " + e.getMessage());
            System.out.println("   → Is XAMPP MySQL running?");
            System.out.println("   → Does database 'apple_math_game' exist?");
        }
    }

    public boolean isConnected() { return connected && connection != null; }

    // ═════════════════════════════════════════════════════════════════════════
    //  TABLE CREATION
    // ═════════════════════════════════════════════════════════════════════════

    private void createTablesIfNotExist() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // USERS — core identity + role
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id            INT AUTO_INCREMENT PRIMARY KEY," +
                "  username      VARCHAR(50) UNIQUE NOT NULL," +
                "  password_hash VARCHAR(255) NOT NULL," +
                "  salt          VARCHAR(64)  NOT NULL," +
                "  role          ENUM('player','admin') DEFAULT 'player'," +
                "  is_active     TINYINT(1) DEFAULT 1," +
                "  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  last_login    TIMESTAMP NULL" +
                ")");

            // SESSIONS — active login tokens
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                "  id           INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id      INT NOT NULL," +
                "  token        VARCHAR(128) UNIQUE NOT NULL," +
                "  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  expires_at   TIMESTAMP NOT NULL," +
                "  ip_address   VARCHAR(45)," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            // SCORES — per-game results
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

            // LEVEL_PROGRESS — which levels completed
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS level_progress (" +
                "  id           INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id      INT NOT NULL," +
                "  level        INT NOT NULL," +
                "  best_score   INT DEFAULT 0," +
                "  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uq_user_level (user_id, level)," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            // AUDIT_LOG — accounting / activity trail
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

    // ═════════════════════════════════════════════════════════════════════════
    //  PASSWORD HASHING  (PBKDF2-style: SHA-256 + salt, 10k iterations)
    // ═════════════════════════════════════════════════════════════════════════

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

    // ═════════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns user record on success, null on failure. */
    public UserRecord login(String username, String password) {
        if (!isConnected() || username == null || password == null) return null;
        username = username.trim();

        String sql = "SELECT id, username, password_hash, salt, role, is_active " +
                     "FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                audit(null, username, "LOGIN_FAILED", "Unknown username");
                return null;
            }
            int     uid      = rs.getInt("id");
            String  hash     = rs.getString("password_hash");
            String  salt     = rs.getString("salt");
            String  role     = rs.getString("role");
            boolean active   = rs.getBoolean("is_active");

            if (!active) {
                audit(uid, username, "LOGIN_DENIED", "Account disabled");
                return null;
            }
            String attempt = hashPassword(password, salt);
            if (!attempt.equals(hash)) {
                audit(uid, username, "LOGIN_FAILED", "Wrong password");
                return null;
            }

            // Update last_login
            try (PreparedStatement up = connection.prepareStatement(
                    "UPDATE users SET last_login = NOW() WHERE id = ?")) {
                up.setInt(1, uid);
                up.executeUpdate();
            }

            String token = createSession(uid);
            audit(uid, username, "LOGIN_OK", "role=" + role);
            System.out.println("✅ Login: " + username + " (role=" + role + ")");
            return new UserRecord(uid, username, role, token);

        } catch (SQLException e) {
            System.out.println("❌ Login error: " + e.getMessage());
            return null;
        }
    }

    /** Registers a new player account. Returns error message or null on success. */
    public String register(String username, String password) {
        if (!isConnected()) return "No database connection.";
        if (username == null || username.trim().length() < 3)
            return "Username must be at least 3 characters.";
        if (password == null || password.length() < 6)
            return "Password must be at least 6 characters.";

        username = username.trim();

        if (userExists(username)) return "Username '" + username + "' is already taken.";

        String salt = generateSalt();
        String hash = hashPassword(password, salt);

        String sql = "INSERT INTO users (username, password_hash, salt, role) VALUES (?,?,?,'player')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.executeUpdate();
            audit(null, username, "REGISTER_OK", "New player account");
            System.out.println("✅ Registered: " + username);
            return null; // success
        } catch (SQLException e) {
            System.out.println("❌ Register error: " + e.getMessage());
            return "Registration failed: " + e.getMessage();
        }
    }

    public boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  AUTHORIZATION
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns true if the given session token is still valid. */
    public boolean isSessionValid(String token) {
        if (!isConnected() || token == null) return false;
        String sql = "SELECT 1 FROM sessions WHERE token = ? AND expires_at > NOW()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /** Returns the role for a given user id, or null if not found. */
    public String getUserRole(int userId) {
        String sql = "SELECT role FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("role") : null;
        } catch (SQLException e) { return null; }
    }

    /** Checks if a user has permission for a given action. */
    public boolean hasPermission(int userId, String action) {
        String role = getUserRole(userId);
        if (role == null) return false;
        if ("admin".equals(role)) return true;       // admins can do anything
        // Players can play, view leaderboard, save scores
        return switch (action) {
            case "PLAY", "VIEW_LEADERBOARD", "SAVE_SCORE", "VIEW_OWN_SCORES" -> true;
            default -> false;
        };
    }

    private String createSession(int userId) {
        String token = Base64.getEncoder().encodeToString(
            (userId + ":" + System.currentTimeMillis() + ":" + Math.random()).getBytes());
        String sql = "INSERT INTO sessions (user_id, token, expires_at) " +
                     "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 8 HOUR))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠ Logout error: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ACCOUNTING  (scores, progress, audit)
    // ═════════════════════════════════════════════════════════════════════════

    public void saveScore(int userId, String username, int score,
                          String mode, int level, int timeTaken, int streak) {
        if (!isConnected()) return;
        if (!hasPermission(userId, "SAVE_SCORE")) {
            audit(userId, username, "SAVE_SCORE_DENIED", "Insufficient permission");
            return;
        }
        String sql = "INSERT INTO scores (user_id, username, score, mode, level, time_taken, streak) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setInt(3, score);
            ps.setString(4, mode);
            ps.setInt(5, level);
            ps.setInt(6, timeTaken);
            ps.setInt(7, streak);
            ps.executeUpdate();
            audit(userId, username, "SCORE_SAVED",
                  "mode=" + mode + " score=" + score + " level=" + level);
        } catch (SQLException e) {
            System.out.println("❌ Save score error: " + e.getMessage());
        }
    }

    public void saveLevelProgress(int userId, int level, int bestScore) {
        if (!isConnected()) return;
        String sql =
            "INSERT INTO level_progress (user_id, level, best_score) VALUES (?,?,?) " +
            "ON DUPLICATE KEY UPDATE " +
            "  best_score   = GREATEST(best_score, VALUES(best_score))," +
            "  completed_at = IF(best_score < VALUES(best_score), NOW(), completed_at)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, level);
            ps.setInt(3, bestScore);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Save progress error: " + e.getMessage());
        }
    }

    /** Returns set of level numbers completed by this user. */
    public java.util.Set<Integer> getCompletedLevels(int userId) {
        java.util.Set<Integer> done = new java.util.HashSet<>();
        if (!isConnected()) return done;
        String sql = "SELECT level FROM level_progress WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) done.add(rs.getInt("level"));
        } catch (SQLException e) {
            System.out.println("❌ Get progress error: " + e.getMessage());
        }
        return done;
    }

    /** Top 10 scores across all modes. */
    public List<ScoreEntry> getTopScores() {
        List<ScoreEntry> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT username, score, mode, level, played_at " +
                     "FROM scores ORDER BY score DESC LIMIT 10";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ScoreEntry(
                    rs.getString("username"),
                    rs.getInt("score"),
                    rs.getString("mode"),
                    rs.getInt("level"),
                    rs.getTimestamp("played_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ Leaderboard error: " + e.getMessage());
        }
        return list;
    }

    /** Top 10 fastest times (memory levels only). */
    public List<ScoreEntry> getTopTimes() {
        List<ScoreEntry> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT username, score, mode, level, time_taken, played_at " +
                     "FROM scores WHERE mode LIKE 'Memory%' " +
                     "ORDER BY time_taken ASC LIMIT 10";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ScoreEntry e = new ScoreEntry(
                    rs.getString("username"),
                    rs.getInt("score"),
                    rs.getString("mode"),
                    rs.getInt("level"),
                    rs.getTimestamp("played_at").toLocalDateTime()
                );
                e.timeTaken = rs.getInt("time_taken");
                list.add(e);
            }
        } catch (SQLException e) {
            System.out.println("❌ Top times error: " + e.getMessage());
        }
        return list;
    }

    /** All scores for a specific user. */
    public List<ScoreEntry> getUserScores(int userId) {
        List<ScoreEntry> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT username, score, mode, level, played_at " +
                     "FROM scores WHERE user_id = ? ORDER BY played_at DESC LIMIT 20";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ScoreEntry(
                    rs.getString("username"),
                    rs.getInt("score"),
                    rs.getString("mode"),
                    rs.getInt("level"),
                    rs.getTimestamp("played_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ User scores error: " + e.getMessage());
        }
        return list;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  AUDIT LOGGING
    // ═════════════════════════════════════════════════════════════════════════

    public void audit(Integer userId, String username, String action, String detail) {
        if (!isConnected()) return;
        String sql = "INSERT INTO audit_log (user_id, username, action, detail) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, detail);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Don't let audit failures crash the game
            System.out.println("⚠ Audit log error: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ═════════════════════════════════════════════════════════════════════════

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ DB connection closed");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DATA MODELS
    // ═════════════════════════════════════════════════════════════════════════

    public static class UserRecord {
        public final int    id;
        public final String username;
        public final String role;
        public final String sessionToken;

        public UserRecord(int id, String username, String role, String sessionToken) {
            this.id           = id;
            this.username     = username;
            this.role         = role;
            this.sessionToken = sessionToken;
        }

        public boolean isAdmin() { return "admin".equals(role); }
    }

    public static class ScoreEntry {
        public final String   username;
        public final int      score;
        public final String   mode;
        public final int      level;
        public final java.time.LocalDateTime playedAt;
        public int timeTaken = 0;

        public ScoreEntry(String username, int score, String mode,
                          int level, java.time.LocalDateTime playedAt) {
            this.username = username;
            this.score    = score;
            this.mode     = mode;
            this.level    = level;
            this.playedAt = playedAt;
        }
    }
}
