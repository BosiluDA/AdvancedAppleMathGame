package applegame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyAccountScreen extends JPanel {

    private final DataBaseManager db;
    private final MainWindow      window;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    public MyAccountScreen(DataBaseManager db, MainWindow window) {
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
                g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(Theme.emptyBorder(12, 24));

        JLabel title = new JLabel("👤  My Account");
        title.setFont(Theme.fontTitle(24));
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton back = Theme.ghostButton("⌂ Levels");
        back.addActionListener(e -> window.goToLevels());
        right.add(back);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        // Content — two columns
        JPanel content = new JPanel(new GridLayout(1, 2, 16, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        content.add(buildLeftPanel());
        content.add(buildRightPanel());

        add(header,  BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(sectionTitle("📊 Level Statistics"));
        panel.add(Box.createVerticalStrut(8));

        List<DataBaseManager.LevelStat> stats = db.getUserLevelStats(Session.getUserId());
        if (stats.isEmpty()) {
            panel.add(muted("No levels completed yet."));
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 1, 0, 4));
            grid.setOpaque(false);

            // Header row
            JPanel hdr = levelStatRow("Level", "Best Score", "Attempts", "Completed", true);
            grid.add(hdr);

            for (DataBaseManager.LevelStat s : stats) {
                String label = s.level <= GameState.LEVEL_LABELS.length
                    ? GameState.LEVEL_LABELS[s.level - 1] : "Level " + s.level;
                grid.add(levelStatRow(
                    "L" + s.level + " – " + label,
                    String.valueOf(s.bestScore),
                    s.attempts + "x",
                    s.completedAt.format(FMT),
                    false));
            }
            panel.add(grid);
        }

        panel.add(Box.createVerticalStrut(24));
        panel.add(sectionTitle("🎮 Recent Games"));
        panel.add(Box.createVerticalStrut(8));

        List<DataBaseManager.ScoreEntry> scores = db.getUserScores(Session.getUserId());
        if (scores.isEmpty()) {
            panel.add(muted("No games played yet."));
        } else {
            JPanel grid2 = new JPanel(new GridLayout(0, 1, 0, 4));
            grid2.setOpaque(false);
            grid2.add(levelStatRow("Mode", "Score", "Level", "Date", true));
            for (DataBaseManager.ScoreEntry e : scores) {
                grid2.add(levelStatRow(e.mode, String.valueOf(e.score),
                    e.level > 0 ? "L" + e.level : "—",
                    e.playedAt.format(FMT), false));
            }
            panel.add(grid2);
        }

        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(sectionTitle("🔐 Login History"));
        panel.add(Box.createVerticalStrut(8));

        List<DataBaseManager.AuditEntry> history = db.getUserLoginHistory(Session.getUserId());
        if (history.isEmpty()) {
            panel.add(muted("No login history yet."));
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 1, 0, 4));
            grid.setOpaque(false);
            grid.add(levelStatRow("Event", "Detail", "Time", "", true));

            for (DataBaseManager.AuditEntry entry : history) {
                String icon = switch (entry.action) {
                    case "LOGIN_OK"       -> "✅";
                    case "LOGIN_FAILED"   -> "❌";
                    case "ACCOUNT_LOCKED" -> "🔒";
                    case "LOGIN_DENIED"   -> "⛔";
                    default               -> "•";
                };
                String detail = entry.detail != null ? entry.detail : "";
                if (detail.length() > 30) detail = detail.substring(0, 28) + "…";
                grid.add(levelStatRow(
                    icon + " " + entry.action.replace("_", " "),
                    detail,
                    entry.time.format(FMT),
                    "", false));
            }
            panel.add(grid);
        }

        // Account info card
        panel.add(Box.createVerticalStrut(24));
        panel.add(sectionTitle("ℹ Account Info"));
        panel.add(Box.createVerticalStrut(8));

        DataBaseManager.UserRecord u = Session.getUser();
        if (u != null) {
            panel.add(infoRow("Username", u.username));
            panel.add(infoRow("Role", u.isAdmin() ? "👑 Admin" : "🎮 Player"));
            if (u.lastLogin != null)
                panel.add(infoRow("Last Login", u.lastLogin.format(FMT)));
            if (u.createdAt != null)
                panel.add(infoRow("Member Since", u.createdAt.format(FMT)));
        }

        return panel;
    }

    private JPanel levelStatRow(String c1, String c2, String c3, String c4, boolean header) {
        JPanel row = new JPanel(new GridLayout(1, 4, 4, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        Font font  = header ? Theme.fontBodyBold(12) : Theme.fontBody(12);
        Color col  = header ? Theme.TEXT_MUTED : Theme.TEXT_PRIMARY;
        row.add(cell(c1, font, col));
        row.add(cell(c2, header ? font : Theme.fontMono(12), header ? col : Theme.APPLE_GOLD));
        row.add(cell(c3, font, header ? col : Theme.TEXT_MUTED));
        row.add(cell(c4, font, header ? col : Theme.TEXT_DIM));

        if (!header) {
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        }
        return row;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        JLabel l = new JLabel(label);
        l.setFont(Theme.fontBodyBold(12));
        l.setForeground(Theme.TEXT_MUTED);
        l.setPreferredSize(new Dimension(110, 20));
        JLabel v = new JLabel(value);
        v.setFont(Theme.fontBody(13));
        v.setForeground(Theme.TEXT_PRIMARY);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        return row;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBodyBold(15));
        l.setForeground(Theme.APPLE_LIME);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    private JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.fontBody(13));
        l.setForeground(Theme.TEXT_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel cell(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font); l.setForeground(color);
        return l;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
