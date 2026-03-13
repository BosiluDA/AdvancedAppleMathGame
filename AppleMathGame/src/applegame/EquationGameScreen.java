package applegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class EquationGameScreen extends JPanel {

    private final GameState state;
    private final DataBaseManager db;
    private final MainWindow window;

    // ── Game state ────────────────────────────────────────────────────────────
    private BufferedImage questionImage = null;
    private int  correctAnswer     = -1;
    private boolean answered       = false;
    private boolean loading        = false;
    private int  questionsAnswered = 0;
    private int  score             = 0;
    private int  streak            = 0;
    private int  timeLeft          = 60;
    private boolean gameOver       = false;
    private String  apiSourceLabel = "";

    private Timer gameTimer;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel  timerLabel, scoreLabel, streakLabel, statusLabel, apiLabel;
    private JPanel  imagePanel;
    private JButton[] answerButtons;
    private JButton skipBtn;

    // ── Spinner animation ─────────────────────────────────────────────────────
    private float spinAngle = 0f;
    private Timer spinTimer;

    // ── Answer button colors ──────────────────────────────────────────────────
    private final Color[] btnColors = new Color[10];

    public EquationGameScreen(GameState state, DataBaseManager db, MainWindow window) {
        this.state  = state;
        this.db     = db;
        this.window = window;
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        startTimer();
        fetchNextQuestion();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ═════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(14, 32, 8, 32));
        center.add(buildHUD(),         BorderLayout.NORTH);
        center.add(buildImagePanel(),  BorderLayout.CENTER);
        center.add(buildAnswerPanel(), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER_SOFT);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
            }
        };
        h.setOpaque(false);
        h.setBorder(Theme.emptyBorder(10, 20));

        JLabel title = new JLabel("🧮  Apple Equation Challenge");
        title.setFont(Theme.fontTitle(20));
        title.setForeground(Theme.TEXT_PRIMARY);

        apiLabel = new JLabel("Connecting…");
        apiLabel.setFont(Theme.fontMono(11));
        apiLabel.setForeground(Theme.TEXT_DIM);

        JButton quit = Theme.ghostButton("⌂ Levels");
        quit.addActionListener(e -> { cleanup(); window.goToLevels(); });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(apiLabel);
        right.add(quit);

        h.add(title, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildHUD() {
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        hud.setOpaque(false);

        timerLabel  = hudPill("⏱",  "60s", Theme.APPLE_GOLD);
        scoreLabel  = hudPill("⭐", "0",    Theme.APPLE_LIME);
        streakLabel = hudPill("🔥", "0",    new Color(0xFF7043));

        statusLabel = new JLabel("Loading first question…");
        statusLabel.setFont(Theme.fontBody(13));
        statusLabel.setForeground(Theme.TEXT_MUTED);

        hud.add(timerLabel);
        hud.add(scoreLabel);
        hud.add(streakLabel);
        hud.add(statusLabel);
        return hud;
    }

    private JLabel hudPill(String icon, String val, Color col) {
        JLabel l = new JLabel(icon + "  " + val) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x122010));
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g.setColor(Theme.BORDER_SOFT);
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 10, 10));
                super.paintComponent(g0);
                g.dispose();
            }
        };
        l.setFont(Theme.fontMono(14));
        l.setForeground(col);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        l.setPreferredSize(new Dimension(110, 40));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private JPanel buildImagePanel() {
        imagePanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1A3020), 0, h, new Color(0x0E1A12));
                g.setPaint(gp);
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
                g.setColor(loading ? Theme.BORDER_GLOW : Theme.BORDER_SOFT);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w-1, h-1, 18, 18));

                if (questionImage != null && !loading) {
                    int iw = questionImage.getWidth(), ih = questionImage.getHeight();
                    double scale = Math.min((double)(w - 48) / iw, (double)(h - 32) / ih);
                    int dw = (int)(iw * scale), dh = (int)(ih * scale);
                    int dx = (w - dw) / 2, dy = (h - dh) / 2;
                    Shape clip = g.getClip();
                    g.setClip(new RoundRectangle2D.Float(dx-4, dy-4, dw+8, dh+8, 10, 10));
                    g.drawImage(questionImage, dx, dy, dw, dh, null);
                    g.setClip(clip);
                } else if (loading) {
                    drawSpinner(g, w / 2, h / 2, 28);
                }
                g.dispose();
                super.paintComponent(g0);
            }
        };
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(600, 230));
        return imagePanel;
    }

    private void drawSpinner(Graphics2D g, int cx, int cy, int r) {
        int spokes = 10;
        for (int i = 0; i < spokes; i++) {
            float fraction = (float) i / spokes;
            double angle = spinAngle + fraction * Math.PI * 2;
            float alpha = 0.15f + 0.85f * fraction;
            g.setColor(new Color(
                Theme.APPLE_LIME.getRed()   / 255f,
                Theme.APPLE_LIME.getGreen() / 255f,
                Theme.APPLE_LIME.getBlue()  / 255f,
                alpha));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int x1 = (int)(cx + Math.cos(angle) * (r * 0.55));
            int y1 = (int)(cy + Math.sin(angle) * (r * 0.55));
            int x2 = (int)(cx + Math.cos(angle) * r);
            int y2 = (int)(cy + Math.sin(angle) * r);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void startSpinner() {
        if (spinTimer != null && spinTimer.isRunning()) return;
        spinTimer = new Timer(40, e -> {
            spinAngle += 0.22f;
            if (spinAngle > Math.PI * 2) spinAngle -= (float)(Math.PI * 2);
            imagePanel.repaint();
        });
        spinTimer.start();
    }

    private void stopSpinner() {
        if (spinTimer != null) { spinTimer.stop(); spinTimer = null; }
        imagePanel.repaint();
    }

    private JPanel buildAnswerPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        JLabel prompt = new JLabel("What is the missing number?  (0 – 9)");
        prompt.setFont(Theme.fontBody(13));
        prompt.setForeground(Theme.TEXT_MUTED);
        prompt.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel btnGrid = new JPanel(new GridLayout(2, 5, 8, 8));
        btnGrid.setOpaque(false);
        answerButtons = new JButton[10];

        for (int i = 0; i < 10; i++) {
            final int num = i;
            answerButtons[i] = new JButton(String.valueOf(i)) {
                private boolean hovered = false;
                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) {
                            if (btnColors[num] == null) { hovered = true; repaint(); }
                        }
                        @Override public void mouseExited(MouseEvent e) {
                            hovered = false; repaint();
                        }
                    });
                }
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color override = btnColors[num];
                    Color bg = override != null ? override
                             : hovered          ? new Color(0x2A4A2A)
                             :                    new Color(0x122010);
                    g.setColor(bg);
                    g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                    Color border = override != null ? override.brighter()
                                 : hovered          ? Theme.APPLE_LIME
                                 :                    Theme.BORDER_SOFT;
                    g.setColor(border);
                    g.setStroke(new BasicStroke(override != null ? 2.5f : 1.5f));
                    g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 12, 12));
                    g.setFont(Theme.fontMono(20));
                    g.setColor(override != null ? Color.WHITE : Theme.TEXT_PRIMARY);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g.dispose();
                }
            };
            answerButtons[i].setOpaque(false);
            answerButtons[i].setContentAreaFilled(false);
            answerButtons[i].setBorderPainted(false);
            answerButtons[i].setFocusPainted(false);
            answerButtons[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            answerButtons[i].setPreferredSize(new Dimension(70, 60));
            answerButtons[i].addActionListener(e -> onAnswerClicked(num));
            btnGrid.add(answerButtons[i]);
        }

        wrapper.add(prompt, BorderLayout.NORTH);
        wrapper.add(btnGrid, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        footer.setOpaque(false);

        skipBtn = Theme.ghostButton("Skip →");
        skipBtn.setToolTipText("Skip this question (breaks streak)");
        skipBtn.addActionListener(e -> {
            if (!loading && !gameOver) {
                streak = 0;
                state.resetStreak();
                updateHUD();
                fetchNextQuestion();
            }
        });

        JButton lbBtn = Theme.ghostButton("🏆 Leaderboard");
        lbBtn.addActionListener(e -> { cleanup(); window.goToLeaderboard(); });

        footer.add(skipBtn);
        footer.add(lbBtn);
        return footer;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  QUESTION LOADING  (via QuestionLoader)
    // ═════════════════════════════════════════════════════════════════════════

    private void fetchNextQuestion() {
        if (gameOver) return;

        loading = true;
        answered = false;
        questionImage = null;
        setAnswerButtonsEnabled(false);
        clearButtonColors();
        statusLabel.setText("Fetching question from API…");
        statusLabel.setForeground(Theme.TEXT_MUTED);
        apiLabel.setText("Connecting…");
        apiLabel.setForeground(Theme.TEXT_DIM);
        startSpinner();

        QuestionLoader.fetch(result -> {
            loading = false;
            stopSpinner();

            if (result.isOk()) {
                // ── Success ───────────────────────────────────────────────────
                questionImage  = result.image;
                correctAnswer  = result.answer;
                apiSourceLabel = result.detail;

                apiLabel.setText("● " + result.detail);
                apiLabel.setForeground(Theme.APPLE_LIME);
                statusLabel.setText("What is the missing number?");
                statusLabel.setForeground(Theme.TEXT_MUTED);
                setAnswerButtonsEnabled(true);
                imagePanel.revalidate();
                imagePanel.repaint();

                // Pre-warm next question in background
                QuestionLoader.prefetch();

            } else {
                // ── API unavailable ───────────────────────────────────────────
                apiLabel.setText("● API offline");
                apiLabel.setForeground(Theme.WRONG_GLOW);
                statusLabel.setText("⚠  Cannot reach API.");
                statusLabel.setForeground(Theme.WRONG_GLOW);
                showApiErrorOverlay(result.detail);
            }
        });
    }

    private void showApiErrorOverlay(String errorDetail) {
        imagePanel.removeAll();
        imagePanel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.insets = new Insets(6, 0, 6, 0);
        gc.anchor = GridBagConstraints.CENTER;

        JLabel icon = new JLabel("🌐");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

        JLabel msg = new JLabel("Could not reach the puzzle API");
        msg.setFont(Theme.fontBodyBold(15));
        msg.setForeground(Theme.TEXT_PRIMARY);

        JLabel detail = new JLabel("<html><div style='text-align:center;width:340px'>"
            + errorDetail + "</div></html>");
        detail.setFont(Theme.fontBody(12));
        detail.setForeground(Theme.TEXT_DIM);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btns.setOpaque(false);

        JButton retryBtn = Theme.primaryButton("↺  Retry");
        retryBtn.addActionListener(e -> {
            imagePanel.removeAll();
            imagePanel.setLayout(new GridBagLayout());
            imagePanel.revalidate();
            imagePanel.repaint();
            fetchNextQuestion();
        });

        JButton quitBtn = Theme.ghostButton("⌂ Back to Levels");
        quitBtn.addActionListener(e -> { cleanup(); window.goToLevels(); });

        btns.add(retryBtn);
        btns.add(quitBtn);

        imagePanel.add(icon,   gc);
        imagePanel.add(msg,    gc);
        imagePanel.add(detail, gc);
        imagePanel.add(btns,   gc);
        imagePanel.revalidate();
        imagePanel.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ANSWER HANDLING
    // ═════════════════════════════════════════════════════════════════════════

    private void onAnswerClicked(int chosen) {
        if (answered || loading || gameOver || correctAnswer < 0) return;
        answered = true;
        setAnswerButtonsEnabled(false);

        if (chosen == correctAnswer) {
            streak++;
            int points = 10 + (streak > 1 ? (streak - 1) * 3 : 0);
            score += points;
            state.addEquationScore(points);
            state.incrementStreak();
            questionsAnswered++;

            btnColors[chosen] = new Color(0x1B5E20);
            repaintAnswerButtons();

            String streakMsg = streak > 1 ? "  🔥 " + streak + "× streak!" : "";
            statusLabel.setText("✓ Correct! +" + points + streakMsg);
            statusLabel.setForeground(Theme.APPLE_LIME);
            updateHUD();

            if (questionsAnswered % 3 == 0) {
                pauseAndShowFact();
            } else {
                Timer t = new Timer(700, e -> fetchNextQuestion());
                t.setRepeats(false);
                t.start();
            }

        } else {
            streak = 0;
            state.resetStreak();
            score = Math.max(0, score - 2);

            btnColors[chosen]        = new Color(0x7F0000);
            btnColors[correctAnswer] = new Color(0x1B5E20);
            repaintAnswerButtons();

            statusLabel.setText("✗ Wrong — the answer was " + correctAnswer + ". Next question coming…");
            statusLabel.setForeground(Theme.WRONG_GLOW);
            updateHUD();

            Timer t = new Timer(1500, e -> fetchNextQuestion());
            t.setRepeats(false);
            t.start();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TIMER
    // ═════════════════════════════════════════════════════════════════════════

    private void startTimer() {
        gameTimer = new Timer(1000, e -> {
            if (loading) return; // pause countdown while fetching
            timeLeft--;
            updateHUD();
            if (timeLeft <= 0) {
                gameTimer.stop();
                gameOver = true;
                setAnswerButtonsEnabled(false);
                SwingUtilities.invokeLater(this::showEndDialog);
            }
        });
        gameTimer.start();
    }

    private void updateHUD() {
        timerLabel.setText("⏱  " + timeLeft + "s");
        timerLabel.setForeground(timeLeft <= 10 ? Theme.WRONG_GLOW : Theme.APPLE_GOLD);
        scoreLabel.setText("⭐  " + score);
        streakLabel.setText("🔥  " + streak);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DIALOGS
    // ═════════════════════════════════════════════════════════════════════════

    private void pauseAndShowFact() {
        if (gameTimer != null) gameTimer.stop();
        String[] fact = state.getFactForLevel(questionsAnswered / 3);

        JDialog dlg = makeDialog(480, 310);
        JPanel p = dialogPanel(Theme.APPLE_LIME, Theme.APPLE_GREEN);
        p.setBorder(Theme.emptyBorder(28, 32));
        GridBagConstraints gbc = centeredGbc();

        p.add(emojiLabel(fact[0], 38), gbc);
        p.add(styledLabel("🍏 Did You Know? — " + fact[1], 14, Theme.APPLE_LIME), gbc);
        p.add(wrapText(fact[2], 380, 72), gbc);

        JButton cont = Theme.primaryButton("Continue Playing →");
        cont.addActionListener(e -> {
            dlg.dispose();
            if (gameTimer != null) gameTimer.start();
            fetchNextQuestion();
        });
        gbc.insets = new Insets(14, 0, 0, 0);
        p.add(cont, gbc);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void showEndDialog() {
        QuestionLoader.cancelPrefetch();
        // Persist to DB
        int uid = state.getUserId();
        if (db != null && db.isConnected() && uid > 0) {
            db.saveScore(uid, state.getPlayerName(), score, "Equations", 0, 60 - timeLeft, streak);
            db.audit(uid, state.getPlayerName(), "EQUATION_GAME_END",
                     "score=" + score + " solved=" + questionsAnswered + " streak=" + streak);
        }
        // leaderboard refreshed from DB on next open
        String[] fact = state.getFactForLevel(Math.max(1, questionsAnswered / 3));

        JDialog dlg = makeDialog(500, 430);
        JPanel p = dialogPanel(Theme.APPLE_RED, Theme.APPLE_GOLD);
        p.setBorder(Theme.emptyBorder(28, 36));
        GridBagConstraints gbc = centeredGbc();

        p.add(emojiLabel("🍎", 44), gbc);
        p.add(styledLabel("Time's Up!", 28, Theme.APPLE_RED), gbc);

        JLabel scoreInfo = new JLabel("Final Score: " + score
            + "   Best Streak: " + state.getEquationStreak());
        scoreInfo.setFont(Theme.fontMono(14));
        scoreInfo.setForeground(Theme.APPLE_GOLD);
        p.add(scoreInfo, gbc);

        JLabel solved = new JLabel("Questions Solved: " + questionsAnswered
            + "  via " + (apiSourceLabel.isEmpty() ? "API" : apiSourceLabel));
        solved.setFont(Theme.fontBody(13));
        solved.setForeground(Theme.TEXT_MUTED);
        p.add(solved, gbc);

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER_SOFT);
        sep.setPreferredSize(new Dimension(400, 1));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        p.add(sep, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 0, 4, 0);
        p.add(styledLabel("🍎 Math Fun Fact — " + fact[1], 13, Theme.APPLE_LIME), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(wrapText(fact[2], 400, 60), gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        JButton again = Theme.primaryButton("Play Again");
        JButton home  = Theme.ghostButton("⌂ Level Select");
        again.addActionListener(e -> { dlg.dispose(); window.startEquationGame(); });
        home.addActionListener(e  -> { dlg.dispose(); window.goToLevels(); });
        btnRow.add(again);
        btnRow.add(home);

        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(14, 0, 0, 0);
        p.add(btnRow, gbc);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private void setAnswerButtonsEnabled(boolean on) {
        if (answerButtons == null) return;
        for (JButton b : answerButtons) b.setEnabled(on);
    }

    private void clearButtonColors() {
        for (int i = 0; i < btnColors.length; i++) btnColors[i] = null;
        repaintAnswerButtons();
    }

    private void repaintAnswerButtons() {
        if (answerButtons == null) return;
        for (JButton b : answerButtons) b.repaint();
    }

    private void cleanup() {
        if (gameTimer != null) { gameTimer.stop(); gameTimer = null; }
        stopSpinner();
        QuestionLoader.cancelPrefetch();
    }

    private JDialog makeDialog(int w, int h) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        d.setUndecorated(true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        return d;
    }

    private JPanel dialogPanel(Color barLeft, Color barRight) {
        return new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 22);
                GradientPaint bar = new GradientPaint(0, 0, barLeft, getWidth(), 0, barRight);
                g.setPaint(bar);
                g.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g.dispose();
            }
        };
    }

    private GridBagConstraints centeredGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx  = 0;
        gbc.gridy  = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        return gbc;
    }

    private JLabel emojiLabel(String emoji, int size) {
        JLabel l = new JLabel(emoji);
        l.setFont(new Font("Segoe UI Emoji", Font.PLAIN, size));
        return l;
    }

    private JLabel styledLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBodyBold(size));
        l.setForeground(color);
        return l;
    }

    private JTextArea wrapText(String text, int w, int h) {
        JTextArea ta = new JTextArea(text);
        ta.setFont(Theme.fontBody(13));
        ta.setForeground(Theme.TEXT_MUTED);
        ta.setBackground(new Color(0, 0, 0, 0));
        ta.setOpaque(false);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setPreferredSize(new Dimension(w, h));
        ta.setBorder(null);
        return ta;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
