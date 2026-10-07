package pub;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import pub.Model.*;
import static pub.Sim.S;

/** All Java2D drawing for the pub scene and town map. Logical canvas is 960x620. */
public final class Gfx {
    private Gfx() {}
    public static final int W = Nav.W, H = Nav.H;
    public static String selId = "", hoverId = "", hoverHot = "";
    public static boolean showNames = true;
    static double t; // animation time (seconds)

    public record Hot(String id, String label, int x, int y, int w, int h) { boolean in(double px, double py) { return px >= x && py >= y && px <= x + w && py <= y + h; } }
    public static final List<Hot> HOTS = List.of(
        new Hot("tv", "TV", 516, 24, 188, 84), new Hot("notice", "Notice board", 725, 26, 60, 72), new Hot("darts", "Dartboard", 12, 388, 46, 64),
        new Hot("pool", "Pool table", 694, 394, 212, 122), new Hot("juke", "Jukebox", 900, 284, 52, 80), new Hot("bar", "The bar", 36, 168, 298, 40),
        new Hot("kitchen", "Kitchen", 795, 20, 140, 100), new Hot("loos", "Toilets", 430, 80, 60, 46), new Hot("door", "Front door", 440, 590, 80, 30),
        new Hot("fruit", "Fruit machine", 900, 194, 52, 70), new Hot("fire", "Fireplace", 12, 488, 56, 106), new Hot("stage", "Karaoke stage", 520, 395, 90, 50));

    static final Font F9 = new Font("SansSerif", Font.PLAIN, 9), F10 = new Font("SansSerif", Font.PLAIN, 10), F11 = new Font("SansSerif", Font.PLAIN, 11),
        F11B = new Font("SansSerif", Font.BOLD, 11), F12B = new Font("SansSerif", Font.BOLD, 12), SERIF_B = new Font("Serif", Font.BOLD, 18), F20B = new Font("SansSerif", Font.BOLD, 20);

    static Color c(int rgb) { return new Color(rgb); }
    static Color c(int rgb, int a) { return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a); }
    static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (gg << 8) | bb;
    }

    // ================= static background =================
    static BufferedImage bg, vignette; static String bgKey = "";
    static String key() { return S.upgrades.hashCode() + "|" + S.decor.hashCode() + "|" + S.flags.getOrDefault("wall", "0") + "|" + Sim.season(); }

    static void ensureBg() {
        String k = key();
        if (bg != null && k.equals(bgKey)) return;
        bgKey = k; bg = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bg.createGraphics(); AA(g); drawBg(g); g.dispose();
        if (vignette == null) {
            vignette = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D v = vignette.createGraphics();
            v.setPaint(new RadialGradientPaint(W / 2f, H / 2f + 20, 560, new float[]{.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(10, 4, 0, 150)}));
            v.fillRect(0, 0, W, H); v.dispose();
        }
    }
    static void AA(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static void rr(Graphics2D g, double x, double y, double w, double h, double r, int col) { g.setColor(c(col)); g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r)); }
    static void rrs(Graphics2D g, double x, double y, double w, double h, double r, int col, int stroke) { rr(g, x, y, w, h, r, col); g.setColor(c(stroke)); g.setStroke(new BasicStroke(1.3f)); g.draw(new RoundRectangle2D.Double(x, y, w, h, r, r)); }
    static void str(Graphics2D g, String s, double x, double y, Font f, int col) { g.setFont(f); g.setColor(c(col)); g.drawString(s, (float) x, (float) y); }
    static void strC(Graphics2D g, String s, double cx, double y, Font f, int col) { g.setFont(f); int w = g.getFontMetrics().stringWidth(s); g.setColor(c(col)); g.drawString(s, (float) (cx - w / 2.0), (float) y); }

    static void drawBg(Graphics2D g) {
        // floor planks
        for (int y = 124, row = 0; y < H; y += 20, row++) for (int x = -(row % 3) * 27; x < W; x += 80) {
            int base = 0x56391f; int sh = ((x * 31 + y * 17) >> 3) % 7 - 3; g.setColor(c(mix(base, 0x70482a, ((sh + 3) / 6.0)))); g.fillRect(x, y, 79, 19);
            g.setColor(c(0x2e1c0f, 120)); g.drawRect(x, y, 79, 19);
        }
        // carpet under tables
        g.setColor(c(0x6e2530, 210)); g.fill(new RoundRectangle2D.Double(250, 255, 410, 270, 24, 24));
        g.setColor(c(0xb08d46, 120)); g.setStroke(new BasicStroke(2f)); g.draw(new RoundRectangle2D.Double(258, 263, 394, 254, 18, 18));
        for (int x = 270; x < 640; x += 22) for (int y = 272; y < 508; y += 22) { g.setColor(c(0x8a3340, 120)); g.fillOval(x, y, 4, 4); }
        // back wall
        int wc = Mgmt.WALLCOL[Integer.parseInt(S.flags.getOrDefault("wall", "0"))];
        g.setColor(c(wc)); g.fillRect(0, 0, W, 124);
        for (int x = 0; x < W; x += 24) { g.setColor(c(mix(wc, 0xffffff, .06))); g.fillRect(x, 0, 10, 92); }
        rr(g, 0, 90, W, 34, 0, 0x3b2314); g.setColor(c(0x7a5230)); g.fillRect(0, 88, W, 5); g.setColor(c(0x2a170b)); g.fillRect(0, 120, W, 5);
        for (int x = 8; x < W; x += 60) { g.setColor(c(0x4a2d19)); g.drawRect(x, 98, 50, 18); }
        // left & right walls
        g.setColor(c(wc)); g.fillRect(0, 125, 22, H); g.fillRect(940, 125, 22, H);
        g.setColor(c(0x3b2314)); g.fillRect(0, 125, 22, H); g.fillRect(940, 125, 22, H);
        // back bar shelves
        rrs(g, 22, 18, 316, 72, 6, 0x24150b, 0x7a5230);
        for (int i = 0; i < 3; i++) { g.setColor(c(0x7a5230)); g.fillRect(28, 38 + i * 22, 304, 4); }
        int[] bc = {0x2e7d32, 0x8d6e63, 0xc62828, 0x1565c0, 0xf9a825, 0x6a1b9a, 0x00897b, 0xd84315};
        for (int r = 0; r < 3; r++) for (int i = 0; i < 18; i++) { int col = bc[(i * 3 + r * 5) % bc.length]; rr(g, 34 + i * 16.5, 24 + r * 22 - (r == 2 ? 0 : 0), 7, 15, 3, col); g.setColor(c(0xffffff, 90)); g.fillRect((int) (36 + i * 16.5), 27 + r * 22, 1, 8); }
        // optics
        for (int i = 0; i < 4; i++) { rr(g, 60 + i * 70, 78, 9, 14, 3, 0xcfd8dc); g.setColor(c(0xb08d46)); g.fillRect(62 + i * 70, 90, 5, 3); }
        // clock
        g.setColor(c(0xf3e5ab)); g.fillOval(300, 8, 22, 22); g.setColor(c(0x333333)); g.drawOval(300, 8, 22, 22); g.drawLine(311, 19, 311, 12); g.drawLine(311, 19, 316, 21);
        // window A
        window(g, 350, 28, 62, 66);
        // loos door
        rrs(g, 432, 40, 56, 84, 3, 0x5a3a22, 0x2a170b); g.setColor(c(0xb08d46)); g.fillOval(476, 88, 5, 5);
        rrs(g, 440, 20, 40, 14, 4, 0x101010, 0xb08d46); strC(g, "LOOS", 460, 31, F10, 0xf3e5ab);
        // notice board
        rrs(g, 726, 28, 58, 66, 3, 0xa0733a, 0x5a3a22);
        int[] nc = {0xfff9c4, 0xc8e6c9, 0xffccbc, 0xb3e5fc};
        for (int i = 0; i < 5; i++) { g.setColor(c(nc[i % 4])); g.fillRect(732 + (i % 2) * 26, 34 + (i / 2) * 20, 22, 16); g.setColor(c(0x888888)); g.drawLine(734 + (i % 2) * 26, 40 + (i / 2) * 20, 750 + (i % 2) * 26, 40 + (i / 2) * 20); }
        // kitchen hatch
        rrs(g, 800, 16, 134, 104, 4, 0x3a2a20, 0x1c110a); rr(g, 812, 40, 110, 56, 4, 0x15100c);
        g.setColor(c(0xcfd8dc)); g.fillRect(812, 96, 110, 6); rrs(g, 830, 22, 74, 14, 4, 0x101010, 0xb08d46); strC(g, "KITCHEN", 867, 33, F10, 0xf3e5ab);
        if (S.upgrades.contains("kitchen2")) { rr(g, 840, 54, 30, 26, 3, 0x90a4ae); rr(g, 876, 54, 30, 26, 3, 0x78909c); }
        // TV bracket (dynamic screen)
        g.setColor(c(0x222222)); g.fillRect(606, 4, 8, 20);
        // darts board (static)
        double[] db = Nav.DARTBOARD; dartboard(g, db[0], db[1], 26);
        g.setColor(c(0xffffff, 160)); g.fillRect((int) Nav.OCHE[0] + 16, 398, 3, 46);
        // left wall window
        window(g, 2, 250, 16, 80);
        // jukebox
        rrs(g, 904, 288, 46, 74, 16, 0x7a1f2c, 0xb08d46); rr(g, 912, 296, 30, 24, 10, 0xf6d776);
        for (int i = 0; i < 3; i++) { g.setColor(c(0xb08d46)); g.fillRect(914, 328 + i * 8, 26, 3); }
        // fruit machine
        if (S.upgrades.contains("fruit")) { rrs(g, 906, 198, 42, 62, 5, 0x1565c0, 0x0d2a5a); rr(g, 912, 206, 30, 16, 3, 0x111111); for (int i = 0; i < 3; i++) { g.setColor(c(0xffeb3b)); g.fillRect(914 + i * 9, 209, 7, 10); } g.setColor(c(0xe53935)); g.fillOval(922, 236, 10, 10); }
        // pool table
        int[] pr = Nav.POOLRECT;
        rrs(g, pr[0] - 8, pr[1] - 8, pr[2] + 16, pr[3] + 16, 10, 0x5a3a22, 0x2a170b);
        rr(g, pr[0], pr[1], pr[2], pr[3], 4, 0x1f7a4a);
        g.setColor(c(0x17603a)); g.drawRect(pr[0] + 6, pr[1] + 6, pr[2] - 12, pr[3] - 12);
        g.setColor(c(0x050505)); int[][] pk = {{0, 0}, {pr[2] / 2, -2}, {pr[2], 0}, {0, pr[3]}, {pr[2] / 2, pr[3] + 2}, {pr[2], pr[3]}};
        for (int[] p : pk) g.fillOval(pr[0] + p[0] - 6, pr[1] + p[1] - 6, 12, 12);
        // chairs, stools, tables
        for (Nav.Spot s : Nav.SEATS) {
            if (s.kind.equals("stool")) { g.setColor(c(0x3a1f12)); g.fillOval((int) s.x - 8, (int) s.y - 5, 16, 14); g.setColor(c(0x9b1c1c)); g.fillOval((int) s.x - 8, (int) s.y - 8, 16, 14); g.setColor(c(0xb08d46)); g.drawOval((int) s.x - 8, (int) s.y - 8, 16, 14); }
            else if (s.kind.equals("arm")) { rrs(g, s.x - 17, s.y - 17, 34, 34, 12, 0x5b2c2c, 0x2a1313); rr(g, s.x - 12, s.y - 12, 24, 20, 8, 0x7a3b3b); }
            else { rrs(g, s.x - 8, s.y - 8, 16, 16, 6, 0x6b4426, 0x2a170b); }
        }
        for (double[] t : Nav.TABLES) {
            g.setColor(c(0, 70)); g.fillOval((int) t[0] - 28, (int) t[1] - 18, 60, 48);
            g.setColor(c(0x8a5a30)); g.fillOval((int) t[0] - 27, (int) t[1] - 24, 54, 48); g.setColor(c(0xa87442)); g.fillOval((int) t[0] - 23, (int) t[1] - 20, 46, 40);
            g.setColor(c(0xf3e5ab)); g.fillOval((int) t[0] - 4, (int) t[1] - 4, 9, 9);
        }
        // bar counter
        rr(g, 36, 168, 298, 42, 8, 0x2a170b); rr(g, 36, 168, 298, 30, 8, 0x8a5a30); g.setColor(c(0xb98a52)); g.fillRect(40, 172, 290, 5);
        g.setColor(c(0x3a1f12)); g.fillRect(36, 198, 298, 12); g.setColor(c(0xb08d46)); g.fillRect(38, 206, 294, 3);
        for (int i = 0; i < 5; i++) { double x = 80 + i * 48; rr(g, x, 150, 7, 20, 3, 0xcfa24a); g.setColor(c(0x333333)); g.fillRect((int) x + 1, 140, 5, 12); rr(g, x - 3, 134, 13, 8, 4, new int[]{0xf4c542, 0xb8651b, 0xd9b13b, 0x2a1a12, 0xc62828}[i]); }
        for (int i = 0; i < 3; i++) { rr(g, 40 + i * 30, 174, 14, 8, 3, 0xe0d2a8); }
        // front door
        rrs(g, 440, 594, 80, 26, 3, 0x2a170b, 0xb08d46); g.setColor(c(0xa5d6ff, 170)); g.fillRect(447, 598, 28, 18); g.fillRect(485, 598, 28, 18);
        rr(g, 450, 580, 60, 12, 4, 0x4a2020);
        // decor
        if (S.decor.contains("plants")) for (double[] p : new double[][]{{40, 120}, {930, 140}, {30, 600}, {930, 590}}) { rr(g, p[0] - 8, p[1] - 4, 16, 14, 3, 0x8d4e2e); g.setColor(c(0x2e7d32)); g.fillOval((int) p[0] - 13, (int) p[1] - 22, 26, 22); g.setColor(c(0x43a047)); g.fillOval((int) p[0] - 8, (int) p[1] - 28, 18, 18); }
        if (S.decor.contains("shirt")) { rrs(g, 180, 28, 34, 46, 2, 0x6b4426, 0x2a170b); g.setColor(c(0xc62828)); g.fillRect(186, 36, 22, 30); g.fillRect(180 + 2, 36, 6, 10); g.fillRect(208, 36, 6, 10); }
        if (S.decor.contains("ads")) { rrs(g, 218, 30, 26, 38, 2, 0xe8d8a8, 0x6b4426); strC(g, "ALE", 231, 48, F10, 0x7a1f1f); strC(g, "&STOUT", 231, 60, F9, 0x7a1f1f); }
        if (S.decor.contains("brass")) for (int i = 0; i < 5; i++) { g.setColor(c(0xcfa24a)); g.fillOval(422 + i * 14, 4 + (i % 2) * 6, 9, 9); }
        if (S.decor.contains("chalk")) { rrs(g, 660, 104, 0, 0, 1, 0, 0); }
        if (S.decor.contains("trophy")) { rrs(g, 706, 2, 0, 0, 1, 0, 0); rrs(g, 130, 96, 0, 0, 1, 0, 0); }
        if (S.decor.contains("chalk")) { rrs(g, 80, 122, 0, 0, 0, 0, 0); }
        // fireplace
        if (S.upgrades.contains("fire")) { rrs(g, 10, 490, 52, 100, 4, 0x8a4b3a, 0x3a1f12); rr(g, 18, 520, 36, 56, 4, 0x1a0e08); g.setColor(c(0x5a3a22)); g.fillRect(6, 484, 60, 8); }
        else { rrs(g, 10, 540, 30, 40, 6, 0x5a3a22, 0x2a170b); }
        // stage for karaoke
        if (S.upgrades.contains("karaoke")) { rrs(g, 522, 398, 86, 46, 6, 0x3a2a4a, 0x7a5a9a); strC(g, "STAGE", 565, 424, F10, 0xe6ccff); }
        // trophies shelf & pump clips
        if (S.decor.contains("trophy")) { rrs(g, 90, 96, 70, 20, 2, 0x6b4426, 0x2a170b); for (int i = 0; i < 4; i++) { g.setColor(c(0xe6b800)); g.fillOval(98 + i * 16, 99, 8, 10); } }
        if (S.decor.contains("chalk")) { rrs(g, 560, 126, 72, 14, 3, 0x1f2a24, 0xb08d46); strC(g, "SCAMPI FRIES £1.60", 596, 137, F9, 0xeeeeee); }
    }

    static void window(Graphics2D g, int x, int y, int w, int h) { rrs(g, x - 3, y - 3, w + 6, h + 6, 4, 0x2a170b, 0x7a5230); }
    static void dartboard(Graphics2D g, double cx, double cy, double r) {
        g.setColor(c(0x111111)); g.fillOval((int) (cx - r), (int) (cy - r), (int) (2 * r), (int) (2 * r));
        for (int i = 0; i < 20; i++) { g.setColor(c(i % 2 == 0 ? 0xf3e5ab : 0x222222)); g.fill(new Arc2D.Double(cx - r + 3, cy - r + 3, 2 * r - 6, 2 * r - 6, i * 18 - 9, 18, Arc2D.PIE)); }
        for (int i = 0; i < 20; i++) { g.setColor(c(i % 2 == 0 ? 0xc62828 : 0x2e7d32)); g.fill(new Arc2D.Double(cx - r * .72, cy - r * .72, r * 1.44, r * 1.44, i * 18 - 9, 18, Arc2D.PIE)); g.setColor(c(i % 2 == 0 ? 0xf3e5ab : 0x222222)); g.fill(new Arc2D.Double(cx - r * .62, cy - r * .62, r * 1.24, r * 1.24, i * 18 - 9, 18, Arc2D.PIE)); }
        g.setColor(c(0x2e7d32)); g.fillOval((int) (cx - r * .14), (int) (cy - r * .14), (int) (r * .28), (int) (r * .28)); g.setColor(c(0xc62828)); g.fillOval((int) (cx - r * .07), (int) (cy - r * .07), (int) (r * .14), (int) (r * .14));
    }

    // ================= main render =================
    public static void render(Graphics2D g, double dt) {
        if (S == null) return;
        t += dt; AA(g);
        ensureBg();
        g.drawImage(bg, 0, 0, null);
        drawWindows(g);
        drawTv(g);
        if (S.upgrades.contains("fire")) fire(g);
        fairy(g);
        // messes
        for (Mess m : S.messes) { g.setColor(c(0xe2b04a, 70)); g.fill(new Ellipse2D.Double(m.x - 9, m.y - 3, 18, 7)); g.setColor(c(0xffffff, 40)); g.fill(new Ellipse2D.Double(m.x - 3, m.y - 2, 6, 2)); }
        // neon open sign
        boolean open = Sim.isOpen(); strC(g, open ? "OPEN" : "CLOSED", 480, 577, F11B, open ? 0x66ff99 : 0xff6666);
        // collect drawables
        List<Object[]> items = new ArrayList<>();
        for (Npc n : S.npcs) if (n.inPub) items.add(new Object[]{n.y + (n.st.equals("sit") || n.st.equals("tv") && isSeat(n) ? 6 : 0), n});
        for (Staff s : S.staff) if (s.present) items.add(new Object[]{s.y, s});
        items.add(new Object[]{Player.y, "player"});
        if (S.pigeonHere) items.add(new Object[]{S.pgy, "pigeon"});
        items.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));
        for (Object[] it : items) {
            if (it[1] instanceof Npc n) drawNpc(g, n);
            else if (it[1] instanceof Staff s) drawStaff(g, s);
            else if (it[1].equals("player")) drawPlayer(g);
            else drawPigeon(g);
        }
        // bubbles
        List<Rectangle2D> placed = new ArrayList<>();
        for (Npc n : S.npcs) if (n.inPub && n.bubbleUntil > Sim.rt && !n.bubble.isEmpty()) bubble(g, n.x, headY(n), n.bubble, n.bubbleShout || n.anger > 60, placed);
        for (Staff s : S.staff) if (s.present && !s.note.isEmpty() && s.busy) {}
        if (Player.bubbleUntil > Sim.rt) bubble(g, Player.x, Player.y - 46, Player.bubble, false, placed);
        // hotspots
        for (Hot h : HOTS) if (h.id.equals(hoverHot)) { g.setColor(c(0xffe28a, 70)); g.fillRoundRect(h.x, h.y, h.w, h.h, 8, 8); g.setColor(c(0xffe28a)); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(h.x, h.y, h.w, h.h, 8, 8); strC(g, h.label, h.x + h.w / 2.0, h.y - 4, F11B, 0xffffff); }
        // lighting
        light(g);
        g.drawImage(vignette, 0, 0, null);
        // alerts overlay: waiting list hint
        if (Sim.isOpen() && Sim.S.queue.size() > 0 && Sim.staffByRole("bartender") == null && Sim.staffByRole("manager") == null) strC(g, "No bar staff! Click customers with ! to serve them", 480, 150, F12B, 0xffd27a);
    }
    static boolean isSeat(Npc n) { return !n.spot.isEmpty() && Nav.SPOTS.get(n.spot) != null && Nav.SPOTS.get(n.spot).kind.equals("seat"); }

    static double headY(Npc n) { boolean sit = n.st.equals("sit") || (n.st.equals("tv") && isSeat(n)); return n.y - (sit ? 34 : 44) * n.scale; }

    static void drawWindows(Graphics2D g) {
        double h = S.min / 60.0;
        int sky; double dark;
        if (h < 5 || h >= 21) { sky = 0x0e1430; dark = 1; } else if (h < 7) { sky = mix(0x0e1430, 0xf2a65a, (h - 5) / 2); dark = 1 - (h - 5) / 2; } else if (h < 17) { sky = S.weather >= 2 ? 0x8a949e : (S.weather == 1 ? 0xaecbe0 : 0x8ec5ff); dark = 0; }
        else if (h < 19) { sky = mix(0x8ec5ff, 0xf2894a, (h - 17) / 2); dark = (h - 17) / 4; } else { sky = mix(0xf2894a, 0x0e1430, (h - 19) / 2); dark = .5 + (h - 19) / 4; }
        int[][] ws = {{350, 28, 62, 66}, {2, 250, 16, 80}};
        for (int[] w : ws) {
            Shape clip = g.getClip(); g.setClip(w[0], w[1], w[2], w[3]);
            g.setColor(c(sky)); g.fillRect(w[0], w[1], w[2], w[3]);
            if (dark > .5) { Random r = new Random(7); g.setColor(c(0xffffff, 180)); for (int i = 0; i < 6; i++) g.fillRect(w[0] + r.nextInt(w[2]), w[1] + r.nextInt(w[3] / 2), 1, 1); }
            g.setColor(c(0x30402f)); g.fillRect(w[0], w[1] + w[3] - 10, w[2], 10);
            if (S.weather == 2) { g.setColor(c(0xaad4ff, 160)); for (int i = 0; i < 14; i++) { double x = w[0] + (i * 13 + t * 60) % w[2], y = w[1] + (i * 29 + t * 160) % w[3]; g.drawLine((int) x, (int) y, (int) x - 2, (int) y + 6); } }
            if (S.weather == 3) { g.setColor(c(0xffffff, 220)); for (int i = 0; i < 16; i++) { double x = w[0] + (i * 17 + Math.sin(t + i) * 4 + t * 8) % w[2], y = w[1] + (i * 23 + t * 25) % w[3]; g.fillOval((int) x, (int) y, 2, 2); } }
            g.setClip(clip);
            g.setColor(c(0x2a170b)); g.drawRect(w[0], w[1], w[2], w[3]); g.drawLine(w[0] + w[2] / 2, w[1], w[0] + w[2] / 2, w[1] + w[3]);
        }
        // bottom wall windows
        for (int[] x : new int[][]{{80, 140}, {300, 100}, {560, 140}, {740, 140}}) { g.setColor(c(sky)); g.fillRect(x[0], 604, x[1], 12); g.setColor(c(0x2a170b)); g.drawRect(x[0], 604, x[1], 12); }
    }

    static void fire(Graphics2D g) {
        for (int i = 0; i < 5; i++) { double fl = Math.sin(t * 9 + i * 2) * 3; g.setColor(c(i % 2 == 0 ? 0xff8a1f : 0xffd23a, 220)); g.fill(new Ellipse2D.Double(26 + i * 5, 550 - Math.abs(fl) - i % 3 * 4, 8, 18)); }
        g.setPaint(new RadialGradientPaint(36, 550, 90, new float[]{0, 1}, new Color[]{new Color(255, 150, 40, 70), new Color(255, 150, 40, 0)})); g.fillRect(0, 460, 160, 160);
    }
    static void fairy(Graphics2D g) {
        if (!S.decor.contains("fairy")) return;
        for (int i = 0; i < 30; i++) { double x = 8 + i * 31, y = 112 + Math.sin(i * .9) * 4; g.setColor(c(new int[]{0xffd27a, 0xff9ab0, 0x9ae0ff}[i % 3], 150 + (int) (90 * Math.sin(t * 3 + i)))); g.fillOval((int) x, (int) y, 5, 5); }
    }

    static void drawTv(Graphics2D g) {
        int x = 520, y = 28, w = 180, h = 74;
        rrs(g, x - 4, y - 4, w + 8, h + 8, 6, 0x151515, 0x333333);
        Shape clip = g.getClip(); g.setClip(x, y, w, h);
        Match m = S.match;
        if (S.tvBroken || S.powerCut) {
            Random r = new Random((long) (t * 20)); for (int i = 0; i < 90; i++) { g.setColor(c(r.nextInt(255) * 0x010101)); g.fillRect(x + r.nextInt(w), y + r.nextInt(h), 14, 2); }
            if (S.powerCut) { g.setColor(c(0, 255)); g.fillRect(x, y, w, h); }
            else strC(g, "NO SIGNAL", x + w / 2.0, y + h / 2 + 4, F12B, 0xffffff);
        } else if (m != null && !m.phase.equals("pre")) {
            g.setColor(c(0x2a8a46)); g.fillRect(x, y, w, h);
            for (int i = 0; i < 6; i++) { g.setColor(c(0x238040)); g.fillRect(x + i * 30, y, 15, h); }
            g.setColor(c(0xffffff, 190)); g.drawRect(x + 10, y + 10, w - 20, h - 28); g.drawLine(x + w / 2, y + 10, x + w / 2, y + h - 18); g.drawOval(x + w / 2 - 14, y + h / 2 - 14, 28, 20);
            double ph = (t * .7) % 1; double bx = x + 20 + Math.abs(Math.sin(t * .8 + m.minute)) * (w - 40), by = y + 18 + Math.abs(Math.sin(t * 1.3)) * (h - 44);
            g.setColor(c(0xffffff)); g.fillOval((int) bx, (int) by, 5, 5);
            for (int i = 0; i < 6; i++) { g.setColor(c(Data.TEAM_COL[m.home])); g.fillOval((int) (x + 25 + i * 24 + Math.sin(t + i) * 5), (int) (y + 20 + (i % 3) * 14), 5, 5); g.setColor(c(Data.TEAM_COL[m.away])); g.fillOval((int) (x + 35 + i * 24 + Math.cos(t + i) * 5), (int) (y + 26 + (i % 3) * 12), 5, 5); }
            g.setColor(c(0, 180)); g.fillRect(x, y, w, 13); String sc = Data.TEAM_ID[m.home] + " " + m.hg + "-" + m.ag + " " + Data.TEAM_ID[m.away] + "   " + (m.phase.equals("ft") ? "FT" : m.phase.equals("ht") ? "HT" : m.minute + "'");
            strC(g, sc, x + w / 2.0, y + 10, F10, 0xffffff);
            g.setColor(c(0, 190)); g.fillRect(x, y + h - 13, w, 13); String tk = m.lastEvent.isEmpty() ? m.label : m.lastEvent;
            int off = (int) (t * 30) % (g.getFontMetrics(F9).stringWidth(tk + "     ") + 1); g.setFont(F9); g.setColor(c(0xffe28a)); g.drawString(tk + "     " + tk, x + 4 - off, y + h - 3);
            if (m.flash > 0) { g.setColor(c(0xffd23a, 120 + (int) (80 * Math.sin(t * 20)))); g.fillRect(x, y, w, h); strC(g, "GOAL!", x + w / 2.0, y + h / 2 + 8, F20B, 0xffffff); }
        } else if (m != null && m.phase.equals("pre")) { g.setColor(c(0x15305a)); g.fillRect(x, y, w, h); strC(g, "KICK-OFF SOON", x + w / 2.0, y + 32, F12B, 0xffffff); }
        else {
            double hr = S.min / 60.0; g.setColor(c(0x1a2a5a)); g.fillRect(x, y, w, h);
            if (!Sim.isOpen()) { g.setColor(c(0x111111)); g.fillRect(x, y, w, h); }
            else { String show = Data.DAYTIME_TV[(S.day + (int) hr / 2) % Data.DAYTIME_TV.length]; g.setColor(c(0x3050a0)); g.fillRect(x + 6, y + 8, w - 12, h - 26); strC(g, show, x + w / 2.0, y + h / 2 - 2, F11B, 0xffffff); strC(g, "LIVE", x + w - 18, y + 14, F9, 0xff6666); }
        }
        g.setClip(clip);
        if (!S.upgrades.contains("tv")) { g.setColor(c(0, 200)); g.fillRect(x, y, w, h); }
    }

    static void light(Graphics2D g) {
        double h = S.min / 60.0; double a = 0;
        if (h >= 20 || h < 5) a = .22; else if (h >= 17) a = .22 * (h - 17) / 3; else if (h < 7) a = .22 * (7 - h) / 2;
        if (Sim.season() == 1) a += .04;
        if (S.powerCut) {
            g.setColor(c(0x050310, 205)); g.fillRect(0, 0, W, H);
            for (double[] tb : Nav.TABLES) { g.setPaint(new RadialGradientPaint((float) tb[0], (float) tb[1], 70, new float[]{0, 1}, new Color[]{new Color(255, 190, 90, 120), new Color(255, 190, 90, 0)})); g.fillRect((int) tb[0] - 80, (int) tb[1] - 80, 160, 160); }
            return;
        }
        if (a > 0) { g.setColor(c(0x0b0620, (int) (a * 255))); g.fillRect(0, 0, W, H); }
        // warm pools around lamps in the evening
        if (a > .05) for (double[] tb : Nav.TABLES) { g.setPaint(new RadialGradientPaint((float) tb[0], (float) tb[1], 90, new float[]{0, 1}, new Color[]{new Color(255, 200, 120, (int) (a * 220)), new Color(255, 200, 120, 0)})); g.fillRect((int) tb[0] - 90, (int) tb[1] - 90, 180, 180); }
    }

    // ================= people =================
    static void drawNpc(Graphics2D g, Npc n) {
        boolean sit = n.st.equals("sit") || (n.st.equals("tv") && isSeat(n)) || n.st.equals("eat");
        double wob = n.drunk > 35 && !n.st.equals("walk") ? Math.sin(t * 2 + n.x) * n.drunk * .03 : 0;
        person(g, n.x + wob, n.y, n.scale, n.skin, n.hair, n.shirt, n.hairStyle, sit, n.walkPhase, n.st.equals("walk"), n.glass >= 0 ? Data.DCOL[n.glass] : -1, n.glassLevel, 0, n.st.equals("darts"), n.eating);
        if (n.id.equals(selId) || n.id.equals(hoverId)) { g.setColor(c(n.id.equals(selId) ? 0xffe28a : 0xffffff, 200)); g.setStroke(new BasicStroke(1.8f)); g.draw(new Ellipse2D.Double(n.x - 13, n.y - 5, 26, 11)); }
        double hy = headY(n);
        // status glyph
        String gl = null; int col = 0xffffff;
        if (n.st.equals("waitBar") && n.order != null) { gl = "!"; col = 0xffb300; }
        else if (n.anger > 55) { gl = "!!"; col = 0xe53935; }
        else if (n.conv != null && !n.conv.isEmpty() && Social.convs.stream().anyMatch(c -> c.id.equals(n.conv) && c.flagged)) { gl = "!!"; col = 0xe53935; }
        else if (n.embar > 45) { gl = "..."; col = 0xf48fb1; }
        else if (n.drunk > 55) { gl = "~"; col = 0xaed581; }
        else if (S.crush.stream().anyMatch(s -> s.startsWith(n.id + "|"))) { gl = "♥"; col = 0xf06292; }
        else if (n.energy < 12) { gl = "z"; col = 0x90caf9; }
        else if (n.st.equals("loo") && n.timer > 18) { gl = "?!"; col = 0xe53935; }
        if (gl != null) { double bx = n.x + 10, by = hy - 4; g.setColor(c(col)); g.fill(new Ellipse2D.Double(bx - 7, by - 9, 14, 14)); strC(g, gl, bx, by + 2, gl.length() > 1 ? F9 : F11B, col == 0xffb300 ? 0x000000 : 0xffffff); }
        // mood face (tiny)
        if (showNames) { g.setFont(F9); String nm = Sim.first(n); int w = g.getFontMetrics().stringWidth(nm); g.setColor(c(0, 130)); g.fillRoundRect((int) (n.x - w / 2.0 - 3), (int) n.y + 3, w + 6, 11, 6, 6); g.setColor(c(n.stranger ? 0xcfd8dc : 0xfff3c4)); g.drawString(nm, (float) (n.x - w / 2.0), (float) n.y + 12); }
    }

    static void drawStaff(Graphics2D g, Staff s) {
        person(g, s.x, s.y, 1, 0xE0AC8C, s.hair, s.shirt, 0, false, t * 8, Math.hypot(s.tx - s.x, s.ty - s.y) > 2, s.busy && s.role.equals("bartender") ? 0xF4C542 : -1, 1, 1, false, false);
        g.setFont(F9); String nm = s.name.split(" ")[0]; int w = g.getFontMetrics().stringWidth(nm);
        g.setColor(c(0x1565c0, 190)); g.fillRoundRect((int) (s.x - w / 2.0 - 3), (int) s.y + 3, w + 6, 11, 6, 6); g.setColor(c(0xffffff)); g.drawString(nm, (float) (s.x - w / 2.0), (float) s.y + 12);
    }
    static void drawPlayer(Graphics2D g) {
        person(g, Player.x, Player.y, 1.06, 0xE0AC8C, 0x3b2a1a, 0x2e7d32, 0, false, Player.phase, Player.walking, -1, 0, 2, false, false);
        g.setColor(c(0xffd54f)); g.fill(star(Player.x + 3, Player.y - 18, 3.4));
        g.setFont(F9); String nm = "You"; int w = g.getFontMetrics().stringWidth(nm);
        g.setColor(c(0xb08d46, 220)); g.fillRoundRect((int) (Player.x - w / 2.0 - 3), (int) Player.y + 3, w + 6, 11, 6, 6); g.setColor(c(0x111111)); g.drawString(nm, (float) (Player.x - w / 2.0), (float) Player.y + 12);
    }
    static Shape star(double cx, double cy, double r) {
        Path2D p = new Path2D.Double(); for (int i = 0; i < 10; i++) { double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * .45; double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr; if (i == 0) p.moveTo(x, y); else p.lineTo(x, y); } p.closePath(); return p;
    }

    static void person(Graphics2D g, double x, double y, double sc, int skin, int hair, int shirt, int style, boolean sit, double phase, boolean walking, int glassCol, double glassLvl, int apron, boolean arm, boolean eating) {
        AffineTransform old = g.getTransform(); g.translate(x, y); g.scale(sc, sc);
        g.setColor(c(0, 70)); g.fill(new Ellipse2D.Double(-8, -2, 16, 6));
        double oy = sit ? 5 : 0;
        if (!sit) { double sw = walking ? Math.sin(phase) * 3 : 0; g.setColor(c(0x2b2b3a)); g.fillRoundRect((int) (-5 + sw), -10, 4, 10, 2, 2); g.fillRoundRect((int) (1 - sw), -10, 4, 10, 2, 2); g.setColor(c(0x111111)); g.fillRect((int) (-5 + sw), -2, 4, 2); g.fillRect((int) (1 - sw), -2, 4, 2); }
        rr(g, -7, -23 + oy, 14, 15, 6, shirt);
        if (apron == 1) { g.setColor(c(0xf5f5f5)); g.fillRoundRect(-5, -18 + (int) oy, 10, 10, 3, 3); }
        if (apron == 2) { g.setColor(c(0x3a2a1a)); g.fillRect(-7, -18 + (int) oy, 14, 8); g.setColor(c(0xb08d46)); g.fillRect(-1, -18 + (int) oy, 2, 8); }
        g.setColor(c(skin)); g.fillRoundRect(-10, -21 + (int) oy, 3, 9, 2, 2); g.fillRoundRect(7, -21 + (int) oy, 3, 9, 2, 2);
        if (glassCol >= 0) { double gy = -22 + oy; g.setColor(c(0xeceff1, 220)); g.fillRect(6, (int) gy - 2, 6, 9); g.setColor(c(glassCol)); double lv = Math.max(.15, glassLvl); g.fillRect(7, (int) (gy + 7 - 6 * lv), 4, (int) (6 * lv)); g.setColor(c(0xffffff, 230)); g.fillRect(7, (int) gy - 1, 4, 1); }
        g.setColor(c(skin)); g.fill(new Ellipse2D.Double(-6, -34 + oy, 12, 12));
        g.setColor(c(hair));
        switch (style) {
            case 0 -> g.fill(new Arc2D.Double(-6.5, -35 + oy, 13, 11, 0, 180, Arc2D.CHORD));
            case 1 -> { g.setColor(c(0xffffff, 80)); g.fillOval(-2, (int) (-33 + oy), 3, 2); }
            case 2 -> { g.fill(new Arc2D.Double(-7, -36 + oy, 14, 11, 0, 180, Arc2D.CHORD)); g.fillRect(-7, (int) (-30 + oy), 14, 2); g.fillRect(2, (int) (-30 + oy), 8, 2); }
            case 3 -> { g.fill(new Arc2D.Double(-7, -36 + oy, 14, 12, 0, 180, Arc2D.CHORD)); g.fillRoundRect(-8, (int) (-33 + oy), 4, 15, 3, 3); g.fillRoundRect(4, (int) (-33 + oy), 4, 15, 3, 3); }
            case 4 -> { g.fillOval(-8, (int) (-37 + oy), 16, 13); }
            default -> { g.fill(new Arc2D.Double(-6.5, -35 + oy, 13, 11, 0, 180, Arc2D.CHORD)); g.fillOval(-3, (int) (-40 + oy), 7, 7); }
        }
        g.setColor(c(0x222222)); g.fillRect(-3, (int) (-29 + oy), 2, 2); g.fillRect(2, (int) (-29 + oy), 2, 2);
        g.setColor(c(0x7a3a2a)); if (eating) g.fillOval(-1, (int) (-25 + oy), 3, 3); else g.drawLine(-2, (int) (-25 + oy), 2, (int) (-25 + oy));
        g.setTransform(old);
    }

    static void drawPigeon(Graphics2D g) {
        double x = S.pgx, y = S.pgy, b = Math.sin(t * 10) * 1.2;
        g.setColor(c(0, 60)); g.fill(new Ellipse2D.Double(x - 8, y - 1, 16, 5));
        g.setColor(c(0x8d98a6)); g.fill(new Ellipse2D.Double(x - 8, y - 10 + b, 16, 10)); g.setColor(c(0x6a7482)); g.fill(new Ellipse2D.Double(x - 8, y - 8 + b, 9, 7));
        g.setColor(c(0x4f6a86)); g.fill(new Ellipse2D.Double(x + 3, y - 15 + b, 8, 8)); g.setColor(c(0x8d98a6)); g.fillOval((int) x + 4, (int) (y - 14 + b), 6, 6);
        g.setColor(c(0xf5a623)); g.fillPolygon(new int[]{(int) x + 10, (int) x + 14, (int) x + 10}, new int[]{(int) (y - 11 + b), (int) (y - 10 + b), (int) (y - 9 + b)}, 3);
        g.setColor(c(0x111111)); g.fillRect((int) x + 8, (int) (y - 12 + b), 1, 1);
        g.setColor(c(0xf5a623)); g.drawLine((int) x - 2, (int) y, (int) x - 2, (int) y + 3 + (int) b); g.drawLine((int) x + 2, (int) y, (int) x + 2, (int) y + 3);
        g.setFont(F9); str(g, S.pigeonName.equals("the pigeon") ? "Pigeon" : S.pigeonName, x - 12, y + 14, F9, 0xffffff);
    }

    static void bubble(Graphics2D g, double x, double y, String text, boolean loud, List<Rectangle2D> placed) {
        g.setFont(loud ? F11B : F11); FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>(); StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) { if (fm.stringWidth(cur + w) > 150 && cur.length() > 0) { lines.add(cur.toString().trim()); cur = new StringBuilder(); } cur.append(w).append(' '); }
        lines.add(cur.toString().trim());
        int w = 0; for (String l : lines) w = Math.max(w, fm.stringWidth(l)); int h = lines.size() * (fm.getHeight() - 1) + 6;
        double bx = Util.clamp(x - w / 2.0 - 5, 2, W - w - 12), by = y - h - 8;
        Rectangle2D r = new Rectangle2D.Double(bx, by, w + 10, h);
        for (int i = 0; i < 6; i++) { boolean hit = false; for (Rectangle2D p : placed) if (p.intersects(r)) { hit = true; r.setRect(r.getX(), p.getY() - h - 2, r.getWidth(), h); break; } if (!hit) break; }
        placed.add(r);
        g.setColor(c(0, 60)); g.fill(new RoundRectangle2D.Double(r.getX() + 1.5, r.getY() + 2, r.getWidth(), r.getHeight(), 9, 9));
        g.setColor(c(loud ? 0xfff0f0 : 0xfffaf0, 245)); g.fill(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 9, 9));
        g.setColor(c(loud ? 0xc62828 : 0x6b4426)); g.setStroke(new BasicStroke(1.2f)); g.draw(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 9, 9));
        Path2D tail = new Path2D.Double(); double tx = Util.clamp(x, r.getX() + 8, r.getMaxX() - 8); tail.moveTo(tx - 4, r.getMaxY()); tail.lineTo(tx, r.getMaxY() + 6); tail.lineTo(tx + 4, r.getMaxY()); g.setColor(c(loud ? 0xfff0f0 : 0xfffaf0)); g.fill(tail);
        g.setColor(c(0x222222)); int ty = (int) r.getY() + fm.getAscent() + 2; for (String l : lines) { g.drawString(l, (float) r.getX() + 5, ty); ty += fm.getHeight() - 1; }
    }

    // ================= town map =================
    public record Loc(String id, String name, int x, int y, int w, int h, int col) {}
    public static final List<Loc> TOWN = List.of(
        new Loc("pub", "The Speckled Pigeon", 400, 220, 120, 70, 0x9a5a2c), new Loc("frog", "The Frog & Trumpet", 130, 90, 120, 60, 0x4a6a3a), new Loc("oak", "The Royal Oak", 720, 90, 110, 60, 0x4a3a2a),
        new Loc("super", "Savemore", 660, 300, 140, 70, 0xb23a3a), new Loc("stadium", "Westbridge Rovers FC", 700, 410, 180, 90, 0x2f7a4a), new Loc("station", "Train Station", 60, 230, 120, 55, 0x555d68),
        new Loc("houses", "Maple Close (houses)", 90, 370, 170, 90, 0xb59a6a), new Loc("park", "Victoria Park", 420, 400, 170, 100, 0x3f8f4a), new Loc("takeaway", "Kebabylon", 280, 300, 90, 45, 0xd9822b),
        new Loc("cafe", "Brenda's Caff", 290, 150, 90, 45, 0xe0b46a), new Loc("police", "Police Station", 560, 100, 100, 55, 0x2c4d7a), new Loc("church", "St Wulfstan's", 590, 215, 70, 55, 0xbfae8e),
        new Loc("hospital", "Westbridge General", 840, 220, 90, 55, 0xdddddd), new Loc("uni", "Uni of Westbridge", 280, 400, 110, 60, 0x6a4a8a), new Loc("council", "Council Offices", 440, 100, 100, 50, 0x777777),
        new Loc("work", "Industrial Estate", 840, 340, 90, 55, 0x666655), new Loc("shop", "High Street Shops", 400, 320, 100, 50, 0xa07850));

    public static void drawTown(Graphics2D g, int w, int h, String hover) {
        AA(g); g.setColor(c(0x4f7a46)); g.fillRect(0, 0, w, h);
        g.setColor(c(0x6b6b6b)); g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(60, 200, 900, 200); g.drawLine(380, 80, 380, 520); g.drawLine(640, 80, 640, 440); g.drawLine(60, 330, 640, 330);
        g.setColor(c(0xd9d2b8, 90)); g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{8, 8}, 0)); g.drawLine(60, 200, 900, 200); g.drawLine(380, 80, 380, 520); g.drawLine(640, 80, 640, 440); g.drawLine(60, 330, 640, 330);
        g.setColor(c(0x8ec5ff, 160)); g.fillRoundRect(60, 460, 330, 40, 30, 30); strC(g, "River Wes", 220, 485, F10, 0xffffff);
        Map<String, List<String>> at = new LinkedHashMap<>();
        for (Npc n : S.npcs) { String loc = n.inPub ? "pub" : n.loc; at.computeIfAbsent(loc, k -> new ArrayList<>()).add(n.name.split(" ")[0].replace("Big", "Tony").replace("Rev.", "Alan").replace("Dr", "Fiona")); }
        for (Loc l : TOWN) {
            if (l.id.equals("oak") && !S.royalOak) continue;
            rrs(g, l.x, l.y, l.w, l.h, 8, l.col, 0x2a170b);
            g.setColor(c(0, 40)); g.fillRect(l.x + 4, l.y + l.h - 8, l.w - 8, 4);
            String nm = l.name; strC(g, nm, l.x + l.w / 2.0, l.y + 14, F10, 0xffffff);
            if (l.id.equals(hover)) { g.setColor(c(0xffe28a)); g.setStroke(new BasicStroke(2.5f)); g.drawRoundRect(l.x, l.y, l.w, l.h, 8, 8); }
            List<String> ns = at.get(l.id); if (ns != null) {
                int i = 0; for (String s : ns) { double px = l.x + 10 + (i % 6) * 17, py = l.y + 30 + (i / 6) * 17; g.setColor(c(0xfff3c4)); g.fill(new Ellipse2D.Double(px, py, 12, 12)); g.setColor(c(0x2a170b)); g.draw(new Ellipse2D.Double(px, py, 12, 12)); strC(g, s.substring(0, 1), px + 6, py + 10, F9, 0x2a170b); i++; }
            }
        }
        if (S.match != null) strC(g, "MATCH: " + Football.score(S.match), w / 2.0, 24, F12B, 0xffffff);
    }
}
