package applegame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class LevelSelectScreen extends JPanel {
    private final GameState state;
    private final MainWindow window;

    public LevelSelectScreen(GameState state, MainWindow window) {
        this.state = state;
        this.window = window;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setColor(new Color(0x0A1808));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER_SOFT);
                g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(Theme.emptyBorder(14, 24));

        JLabel title = new JLabel("🍎  Select Level");
        title.setFont(Theme.fontTitle(26));
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        JButton eqBtn = Theme.successButton("🧮 Equation Mode");
        eqBtn.addActionListener(e -> window.startEquationGame());

        JButton accountBtn = Theme.ghostButton("👤 My Account");
        accountBtn.addActionListener(e -> window.goToMyAccount());

        JButton lbBtn = Theme.ghostButton("🏆 Leaderboard");
        lbBtn.addActionListener(e -> window.goToLeaderboard());

        JButton homeBtn = Theme.ghostButton("🔓 Logout");
        homeBtn.addActionListener(e -> window.goToLogin());

        headerRight.add(eqBtn);
        headerRight.add(accountBtn);
        headerRight.add(lbBtn);
        // Only show admin button to admins
        if (Session.isAdmin()) {
            JButton adminBtn = Theme.ghostButton("🛡 Admin");
            adminBtn.setForeground(Theme.APPLE_RED);
            adminBtn.addActionListener(e -> window.goToAdminPanel());
            headerRight.add(adminBtn);
        }
        headerRight.add(homeBtn);

        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // Player info strip
        JPanel infoStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0,0,getWidth(),getHeight());
                g.dispose();
            }
        };
        infoStrip.setOpaque(false);
        infoStrip.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        JLabel playerLbl = new JLabel("Player: " + state.getPlayerName());
        playerLbl.setFont(Theme.fontBodyBold(13));
        playerLbl.setForeground(Theme.APPLE_LIME);

        JLabel scoreLbl = new JLabel("Total Score: " + state.getTotalScore());
        scoreLbl.setFont(Theme.fontMono(13));
        scoreLbl.setForeground(Theme.APPLE_GOLD);

        int done = state.getCompletedLevels().size();
        JLabel progressLbl = new JLabel("Progress: " + done + " / " + GameState.LEVEL_CONFIG.length + " levels");
        progressLbl.setFont(Theme.fontBody(13));
        progressLbl.setForeground(Theme.TEXT_MUTED);

        infoStrip.add(playerLbl);
        infoStrip.add(new JSeparator(SwingConstants.VERTICAL) {{
            setPreferredSize(new Dimension(1, 20));
            setForeground(Theme.BORDER_SOFT);
        }});
        infoStrip.add(scoreLbl);
        infoStrip.add(new JSeparator(SwingConstants.VERTICAL) {{
            setPreferredSize(new Dimension(1, 20));
            setForeground(Theme.BORDER_SOFT);
        }});
        infoStrip.add(progressLbl);

        // Level cards grid
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);

        for (int i = 0; i < GameState.LEVEL_CONFIG.length; i++) {
            gbc.gridx = i; gbc.gridy = 0;
            grid.add(buildLevelCard(i + 1), gbc);
        }

        // Description panel at bottom
        JPanel desc = new JPanel(new FlowLayout(FlowLayout.CENTER)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        desc.setOpaque(false);
        desc.setBorder(Theme.emptyBorder(10, 20));
        JLabel hint = new JLabel("🍏  Complete each level to unlock the next — and discover a Math Fun Fact!");
        hint.setFont(Theme.fontBody(13));
        hint.setForeground(Theme.TEXT_MUTED);
        desc.add(hint);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(infoStrip, BorderLayout.SOUTH);

        add(topSection, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
        add(desc, BorderLayout.SOUTH);
    }

    private JPanel buildLevelCard(int level) {
        int[] cfg = GameState.LEVEL_CONFIG[level - 1];
        boolean completed = state.isLevelCompleted(level);
        boolean locked = level > 1 && !state.isLevelCompleted(level - 1);

        JPanel card = new JPanel() {
            private boolean hovered = false;
            {
                if (!locked) {
                    addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                        public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (!locked) window.startMemoryLevel(level);
                        }
                    });
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D)g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Card bg
                Color bg1 = locked ? new Color(0x0E1208) :
                            completed ? new Color(0x0D2B0D) : new Color(0x122010);
                Color bg2 = locked ? new Color(0x080D06) :
                            completed ? new Color(0x082008) : new Color(0x0A1808);
                GradientPaint gp = new GradientPaint(0,0,bg1, 0,h,bg2);
                g.setPaint(gp);
                g.fill(new RoundRectangle2D.Float(0,0,w,h,18,18));
                // Border
                Color border = locked ? Theme.BORDER_SOFT :
                               completed ? Theme.APPLE_GREEN :
                               hovered ? Theme.BORDER_GLOW : Theme.BORDER_SOFT;
                if (hovered && !locked) {
                    g.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 30));
                    g.fill(new RoundRectangle2D.Float(0,0,w,h,18,18));
                }
                g.setColor(border);
                g.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
                g.draw(new RoundRectangle2D.Float(1,1,w-2,h-2,18,18));
                // Apple decoration
                if (!locked) {
                    Color appleCol = completed ? Theme.APPLE_GREEN : Theme.APPLE_RED;
                    Theme.drawApple(g, w/2 - 10, 44, 22, appleCol, !locked);
                } else {
                    // Lock icon
                    g.setColor(Theme.TEXT_DIM);
                    g.setFont(Theme.fontMono(28));
                    g.drawString("🔒", w/2 - 14, 58);
                }
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(148, 210));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.CENTER;

        // Spacer for apple art
        card.add(Box.createVerticalStrut(76), gbc);

        JLabel numLabel = new JLabel("Level " + level);
        numLabel.setFont(Theme.fontTitle(18));
        numLabel.setForeground(locked ? Theme.TEXT_DIM : Theme.TEXT_PRIMARY);
        card.add(numLabel, gbc);

        JLabel rankLabel = new JLabel(GameState.LEVEL_LABELS[level-1]);
        rankLabel.setFont(Theme.fontMono(11));
        rankLabel.setForeground(locked ? Theme.TEXT_DIM : Theme.APPLE_LIME);
        card.add(rankLabel, gbc);

        // Stats
        JPanel stats = new JPanel(new GridLayout(3, 1, 2, 2));
        stats.setOpaque(false);
        addStat(stats, "⏱ " + cfg[2] + "s", locked);
        addStat(stats, "🃏 " + cfg[1] + " pairs", locked);
        addStat(stats, "💡 " + cfg[3] + " hints", locked);
        gbc.insets = new Insets(6, 4, 4, 4);
        card.add(stats, gbc);

        if (completed) {
            JLabel done = new JLabel("✓ Complete");
            done.setFont(Theme.fontBodyBold(12));
            done.setForeground(Theme.APPLE_GREEN);
            gbc.insets = new Insets(4, 4, 8, 4);
            card.add(done, gbc);
        }

        return card;
    }

    private void addStat(JPanel panel, String text, boolean locked) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.fontBody(12));
        l.setForeground(locked ? Theme.TEXT_DIM : Theme.TEXT_MUTED);
        panel.add(l);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D)g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
