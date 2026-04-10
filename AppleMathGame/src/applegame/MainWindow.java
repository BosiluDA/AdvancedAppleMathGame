package applegame;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private final GameState       state;
    private final DataBaseManager db;
    private final CardLayout      cardLayout;
    private final JPanel          root;

    public static final String SCREEN_LOGIN       = "LOGIN";
    public static final String SCREEN_LEVELS      = "LEVELS";
    public static final String SCREEN_MEMORY      = "MEMORY";
    public static final String SCREEN_EQUATION    = "EQUATION";
    public static final String SCREEN_LEADERBOARD = "LEADERBOARD";
    public static final String SCREEN_ACCOUNT     = "ACCOUNT";
    public static final String SCREEN_ADMIN       = "ADMIN";

    public MainWindow(GameState state, DataBaseManager db) {
        this.state      = state;
        this.db         = db;
        this.cardLayout = new CardLayout();
        this.root       = new JPanel(cardLayout);
        root.setBackground(Theme.BG_DEEP);

        setTitle("🍎 Apple Math Puzzle");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 680));
        setPreferredSize(new Dimension(1050, 740));
        setLocationRelativeTo(null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (Session.isLoggedIn()) Session.logout();
            db.close();
        }));

        root.add(new LoginScreen(db, this), SCREEN_LOGIN);
        setContentPane(root);
        pack();
        showScreen(SCREEN_LOGIN);
    }

    public void showScreen(String name) { cardLayout.show(root, name); }

    private void replace(String name, JPanel panel) {
        root.add(panel, name);
        root.revalidate();
    }

    public void goToLevels() {
        replace(SCREEN_LEVELS, new LevelSelectScreen(state, this));
        showScreen(SCREEN_LEVELS);
    }

    public void startMemoryLevel(int level) {
        state.setCurrentLevel(level);
        replace(SCREEN_MEMORY, new MemoryGameScreen(state, db, this, level));
        showScreen(SCREEN_MEMORY);
    }

    public void startEquationGame() {
        state.resetEquationScore();
        replace(SCREEN_EQUATION, new EquationGameScreen(state, db, this));
        showScreen(SCREEN_EQUATION);
    }

    public void goToLeaderboard() {
        replace(SCREEN_LEADERBOARD, new LeaderboardScreen(state, db, this));
        showScreen(SCREEN_LEADERBOARD);
    }

    public void goToMyAccount() {
        replace(SCREEN_ACCOUNT, new MyAccountScreen(db, this));
        showScreen(SCREEN_ACCOUNT);
    }

    public void goToAdminPanel() {
        if (!Session.isAdmin()) return; // authorization guard
        replace(SCREEN_ADMIN, new AdminPanel(db, this));
        showScreen(SCREEN_ADMIN);
    }

    public void goToLogin() {
        Session.logout();
        replace(SCREEN_LOGIN, new LoginScreen(db, this));
        showScreen(SCREEN_LOGIN);
    }

    public GameState       getGameState() { return state; }
    public DataBaseManager getDb()        { return db; }
}
