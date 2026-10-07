package pub;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/** Theme and reusable widgets with a warm, slightly worn pub look. */
public final class UI {
    private UI() {}
    public static final Color WOOD = new Color(0x2a1a12), WOOD2 = new Color(0x3b2417), WOOD3 = new Color(0x4d3020), BRASS = new Color(0xc9973f), BRASS2 = new Color(0xe8c06a),
        CREAM = new Color(0xf2e6c8), CHALK = new Color(0x25302c), RED = new Color(0x9b1c1c), PAPER = new Color(0xf3ead2), INK = new Color(0x2b2118), GREEN = new Color(0x4c9a54);
    public static Font serif(float s, boolean bold) { return new Font("Georgia", bold ? Font.BOLD : Font.PLAIN, (int) s); }
    public static Font sans(float s, boolean bold) { return new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, (int) s); }

    public static void AA(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static class BrassButton extends JButton {
        boolean small; Color base = BRASS;
        public BrassButton(String t) { super(t); setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false); setOpaque(false); setForeground(INK); setFont(sans(13, true)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setMargin(new Insets(5, 12, 5, 12)); setFocusable(false); }
        public BrassButton tint(Color c) { base = c; return this; }
        public BrassButton small() { small = true; setFont(sans(11, true)); setMargin(new Insets(2, 8, 2, 8)); return this; }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g);
            boolean en = isEnabled(), hov = getModel().isRollover(), prs = getModel().isPressed();
            Color a = en ? (hov ? base.brighter() : base) : new Color(0x6a5e4a), b = en ? a.darker() : new Color(0x4a4030);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(0, 0, 0, 90)); g.fillRoundRect(1, 2, w - 2, h - 2, 10, 10);
            g.setPaint(new GradientPaint(0, 0, a, 0, h, b)); g.fillRoundRect(0, prs ? 2 : 0, w - 1, h - 3, 10, 10);
            g.setColor(new Color(255, 255, 255, 70)); g.drawRoundRect(1, prs ? 3 : 1, w - 3, h - 5, 9, 9);
            g.setColor(en ? INK : new Color(0xa09680)); g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics(); String t = getText();
            g.drawString(t, (w - fm.stringWidth(t)) / 2, (h - 3 + fm.getAscent() - fm.getDescent()) / 2 + (prs ? 2 : 0));
            g.dispose();
        }
        @Override public Dimension getPreferredSize() { FontMetrics fm = getFontMetrics(getFont()); return new Dimension(fm.stringWidth(getText()) + getMargin().left + getMargin().right + 4, fm.getHeight() + getMargin().top + getMargin().bottom); }
    }
    public static BrassButton btn(String t, Runnable r) { BrassButton b = new BrassButton(t); b.addActionListener(e -> r.run()); return b; }
    public static BrassButton sbtn(String t, Runnable r) { BrassButton b = btn(t, r).small(); return b; }

    public static JLabel label(String t, float size, Color c) { JLabel l = new JLabel(t); l.setFont(sans(size, false)); l.setForeground(c); return l; }
    public static JLabel title(String t, float size, Color c) { JLabel l = new JLabel(t); l.setFont(serif(size, true)); l.setForeground(c); return l; }

    public static class WoodPanel extends JPanel {
        public WoodPanel(LayoutManager lm) { super(lm); setOpaque(true); }
        public WoodPanel() { this(new BorderLayout()); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(new GradientPaint(0, 0, WOOD2, 0, getHeight(), WOOD)); g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 8)); for (int y = 0; y < getHeight(); y += 7) g.drawLine(0, y, getWidth(), y + 2);
            g.dispose();
        }
    }
    public static class PaperPanel extends JPanel {
        public PaperPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g);
            g.setColor(new Color(0, 0, 0, 120)); g.fillRoundRect(4, 5, getWidth() - 6, getHeight() - 6, 18, 18);
            g.setPaint(new GradientPaint(0, 0, PAPER, 0, getHeight(), new Color(0xe6d8b4))); g.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 18, 18);
            g.setColor(new Color(0x8a6a3a)); g.setStroke(new BasicStroke(2.5f)); g.drawRoundRect(2, 2, getWidth() - 10, getHeight() - 10, 16, 16);
            g.dispose();
        }
    }
    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c); s.setBorder(BorderFactory.createEmptyBorder()); s.getViewport().setOpaque(false); s.setOpaque(false); s.getVerticalScrollBar().setUnitIncrement(16); return s;
    }
    public static JPanel vbox() { JPanel p = new JPanel() { @Override public Component add(Component c) { if (c instanceof JComponent j) j.setAlignmentX(LEFT_ALIGNMENT); return super.add(c); } }; p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false); return p; }
    public static JPanel row() { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3)); p.setOpaque(false); return p; }

    public static class Bar extends JComponent {
        double v = 0; Color col; String text;
        public Bar(Color c) { col = c; setPreferredSize(new Dimension(100, 12)); }
        public void set(double val, String t) { v = Util.clamp(val, 0, 100); text = t; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g); int w = getWidth(), h = getHeight();
            g.setColor(new Color(0, 0, 0, 120)); g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(col); g.fillRoundRect(0, 0, (int) (w * v / 100), h, h, h);
            g.setColor(new Color(255, 255, 255, 60)); g.fillRoundRect(0, 0, (int) (w * v / 100), h / 2, h, h);
            if (text != null) { g.setFont(sans(9, true)); g.setColor(Color.WHITE); g.drawString(text, 4, h - 2); }
            g.dispose();
        }
    }

    public static JTextPane htmlPane(String html, int w) {
        JTextPane p = new JTextPane(); p.setContentType("text/html"); p.setEditable(false); p.setOpaque(false); p.setText(html); p.setSize(w, 10); return p;
    }
    public static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    public static String moodFace(double m) { return m > 75 ? "☺" : m > 45 ? "•" : "☹"; }
    public static String moodWord(Model.Npc n) {
        if (n.anger > 55) return "Furious"; if (n.embar > 45) return "Mortified"; if (n.drunk > 70) return "Absolutely hammered"; if (n.drunk > 40) return "Merry"; if (n.mood > 80) return "Elated"; if (n.mood > 62) return "Happy"; if (n.mood > 42) return "Fine"; if (n.mood > 25) return "Grumpy"; return "Miserable";
    }
    public static String statusLine(Model.Npc n) {
        if (!n.inPub) return "Out and about: " + niceLoc(n.loc);
        if (n.doing != null && !n.doing.isEmpty()) return n.doing;
        return switch (n.st) { case "sit" -> "Sitting with a drink"; case "stand" -> "Standing around"; case "tv" -> "Watching the telly"; case "waitBar" -> "Waiting at the bar"; case "leave" -> "Heading home"; case "darts" -> "Playing darts"; case "pool" -> "Playing pool"; case "walk" -> "Wandering"; default -> "Here"; };
    }
    public static String niceLoc(String l) {
        return switch (l) { case "pub" -> "your pub"; case "frog" -> "the Frog & Trumpet"; case "oak" -> "the Royal Oak"; case "houses" -> "at home"; case "work" -> "at work"; case "super" -> "Savemore"; case "stadium" -> "the stadium"; case "station" -> "the station"; case "park" -> "Victoria Park";
            case "takeaway" -> "Kebabylon"; case "cafe" -> "Brenda's Caff"; case "police" -> "the police station"; case "church" -> "church"; case "hospital" -> "the hospital"; case "uni" -> "uni"; case "council" -> "the council offices"; case "shop" -> "the high street"; default -> l; };
    }
}
