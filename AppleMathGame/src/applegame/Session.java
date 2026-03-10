package applegame;

/**
 * Holds the currently authenticated user for the lifetime of the app session.
 * Set on successful login, cleared on logout.
 */
public class Session {

    private static DataBaseManager.UserRecord currentUser = null;
    private static DataBaseManager db = null;

    // ── Set / clear ───────────────────────────────────────────────────────────
    public static void login(DataBaseManager.UserRecord user, DataBaseManager dbManager) {
        currentUser = user;
        db = dbManager;
        System.out.println("🔐 Session started: " + user.username + " [" + user.role + "]");
    }

    public static void logout() {
        if (currentUser != null && db != null) {
            db.logout(currentUser.id, currentUser.sessionToken);
            System.out.println("🔓 Session ended: " + currentUser.username);
        }
        currentUser = null;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public static boolean isLoggedIn()          { return currentUser != null; }
    public static DataBaseManager.UserRecord getUser() { return currentUser; }
    public static int    getUserId()            { return currentUser != null ? currentUser.id : -1; }
    public static String getUsername()          { return currentUser != null ? currentUser.username : "Guest"; }
    public static String getRole()              { return currentUser != null ? currentUser.role : "none"; }
    public static boolean isAdmin()             { return currentUser != null && currentUser.isAdmin(); }

    /** Check permission via DB (uses cached role if DB unavailable). */
    public static boolean can(String action, DataBaseManager dbManager) {
        if (currentUser == null) return false;
        if (currentUser.isAdmin()) return true;
        return dbManager.hasPermission(currentUser.id, action);
    }
}
