package applegame;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.*;

public class CardPanel extends JPanel {
    private final MemoryCard card;
    private Timer flipTimer;
    private boolean flipping = false;
    private boolean targetFaceUp = false;
    private float angle = 0f;  // 0.0 = back, 1.0 = front

    public CardPanel(MemoryCard card) {
        this.card = card;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public MemoryCard getCard() { return card; }

    public void animateFlip(boolean faceUp) {
        if (flipping) return;
        this.targetFaceUp = faceUp;
        flipping = true;
        flipTimer = new Timer(16, null);
        flipTimer.addActionListener(e -> {
            if (targetFaceUp) {
                angle += 0.1f;
                if (angle >= 1f) { angle = 1f; flipping = false; flipTimer.stop(); }
            } else {
                angle -= 0.1f;
                if (angle <= 0f) { angle = 0f; flipping = false; flipTimer.stop(); }
            }
            repaint();
        });
        flipTimer.start();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth(), h = getHeight();

        // Determine horizontal scale for 3D flip effect
        float scaleX = Math.abs((float)Math.cos(Math.PI * angle));
        int drawW = Math.max(1, (int)(w * scaleX));
        int offsetX = (w - drawW) / 2;

        if (card.isMatched()) {
            drawMatched(g, offsetX, 0, drawW, h);
        } else if (angle < 0.5f) {
            drawBack(g, offsetX, 0, drawW, h);
        } else {
            drawFront(g, offsetX, 0, drawW, h);
        }
        g.dispose();
    }

    private void drawBack(Graphics2D g, int x, int y, int w, int h) {
        // Dark card back with apple pattern
        GradientPaint gp = new GradientPaint(x, y, new Color(0x1A3020), x, y+h, new Color(0x0E1E12));
        g.setPaint(gp);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));

        // Border
        g.setColor(card.isGlowWrong() ? Theme.WRONG_GLOW : Theme.BORDER_GLOW);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x+0.5f, y+0.5f, w-1, h-1, 12, 12));

        // Small apple icon on back
        int cx = x + w/2 - 8;
        int cy = y + h/2;
        Theme.drawApple(g, cx, cy, (int)(Math.min(w,h)*0.22), new Color(Theme.APPLE_RED.getRed(), Theme.APPLE_RED.getGreen(), Theme.APPLE_RED.getBlue(), 100), false);
    }

    private void drawFront(Graphics2D g, int x, int y, int w, int h) {
        int[] cols = card.getColors();
        Color c1 = new Color(cols[0]);
        Color c2 = new Color(cols[1]);

        GradientPaint gp = new GradientPaint(x, y, c1, x, y+h, c2);
        g.setPaint(gp);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));

        // Highlight shimmer
        g.setColor(new Color(255,255,255,25));
        g.fill(new RoundRectangle2D.Float(x+2, y+2, w-4, h/2-2, 10, 10));

        // Match/wrong glow border
        if (card.isGlowMatch()) {
            g.setColor(Theme.MATCH_GLOW);
            g.setStroke(new BasicStroke(3f));
        } else if (card.isGlowWrong()) {
            g.setColor(Theme.WRONG_GLOW);
            g.setStroke(new BasicStroke(2f));
        } else {
            g.setColor(new Color(255,255,255,60));
            g.setStroke(new BasicStroke(1.5f));
        }
        g.draw(new RoundRectangle2D.Float(x+0.5f, y+0.5f, w-1, h-1, 12, 12));

        // Symbol text
        String sym = card.getSymbol();
        Font font = sym.length() <= 3 ? Theme.fontMono(18) : Theme.fontMono(13);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        int tx = x + (w - fm.stringWidth(sym)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(sym, tx, ty);
    }

    private void drawMatched(Graphics2D g, int x, int y, int w, int h) {
        // Faded matched state
        g.setColor(new Color(0x0A2010));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        g.setColor(new Color(Theme.APPLE_GREEN.getRed(), Theme.APPLE_GREEN.getGreen(), Theme.APPLE_GREEN.getBlue(), 100));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x+0.5f, y+0.5f, w-1, h-1, 12, 12));
        // Checkmark
        g.setFont(Theme.fontMono(20));
        g.setColor(new Color(Theme.APPLE_GREEN.getRed(), Theme.APPLE_GREEN.getGreen(), Theme.APPLE_GREEN.getBlue(), 160));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("✓", x + (w - fm.stringWidth("✓")) / 2, y + (h + fm.getAscent()) / 2 - fm.getDescent());
    }
}
