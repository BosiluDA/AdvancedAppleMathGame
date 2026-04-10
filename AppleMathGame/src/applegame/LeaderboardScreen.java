package applegame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class LeaderboardScreen extends JPanel {

    private final GameState        state;
    private final DataBaseManager  db;
    private final MainWindow       window;
    private boolean showingTimes   = false;

    private JPanel tablePanel;

    public LeaderboardScreen(GameState state, DataBaseManager db, MainWindow window) {
        this.state  = state;
        this.db     = db;
        this.window = window;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER_SOFT);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(Theme.emptyBorder(12, 24));

        JLabel title = new JLabel("Leaderboard");
        title.setFont(Theme.fontTitle(26));
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        JButton levBtn  = Theme.ghostButton("🎮 Levels");
        JButton logBtn  = Theme.ghostButton("🔓 Logout");
        levBtn.addActionListener(e -> window.goToLevels());
        logBtn.addActionListener(e -> window.goToLogin());
        btnRow.add(levBtn);
        btnRow.add(logBtn);

        header.add(title,  BorderLayout.WEST);
        header.add(btnRow, BorderLayout.EAST);

        // Toggle tabs
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
        tabs.setOpaque(false);

        JButton scoresTab = tabButton("Highest Scores", true);
        JButton timesTab  = tabButton("⏱  Fastest Times",  false);

        scoresTab.addActionListener(e -> { showingTimes = false; refreshTable(); });
        timesTab.addActionListener(e  -> { showingTimes = true;  refreshTable(); });

        tabs.add(scoresTab);
        tabs.add(timesTab);

        // Table area
        tablePanel = new JPanel();
        tablePanel.setOpaque(false);
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));

        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.paintCardBackground(g, getWidth(), getHeight(), 16);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tableCard.add(new JScrollPane(tablePanel) {{
            setOpaque(false);
            getViewport().setOpaque(false);
            setBorder(null);
        }}, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 32, 16, 32));
        center.add(tabs,      BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);

        // Refresh button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(new Color(0x061005));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        footer.setOpaque(false);
        JButton refresh = Theme.ghostButton("🔄 Refresh");
        refresh.addActionListener(e -> refreshTable());
        JLabel note = new JLabel("  Scores are saved to MySQL");
        note.setFont(Theme.fontMono(11));
        note.setForeground(Theme.TEXT_DIM);
        footer.add(refresh);
        footer.add(note);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);

        add(topSection, BorderLayout.NORTH);
        add(center,     BorderLayout.CENTER);
        add(footer,     BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        tablePanel.removeAll();

        List<DataBaseManager.ScoreEntry> entries;
        if (showingTimes) {
            entries = db.isConnected() ? db.getTopTimes() : List.of();
            tablePanel.add(buildHeaderRow("Rank", "Player", "Level", "Time (s)"));
        } else {
            entries = db.isConnected() ? db.getTopScores() : List.of();
            tablePanel.add(buildHeaderRow("Rank", "Player", "Mode", "Score"));
        }

        tablePanel.add(makeDivider());

        if (entries.isEmpty()) {
            JLabel empty = new JLabel(
                db.isConnected() ? "No scores yet — play a game!" : "Database offline",
                SwingConstants.CENTER);
            empty.setFont(Theme.fontBody(14));
            empty.setForeground(Theme.TEXT_DIM);
            empty.setBorder(BorderFactory.createEmptyBorder(28, 0, 28, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            tablePanel.add(empty);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                DataBaseManager.ScoreEntry e = entries.get(i);
                String rank = i == 0 ? "🥇 1" : i == 1 ? "🥈 2" : i == 2 ? "🥉 3" : "    " + (i + 1);
                boolean mine = e.username.equals(state.getPlayerName());
                String col4  = showingTimes ? String.valueOf(e.timeTaken) : String.valueOf(e.score);
                String col3  = showingTimes ? "L" + e.level : e.mode;
                tablePanel.add(buildDataRow(rank, e.username, col3, col4, mine));
                if (i < entries.size() - 1) tablePanel.add(makeDivider());
            }
        }

        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private JPanel buildHeaderRow(String c1, String c2, String c3, String c4) {
        return buildRow(c1, c2, c3, c4, Theme.TEXT_MUTED, Theme.fontBodyBold(13), false);
    }

    private JPanel buildDataRow(String c1, String c2, String c3, String c4, boolean mine) {
        return buildRow(c1, c2, c3, c4,
            mine ? Theme.APPLE_LIME : Theme.TEXT_PRIMARY,
            Theme.fontBody(14), mine);
    }

    private JPanel buildRow(String c1, String c2, String c3, String c4,
                            Color col, Font font, boolean highlight) {
        JPanel row = new JPanel(new GridLayout(1, 4)) {
            @Override protected void paintComponent(Graphics g0) {
                if (highlight) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setColor(new Color(
                        Theme.APPLE_GREEN.getRed(),
                        Theme.APPLE_GREEN.getGreen(),
                        Theme.APPLE_GREEN.getBlue(), 18));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.dispose();
                }
                super.paintComponent(g0);
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        row.add(cell(c1, font, col, SwingConstants.LEFT));
        row.add(cell(c2, font, highlight ? Theme.APPLE_LIME : Theme.TEXT_PRIMARY, SwingConstants.LEFT));
        row.add(cell(c3, Theme.fontBody(13), Theme.TEXT_MUTED, SwingConstants.CENTER));
        row.add(cell(c4, Theme.fontMono(14), Theme.APPLE_GOLD, SwingConstants.RIGHT));
        return row;
    }

    private JLabel cell(String text, Font font, Color color, int align) {
        JLabel l = new JLabel(text, align);
        l.setFont(font);
        l.setForeground(color);
        l.setOpaque(false);
        return l;
    }

    private JSeparator makeDivider() {
        JSeparator s = new JSeparator();
        s.setForeground(Theme.BORDER_SOFT);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private JButton tabButton(String text, boolean active) {
        JButton b = active ? Theme.ghostButton(text) : Theme.ghostButton(text);
        b.setPreferredSize(new Dimension(180, 36));
        return b;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
