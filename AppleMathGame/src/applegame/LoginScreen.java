package applegame;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class LoginScreen extends JPanel {

    private final DataBaseManager db;
    private final MainWindow      window;

    private JPanel     cardPanel;
    private CardLayout cardLayout;

    private JTextField     loginUserField;
    private JPasswordField loginPassField;
    private JLabel         loginFeedback;
    private JButton        loginBtn;
    private Timer          lockoutTimer;
    private int            lockoutSecsLeft = 0;

    private JTextField     regUserField, regEmailField;
    private JPasswordField regPassField, regConfirmField;
    private JLabel         regFeedback, strengthLabel;
    private JProgressBar   strengthBar;

    private final float[]   px = new float[14], py = new float[14];
    private final float[]   pspd = new float[14], psz = new float[14];
    private final boolean[] pred = new boolean[14];
    private Timer           animTimer;

    // Field styling constants
    private static final int    FIELD_W      = 360;
    private static final Color  FIELD_BG     = new Color(0x0B1A0B);
    private static final Color  FIELD_FG     = new Color(0xDCF5DC);
    private static final Color  FIELD_BORDER = new Color(0x2E4E2E);
    private static final Color  FIELD_FOCUS  = new Color(0x56C456);

    public LoginScreen(DataBaseManager db, MainWindow window) {
        this.db     = db;
        this.window = window;
        setOpaque(false);
        setLayout(new GridBagLayout());
        initParticles();
        buildUI();
        startAnim();
    }

    // =========================================================================
    //  MAIN UI
    // =========================================================================

    private void buildUI() {
        // Outer card — centers everything inside it
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g.setColor(new Color(0x0F200F));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g.setColor(new Color(0x1E3A1E));
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                // Top gradient bar
                GradientPaint bar = new GradientPaint(0,0, new Color(0xC62828), getWidth(),0, new Color(0x558B2F));
                g.setPaint(bar);
                g.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        card.setPreferredSize(new Dimension(480, 680));

        GridBagConstraints gc = centeredGbc();

        // ── Logo ──────────────────────────────────────────────────────────────
        JLabel logo = new JLabel("🍎", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        card.add(logo, gc);

        // ── Title ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("Apple Math Puzzle", SwingConstants.CENTER);
        title.setFont(Theme.fontTitle(26));
        title.setForeground(new Color(0xE8F5E9));
        gc.insets = new Insets(2, 0, 14, 0);
        card.add(title, gc);

        // ── Tabs ──────────────────────────────────────────────────────────────
        JButton loginTab = tabBtn("Sign In",  true);
        JButton regTab   = tabBtn("Register", false);
        JPanel tabs = new JPanel(new GridLayout(1, 2, 0, 0));
        tabs.setOpaque(false);
        tabs.setPreferredSize(new Dimension(FIELD_W, 38));
        tabs.add(loginTab); tabs.add(regTab);
        gc.insets = new Insets(0, 0, 16, 0);
        card.add(tabs, gc);

        // ── Card switcher ─────────────────────────────────────────────────────
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(FIELD_W, 360));
        cardPanel.add(buildLoginPanel(),    "LOGIN");
        cardPanel.add(buildRegisterPanel(), "REGISTER");

        loginTab.addActionListener(e -> {
            cardLayout.show(cardPanel, "LOGIN");
            loginTab.setForeground(FIELD_FOCUS); regTab.setForeground(new Color(0x779977));
        });
        regTab.addActionListener(e -> {
            cardLayout.show(cardPanel, "REGISTER");
            regTab.setForeground(FIELD_FOCUS); loginTab.setForeground(new Color(0x779977));
        });

        gc.insets = new Insets(0, 0, 0, 0);
        gc.fill   = GridBagConstraints.HORIZONTAL;
        card.add(cardPanel, gc);

        // ── Divider ───────────────────────────────────────────────────────────
        card.add(divider(), withInsets(gc, 14, 0, 10, 0));

        // ── Guest button ──────────────────────────────────────────────────────
        JButton guestBtn = guestButton("👤  Play as Guest");
        guestBtn.addActionListener(e -> {
            Session.loginAsGuest();
            window.getGameState().setPlayerName("Guest");
            window.getGameState().setUserId(-1);
            window.goToLevels();
        });
        card.add(guestBtn, withInsets(gc, 0, 0, 4, 0));

        JLabel guestNote = centeredNote("⚠  Guests: Memory mode only — scores not saved");
        card.add(guestNote, withInsets(gc, 0, 0, 16, 0));

        // ── DB status ─────────────────────────────────────────────────────────
        boolean connected = db.isConnected();
        JLabel dbLbl = new JLabel(
            connected ? "● Connected to database" : "● Database offline",
            SwingConstants.CENTER);
        dbLbl.setFont(Theme.fontMono(11));
        dbLbl.setForeground(connected ? new Color(0x66BB6A) : new Color(0xEF5350));
        gc.fill = GridBagConstraints.NONE;
        card.add(dbLbl, withInsets(gc, 0, 0, 0, 0));

        // Add card to screen — vertically and horizontally centered
        add(card, new GridBagConstraints());
    }

    // =========================================================================
    //  LOGIN PANEL
    // =========================================================================

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(fieldLabel("Username"));
        p.add(Box.createVerticalStrut(4));
        loginUserField = textField();
        loginUserField.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(loginUserField);

        p.add(Box.createVerticalStrut(12));

        p.add(fieldLabel("Password"));
        p.add(Box.createVerticalStrut(4));
        loginPassField = passwordField();
        JPanel passRow = wrapWithEye(loginPassField);
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(passRow);

        p.add(Box.createVerticalStrut(6));
        loginFeedback = feedbackLabel();
        loginFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginFeedback.setHorizontalAlignment(SwingConstants.LEFT);
        p.add(loginFeedback);

        p.add(Box.createVerticalStrut(4));
        loginBtn = Theme.primaryButton("Sign In");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> doLogin());
        p.add(loginBtn);

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        loginUserField.addKeyListener(enter);
        loginPassField.addKeyListener(enter);

        return p;
    }

    // =========================================================================
    //  REGISTER PANEL
    // =========================================================================

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(fieldLabel("Username  (letters, numbers, _ · 3–20 chars)"));
        p.add(Box.createVerticalStrut(4));
        regUserField = textField();
        regUserField.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(regUserField);

        p.add(Box.createVerticalStrut(10));
        p.add(fieldLabel("Email"));
        p.add(Box.createVerticalStrut(4));
        regEmailField = textField();
        regEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(regEmailField);

        p.add(Box.createVerticalStrut(10));
        p.add(fieldLabel("Password"));
        p.add(Box.createVerticalStrut(4));
        regPassField = passwordField();
        JPanel passRow = wrapWithEye(regPassField);
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(passRow);

        // Strength meter
        strengthBar = new JProgressBar(0, 3);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        strengthBar.setPreferredSize(new Dimension(FIELD_W, 5));
        strengthBar.setBorderPainted(false);
        strengthBar.setForeground(new Color(0xEF5350));
        strengthBar.setBackground(new Color(0x1B2E1B));
        strengthBar.setOpaque(true);
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        strengthLabel = new JLabel("Enter a password");
        strengthLabel.setFont(Theme.fontMono(10));
        strengthLabel.setForeground(new Color(0x779977));
        strengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        regPassField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateStrength(); }
            public void removeUpdate(DocumentEvent e)  { updateStrength(); }
            public void changedUpdate(DocumentEvent e) { updateStrength(); }
            void updateStrength() {
                String pw = new String(regPassField.getPassword());
                int str = DataBaseManager.passwordStrength(pw);
                strengthBar.setValue(str);
                Color[] cols = { new Color(0xEF5350), new Color(0xFF8C00), new Color(0xFFCA28), new Color(0x66BB6A) };
                strengthBar.setForeground(cols[str]);
                strengthLabel.setText(DataBaseManager.strengthLabel[str]);
                strengthLabel.setForeground(cols[str]);
            }
        });
        p.add(Box.createVerticalStrut(4));
        p.add(strengthBar);
        p.add(Box.createVerticalStrut(2));
        p.add(strengthLabel);

        p.add(Box.createVerticalStrut(10));
        p.add(fieldLabel("Confirm Password"));
        p.add(Box.createVerticalStrut(4));
        regConfirmField = passwordField();
        JPanel confirmRow = wrapWithEye(regConfirmField);
        confirmRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(confirmRow);

        p.add(Box.createVerticalStrut(6));
        regFeedback = feedbackLabel();
        regFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        regFeedback.setHorizontalAlignment(SwingConstants.LEFT);
        p.add(regFeedback);

        p.add(Box.createVerticalStrut(4));
        JButton regBtn = Theme.successButton("Create Account");
        regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        regBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        regBtn.addActionListener(e -> doRegister());
        p.add(regBtn);

        return p;
    }

    // =========================================================================
    //  FIELD FACTORY
    // =========================================================================

    private JTextField textField() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setEchoChar('●');
        styleField(f);
        return f;
    }

    private void styleField(JTextComponent f) {
        f.setFont(new Font("Dialog", Font.PLAIN, 14));
        f.setForeground(FIELD_FG);
        f.setBackground(FIELD_BG);
        f.setCaretColor(FIELD_FOCUS);
        f.setOpaque(true);
        f.setPreferredSize(new Dimension(FIELD_W, 40));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setMinimumSize(new Dimension(100, 40));
        applyBorder(f, false);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { applyBorder(f, true);  }
            public void focusLost(FocusEvent e)   { applyBorder(f, false); }
        });
    }

    private void applyBorder(JTextComponent f, boolean focused) {
        Color c = focused ? FIELD_FOCUS : FIELD_BORDER;
        int t    = focused ? 2 : 1;
        int pad  = focused ? 7 : 8;
        // password fields reserve right space for eye button handled in wrapWithEye
        int rPad = (f instanceof JPasswordField) ? pad : pad;
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c, t, true),
            BorderFactory.createEmptyBorder(pad, 10, pad, rPad)));
    }

    private JPanel wrapWithEye(JPasswordField pf) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(FIELD_W, 40));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setMinimumSize(new Dimension(100, 40));

        // Give password field no right border so it merges with eye button
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 0, FIELD_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 6)));
        row.add(pf, BorderLayout.CENTER);

        JButton eye = new JButton("👁");
        eye.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        eye.setForeground(new Color(0x779977));
        eye.setBackground(FIELD_BG);
        eye.setOpaque(true);
        eye.setContentAreaFilled(true);
        eye.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 1, FIELD_BORDER),
            BorderFactory.createEmptyBorder(0, 6, 0, 8)));
        eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.setPreferredSize(new Dimension(36, 40));
        eye.setToolTipText("Show / hide password");

        // Sync focus border with password field
        pf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(2, 2, 2, 0, FIELD_FOCUS),
                    BorderFactory.createEmptyBorder(7, 9, 7, 6)));
                eye.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(2, 0, 2, 2, FIELD_FOCUS),
                    BorderFactory.createEmptyBorder(0, 6, 0, 8)));
            }
            public void focusLost(FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 0, FIELD_BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 6)));
                eye.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 1, FIELD_BORDER),
                    BorderFactory.createEmptyBorder(0, 6, 0, 8)));
            }
        });

        final boolean[] showing = {false};
        eye.addActionListener(e -> {
            showing[0] = !showing[0];
            pf.setEchoChar(showing[0] ? (char) 0 : '●');
            eye.setText(showing[0] ? "🙈" : "👁");
            pf.requestFocusInWindow();
        });

        row.add(eye, BorderLayout.EAST);
        return row;
    }

    // =========================================================================
    //  LAYOUT HELPERS
    // =========================================================================

    /** GBC for the main card — everything centered horizontally */
    private GridBagConstraints centeredGbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 0;
        gc.gridy   = GridBagConstraints.RELATIVE;
        gc.fill    = GridBagConstraints.NONE;
        gc.anchor  = GridBagConstraints.CENTER;
        gc.insets  = new Insets(0, 0, 0, 0);
        gc.weightx = 0;
        return gc;
    }

    /** GBC for form fields — fills width, anchors top */
    private GridBagConstraints formGbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 0;
        gc.gridy   = GridBagConstraints.RELATIVE;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.anchor  = GridBagConstraints.NORTH;
        gc.weightx = 1.0;
        gc.weighty = 0;
        gc.insets  = new Insets(0, 0, 0, 0);
        return gc;
    }

    private GridBagConstraints withInsets(GridBagConstraints base, int t, int l, int b, int r) {
        GridBagConstraints copy = (GridBagConstraints) base.clone();
        copy.insets = new Insets(t, l, b, r);
        return copy;
    }

    private JPanel divider() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(FIELD_W, 18));
        JSeparator sl = new JSeparator(); sl.setForeground(new Color(0x2E4E2E));
        JSeparator sr = new JSeparator(); sr.setForeground(new Color(0x2E4E2E));
        JLabel or = new JLabel("or", SwingConstants.CENTER);
        or.setFont(Theme.fontBody(10)); or.setForeground(new Color(0x557755));
        row.add(sl, BorderLayout.WEST); row.add(or, BorderLayout.CENTER); row.add(sr, BorderLayout.EAST);
        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBody(11));
        l.setForeground(new Color(0x8AAA8A));
        l.setBorder(BorderFactory.createEmptyBorder(0, 2, 3, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel feedbackLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(Theme.fontBody(11));
        l.setForeground(new Color(0xEF5350));
        l.setPreferredSize(new Dimension(FIELD_W, 22));
        return l;
    }

    private JLabel centeredNote(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.fontBody(10));
        l.setForeground(new Color(0x557755));
        l.setPreferredSize(new Dimension(FIELD_W, 16));
        return l;
    }

    private Component vspace(int h) {
        return Box.createRigidArea(new Dimension(FIELD_W, h));
    }

    // =========================================================================
    //  STYLED BUTTONS
    // =========================================================================

    private JButton tabBtn(String text, boolean active) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean lit = getForeground().equals(FIELD_FOCUS);
                g.setColor(lit ? new Color(0x112211) : new Color(0x0D1A0D));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g.setColor(lit ? FIELD_FOCUS : new Color(0x2E4E2E));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                super.paintComponent(g0);
                g.dispose();
            }
        };
        b.setFont(Theme.fontBodyBold(13));
        b.setForeground(active ? FIELD_FOCUS : new Color(0x779977));
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton guestButton(String text) {
        JButton b = new JButton(text) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov=true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov=false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(hov ? new Color(0x162816) : new Color(0x0D1A0D));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g.setColor(new Color(0x2E4E2E));
                g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g.setFont(Theme.fontBody(13));
                g.setColor(new Color(hov ? 0xAABBAA : 0x779977));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g.dispose();
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(FIELD_W, 38));
        return b;
    }

    // =========================================================================
    //  AUTH ACTIONS
    // =========================================================================

    private void doLogin() {
        // Block if UI countdown is still running
        if (lockoutSecsLeft > 0) return;
        String username = loginUserField.getText().trim();
        String password = new String(loginPassField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            setLoginFeedback("Please enter username and password.", false); return;
        }
        loginBtn.setEnabled(false);
        setLoginFeedback("Signing in…", true);
        new SwingWorker<DataBaseManager.LoginResult, Void>() {
            @Override protected DataBaseManager.LoginResult doInBackground() {
                return db.login(username, password);
            }
            @Override protected void done() {
                try {
                    DataBaseManager.LoginResult r = get();
                    if (r.isSuccess()) {
                        // Stop any running timer on success
                        if (lockoutTimer != null) lockoutTimer.stop();
                        lockoutSecsLeft = 0;
                        Session.login(r.user, db);
                        window.getGameState().setPlayerName(r.user.username);
                        window.getGameState().setUserId(r.user.id);
                        window.getGameState().loadCompletedLevels(db.getCompletedLevels(r.user.id));
                        window.goToLevels();
                    } else if (r.status == DataBaseManager.LoginResult.Status.LOCKED) {
                        // Only start a fresh countdown if one isn't already running
                        if (lockoutSecsLeft <= 0) {
                            startLockoutCountdown();
                        } else {
                            // Timer already running — just update the message
                            setLoginFeedback(r.message, false);
                            loginBtn.setEnabled(true);
                        }
                    } else {
                        setLoginFeedback(r.message, false);
                        loginBtn.setEnabled(true);
                        loginPassField.setText("");
                    }
                } catch (Exception ex) {
                    setLoginFeedback("Error: " + ex.getMessage(), false);
                    loginBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void startLockoutCountdown() {
        lockoutSecsLeft = 60;
        if (lockoutTimer != null) lockoutTimer.stop();
        loginBtn.setEnabled(false);
        lockoutTimer = new Timer(1000, e -> {
            lockoutSecsLeft--;
            if (lockoutSecsLeft <= 0) {
                lockoutSecsLeft = 0;
                lockoutTimer.stop();
                loginBtn.setEnabled(true);
                setLoginFeedback("Lockout expired. Please try again.", true);
            } else {
                setLoginFeedback("Too many attempts. Wait " + lockoutSecsLeft + "s…", false);
            }
        });
        lockoutTimer.start();
        setLoginFeedback("Too many attempts. Wait 60s…", false);
    }

    private void doRegister() {
        String username = regUserField.getText().trim();
        String email    = regEmailField.getText().trim();
        String password = new String(regPassField.getPassword());
        String confirm  = new String(regConfirmField.getPassword());
        if (!password.equals(confirm)) { setRegFeedback("Passwords do not match.", false); return; }
        if (DataBaseManager.passwordStrength(password) < 1) { setRegFeedback("Password is too weak.", false); return; }
        setRegFeedback("Creating account…", true);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return db.register(username, email, password); }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err == null) showVerifyDialog(username, db.getVerifyToken(username));
                    else setRegFeedback(err, false);
                } catch (Exception ex) { setRegFeedback("Error: " + ex.getMessage(), false); }
            }
        }.execute();
    }

    private void showVerifyDialog(String username, String token) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dlg.setUndecorated(true); dlg.setSize(460, 320); dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(new Color(0x0F200F)); g.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g.setColor(new Color(0x66BB6A)); g.fillRoundRect(0,0,getWidth(),5,4,4);
                g.dispose();
            }
        };
        p.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx=0; gc.gridy=GridBagConstraints.RELATIVE;
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(5,0,5,0); gc.anchor=GridBagConstraints.CENTER;

        JLabel icon = new JLabel("📧", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        gc.fill=GridBagConstraints.NONE; p.add(icon, gc);

        JLabel ttl = new JLabel("Verify Your Account", SwingConstants.CENTER);
        ttl.setFont(Theme.fontTitle(17)); ttl.setForeground(new Color(0x66BB6A)); p.add(ttl, gc);

        JLabel sub = new JLabel("<html><div style='text-align:center;width:360px'>Account created! Copy your token below and paste it to verify.</div></html>", SwingConstants.CENTER);
        sub.setFont(Theme.fontBody(12)); sub.setForeground(new Color(0x8AAA8A));
        gc.fill=GridBagConstraints.HORIZONTAL; p.add(sub, gc);

        JTextField tokenFld = new JTextField(token);
        tokenFld.setFont(Theme.fontMono(11)); tokenFld.setForeground(new Color(0xFFCA28));
        tokenFld.setBackground(new Color(0x0B1A0B)); tokenFld.setOpaque(true);
        tokenFld.setHorizontalAlignment(SwingConstants.CENTER); tokenFld.setEditable(false);
        tokenFld.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x2E4E2E)), BorderFactory.createEmptyBorder(6,8,6,8)));
        p.add(tokenFld, gc);

        JTextField inputFld = new JTextField();
        inputFld.setFont(Theme.fontMono(13)); inputFld.setForeground(FIELD_FG);
        inputFld.setBackground(FIELD_BG); inputFld.setOpaque(true);
        inputFld.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(FIELD_BORDER,1), BorderFactory.createEmptyBorder(7,10,7,10)));
        p.add(new JLabel("Paste token here to activate:"){{ setFont(Theme.fontBody(11)); setForeground(new Color(0x8AAA8A)); }}, gc);
        p.add(inputFld, gc);

        JButton vBtn = Theme.primaryButton("✓  Verify & Activate");
        gc.fill=GridBagConstraints.NONE; gc.insets=new Insets(10,0,0,0);
        vBtn.addActionListener(e -> {
            String err = db.verifyEmail(inputFld.getText().trim());
            if (err == null) {
                dlg.dispose();
                setRegFeedback("✓ Verified! You can now sign in.", true);
                regFeedback.setForeground(new Color(0x66BB6A));
                cardLayout.show(cardPanel, "LOGIN");
            } else { inputFld.setForeground(new Color(0xEF5350)); inputFld.setText("Invalid token — try again"); }
        });
        p.add(vBtn, gc);
        dlg.setContentPane(p); dlg.setVisible(true);
    }

    private void setLoginFeedback(String m, boolean ok) {
        loginFeedback.setText(m); loginFeedback.setForeground(ok ? new Color(0x779977) : new Color(0xEF5350)); }
    private void setRegFeedback(String m, boolean ok) {
        regFeedback.setText(m); regFeedback.setForeground(ok ? new Color(0x779977) : new Color(0xEF5350)); }

    // =========================================================================
    //  PARTICLES
    // =========================================================================

    private void initParticles() {
        for (int i = 0; i < px.length; i++) {
            px[i]=(float)Math.random(); py[i]=(float)Math.random();
            pspd[i]=0.0004f+(float)(Math.random()*0.0008f);
            psz[i]=10+(float)(Math.random()*28); pred[i]=Math.random()>0.5;
        }
    }
    private void startAnim() {
        animTimer = new Timer(33, e -> {
            for (int i=0;i<py.length;i++) {
                py[i]+=pspd[i];
                if (py[i]>1.1f) { py[i]=-0.1f; px[i]=(float)Math.random(); }
            }
            repaint();
        });
        animTimer.start();
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Deep background gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(0x071207), 0, getHeight(), new Color(0x0A1A0A));
        g.setPaint(bg);
        g.fillRect(0, 0, getWidth(), getHeight());
        // Floating orbs
        for (int i=0;i<px.length;i++) {
            int x=(int)(px[i]*getWidth()), y=(int)(py[i]*getHeight()), r=(int)(psz[i]/2);
            Color base = pred[i] ? new Color(0xC62828) : new Color(0x2E7D32);
            g.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(), 28));
            g.fillOval(x-r, y-r, r*2, r*2);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
