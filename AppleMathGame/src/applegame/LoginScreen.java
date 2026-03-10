package applegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class LoginScreen extends JPanel {

    private final DataBaseManager db;
    private final MainWindow window;

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         feedbackLabel;
    private JButton        loginBtn, registerBtn;

    // Floating apple animation
    private final float[] px    = new float[10];
    private final float[] py    = new float[10];
    private final float[] pspd  = new float[10];
    private final float[] psz   = new float[10];
    private final boolean[] pred = new boolean[10];
    private float animPhase = 0f;
    private Timer animTimer;

    public LoginScreen(DataBaseManager db, MainWindow window) {
        this.db     = db;
        this.window = window;
        setOpaque(false);
        setLayout(new GridBagLayout());
        initParticles();
        buildUI();
        startAnim();
    }

    private void initParticles() {
        for (int i = 0; i < px.length; i++) {
            px[i]   = (float) Math.random();
            py[i]   = (float) Math.random();
            pspd[i] = 0.0006f + (float)(Math.random() * 0.001f);
            psz[i]  = 14 + (float)(Math.random() * 20);
            pred[i] = Math.random() > 0.5;
        }
    }

    private void startAnim() {
        animTimer = new Timer(30, e -> {
            animPhase += 0.025f;
            for (int i = 0; i < py.length; i++) {
                py[i] += pspd[i];
                if (py[i] > 1.1f) { py[i] = -0.1f; px[i] = (float)Math.random(); }
            }
            repaint();
        });
        animTimer.start();
    }

    private void buildUI() {
        // ── Card panel ────────────────────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 24);
                // Top accent bar
                GradientPaint bar = new GradientPaint(0,0,Theme.APPLE_RED,getWidth(),0,Theme.APPLE_LIME);
                g.setPaint(bar);
                g.fillRoundRect(0,0,getWidth(),5,4,4);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(Theme.emptyBorder(32, 40));
        card.setPreferredSize(new Dimension(400, 500));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill  = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Apple icon
        JLabel apple = new JLabel("🍎", SwingConstants.CENTER);
        apple.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(apple, gbc);

        // Title
        JLabel title = new JLabel("Apple Math Puzzle", SwingConstants.CENTER);
        title.setFont(Theme.fontTitle(26));
        title.setForeground(Theme.TEXT_PRIMARY);
        card.add(title, gbc);

        JLabel sub = new JLabel("Sign in to save your progress", SwingConstants.CENTER);
        sub.setFont(Theme.fontBody(13));
        sub.setForeground(Theme.TEXT_MUTED);
        gbc.insets = new Insets(2, 0, 18, 0);
        card.add(sub, gbc);

        // Fields
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);

        card.add(fieldLabel("Username"), gbc);
        usernameField = styledTextField();
        card.add(usernameField, gbc);

        card.add(fieldLabel("Password"), gbc);
        passwordField = styledPasswordField();
        card.add(passwordField, gbc);

        // Feedback label
        feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
        feedbackLabel.setFont(Theme.fontBody(12));
        feedbackLabel.setForeground(Theme.WRONG_GLOW);
        gbc.insets = new Insets(4, 0, 4, 0);
        card.add(feedbackLabel, gbc);

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        loginBtn    = Theme.primaryButton("Sign In");
        registerBtn = Theme.successButton("Register");
        loginBtn.setPreferredSize(new Dimension(140, 42));
        registerBtn.setPreferredSize(new Dimension(140, 42));
        loginBtn.addActionListener(e    -> doLogin());
        registerBtn.addActionListener(e -> doRegister());

        // Enter key triggers login
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);

        btnRow.add(loginBtn);
        btnRow.add(registerBtn);
        gbc.insets = new Insets(10, 0, 0, 0);
        card.add(btnRow, gbc);

        // DB status
        JLabel dbStatus = new JLabel(
            db.isConnected() ? "● Database connected" : "● Database offline",
            SwingConstants.CENTER);
        dbStatus.setFont(Theme.fontMono(11));
        dbStatus.setForeground(db.isConnected() ? Theme.APPLE_LIME : Theme.WRONG_GLOW);
        gbc.insets = new Insets(16, 0, 0, 0);
        card.add(dbStatus, gbc);

        add(card, new GridBagConstraints());
    }

    // ── Field helpers ─────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBodyBold(12));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private JTextField styledTextField() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x0A1808));
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g.setColor(isFocusOwner() ? Theme.APPLE_LIME : Theme.BORDER_GLOW);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1,getHeight()-1,10,10));
                super.paintComponent(g0);
                g.dispose();
            }
        };
        f.setFont(Theme.fontBody(14));
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.APPLE_LIME);
        f.setBackground(new Color(0,0,0,0));
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        f.setPreferredSize(new Dimension(300, 40));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x0A1808));
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g.setColor(isFocusOwner() ? Theme.APPLE_LIME : Theme.BORDER_GLOW);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1,getHeight()-1,10,10));
                super.paintComponent(g0);
                g.dispose();
            }
        };
        f.setFont(Theme.fontBody(14));
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.APPLE_LIME);
        f.setBackground(new Color(0,0,0,0));
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        f.setPreferredSize(new Dimension(300, 40));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    // ── Auth actions ──────────────────────────────────────────────────────────

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            setFeedback("Please enter username and password.", false);
            return;
        }

        setButtonsEnabled(false);
        setFeedback("Signing in…", true);

        SwingWorker<DataBaseManager.UserRecord, Void> worker = new SwingWorker<>() {
            @Override protected DataBaseManager.UserRecord doInBackground() {
                return db.login(username, password);
            }
            @Override protected void done() {
                try {
                    DataBaseManager.UserRecord user = get();
                    if (user != null) {
                        Session.login(user, db);
                        // Load persisted level progress into GameState
                        java.util.Set<Integer> done = db.getCompletedLevels(user.id);
                        window.getGameState().loadCompletedLevels(done);
                        window.getGameState().setPlayerName(user.username);
                        window.getGameState().setUserId(user.id);
                        setFeedback("Welcome back, " + user.username + "!", true);
                        Timer t = new Timer(600, e -> window.goToLevels());
                        t.setRepeats(false); t.start();
                    } else {
                        setFeedback("Invalid username or password.", false);
                        setButtonsEnabled(true);
                        passwordField.setText("");
                    }
                } catch (Exception e) {
                    setFeedback("Error: " + e.getMessage(), false);
                    setButtonsEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        setButtonsEnabled(false);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() {
                return db.register(username, password);
            }
            @Override protected void done() {
                try {
                    String error = get();
                    if (error == null) {
                        setFeedback("Account created! You can now sign in.", true);
                        feedbackLabel.setForeground(Theme.APPLE_LIME);
                    } else {
                        setFeedback(error, false);
                    }
                } catch (Exception e) {
                    setFeedback("Error: " + e.getMessage(), false);
                }
                setButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    private void setFeedback(String msg, boolean neutral) {
        feedbackLabel.setText(msg);
        feedbackLabel.setForeground(neutral ? Theme.TEXT_MUTED : Theme.WRONG_GLOW);
    }

    private void setButtonsEnabled(boolean on) {
        loginBtn.setEnabled(on);
        registerBtn.setEnabled(on);
    }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        Theme.paintDeepBackground(g, w, h);
        for (int i = 0; i < px.length; i++) {
            int x = (int)(px[i] * w), y = (int)(py[i] * h);
            int r = (int)(psz[i] / 2);
            Color col = pred[i]
                ? new Color(Theme.APPLE_RED.getRed(),  Theme.APPLE_RED.getGreen(),  Theme.APPLE_RED.getBlue(),  40)
                : new Color(Theme.APPLE_GREEN.getRed(),Theme.APPLE_GREEN.getGreen(),Theme.APPLE_GREEN.getBlue(),40);
            g.setColor(col);
            g.fillOval(x - r, y - r, r*2, r*2);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
