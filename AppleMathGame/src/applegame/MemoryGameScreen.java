package applegame;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class MemoryGameScreen extends JPanel {
    private final GameState state;
    private final DataBaseManager db;
    private final MainWindow window;
    private final int level;
    private final int[] cfg;

    private List<CardPanel> cardPanels = new ArrayList<>();
    private CardPanel firstFlipped = null;
    private CardPanel secondFlipped = null;
    private boolean inputBlocked = false;

    private int matchedPairs = 0;
    private int totalPairs;
    private int score = 0;
    private int hintsLeft;
    private int timeLeft;
    private Timer gameTimer;
    private boolean gameOver = false;

    // HUD labels
    private JLabel timerLabel, scoreLabel, hintLabel, pairsLabel;
    private JPanel gridPanel;

    public MemoryGameScreen(GameState state, DataBaseManager db, MainWindow window, int level) {
        this.state = state;
        this.db    = db;
        this.window = window;
        this.level = level;
        this.cfg = GameState.LEVEL_CONFIG[level - 1];
        this.totalPairs = cfg[1];
        this.hintsLeft = cfg[3];
        this.timeLeft = cfg[2];
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        startTimer();
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildHUD(), BorderLayout.NORTH); // Will be wrapped below
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(buildHUD(), BorderLayout.NORTH);
        center.add(buildGrid(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER_SOFT);
                g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(Theme.emptyBorder(10, 20));

        JLabel title = new JLabel("🍎  Level " + level + " — " + GameState.LEVEL_LABELS[level-1] + " (" + cfg[0] + "×" + cfg[0] + " grid)");
        title.setFont(Theme.fontTitle(20));
        title.setForeground(Theme.TEXT_PRIMARY);

        JButton exitBtn = Theme.ghostButton("✕ Quit");
        exitBtn.addActionListener(e -> {
            if (gameTimer != null) gameTimer.stop();
            window.goToLevels();
        });

        header.add(title, BorderLayout.WEST);
        header.add(exitBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel buildHUD() {
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setColor(new Color(0x0A1808));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        hud.setOpaque(false);

        timerLabel = hudPill("⏱", String.valueOf(timeLeft) + "s", Theme.APPLE_GOLD);
        scoreLabel = hudPill("⭐", "0", Theme.APPLE_LIME);
        pairsLabel = hudPill("🃏", "0 / " + totalPairs, Theme.TEXT_MUTED);
        hintLabel  = hudPill("💡", String.valueOf(hintsLeft), new Color(0xFFD700));

        hud.add(timerLabel);
        hud.add(scoreLabel);
        hud.add(pairsLabel);
        hud.add(hintLabel);
        return hud;
    }

    private JLabel hudPill(String icon, String val, Color valColor) {
        JLabel lbl = new JLabel(icon + "  " + val) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x122010));
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g.setColor(Theme.BORDER_SOFT);
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 10, 10));
                super.paintComponent(g0);
                g.dispose();
            }
        };
        lbl.setFont(Theme.fontMono(14));
        lbl.setForeground(valColor);
        lbl.setOpaque(false);
        lbl.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        lbl.setPreferredSize(new Dimension(130, 40));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JPanel buildGrid() {
        // Create and shuffle cards
        List<MemoryCard> cards = new ArrayList<>();
        for (int i = 0; i < totalPairs; i++) {
            // Each pair: two symbols that map to same pair id
            // symbolIndex = i*2 and i*2+1 for the two sides of the pair
            int sym1 = (i * 2) % MemoryCard.SYMBOLS.length;
            int sym2 = (i * 2 + 1) % MemoryCard.SYMBOLS.length;
            cards.add(new MemoryCard(i, sym1));
            cards.add(new MemoryCard(i, sym2));
        }
        Collections.shuffle(cards);

        int cols = cfg[0];
        int rows = (cards.size() + cols - 1) / cols;

        gridPanel = new JPanel(new GridLayout(rows, cols, 8, 8)) {
            @Override protected void paintComponent(Graphics g0) {
                // Transparent
            }
        };
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        for (MemoryCard card : cards) {
            CardPanel cp = new CardPanel(card);
            cp.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    handleCardClick(cp);
                }
            });
            cardPanels.add(cp);
            gridPanel.add(cp);
        }

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        footer.setOpaque(false);

        JButton hintBtn = Theme.ghostButton("💡  Use Hint (" + hintsLeft + " left)");
        hintBtn.addActionListener(e -> useHint(hintBtn));

        JButton leaderBtn = Theme.ghostButton("🏆 Leaderboard");
        leaderBtn.addActionListener(e -> {
            if (gameTimer != null) gameTimer.stop();
            window.goToLeaderboard();
        });

        footer.add(hintBtn);
        footer.add(leaderBtn);
        return footer;
    }

    // ── Game logic ───────────────────────────────────────────────────────────

    private void handleCardClick(CardPanel cp) {
        if (inputBlocked || gameOver) return;
        if (cp.getCard().isMatched() || cp.getCard().isFaceUp()) return;

        cp.getCard().setFaceUp(true);
        cp.animateFlip(true);

        if (firstFlipped == null) {
            firstFlipped = cp;
        } else if (secondFlipped == null && cp != firstFlipped) {
            secondFlipped = cp;
            inputBlocked = true;

            if (firstFlipped.getCard().getPairId() == secondFlipped.getCard().getPairId()) {
                // Match!
                firstFlipped.getCard().setGlowMatch(true);
                secondFlipped.getCard().setGlowMatch(true);
                firstFlipped.repaint();
                secondFlipped.repaint();

                Timer delay = new Timer(600, e -> {
                    firstFlipped.getCard().setMatched(true);
                    secondFlipped.getCard().setMatched(true);
                    firstFlipped.getCard().setGlowMatch(false);
                    secondFlipped.getCard().setGlowMatch(false);
                    firstFlipped.repaint();
                    secondFlipped.repaint();
                    matchedPairs++;
                    score += 10 + Math.max(0, timeLeft / 5);
                    updateHUD();
                    firstFlipped = null;
                    secondFlipped = null;
                    inputBlocked = false;
                    if (matchedPairs == totalPairs) winGame();
                });
                delay.setRepeats(false);
                delay.start();
            } else {
                // No match
                firstFlipped.getCard().setGlowWrong(true);
                secondFlipped.getCard().setGlowWrong(true);
                firstFlipped.repaint();
                secondFlipped.repaint();
                score = Math.max(0, score - 2);

                final CardPanel f = firstFlipped, s = secondFlipped;
                Timer delay = new Timer(900, e -> {
                    f.getCard().setFaceUp(false);
                    s.getCard().setFaceUp(false);
                    f.getCard().setGlowWrong(false);
                    s.getCard().setGlowWrong(false);
                    f.animateFlip(false);
                    s.animateFlip(false);
                    firstFlipped = null;
                    secondFlipped = null;
                    inputBlocked = false;
                    updateHUD();
                });
                delay.setRepeats(false);
                delay.start();
            }
        }
    }

    private void useHint(JButton hintBtn) {
        if (hintsLeft <= 0 || inputBlocked || gameOver) return;
        // Find first unmatched pair and briefly reveal them
        for (int i = 0; i < cardPanels.size(); i++) {
            if (!cardPanels.get(i).getCard().isMatched() && !cardPanels.get(i).getCard().isFaceUp()) {
                int pairId = cardPanels.get(i).getCard().getPairId();
                for (int j = i+1; j < cardPanels.size(); j++) {
                    if (!cardPanels.get(j).getCard().isMatched() &&
                        cardPanels.get(j).getCard().getPairId() == pairId) {
                        hintsLeft--;
                        hintLabel.setText("💡  " + hintsLeft);
                        hintBtn.setText("💡  Use Hint (" + hintsLeft + " left)");
                        CardPanel a = cardPanels.get(i), b = cardPanels.get(j);
                        a.getCard().setFaceUp(true); a.animateFlip(true);
                        b.getCard().setFaceUp(true); b.animateFlip(true);
                        inputBlocked = true;
                        Timer t = new Timer(1800, e -> {
                            if (!a.getCard().isMatched()) { a.getCard().setFaceUp(false); a.animateFlip(false); }
                            if (!b.getCard().isMatched()) { b.getCard().setFaceUp(false); b.animateFlip(false); }
                            inputBlocked = false;
                        });
                        t.setRepeats(false); t.start();
                        return;
                    }
                }
            }
        }
    }

    private void startTimer() {
        gameTimer = new Timer(1000, e -> {
            timeLeft--;
            updateHUD();
            if (timeLeft <= 0) {
                gameTimer.stop();
                timeLeft = 0;
                updateHUD();
                loseGame();
            }
        });
        gameTimer.start();
    }

    private void updateHUD() {
        timerLabel.setText("⏱  " + timeLeft + "s");
        timerLabel.setForeground(timeLeft <= 15 ? Theme.WRONG_GLOW : Theme.APPLE_GOLD);
        scoreLabel.setText("Score: " + score);
        pairsLabel.setText("🃏  " + matchedPairs + " / " + totalPairs);
        hintLabel.setText("💡  " + hintsLeft);
    }

    private void winGame() {
        gameOver = true;
        if (gameTimer != null) gameTimer.stop();
        int bonus = timeLeft * 2;
        score += bonus;
        state.addScore(score);
        state.markLevelCompleted(level);

        // Persist to DB
        int uid = state.getUserId();
        if (db != null && db.isConnected() && uid > 0) {
            int timeTaken = cfg[2] - timeLeft;
            db.saveScore(uid, state.getPlayerName(), score, "Memory L" + level, level, timeTaken, 0);
            db.saveLevelProgress(uid, level, score);
            db.audit(uid, state.getPlayerName(), "LEVEL_COMPLETE",
                     "level=" + level + " score=" + score + " bonus=" + bonus);
        }

        SwingUtilities.invokeLater(() -> showWinDialog(bonus));
    }

    private void loseGame() {
        gameOver = true;
        int uid = state.getUserId();
        if (db != null && db.isConnected() && uid > 0) {
            int timeTaken = cfg[2] - timeLeft;
            db.saveScore(uid, state.getPlayerName(), score, "Memory L" + level, level, timeTaken, 0);
            db.audit(uid, state.getPlayerName(), "LEVEL_FAILED",
                     "level=" + level + " score=" + score);
        }
        SwingUtilities.invokeLater(this::showLoseDialog);
    }

    private void showWinDialog(int bonus) {
        String[] fact = state.getFactForLevel(level);
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dlg.setUndecorated(true);
        dlg.setSize(520, 440);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Theme.paintCardBackground(g, getWidth(), getHeight(), 24);
                // Top accent bar
                GradientPaint bar = new GradientPaint(0,0,Theme.APPLE_RED,getWidth(),0,Theme.APPLE_LIME);
                g.setPaint(bar);
                g.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g.dispose();
            }
        };
        content.setBorder(Theme.emptyBorder(32, 36));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel appleIcon = new JLabel("🍎") { { setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52)); } };
        JLabel winTitle = new JLabel("Level Complete!");
        winTitle.setFont(Theme.fontTitle(30));
        winTitle.setForeground(Theme.APPLE_LIME);

        JLabel scoreInfo = new JLabel("Score: " + score + "  (+"+bonus+" time bonus)");
        scoreInfo.setFont(Theme.fontMono(15));
        scoreInfo.setForeground(Theme.APPLE_GOLD);

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER_SOFT);
        sep.setPreferredSize(new Dimension(400, 1));

        JLabel factTitle = new JLabel("🍏 Math Fun Fact — " + fact[1]);
        factTitle.setFont(Theme.fontBodyBold(14));
        factTitle.setForeground(Theme.APPLE_LIME);

        JTextArea factText = new JTextArea(fact[2]);
        factText.setFont(Theme.fontBody(13));
        factText.setForeground(Theme.TEXT_MUTED);
        factText.setBackground(new Color(0,0,0,0));
        factText.setOpaque(false);
        factText.setEditable(false);
        factText.setLineWrap(true);
        factText.setWrapStyleWord(true);
        factText.setPreferredSize(new Dimension(420, 72));
        factText.setBorder(null);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        JButton nextBtn = Theme.primaryButton(level < GameState.LEVEL_CONFIG.length ? "Next Level →" : "🏆 All Done!");
        JButton homeBtn = Theme.ghostButton("Level Select");
        nextBtn.addActionListener(e -> {
            dlg.dispose();
            if (level < GameState.LEVEL_CONFIG.length) window.startMemoryLevel(level + 1);
            else window.goToLevels();
        });
        homeBtn.addActionListener(e -> { dlg.dispose(); window.goToLevels(); });
        btnRow.add(nextBtn);
        btnRow.add(homeBtn);

        gbc.insets = new Insets(0, 0, 4, 0);
        content.add(appleIcon, gbc);
        gbc.insets = new Insets(4, 0, 8, 0);
        content.add(winTitle, gbc);
        content.add(scoreInfo, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        content.add(sep, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 0, 4, 0);
        content.add(factTitle, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        content.add(factText, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(16, 0, 0, 0);
        content.add(btnRow, gbc);

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    private void showLoseDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dlg.setUndecorated(true);
        dlg.setSize(420, 280);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 20);
                GradientPaint bar = new GradientPaint(0,0,Theme.WRONG_GLOW,getWidth(),0,new Color(0x8B0000));
                g.setPaint(bar);
                g.fillRoundRect(0, 0, getWidth(), 5, 4, 4);
                g.dispose();
            }
        };
        content.setBorder(Theme.emptyBorder(28, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel icon = new JLabel("⏰");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        JLabel title = new JLabel("Time's Up!");
        title.setFont(Theme.fontTitle(26));
        title.setForeground(Theme.WRONG_GLOW);
        JLabel sub = new JLabel("Score: " + score + "  (" + matchedPairs + "/" + totalPairs + " pairs matched)");
        sub.setFont(Theme.fontMono(13));
        sub.setForeground(Theme.TEXT_MUTED);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        JButton retry = Theme.primaryButton("↺ Retry");
        JButton home  = Theme.ghostButton("Level Select");
        retry.addActionListener(e -> { dlg.dispose(); window.startMemoryLevel(level); });
        home.addActionListener(e  -> { dlg.dispose(); window.goToLevels(); });
        btnRow.add(retry); btnRow.add(home);

        content.add(icon, gbc);
        content.add(title, gbc);
        content.add(sub, gbc);
        gbc.insets = new Insets(16, 0, 0, 0);
        content.add(btnRow, gbc);

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D)g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
