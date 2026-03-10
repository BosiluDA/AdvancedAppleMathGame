package applegame;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.Border;

public class Theme {
    // ── Palette ──────────────────────────────────────────────────────────────
    public static final Color BG_DEEP      = new Color(0x0D1F0A);
    public static final Color BG_CARD      = new Color(0x122010);
    public static final Color BG_PANEL     = new Color(0x0A1808);
    public static final Color APPLE_RED    = new Color(0xE8302A);
    public static final Color APPLE_GREEN  = new Color(0x4CAF50);
    public static final Color APPLE_LIME   = new Color(0x8BC34A);
    public static final Color APPLE_GOLD   = new Color(0xF9A825);
    public static final Color APPLE_CREAM  = new Color(0xFFF8F0);
    public static final Color TEXT_PRIMARY = new Color(0xF0FFF0);
    public static final Color TEXT_MUTED   = new Color(0x7A9E7A);
    public static final Color TEXT_DIM     = new Color(0x3D6B3D);
    public static final Color BORDER_SOFT  = new Color(0x1E3A1E);
    public static final Color BORDER_GLOW  = new Color(0x3A7A3A);
    public static final Color MATCH_GLOW   = new Color(0x00FF88);
    public static final Color WRONG_GLOW   = new Color(0xFF4444);
    public static final Color HINT_COLOR   = new Color(0xFFD700);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static Font fontTitle(float size) {
        return new Font("Georgia", Font.BOLD, (int)size);
    }
    public static Font fontMono(float size) {
        return new Font("Monospaced", Font.BOLD, (int)size);
    }
    public static Font fontBody(float size) {
        return new Font("SansSerif", Font.PLAIN, (int)size);
    }
    public static Font fontBodyBold(float size) {
        return new Font("SansSerif", Font.BOLD, (int)size);
    }

    // ── Gradient helpers ──────────────────────────────────────────────────────
    public static void paintDeepBackground(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        // Base gradient
        GradientPaint base = new GradientPaint(0, 0, BG_DEEP, 0, h, new Color(0x06120A));
        g.setPaint(base);
        g.fillRect(0, 0, w, h);
        // Subtle radial glow top-left
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point2D.Float(w * 0.15f, h * 0.1f),
            w * 0.5f,
            new float[]{0f, 1f},
            new Color[]{new Color(0x1A3D1A, true), new Color(0x00000000, true)}
        );
        g.setPaint(glow);
        g.fillRect(0, 0, w, h);
    }

    public static void paintCardBackground(Graphics2D g, int w, int h, float arc) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint grad = new GradientPaint(0, 0, new Color(0x1A3020), 0, h, new Color(0x0E1E12));
        g.setPaint(grad);
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
        g.setColor(BORDER_SOFT);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w-1, h-1, arc, arc));
    }

    // ── Buttons ───────────────────────────────────────────────────────────────
    public static JButton primaryButton(String text) {
        return styledButton(text,
            new Color(0xC0392B), new Color(0xE74C3C), Color.WHITE);
    }
    public static JButton successButton(String text) {
        return styledButton(text,
            new Color(0x2E7D32), new Color(0x43A047), Color.WHITE);
    }
    public static JButton ghostButton(String text) {
        return styledButton(text,
            new Color(0x1A3020), new Color(0x1E3A25), TEXT_PRIMARY);
    }

    private static JButton styledButton(String text, Color bg, Color hover, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = hovered ? hover : bg;
                GradientPaint gp = new GradientPaint(0, 0, c.brighter(), 0, getHeight(), c);
                g.setPaint(gp);
                g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),16,16));
                if (hovered) {
                    g.setColor(new Color(255,255,255,30));
                    g.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),16,16));
                }
                g.setColor(fg);
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(getText(), tx, ty);
                g.dispose();
            }
        };
        btn.setFont(fontBodyBold(14));
        btn.setForeground(fg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 32, 42));
        return btn;
    }

    // ── Apple shape renderer ──────────────────────────────────────────────────
    public static void drawApple(Graphics2D g, int cx, int cy, int r, Color appleColor, boolean drawLeaf) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Body — two overlapping circles
        g.setColor(appleColor);
        g.fillOval(cx - r, cy - (int)(r * 0.8), r, (int)(r * 1.6));
        g.fillOval(cx, cy - (int)(r * 0.8), r, (int)(r * 1.6));
        // Highlight
        g.setColor(new Color(255,255,255,60));
        g.fillOval(cx - (int)(r*0.5), cy - (int)(r*0.5), (int)(r*0.5), (int)(r*0.6));
        // Stem
        g.setColor(new Color(0x5D4037));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx + r/2, cy - (int)(r*0.8), cx + r/2 + 3, cy - (int)(r*0.8) - r/2);
        // Leaf
        if (drawLeaf) {
            g.setColor(APPLE_LIME);
            Path2D leaf = new Path2D.Float();
            int lx = cx + r/2 + 2;
            int ly = cy - (int)(r * 0.8) - r/3;
            leaf.moveTo(lx, ly);
            leaf.curveTo(lx+r/2, ly-r/3, lx+r/3, ly+r/4, lx, ly);
            g.fill(leaf);
        }
    }

    // ── Utility border ─────────────────────────────────────────────────────────
    public static Border emptyBorder(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }
}
