package applegame;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class LoginScreen extends JPanel {

    private final DataBaseManager db;
    private final MainWindow      window;

    // ── Shared fields ─────────────────────────────────────────────────────────
    private JPanel     cardPanel;
    private CardLayout cardLayout;

    // ── Login tab ─────────────────────────────────────────────────────────────
    private JTextField     loginUserField;
    private JPasswordField loginPassField;
    private JLabel         loginFeedback;
    private JButton        loginBtn;
    private Timer          lockoutTimer;
    private int            lockoutSecsLeft = 0;

    // ── Register tab ──────────────────────────────────────────────────────────
    private JTextField     regUserField, regEmailField;
    private JPasswordField regPassField, regConfirmField;
    private JLabel         regFeedback, strengthLabel;
    private JProgressBar   strengthBar;

    // ── Background particles ───────────────────────────────────────────────────
    private final float[]   px = new float[12], py = new float[12];
    private final float[]   pspd = new float[12], psz = new float[12];
    private final boolean[] pred = new boolean[12];
    private Timer           animTimer;

    public LoginScreen(DataBaseManager db, MainWindow window) {
        this.db     = db;
        this.window = window;
        setOpaque(false);
        setLayout(new GridBagLayout());
        initParticles();
        buildUI();
        startAnim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BUILD UI
    // ═════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        JPanel outer = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 24);
                GradientPaint bar = new GradientPaint(0,0,Theme.APPLE_RED,getWidth(),0,Theme.APPLE_LIME);
                g.setPaint(bar);
                g.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g.dispose();
            }
        };
        outer.setOpaque(false);
        outer.setPreferredSize(new Dimension(440, 560));
        outer.setBorder(Theme.emptyBorder(24, 36));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill  = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.anchor = GridBagConstraints.CENTER;

        // Logo
        JLabel logo = new JLabel("🍎", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        gc.fill = GridBagConstraints.NONE;
        outer.add(logo, gc);

        JLabel title = new JLabel("Apple Math Puzzle", SwingConstants.CENTER);
        title.setFont(Theme.fontTitle(24));
        title.setForeground(Theme.TEXT_PRIMARY);
        outer.add(title, gc);

        // Tab switcher
        JPanel tabs = new JPanel(new GridLayout(1, 2, 4, 0));
        tabs.setOpaque(false);
        tabs.setPreferredSize(new Dimension(360, 34));
        JButton loginTab = tabBtn("Sign In",  true);
        JButton regTab   = tabBtn("Register", false);
        tabs.add(loginTab);
        tabs.add(regTab);
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(10, 0, 8, 0);
        outer.add(tabs, gc);

        // Card switcher
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(360, 320));
        cardPanel.add(buildLoginPanel(), "LOGIN");
        cardPanel.add(buildRegisterPanel(), "REGISTER");

        loginTab.addActionListener(e -> {
            cardLayout.show(cardPanel, "LOGIN");
            loginTab.setForeground(Theme.APPLE_LIME);
            regTab.setForeground(Theme.TEXT_MUTED);
        });
        regTab.addActionListener(e -> {
            cardLayout.show(cardPanel, "REGISTER");
            regTab.setForeground(Theme.APPLE_LIME);
            loginTab.setForeground(Theme.TEXT_MUTED);
        });

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);
        outer.add(cardPanel, gc);

        // DB status
        JLabel dbStatus = new JLabel(
            db.isConnected() ? "● Database connected" : "● Database offline — scores won't save",
            SwingConstants.CENTER);
        dbStatus.setFont(Theme.fontMono(11));
        dbStatus.setForeground(db.isConnected() ? Theme.APPLE_LIME : Theme.WRONG_GLOW);
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(12, 0, 0, 0);
        outer.add(dbStatus, gc);

        add(outer, new GridBagConstraints());
    }

    // ── Login panel ───────────────────────────────────────────────────────────

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = fieldGbc();

        p.add(fieldLabel("Username"), gc);
        loginUserField = inputField();
        p.add(loginUserField, gc);

        p.add(fieldLabel("Password"), gc);
        loginPassField = passField();
        JPanel passRow = passRowWithToggle(loginPassField, null);
        p.add(passRow, gc);

        loginFeedback = feedbackLabel();
        gc.insets = new Insets(6, 0, 6, 0);
        p.add(loginFeedback, gc);

        loginBtn = Theme.primaryButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(300, 40));
        loginBtn.addActionListener(e -> doLogin());
        gc.insets = new Insets(4, 0, 4, 0);
        p.add(loginBtn, gc);

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        loginUserField.addKeyListener(enter);
        loginPassField.addKeyListener(enter);

        return p;
    }

    // ── Register panel ────────────────────────────────────────────────────────

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = fieldGbc();

        p.add(fieldLabel("Username  (letters, numbers, _ only, 3–20 chars)"), gc);
        regUserField = inputField();
        p.add(regUserField, gc);

        p.add(fieldLabel("Email"), gc);
        regEmailField = inputField();
        p.add(regEmailField, gc);

        p.add(fieldLabel("Password"), gc);
        regPassField = passField();

        // Strength bar
        strengthBar = new JProgressBar(0, 3);
        strengthBar.setPreferredSize(new Dimension(300, 6));
        strengthBar.setBorderPainted(false);
        strengthBar.setForeground(Theme.WRONG_GLOW);
        strengthBar.setBackground(new Color(0x122010));
        strengthBar.setOpaque(true);

        strengthLabel = new JLabel("Enter a password");
        strengthLabel.setFont(Theme.fontMono(11));
        strengthLabel.setForeground(Theme.TEXT_DIM);

        regPassField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateStrength(); }
            public void removeUpdate(DocumentEvent e) { updateStrength(); }
            public void changedUpdate(DocumentEvent e) { updateStrength(); }
            void updateStrength() {
                String pw = new String(regPassField.getPassword());
                int str = DataBaseManager.passwordStrength(pw);
                strengthBar.setValue(str);
                Color[] cols = { Theme.WRONG_GLOW, new Color(0xFF8C00), Theme.APPLE_GOLD, Theme.APPLE_LIME };
                strengthBar.setForeground(cols[str]);
                strengthLabel.setText(DataBaseManager.strengthLabel[str]);
                strengthLabel.setForeground(cols[str]);
            }
        });

        JPanel passRow = passRowWithToggle(regPassField, null);
        p.add(passRow, gc);

        JPanel strRow = new JPanel(new BorderLayout(6, 0));
        strRow.setOpaque(false);
        strRow.setPreferredSize(new Dimension(300, 18));
        strRow.add(strengthBar, BorderLayout.CENTER);
        strRow.add(strengthLabel, BorderLayout.EAST);
        gc.insets = new Insets(2, 0, 4, 0);
        p.add(strRow, gc);

        gc.insets = new Insets(4, 0, 2, 0);
        p.add(fieldLabel("Confirm Password"), gc);
        regConfirmField = passField();
        JPanel confirmRow = passRowWithToggle(regConfirmField, null);
        gc.insets = new Insets(2, 0, 4, 0);
        p.add(confirmRow, gc);

        regFeedback = feedbackLabel();
        p.add(regFeedback, gc);

        JButton regBtn = Theme.successButton("Create Account");
        regBtn.setPreferredSize(new Dimension(300, 40));
        regBtn.addActionListener(e -> doRegister());
        gc.insets = new Insets(4, 0, 4, 0);
        p.add(regBtn, gc);

        return p;
    }

    // ── Shared field helpers ──────────────────────────────────────────────────

    private JButton tabBtn(String text, boolean active) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x122010));
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g.setColor(getForeground().equals(Theme.APPLE_LIME) ? Theme.APPLE_LIME : Theme.BORDER_SOFT);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1,getHeight()-1,8,8));
                super.paintComponent(g0);
                g.dispose();
            }
        };
        b.setFont(Theme.fontBodyBold(13));
        b.setForeground(active ? Theme.APPLE_LIME : Theme.TEXT_MUTED);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBody(11));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private JTextField inputField() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x0A1808));
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g.setColor(isFocusOwner() ? Theme.APPLE_LIME : Theme.BORDER_GLOW);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1,getHeight()-1,8,8));
                super.paintComponent(g0);
                g.dispose();
            }
        };
        f.setFont(Theme.fontBody(13));
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.APPLE_LIME);
        f.setOpaque(false); f.setBackground(new Color(0,0,0,0));
        f.setBorder(BorderFactory.createEmptyBorder(7,10,7,10));
        f.setPreferredSize(new Dimension(300, 36));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw solid background first
                g.setColor(new Color(0x0A1808));
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                // Draw border
                g.setColor(isFocusOwner() ? Theme.APPLE_LIME : Theme.BORDER_GLOW);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 8, 8));
                g.dispose();
                // Paint text/caret on top via super — but we skip super's own background
                super.paintComponent(g0);
            }
            @Override public boolean isOpaque() { return false; } // let our paintComponent handle bg
        };
        f.setFont(new Font("Monospaced", Font.PLAIN, 16)); // larger so bullets are visible
        f.setForeground(new Color(0xE8F5E9));              // near-white so text shows
        f.setCaretColor(Theme.APPLE_LIME);
        f.setEchoChar('●');
        f.setOpaque(false);
        f.setBackground(new Color(0x0A1808));
        f.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 34));
        f.setPreferredSize(new Dimension(300, 36));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    /** Wraps a password field with a show/hide eye toggle button */
    private JPanel passRowWithToggle(JPasswordField pf, Runnable onChange) {
        JPanel row = new JPanel(null) {
            @Override protected void paintComponent(Graphics g0) {
                // transparent — let the password field paint its own bg
                super.paintComponent(g0);
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(300, 36));

        pf.setBounds(0, 0, 300, 36);
        row.add(pf);

        // Eye toggle button — drawn as simple text label inside the field
        JLabel eye = new JLabel("👁") {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
                g.setColor(Theme.TEXT_MUTED);
                FontMetrics fm = g.getFontMetrics();
                String txt = getText();
                g.drawString(txt,
                    (getWidth()  - fm.stringWidth(txt)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        };
        eye.setBounds(268, 7, 26, 22);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.setToolTipText("Show / hide password");
        eye.setOpaque(false);

        final boolean[] showing = {false};
        eye.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showing[0] = !showing[0];
                pf.setEchoChar(showing[0] ? (char) 0 : '●');
                eye.setText(showing[0] ? "🙈" : "👁");
                eye.repaint();
                pf.requestFocusInWindow();
            }
        });

        row.add(eye);
        return row;
    }

    private JLabel feedbackLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(Theme.fontBody(12));
        l.setForeground(Theme.WRONG_GLOW);
        l.setPreferredSize(new Dimension(300, 18));
        return l;
    }

    private GridBagConstraints fieldGbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill  = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 0, 3, 0);
        gc.anchor = GridBagConstraints.WEST;
        return gc;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  AUTH ACTIONS
    // ═════════════════════════════════════════════════════════════════════════

    private void doLogin() {
        if (lockoutSecsLeft > 0) return;
        String username = loginUserField.getText().trim();
        String password = new String(loginPassField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            setLoginFeedback("Please enter username and password.", false);
            return;
        }
        loginBtn.setEnabled(false);
        setLoginFeedback("Signing in…", true);

        new SwingWorker<DataBaseManager.LoginResult, Void>() {
            @Override protected DataBaseManager.LoginResult doInBackground() {
                return db.login(username, password);
            }
            @Override protected void done() {
                try {
                    DataBaseManager.LoginResult result = get();
                    if (result.isSuccess()) {
                        Session.login(result.user, db);
                        window.getGameState().setPlayerName(result.user.username);
                        window.getGameState().setUserId(result.user.id);
                        window.getGameState().loadCompletedLevels(
                            db.getCompletedLevels(result.user.id));
                        window.goToLevels();
                    } else if (result.status == DataBaseManager.LoginResult.Status.LOCKED) {
                        setLoginFeedback(result.message, false);
                        startLockoutCountdown();
                        loginBtn.setEnabled(false);
                    } else {
                        setLoginFeedback(result.message, false);
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
        lockoutTimer = new Timer(1000, e -> {
            lockoutSecsLeft--;
            if (lockoutSecsLeft <= 0) {
                lockoutTimer.stop();
                loginBtn.setEnabled(true);
                setLoginFeedback("You can try again now.", true);
            } else {
                setLoginFeedback("Too many attempts. Wait " + lockoutSecsLeft + "s…", false);
            }
        });
        lockoutTimer.start();
    }

    private void doRegister() {
        String username = regUserField.getText().trim();
        String email    = regEmailField.getText().trim();
        String password = new String(regPassField.getPassword());
        String confirm  = new String(regConfirmField.getPassword());

        // Client-side validation
        if (!password.equals(confirm)) {
            setRegFeedback("Passwords do not match.", false); return;
        }
        if (DataBaseManager.passwordStrength(password) < 1) {
            setRegFeedback("Password is too weak.", false); return;
        }

        setRegFeedback("Creating account…", true);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return db.register(username, email, password);
            }
            @Override protected void done() {
                try {
                    String error = get();
                    if (error == null) {
                        // Show the verification token (since no email server in dev)
                        String token = db.getVerifyToken(username);
                        showVerifyDialog(username, token);
                    } else {
                        setRegFeedback(error, false);
                    }
                } catch (Exception ex) {
                    setRegFeedback("Error: " + ex.getMessage(), false);
                }
            }
        }.execute();
    }

    /** Shows the email verification token in a dialog (dev mode — no real email server) */
    private void showVerifyDialog(String username, String token) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dlg.setUndecorated(true);
        dlg.setSize(460, 320);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 20);
                g.setColor(Theme.APPLE_LIME);
                g.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g.dispose();
            }
        };
        p.setBorder(Theme.emptyBorder(28, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx=0; gc.gridy=GridBagConstraints.RELATIVE;
        gc.insets=new Insets(6,0,6,0); gc.anchor=GridBagConstraints.CENTER;
        gc.fill=GridBagConstraints.HORIZONTAL;

        JLabel icon = new JLabel("📧", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        gc.fill=GridBagConstraints.NONE;
        p.add(icon, gc);

        JLabel title = new JLabel("Verify Your Account", SwingConstants.CENTER);
        title.setFont(Theme.fontTitle(18)); title.setForeground(Theme.APPLE_LIME);
        p.add(title, gc);

        JLabel sub = new JLabel("<html><div style='text-align:center;width:360px'>" +
            "Account created! Copy your verification token below and paste it in the box to activate your account." +
            "</div></html>", SwingConstants.CENTER);
        sub.setFont(Theme.fontBody(12)); sub.setForeground(Theme.TEXT_MUTED);
        gc.fill=GridBagConstraints.HORIZONTAL;
        p.add(sub, gc);

        JTextField tokenField = new JTextField(token);
        tokenField.setFont(Theme.fontMono(12));
        tokenField.setForeground(Theme.APPLE_GOLD);
        tokenField.setBackground(new Color(0x0A1808));
        tokenField.setHorizontalAlignment(SwingConstants.CENTER);
        tokenField.setEditable(false);
        tokenField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_GLOW),
            BorderFactory.createEmptyBorder(6,8,6,8)));
        p.add(tokenField, gc);

        JTextField inputField = inputField();
        inputField.setOpaque(true);
        inputField.setBackground(new Color(0x0A1808));
        p.add(new JLabel("Paste token here to verify:") {{
            setFont(Theme.fontBody(11)); setForeground(Theme.TEXT_MUTED);
        }}, gc);
        p.add(inputField, gc);

        JButton verifyBtn = Theme.primaryButton("Verify & Activate");
        verifyBtn.addActionListener(e -> {
            String entered = inputField.getText().trim();
            String err = db.verifyEmail(entered);
            if (err == null) {
                dlg.dispose();
                setRegFeedback("✓ Account verified! You can now sign in.", true);
                regFeedback.setForeground(Theme.APPLE_LIME);
                cardLayout.show(cardPanel, "LOGIN");
            } else {
                inputField.setForeground(Theme.WRONG_GLOW);
                inputField.setText("Invalid token — try again");
            }
        });
        gc.fill=GridBagConstraints.NONE;
        gc.insets=new Insets(12,0,0,0);
        p.add(verifyBtn, gc);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    // ── Feedback helpers ──────────────────────────────────────────────────────

    private void setLoginFeedback(String msg, boolean neutral) {
        loginFeedback.setText(msg);
        loginFeedback.setForeground(neutral ? Theme.TEXT_MUTED : Theme.WRONG_GLOW);
    }

    private void setRegFeedback(String msg, boolean neutral) {
        regFeedback.setText(msg);
        regFeedback.setForeground(neutral ? Theme.TEXT_MUTED : Theme.WRONG_GLOW);
    }

    // ── Particles ─────────────────────────────────────────────────────────────

    private void initParticles() {
        for (int i = 0; i < px.length; i++) {
            px[i]   = (float) Math.random();
            py[i]   = (float) Math.random();
            pspd[i] = 0.0005f + (float)(Math.random() * 0.001f);
            psz[i]  = 12 + (float)(Math.random() * 22);
            pred[i] = Math.random() > 0.5;
        }
    }

    private void startAnim() {
        animTimer = new Timer(30, e -> {
            for (int i = 0; i < py.length; i++) {
                py[i] += pspd[i];
                if (py[i] > 1.1f) { py[i] = -0.1f; px[i] = (float)Math.random(); }
            }
            repaint();
        });
        animTimer.start();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        Theme.paintDeepBackground(g, w, h);
        for (int i = 0; i < px.length; i++) {
            int x = (int)(px[i]*w), y = (int)(py[i]*h), r = (int)(psz[i]/2);
            Color base = pred[i] ? Theme.APPLE_RED : Theme.APPLE_GREEN;
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 35));
            g.fillOval(x-r, y-r, r*2, r*2);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
