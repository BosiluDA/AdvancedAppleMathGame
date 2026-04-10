package applegame;

/**
 * Holds the currently authenticated user for the lifetime of the app session.
 * Set on successful login, cleared on logout.
 */
public class Session {

    private static DataBaseManager.UserRecord currentUser = null;
    private static DataBaseManager db = null;
    private static boolean guest = false;

    // ── Set / clear ───────────────────────────────────────────────────────────
    public static void login(DataBaseManager.UserRecord user, DataBaseManager dbManager) {
        currentUser = user;
        db          = dbManager;
        guest       = false;
        System.out.println("🔐 Session started: " + user.username + " [" + user.role + "]");
    }

    public static void loginAsGuest() {
        currentUser = null;
        db          = null;
        guest       = true;
        System.out.println("👤 Guest session started");
    }

    public static void logout() {
        if (currentUser != null && db != null) {
            db.logout(currentUser.id, currentUser.sessionToken);
            System.out.println("🔓 Session ended: " + currentUser.username);
        }
        currentUser = null;
        guest       = false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public static boolean isLoggedIn()   { return currentUser != null; }
    public static boolean isGuest()      { return guest && currentUser == null; }
    public static boolean isRegistered() { return currentUser != null; }

    public static DataBaseManager.UserRecord getUser() { return currentUser; }
    public static int    getUserId()   { return currentUser != null ? currentUser.id : -1; }
    public static String getUsername() {
        if (currentUser != null) return currentUser.username;
        if (guest) return "Guest";
        return "?";
    }
    public static String getRole()   { return currentUser != null ? currentUser.role : (guest ? "guest" : "none"); }
    public static boolean isAdmin()  { return currentUser != null && currentUser.isAdmin(); }

    /** Guests can only play Memory mode — not Equation Challenge */
    public static boolean canPlayEquation() { return currentUser != null; }

    public static boolean can(String action, DataBaseManager dbManager) {
        if (currentUser == null) return false;
        if (currentUser.isAdmin()) return true;
        return dbManager.hasPermission(currentUser.id, action);
    }
}
