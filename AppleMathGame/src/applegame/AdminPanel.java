package applegame;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class AdminPanel extends JPanel {

    private final DataBaseManager db;
    private final MainWindow      window;
    private JPanel                tablePanel;

    public AdminPanel(DataBaseManager db, MainWindow window) {
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
                g.setColor(new Color(0xB71C1C));
                g.fillRect(0, getHeight() - 3, getWidth(), 3);
                g.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(Theme.emptyBorder(12, 24));

        JLabel title = new JLabel("🛡  Admin Panel");
        title.setFont(Theme.fontTitle(24));
        title.setForeground(Theme.APPLE_RED);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton refresh = Theme.ghostButton("🔄 Refresh");
        JButton back    = Theme.ghostButton("⌂ Levels");
        refresh.addActionListener(e -> refreshTable());
        back.addActionListener(e    -> window.goToLevels());
        right.add(refresh); right.add(back);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        // Table
        tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(tablePanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);

        refreshTable();
    }

    private void refreshTable() {
        tablePanel.removeAll();

        // Column headers
        tablePanel.add(headerRow());
        tablePanel.add(divider());

        List<DataBaseManager.UserRecord> users = db.getAllUsers();
        if (users.isEmpty()) {
            JLabel empty = new JLabel("No users found", SwingConstants.CENTER);
            empty.setFont(Theme.fontBody(14));
            empty.setForeground(Theme.TEXT_DIM);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            tablePanel.add(empty);
        } else {
            for (DataBaseManager.UserRecord u : users) {
                tablePanel.add(userRow(u));
                tablePanel.add(divider());
            }
        }

        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private JPanel headerRow() {
        JPanel row = new JPanel(new GridLayout(1, 6));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        String[] cols = {"Username", "Role", "Status", "Verified", "Failed Attempts", "Actions"};
        for (String c : cols) {
            JLabel l = new JLabel(c);
            l.setFont(Theme.fontBodyBold(12));
            l.setForeground(Theme.TEXT_MUTED);
            row.add(l);
        }
        return row;
    }

    private JPanel userRow(DataBaseManager.UserRecord u) {
        JPanel row = new JPanel(new GridLayout(1, 6)) {
            @Override protected void paintComponent(Graphics g0) {
                if (u.isAdmin()) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setColor(new Color(Theme.APPLE_GOLD.getRed(),
                                        Theme.APPLE_GOLD.getGreen(),
                                        Theme.APPLE_GOLD.getBlue(), 15));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.dispose();
                }
                super.paintComponent(g0);
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        // Username
        JLabel nameLbl = new JLabel(u.username);
        nameLbl.setFont(Theme.fontBodyBold(13));
        nameLbl.setForeground(u.isAdmin() ? Theme.APPLE_GOLD : Theme.TEXT_PRIMARY);

        // Role badge
        JLabel roleLbl = new JLabel(u.isAdmin() ? "👑 admin" : "🎮 player");
        roleLbl.setFont(Theme.fontMono(12));
        roleLbl.setForeground(u.isAdmin() ? Theme.APPLE_GOLD : Theme.TEXT_MUTED);

        // Status
        String statusText = !u.isActive ? "🔴 Disabled"
                          : u.isLocked  ? "🟡 Locked"
                          :               "🟢 Active";
        JLabel statusLbl = new JLabel(statusText);
        statusLbl.setFont(Theme.fontBody(12));
        statusLbl.setForeground(Theme.TEXT_PRIMARY);

        // Verified
        JLabel verLbl = new JLabel(u.emailVerified ? "✓ Yes" : "✗ No");
        verLbl.setFont(Theme.fontMono(12));
        verLbl.setForeground(u.emailVerified ? Theme.APPLE_LIME : Theme.WRONG_GLOW);

        // Failed attempts
        JLabel failLbl = new JLabel(u.failedAttempts + " / 5");
        failLbl.setFont(Theme.fontMono(12));
        failLbl.setForeground(u.failedAttempts >= 3 ? Theme.WRONG_GLOW : Theme.TEXT_MUTED);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setOpaque(false);

        if (!u.isAdmin()) {
            int adminId = Session.getUserId();

            JButton toggleBtn = Theme.ghostButton(u.isActive ? "🔒 Disable" : "🔓 Enable");
            toggleBtn.setFont(Theme.fontBody(11));
            toggleBtn.addActionListener(e -> {
                db.setUserActive(adminId, u.id, !u.isActive);
                Timer t = new Timer(200, ev -> refreshTable());
                t.setRepeats(false); t.start();
            });

            JButton resetBtn = Theme.ghostButton("↺ Reset Lock");
            resetBtn.setFont(Theme.fontBody(11));
            resetBtn.addActionListener(e -> {
                db.resetUserLockout(adminId, u.id);
                Timer t = new Timer(200, ev -> refreshTable());
                t.setRepeats(false); t.start();
            });

            actions.add(toggleBtn);
            if (u.failedAttempts > 0 || u.isLocked) actions.add(resetBtn);
        } else {
            JLabel you = new JLabel("(you)");
            you.setFont(Theme.fontMono(11));
            you.setForeground(Theme.TEXT_DIM);
            actions.add(you);
        }

        row.add(nameLbl); row.add(roleLbl); row.add(statusLbl);
        row.add(verLbl); row.add(failLbl); row.add(actions);
        return row;
    }

    private JSeparator divider() {
        JSeparator s = new JSeparator();
        s.setForeground(Theme.BORDER_SOFT);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.paintDeepBackground(g, getWidth(), getHeight());
        // Red accent top strip for admin
        g.setColor(new Color(0xB71C1C, false));
        g.fillRect(0, 0, getWidth(), 3);
        g.dispose();
        super.paintComponent(g0);
    }
}
