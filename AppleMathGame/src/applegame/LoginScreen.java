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

    private final float[]   px = new float[12], py = new float[12];
    private final float[]   pspd = new float[12], psz = new float[12];
    private final boolean[] pred = new boolean[12];
    private Timer           animTimer;

    private static final Color FIELD_BG     = new Color(0x0D1F0D);
    private static final Color FIELD_FG     = new Color(0xDDFFDD);
    private static final Color FIELD_BORDER = new Color(0x2A4A2A);
    private static final Color FIELD_FOCUS  = new Color(0x4CAF50);

    public LoginScreen(DataBaseManager db, MainWindow window) {
        this.db     = db;
        this.window = window;
        setOpaque(false);
        setLayout(new GridBagLayout());
        initParticles();
        buildUI();
        startAnim();
    }

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
        outer.setPreferredSize(new Dimension(500, 730));
        outer.setBorder(Theme.emptyBorder(20, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill  = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.anchor = GridBagConstraints.CENTER;

        JLabel logo = new JLabel("🍎", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        gc.fill = GridBagConstraints.NONE;
        outer.add(logo, gc);

        JLabel title = new JLabel("Apple Math Puzzle", SwingConstants.CENTER);
        title.setFont(Theme.fontTitle(24));
        title.setForeground(Theme.TEXT_PRIMARY);
        outer.add(title, gc);

        JPanel tabs = new JPanel(new GridLayout(1, 2, 4, 0));
        tabs.setOpaque(false);
        tabs.setPreferredSize(new Dimension(400, 36));
        JButton loginTab = tabBtn("Sign In",  true);
        JButton regTab   = tabBtn("Register", false);
        tabs.add(loginTab); tabs.add(regTab);
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(10, 0, 10, 0);
        outer.add(tabs, gc);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(420, 420));
        cardPanel.add(buildLoginPanel(),    "LOGIN");
        cardPanel.add(buildRegisterPanel(), "REGISTER");

        loginTab.addActionListener(e -> { cardLayout.show(cardPanel, "LOGIN");
            loginTab.setForeground(Theme.APPLE_LIME); regTab.setForeground(Theme.TEXT_MUTED); });
        regTab.addActionListener(e -> { cardLayout.show(cardPanel, "REGISTER");
            regTab.setForeground(Theme.APPLE_LIME); loginTab.setForeground(Theme.TEXT_MUTED); });

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);
        outer.add(cardPanel, gc);

        JPanel divRow = new JPanel(new BorderLayout(8, 0));
        divRow.setOpaque(false);
        divRow.setPreferredSize(new Dimension(420, 22));
        JSeparator sl = new JSeparator(); sl.setForeground(Theme.BORDER_SOFT);
        JSeparator sr = new JSeparator(); sr.setForeground(Theme.BORDER_SOFT);
        JLabel orLbl = new JLabel("or", SwingConstants.CENTER);
        orLbl.setFont(Theme.fontBody(11)); orLbl.setForeground(Theme.TEXT_DIM);
        divRow.add(sl, BorderLayout.WEST); divRow.add(orLbl, BorderLayout.CENTER); divRow.add(sr, BorderLayout.EAST);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 0, 4, 0);
        outer.add(divRow, gc);

        JButton guestBtn = guestButton("👤  Play as Guest");
        guestBtn.addActionListener(e -> {
            Session.loginAsGuest();
            window.getGameState().setPlayerName("Guest");
            window.getGameState().setUserId(-1);
            window.goToLevels();
        });
        gc.insets = new Insets(0, 0, 2, 0);
        outer.add(guestBtn, gc);

        JLabel guestNote = new JLabel("⚠  Guests can play Memory levels only — scores are not saved", SwingConstants.CENTER);
        guestNote.setFont(Theme.fontBody(10)); guestNote.setForeground(Theme.TEXT_DIM);
        gc.insets = new Insets(0, 0, 8, 0);
        outer.add(guestNote, gc);

        JLabel dbStatus = new JLabel(
            db.isConnected() ? "● Database connected" : "● Database offline — scores won't save",
            SwingConstants.CENTER);
        dbStatus.setFont(Theme.fontMono(11));
        dbStatus.setForeground(db.isConnected() ? Theme.APPLE_LIME : Theme.WRONG_GLOW);
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(4, 0, 0, 0);
        outer.add(dbStatus, gc);

        add(outer, new GridBagConstraints());
    }

    private JPanel buildLoginPanel() {
        JPanel p = boxPanel();
        p.add(fieldLabel("Username"));
        loginUserField = textField();
        p.add(loginUserField);
        p.add(vgap(8));
        p.add(fieldLabel("Password"));
        loginPassField = passwordField();
        p.add(wrapWithEye(loginPassField));
        loginFeedback = feedbackLabel();
        p.add(loginFeedback);
        loginBtn = Theme.primaryButton("Sign In");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
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

    private JPanel buildRegisterPanel() {
        JPanel p = boxPanel();
        p.add(fieldLabel("Username  (letters, numbers, _ only · 3–20 chars)"));
        regUserField = textField();
        p.add(regUserField);
        p.add(vgap(6));
        p.add(fieldLabel("Email"));
        regEmailField = textField();
        p.add(regEmailField);
        p.add(vgap(6));
        p.add(fieldLabel("Password"));
        regPassField = passwordField();
        p.add(wrapWithEye(regPassField));

        strengthBar = new JProgressBar(0, 3);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        strengthBar.setBorderPainted(false);
        strengthBar.setForeground(Theme.WRONG_GLOW);
        strengthBar.setBackground(new Color(0x122010));
        strengthBar.setOpaque(true);
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        strengthLabel = new JLabel("Enter a password");
        strengthLabel.setFont(Theme.fontMono(11));
        strengthLabel.setForeground(Theme.TEXT_DIM);
        strengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        regPassField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateStrength(); }
            public void removeUpdate(DocumentEvent e)  { updateStrength(); }
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

        p.add(strengthBar);
        p.add(strengthLabel);
        p.add(vgap(6));
        p.add(fieldLabel("Confirm Password"));
        regConfirmField = passwordField();
        p.add(wrapWithEye(regConfirmField));
        regFeedback = feedbackLabel();
        p.add(regFeedback);

        JButton regBtn = Theme.successButton("Create Account");
        regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        regBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        regBtn.addActionListener(e -> doRegister());
        p.add(regBtn);
        return p;
    }

    // ── Field factory ─────────────────────────────────────────────────────────

    private JTextField textField() {
        JTextField f = new JTextField();
        applyFieldStyle(f);
        return f;
    }

    private JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setEchoChar('●');
        applyFieldStyle(f);
        return f;
    }

    private void applyFieldStyle(JTextComponent f) {
        f.setFont(new Font("Dialog", Font.PLAIN, 14));
        f.setForeground(FIELD_FG);
        f.setBackground(FIELD_BG);
        f.setCaretColor(FIELD_FOCUS);
        f.setOpaque(true);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_FOCUS, 2),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });
    }

    private JPanel wrapWithEye(JPasswordField pf) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 0, FIELD_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        row.add(pf, BorderLayout.CENTER);

        JButton eye = new JButton("👁");
        eye.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        eye.setForeground(new Color(0x88AA88));
        eye.setBackground(FIELD_BG);
        eye.setOpaque(true);
        eye.setContentAreaFilled(true);
        eye.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 1, FIELD_BORDER),
            BorderFactory.createEmptyBorder(0, 6, 0, 8)));
        eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.setPreferredSize(new Dimension(38, 40));
        eye.setToolTipText("Show / hide password");

        final boolean[] showing = {false};
        eye.addActionListener(e -> {
            showing[0] = !showing[0];
            pf.setEchoChar(showing[0] ? (char) 0 : '●');
            eye.setText(showing[0] ? "🙈" : "👁");
        });

        row.add(eye, BorderLayout.EAST);
        return row;
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private JPanel boxPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBody(11));
        l.setForeground(Theme.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        return l;
    }

    private JLabel feedbackLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(Theme.fontBody(12));
        l.setForeground(Theme.WRONG_GLOW);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        return l;
    }

    private Component vgap(int h) { return Box.createRigidArea(new Dimension(0, h)); }

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
                g.setColor(hov ? new Color(0x1A2E18) : new Color(0x111E10));
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g.setColor(Theme.BORDER_SOFT);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1,getHeight()-1,10,10));
                g.setFont(Theme.fontBody(13));
                g.setColor(Theme.TEXT_MUTED);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g.dispose();
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    // ── Auth actions ──────────────────────────────────────────────────────────

    private void doLogin() {
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
                    DataBaseManager.LoginResult result = get();
                    if (result.isSuccess()) {
                        Session.login(result.user, db);
                        window.getGameState().setPlayerName(result.user.username);
                        window.getGameState().setUserId(result.user.id);
                        window.getGameState().loadCompletedLevels(db.getCompletedLevels(result.user.id));
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
                lockoutTimer.stop(); loginBtn.setEnabled(true);
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
        if (!password.equals(confirm)) { setRegFeedback("Passwords do not match.", false); return; }
        if (DataBaseManager.passwordStrength(password) < 1) { setRegFeedback("Password is too weak.", false); return; }
        setRegFeedback("Creating account…", true);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return db.register(username, email, password); }
            @Override protected void done() {
                try {
                    String error = get();
                    if (error == null) { showVerifyDialog(username, db.getVerifyToken(username)); }
                    else setRegFeedback(error, false);
                } catch (Exception ex) { setRegFeedback("Error: " + ex.getMessage(), false); }
            }
        }.execute();
    }

    private void showVerifyDialog(String username, String token) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dlg.setUndecorated(true); dlg.setSize(480, 340); dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 20);
                g.setColor(Theme.APPLE_LIME); g.fillRoundRect(0,0,getWidth(),5,4,4); g.dispose();
            }
        };
        p.setBorder(Theme.emptyBorder(28, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx=0; gc.gridy=GridBagConstraints.RELATIVE;
        gc.insets=new Insets(6,0,6,0); gc.anchor=GridBagConstraints.CENTER; gc.fill=GridBagConstraints.HORIZONTAL;

        JLabel icon = new JLabel("📧", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36)); gc.fill=GridBagConstraints.NONE; p.add(icon, gc);
        JLabel ttl = new JLabel("Verify Your Account", SwingConstants.CENTER);
        ttl.setFont(Theme.fontTitle(18)); ttl.setForeground(Theme.APPLE_LIME); p.add(ttl, gc);
        JLabel sub = new JLabel("<html><div style='text-align:center;width:380px'>Copy your verification token, then paste it below to activate.</div></html>", SwingConstants.CENTER);
        sub.setFont(Theme.fontBody(12)); sub.setForeground(Theme.TEXT_MUTED); gc.fill=GridBagConstraints.HORIZONTAL; p.add(sub, gc);

        JTextField tokenField = new JTextField(token);
        tokenField.setFont(Theme.fontMono(12)); tokenField.setForeground(Theme.APPLE_GOLD);
        tokenField.setBackground(new Color(0x0A1808)); tokenField.setOpaque(true);
        tokenField.setHorizontalAlignment(SwingConstants.CENTER); tokenField.setEditable(false);
        tokenField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER_GLOW), BorderFactory.createEmptyBorder(6,8,6,8)));
        p.add(tokenField, gc);

        JTextField inputField = new JTextField();
        inputField.setFont(Theme.fontMono(13)); inputField.setForeground(FIELD_FG);
        inputField.setBackground(FIELD_BG); inputField.setOpaque(true);
        inputField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(FIELD_BORDER,1), BorderFactory.createEmptyBorder(7,10,7,10)));
        p.add(new JLabel("Paste token here:"){{ setFont(Theme.fontBody(11)); setForeground(Theme.TEXT_MUTED); }}, gc);
        p.add(inputField, gc);

        JButton vBtn = Theme.primaryButton("Verify & Activate");
        vBtn.addActionListener(e -> {
            String err = db.verifyEmail(inputField.getText().trim());
            if (err == null) {
                dlg.dispose();
                setRegFeedback("✓ Account verified! You can now sign in.", true);
                regFeedback.setForeground(Theme.APPLE_LIME);
                cardLayout.show(cardPanel, "LOGIN");
            } else { inputField.setForeground(Theme.WRONG_GLOW); inputField.setText("Invalid token — try again"); }
        });
        gc.fill=GridBagConstraints.NONE; gc.insets=new Insets(12,0,0,0); p.add(vBtn, gc);
        dlg.setContentPane(p); dlg.setVisible(true);
    }

    private void setLoginFeedback(String msg, boolean neutral) {
        loginFeedback.setText(msg);
        loginFeedback.setForeground(neutral ? Theme.TEXT_MUTED : Theme.WRONG_GLOW);
    }
    private void setRegFeedback(String msg, boolean neutral) {
        regFeedback.setText(msg);
        regFeedback.setForeground(neutral ? Theme.TEXT_MUTED : Theme.WRONG_GLOW);
    }

    private void initParticles() {
        for (int i = 0; i < px.length; i++) {
            px[i]=(float)Math.random(); py[i]=(float)Math.random();
            pspd[i]=0.0005f+(float)(Math.random()*0.001f);
            psz[i]=12+(float)(Math.random()*22); pred[i]=Math.random()>0.5;
        }
    }
    private void startAnim() {
        animTimer = new Timer(30, e -> {
            for (int i=0;i<py.length;i++) { py[i]+=pspd[i]; if(py[i]>1.1f){py[i]=-0.1f;px[i]=(float)Math.random();} }
            repaint();
        });
        animTimer.start();
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        for (int i=0;i<px.length;i++) {
            int x=(int)(px[i]*getWidth()), y=(int)(py[i]*getHeight()), r=(int)(psz[i]/2);
            Color base = pred[i] ? Theme.APPLE_RED : Theme.APPLE_GREEN;
            g.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),35));
            g.fillOval(x-r,y-r,r*2,r*2);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
