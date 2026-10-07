package pub;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;
import pub.Model.*;
import static pub.UI.*;
import static pub.Sim.S;

/** Main window: HUD, game canvas, side panel, overlays; also the Sim -> UI bridge. */
public final class GameWindow extends JFrame {
    public static GameWindow I;
    public static boolean chatter = true, soak = false;
    final Hud hud = new Hud(); final GamePanel game = new GamePanel(); final Side side = new Side(); final JLabel status = new JLabel(" ");
    final JPanel glass = new JPanel(new GridBagLayout()) { @Override protected void paintComponent(Graphics g) { g.setColor(new Color(0, 0, 0, 120)); g.fillRect(0, 0, getWidth(), getHeight()); } };
    final Deque<Sim.Choice> queue = new ArrayDeque<>(); final Deque<Runnable> later = new ArrayDeque<>();
    boolean overlayOpen, choiceShowing; Runnable onCloseOverlay;
    int speed = 1; static final double[] SPEEDS = {0, 1, 2, 4};
    String alertText = ""; double alertUntil; boolean paperNews;
    final List<String[]> toasts = new ArrayList<>(); // {text, bornTime, color}
    double flash; String tickerText = "";

    public GameWindow() {
        super("British Pub Simulator — The Speckled Pigeon"); I = this;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { if (S != null) Save.save(1); System.exit(0); } });
        JPanel root = new WoodPanel(new BorderLayout());
        root.add(hud, BorderLayout.NORTH); root.add(game, BorderLayout.CENTER); root.add(side, BorderLayout.EAST);
        status.setForeground(CREAM); status.setFont(sans(12, true)); status.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10)); root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        glass.setOpaque(false) ; glass.setVisible(false); setGlassPane(glass);
        glass.addMouseListener(new MouseAdapter() {}); glass.addMouseMotionListener(new MouseMotionAdapter() {}); glass.addKeyListener(new KeyAdapter() {});
        Dimension sc = Toolkit.getDefaultToolkit().getScreenSize(); setSize(Math.min(1400, sc.width - 40), Math.min(880, sc.height - 70)); setMinimumSize(new Dimension(1100, 700)); setLocationRelativeTo(null);
        installKeys();
        Sim.L = bridge();
    }

    // ---------------- keys ----------------
    final Set<Integer> down = new HashSet<>();
    void installKeys() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (!isActive()) return false;
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof javax.swing.text.JTextComponent tc && tc.isEditable()) return false;
            if (e.getID() == KeyEvent.KEY_RELEASED) { down.remove(e.getKeyCode()); return false; }
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            int k = e.getKeyCode(); down.add(k);
            if (k == KeyEvent.VK_ESCAPE) { if (overlayOpen && !choiceShowing && !gameOpen) { close(); return true; } else if (!overlayOpen && S != null) { open("Menu", Screens.menu()); return true; } }
            if (overlayOpen || S == null) return false;
            switch (k) {
                case KeyEvent.VK_SPACE -> { speed = speed == 0 ? 1 : 0; return true; }
                case KeyEvent.VK_1 -> speed = 1; case KeyEvent.VK_2 -> speed = 2; case KeyEvent.VK_3 -> speed = 3;
                case KeyEvent.VK_M -> open("Manage the pub", Screens.manage(0));
                case KeyEvent.VK_N -> openNews();
                case KeyEvent.VK_T -> open("Westbridge", Screens.town());
                case KeyEvent.VK_B -> open("Regulars Book", Screens.book());
                case KeyEvent.VK_J -> open("Jukebox", Screens.jukebox());
                case KeyEvent.VK_F5 -> { toastMsg(Save.save(1) ? "Game saved." : "Save failed!"); }
                default -> {}
            }
            return false;
        });
    }
    void openNews() { hud.newsPulse = false; open("The Westbridge Chronicle", Screens.chronicle()); }

    // ---------------- overlays ----------------
    boolean gameOpen;
    public static void open(String title, JComponent body) { I.openOverlay(title, body, true, true, null); }
    public static void close() { I.closeOverlay(); }

    void openOverlay(String title, JComponent body, boolean closable, boolean paper, Runnable onClose) {
        glass.removeAll(); onCloseOverlay = onClose;
        JPanel card = paper ? new PaperPanel(new BorderLayout(8, 8)) : new WoodPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createEmptyBorder(16, 22, 24, 28));
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        JLabel t = new JLabel(title); t.setFont(serif(24, true)); t.setForeground(paper ? INK : BRASS2); head.add(t, BorderLayout.WEST);
        if (closable) head.add(btn("Close", this::closeOverlay).small(), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH); card.add(body, BorderLayout.CENTER);
        glass.add(card, new GridBagConstraints()); glass.setVisible(true); glass.revalidate(); glass.repaint(); overlayOpen = true;
        card.setBackground(WOOD);
    }
    void closeOverlay() {
        boolean was = overlayOpen; glass.setVisible(false); glass.removeAll(); overlayOpen = false; choiceShowing = false; gameOpen = false; Audio.quietMode = false;
        Runnable oc = onCloseOverlay; onCloseOverlay = null; if (oc != null) oc.run();
        if (!was) return;
        pump();
    }
    void pump() {
        if (overlayOpen) return;
        if (!queue.isEmpty()) { showChoice(queue.poll()); return; }
        if (!later.isEmpty()) { later.poll().run(); }
    }
    void showChoice(Sim.Choice c) {
        JPanel p = vbox(); p.setPreferredSize(new Dimension(560, 60 + c.opts.size() * 46 + 100));
        JLabel tx = new JLabel("<html><body style='width:520px;font-family:Georgia;font-size:14px'>" + esc(c.text) + "</body></html>"); tx.setForeground(INK); p.add(tx); p.add(Box.createVerticalStrut(14));
        for (Sim.Opt o : c.opts) {
            BrassButton b = btn(o.label, () -> { glass.setVisible(false); glass.removeAll(); overlayOpen = false; choiceShowing = false; Events.modalOpen = false; try { o.run.run(); } catch (RuntimeException ex) { ex.printStackTrace(); } pump(); });
            b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(520, 38)); if (!o.hint.isEmpty()) b.setToolTipText(o.hint); p.add(b); p.add(Box.createVerticalStrut(6));
        }
        openOverlay(c.title, p, false, true, null); choiceShowing = true;
    }
    void screen(String title, JComponent body) { if (overlayOpen) later.add(() -> open(title, body)); else open(title, body); }

    public void startGame(String kind, Npc opp) {
        Runnable done = this::closeOverlay;
        JPanel p; String title;
        switch (kind) {
            case "darts" -> { p = Games.darts(opp, done); title = "Darts — 301"; }
            case "pool" -> { p = Games.pool(opp, done); title = "Pool"; }
            case "quiz" -> { p = Games.quiz(done); title = "Pub Quiz"; }
            default -> { p = Games.karaoke(done); title = "Karaoke"; }
        }
        Runnable go = () -> { openOverlay(title, p, false, false, null); gameOpen = true; };
        if (overlayOpen) later.add(go); else go.run();
    }

    // ---------------- Sim bridge ----------------
    Sim.Listener bridge() {
        return new Sim.Listener() {
            @Override public void log(String text, String cat) { side.addLine(text, cat); }
            @Override public void toast(String t) { toastMsg(t); }
            @Override public void sfx(String n) { Audio.sfx(n); }
            @Override public void say(Npc n, String text) {
                n.bubbleShout = text.equals(text.toUpperCase()) && text.length() > 6;
                if (chatter && !text.isEmpty()) side.addChat(n, text);
            }
            @Override public void choice(Sim.Choice c) { if (soak) { Events.modalOpen = false; Sim.Opt o = c.opts.get(Util.ri(0, c.opts.size() - 1)); o.run.run(); return; } if (overlayOpen) queue.add(c); else showChoice(c); }
            @Override public void news(News n) { side.addLine("📰 Chronicle (tomorrow): " + n.head, "news"); }
            @Override public void ach(String t) { toastMsg("🏆 " + t, 0xffd54f); }
            @Override public void fx(String type, double x, double y) { if (type.equals("goal")) flash = 1; if (type.equals("shake")) {} }
            @Override public void commentary(String t) { tickerText = t; Audio.speak(t); }
            @Override public void levelUp(int l) { if (soak) return; later.add(() -> open("LEVEL UP!", levelPanel(l))); if (!overlayOpen) pump(); }
            @Override public void dayEnd(DayRec r) { if (soak) { Screens.daySummary(r); return; } later.add(() -> open("Closing time", Screens.daySummary(r))); if (!overlayOpen) pump(); Save.save(1); }
            @Override public void alert(String t) { alertText = t; alertUntil = Sim.rt + 9; Audio.sfx("wrong"); String cid = S.flags.get("argue"); if (cid != null) for (Social.Conv c : Social.convs) if (c.id.equals(cid)) { Npc a = Sim.npc(c.a); if (a != null) { Gfx.selId = a.id; side.card.show(a); } } }
            @Override public void morning() { hud.newsPulse = true; toastMsg("☀ Morning, " + Sim.dayName() + ". The Chronicle has arrived (N)."); }
            @Override public void game(String kind, Npc opp) { if (soak) { switch (kind) { case "darts" -> Games.darts(opp, () -> {}); case "pool" -> Games.pool(opp, () -> {}); case "quiz" -> Games.quiz(() -> {}); default -> Games.karaoke(() -> {}); } return; } startGame(kind, opp); }
            @Override public void autosave() { Save.save(1); }
            @Override public void closed() { toastMsg("🔔 Closing time. Customers are drifting home."); }
        };
    }
    JComponent levelPanel(int l) {
        JPanel p = vbox(); p.add(Screens.inkSerif("Your pub is now a " + Mgmt.levelName(l) + "!", 22)); p.add(Screens.ink("<html><body style='width:480px'>New facilities unlock at higher levels, and bigger crowds follow. Keep the regulars happy — they will make the pub what it is.</body></html>", 14, false));
        p.add(Box.createVerticalStrut(10)); p.add(btn("Onwards!", this::closeOverlay)); p.setPreferredSize(new Dimension(520, 200)); return p;
    }
    void toastMsg(String t) { toastMsg(t, 0xf2e6c8); }
    void toastMsg(String t, int col) { toasts.add(new String[]{t, "" + Sim.rt, "" + col}); if (toasts.size() > 4) toasts.remove(0); side.addLine(t, "sys"); }

    // ---------------- game start/load ----------------
    public void newGame(String name) {
        Sim.newGame(name); Social.convs.clear(); Ai.games.clear(); Events.cooldown.clear(); Events.modalOpen = false; queue.clear(); later.clear();
        Player.sync(); afterLoad(); speed = 1;
        open("Welcome, landlord", Screens.help());
    }
    public void afterLoad() { Gfx.selId = ""; side.card.show(null); side.clear(); for (int i = Math.max(0, S.log.size() - 40); i < S.log.size(); i++) side.addLine(S.log.get(i), "sys"); Player.sync(); queue.clear(); Gfx.bg = null; }
    public void confirmNew() {
        Sim.Choice c = new Sim.Choice("Start a new pub?", "This abandons the current game (it stays in the save slot until you save again).");
        c.opts.add(new Sim.Opt("Yes, a fresh pub", () -> showTitle())); c.opts.add(new Sim.Opt("No, keep playing", () -> {})); Sim.ask(c);
    }

    public void showTitle() {
        JPanel p = vbox(); p.setPreferredSize(new Dimension(560, 360));
        p.add(Screens.inkSerif("British Pub Simulator", 30)); p.add(Screens.ink("A chaotic, cosy, surprisingly deep life simulator set in a local pub in Westbridge.", 13, false)); p.add(Box.createVerticalStrut(14));
        JTextField name = new JTextField("The Speckled Pigeon", 24); name.setFont(serif(18, true)); name.setMaximumSize(new Dimension(420, 36)); name.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(Screens.ink("Name your pub:", 13, true)); p.add(name); p.add(Box.createVerticalStrut(12));
        BrassButton nb = btn("▶  New game", () -> { String n = name.getText().trim(); closeOverlay(); newGame(n.isEmpty() ? "The Speckled Pigeon" : n); }); nb.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(nb); p.add(Box.createVerticalStrut(8));
        BrassButton cont = btn("Continue — " + Save.describe(1), () -> { closeOverlay(); if (Save.load(1)) afterLoad(); }); cont.setEnabled(Save.exists(1)); cont.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(cont); p.add(Box.createVerticalStrut(8));
        BrassButton hp = btn("How to play", () -> { closeOverlay(); open("How to play", Screens.help()); }); hp.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(hp);
        p.add(Box.createVerticalStrut(12)); p.add(Screens.ink("Based on a true pub. Possibly.", 11, false));
        openOverlay("Welcome to Westbridge", p, false, true, null);
    }

    // =====================================================================
    //  HUD
    // =====================================================================
    final class Hud extends JPanel {
        boolean newsPulse; BrassButton newsBtn = btn("News", () -> { if (!overlayOpen) openNews(); }).small(); final JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 14)); final List<BrassButton> speedBtns = new ArrayList<>();
        Hud() {
            super(new BorderLayout()); setPreferredSize(new Dimension(100, 74)); setOpaque(false);
            btns.setOpaque(false);
            String[] sl = {"II", ">", ">>", ">>>"};
            for (int i = 0; i < 4; i++) { final int k = i; BrassButton b = btn(sl[i], () -> speed = k).small(); b.setToolTipText("Game speed"); speedBtns.add(b); btns.add(b); }
            btns.add(Box.createHorizontalStrut(10));
            btns.add(btn("Manage", () -> { if (!overlayOpen) open("Manage the pub", Screens.manage(0)); }).small());
            btns.add(newsBtn);
            btns.add(btn("Town", () -> { if (!overlayOpen) open("Westbridge", Screens.town()); }).small());
            btns.add(btn("Book", () -> { if (!overlayOpen) open("Regulars Book", Screens.book()); }).small());
            btns.add(btn("Juke", () -> { if (!overlayOpen) open("Jukebox", Screens.jukebox()); }).small());
            btns.add(btn("🏆", () -> { if (!overlayOpen) open("Achievements", Screens.achievements()); }).small());
            btns.add(btn("Menu", () -> { if (!overlayOpen) open("Menu", Screens.menu()); }).small());
            add(btns, BorderLayout.EAST);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g); int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x4d3020), 0, h, new Color(0x241810))); g.fillRect(0, 0, w, h);
            g.setColor(BRASS); g.fillRect(0, h - 3, w, 3);
            // hanging sign
            g.setColor(new Color(0x2a1a12)); g.fillRoundRect(10, 8, 62, 56, 10, 10); g.setColor(BRASS); g.drawRoundRect(10, 8, 62, 56, 10, 10);
            g.setColor(new Color(0x8d98a6)); g.fillOval(20, 26, 36, 24); g.setColor(new Color(0x4f6a86)); g.fillOval(46, 16, 18, 18); g.setColor(new Color(0xf5a623)); g.fillPolygon(new int[]{63, 71, 63}, new int[]{23, 25, 27}, 3); g.setColor(Color.BLACK); g.fillOval(56, 21, 3, 3);
            g.setColor(new Color(0x6a7482)); for (int i = 0; i < 6; i++) g.fillOval(26 + (i % 3) * 7, 32 + (i / 3) * 7, 3, 3);
            if (S == null) { g.dispose(); return; }
            g.setFont(serif(22, true)); g.setColor(BRASS2); g.drawString(S.pubName, 84, 32); g.setFont(sans(12, false)); g.setColor(CREAM); g.drawString(Mgmt.levelName(S.level) + " · Level " + S.level, 84, 50);
            // time
            int x = 340; g.setFont(serif(24, true)); g.setColor(Color.WHITE); g.drawString(Util.hhmm(S.min), x, 34);
            g.setFont(sans(12, true)); g.setColor(CREAM); g.drawString(Sim.dateStr(), x + 78, 28); g.drawString(Data.SEASONS[Sim.season()] + " · " + new String[]{"Sunny", "Cloudy", "Rain", "Snow"}[S.weather], x + 78, 44);
            boolean open = Sim.isOpen(); g.setColor(open ? new Color(0x66ff99) : new Color(0xff7777)); g.setFont(sans(12, true)); g.drawString(open ? (S.lastOrders ? "LAST ORDERS" : "OPEN") : "CLOSED", x, 54);
            weatherIcon(g, x + 78 + 150, 30);
            // money & stats
            x = 620; g.setFont(serif(22, true)); g.setColor(S.money < 0 ? new Color(0xff7777) : new Color(0xf6d776)); g.drawString(Util.money(S.money), x, 34);
            g.setFont(sans(11, false)); g.setColor(CREAM); g.drawString("today " + (S.revToday - S.expToday >= 0 ? "+" : "") + Util.money0(S.revToday - S.expToday), x, 50);
            bar(g, x + 130, 14, "Rep", S.rep, new Color(0xc9973f)); bar(g, x + 130, 30, "Pop", S.pop, new Color(0x4c9a54)); bar(g, x + 130, 46, "Sat", S.sat, new Color(0x4a8fc9));
            
            for (int i = 0; i < speedBtns.size(); i++) speedBtns.get(i).tint(i == speed ? new Color(0xffd27a) : BRASS);
            if (newsPulse && (int) (Sim.rt * 2) % 2 == 0) { g.setColor(new Color(0xff4040)); g.fillOval(newsBtn.getX() + newsBtn.getWidth() - 8 + btns.getX(), 8, 12, 12); }
            g.dispose();
        }
        void bar(Graphics2D g, int x, int y, String l, double v, Color c) { g.setFont(sans(9, true)); g.setColor(CREAM); g.drawString(l, x, y + 9); g.setColor(new Color(0, 0, 0, 140)); g.fillRoundRect(x + 24, y, 80, 10, 8, 8); g.setColor(c); g.fillRoundRect(x + 24, y, (int) (80 * v / 100), 10, 8, 8); }
        void weatherIcon(Graphics2D g, int x, int y) {
            switch (S.weather) {
                case 0 -> { g.setColor(new Color(0xffd54f)); g.fillOval(x, y - 8, 16, 16); g.setStroke(new BasicStroke(2f)); for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; g.drawLine((int) (x + 8 + Math.cos(a) * 11), (int) (y + Math.sin(a) * 11), (int) (x + 8 + Math.cos(a) * 15), (int) (y + Math.sin(a) * 15)); } }
                case 1 -> { g.setColor(new Color(0xcfd8dc)); g.fillOval(x, y - 4, 18, 12); g.fillOval(x + 8, y - 8, 16, 14); }
                case 2 -> { g.setColor(new Color(0x90a4ae)); g.fillOval(x, y - 8, 18, 12); g.fillOval(x + 8, y - 12, 16, 14); g.setColor(new Color(0x64b5f6)); for (int i = 0; i < 3; i++) g.drawLine(x + 5 + i * 6, y + 8, x + 3 + i * 6, y + 14); }
                default -> { g.setColor(new Color(0xeceff1)); g.fillOval(x, y - 8, 18, 12); g.fillOval(x + 8, y - 12, 16, 14); g.setColor(Color.WHITE); for (int i = 0; i < 3; i++) g.fillOval(x + 4 + i * 7, y + 8 + (i % 2) * 3, 3, 3); }
            }
        }
    }

    // =====================================================================
    //  GAME CANVAS
    // =====================================================================
    final class GamePanel extends JPanel {
        double scale = 1, ox, oy; long last = System.nanoTime(); javax.swing.Timer timer; int frame;
        GamePanel() {
            setBackground(Color.BLACK); setFocusable(true); setPreferredSize(new Dimension(1000, 640));
            addMouseMotionListener(new MouseMotionAdapter() { public void mouseMoved(MouseEvent e) { hover(e.getX(), e.getY()); } public void mouseDragged(MouseEvent e) { hover(e.getX(), e.getY()); } });
            addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { requestFocusInWindow(); if (overlayOpen || S == null) return; click(e); } public void mouseExited(MouseEvent e) { Gfx.hoverId = ""; Gfx.hoverHot = ""; } });
            timer = new javax.swing.Timer(15, e -> tick()); timer.start();
        }
        double lx(int x) { return (x - ox) / scale; } double ly(int y) { return (y - oy) / scale; }
        Npc npcAt(double x, double y) { Npc best = null; double bd = 22; for (Npc n : S.npcs) if (n.inPub) { double d = Util.dist(x, y, n.x, n.y - 14); if (d < bd) { bd = d; best = n; } } return best; }
        void hover(int mx, int my) {
            if (S == null) return; double x = lx(mx), y = ly(my); Npc n = npcAt(x, y); Gfx.hoverId = n == null ? "" : n.id; Gfx.hoverHot = "";
            if (n == null) for (Gfx.Hot h : Gfx.HOTS) if (h.in(x, y) && !(h.id().equals("fire") && !S.upgrades.contains("fire")) && !(h.id().equals("fruit") && !S.upgrades.contains("fruit")) && !(h.id().equals("stage") && !S.upgrades.contains("karaoke"))) Gfx.hoverHot = h.id();
            setCursor(Cursor.getPredefinedCursor(n != null || !Gfx.hoverHot.isEmpty() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
        void click(MouseEvent e) {
            double x = lx(e.getX()), y = ly(e.getY());
            Npc n = npcAt(x, y);
            if (n != null) {
                Gfx.selId = n.id; side.card.show(n);
                if (e.getClickCount() >= 2 || SwingUtilities.isRightMouseButton(e)) Player.act(n.id, n.st.equals("waitBar") && n.order != null ? "serve" : "talk");
                else if (n.st.equals("waitBar") && n.order != null) Player.act(n.id, "serve");
                return;
            }
            for (Mess m : new ArrayList<>(S.messes)) if (Util.dist(x, y, m.x, m.y) < 18) { Player.walkTo(m.x, m.y + 14, () -> { S.messes.remove(m); S.clean = Math.min(100, S.clean + 2); Player.say("Mopped."); }); return; }
            if (S.pigeonHere && Util.dist(x, y, S.pgx, S.pgy) < 20) { Player.walkTo(S.pgx, S.pgy + 30, () -> { Player.say(Util.pick("Hello, " + S.pigeonName + ".", "No crisps for you.", "Shoo! ...please.")); Audio.sfx("coo"); }); return; }
            for (Gfx.Hot h : Gfx.HOTS) if (h.in(x, y)) { if (hotspot(h)) return; }
            for (Staff st : S.staff) if (st.present && Util.dist(x, y, st.x, st.y - 14) < 20) { toastMsg(st.name + " (" + st.role + "): skill " + st.skill + ", mood " + (int) st.mood + ". " + st.tags); return; }
            Player.walkTo(x, y, null);
        }
        boolean hotspot(Gfx.Hot h) {
            switch (h.id()) {
                case "tv" -> { open("Football", Screens.football()); return true; }
                case "notice" -> { open("Manage the pub", Screens.manage(4)); return true; }
                case "darts" -> { if (!S.upgrades.contains("darts")) return false; Player.walkTo(Nav.OCHE[0] - 20, Nav.OCHE[1], () -> open("Darts", Screens.pickOpponent("darts"))); return true; }
                case "pool" -> { Player.walkTo(Nav.POOL[4][0] - 10, Nav.POOL[4][1], () -> open("Pool", Screens.pickOpponent("pool"))); return true; }
                case "juke" -> { Player.walkTo(Nav.JUKE[0], Nav.JUKE[1], () -> open("Jukebox", Screens.jukebox())); return true; }
                case "bar" -> { Player.walkTo(Util.clamp(Player.x, 60, 320), 236, () -> serveNext()); return true; }
                case "kitchen" -> { Player.walkTo(Nav.HATCH[0], Nav.HATCH[1], () -> { Staff c = Sim.staffByRole("chef"); toastMsg(c == null ? "No chef today — no hot food. Hire one in Manage." : "Chef " + c.name + " is cooking. Meal stock: " + S.stock[8]); }); return true; }
                case "loos" -> { toastMsg(S.upgrades.contains("loos") ? "The loos are gleaming. For now." : "The loos: best left unexamined."); return true; }
                case "fruit" -> { if (!S.upgrades.contains("fruit")) return false; Player.walkTo(Nav.FRUIT[0], Nav.FRUIT[1], () -> { if (S.money < 1) return; S.money -= 1; Audio.sfx("coin"); if (Util.chance(.14)) { double w = Util.ri(4, 20); S.money += w; toastMsg("JACKPOT! The fruit machine pays " + Util.money0(w) + "!"); Audio.sfx("ach"); } else toastMsg("Spin... nothing. Typical."); }); return true; }
                case "stage" -> { if (!S.upgrades.contains("karaoke")) return false; toastMsg(S.karaokeDay == S.day ? "Karaoke is on tonight!" : "The karaoke stage. Book a night in Manage → Events."); return true; }
                case "door" -> { toastMsg(Sim.isOpen() ? "The front door. Customers keep coming through it." : "Locked. Quiet. Peaceful."); return true; }
                default -> { return false; }
            }
        }
        void serveNext() {
            Npc best = null; for (String id : S.queue) { Npc n = Sim.npc(id); if (n != null && n.inPub && n.st.equals("waitBar") && n.order != null) { best = n; break; } }
            if (best == null) { toastMsg("Nobody waiting at the bar."); return; }
            Ai.serve(best, null); Player.say("Next!");
        }
        void tick() {
            long now = System.nanoTime(); double dt = Math.min(.05, (now - last) / 1e9); last = now; frame++;
            if (S != null) {
                boolean ff = false;
                if (!overlayOpen) {
                    double sp = SPEEDS[speed];
                    if (sp > 0 && Sim.canFastForward()) { sp = 40; ff = true; }
                    if (sp > 0) Sim.advance(dt, sp);
                    int dx = 0, dy = 0; if (down.contains(KeyEvent.VK_A) || down.contains(KeyEvent.VK_LEFT)) dx--; if (down.contains(KeyEvent.VK_D) || down.contains(KeyEvent.VK_RIGHT)) dx++; if (down.contains(KeyEvent.VK_W) || down.contains(KeyEvent.VK_UP)) dy--; if (down.contains(KeyEvent.VK_S) || down.contains(KeyEvent.VK_DOWN)) dy++;
                    Player.update(dt, dx, dy);
                }
                Audio.crowd = Math.min(1, Sim.crowd() / 24.0) * (Sim.isOpen() || Sim.crowd() > 0 ? 1 : 0);
                if (flash > 0) flash = Math.max(0, flash - dt * 1.5);
                fastForward = ff;
                if (frame % 10 == 0) { hud.repaint(); side.refresh(); updateStatus(); }
            }
            repaint();
        }
        boolean fastForward;
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); UI.AA(g);
            g.setPaint(new GradientPaint(0, 0, new Color(0x2a1a12), 0, getHeight(), new Color(0x1a0f0a))); g.fillRect(0, 0, getWidth(), getHeight());
            if (S == null) { g.dispose(); return; }
            scale = Math.min(getWidth() / (double) Gfx.W, getHeight() / (double) Gfx.H); ox = (getWidth() - Gfx.W * scale) / 2; oy = (getHeight() - Gfx.H * scale) / 2;
            AffineTransform at = g.getTransform(); g.translate(ox, oy); g.scale(scale, scale); g.setClip(0, 0, Gfx.W, Gfx.H);
            Gfx.render(g, 0.016);
            if (flash > 0) { g.setColor(new Color(255, 230, 120, (int) (flash * 70))); g.fillRect(0, 0, Gfx.W, Gfx.H); }
            g.setClip(null); g.setTransform(at);
            // toasts
            int ty = 12; double now = Sim.rt;
            for (Iterator<String[]> it = toasts.iterator(); it.hasNext(); ) {
                String[] t = it.next(); double age = now - Double.parseDouble(t[1]); if (age > 6) { it.remove(); continue; }
                float a = (float) Math.min(1, Math.min(age * 4, (6 - age) * 2)); g.setFont(sans(14, true)); FontMetrics fm = g.getFontMetrics(); int w = fm.stringWidth(t[0]) + 28;
                g.setColor(new Color(0, 0, 0, (int) (170 * a))); g.fillRoundRect(getWidth() / 2 - w / 2, ty, w, 30, 18, 18); g.setColor(new Color(((Integer.parseInt(t[2]) >> 16) & 255), (Integer.parseInt(t[2]) >> 8) & 255, Integer.parseInt(t[2]) & 255, (int) (255 * a))); g.drawString(t[0], getWidth() / 2 - w / 2 + 14, ty + 20); ty += 36;
            }
            if (alertUntil > now) { String a = "⚠ " + alertText; g.setFont(sans(15, true)); int w = g.getFontMetrics().stringWidth(a) + 30; g.setColor(new Color(0xb71c1c)); g.fillRoundRect(getWidth() / 2 - w / 2, getHeight() - 54, w, 32, 16, 16); g.setColor(Color.WHITE); g.drawString(a, getWidth() / 2 - w / 2 + 15, getHeight() - 33); }
            if (speed == 0 && !overlayOpen) { g.setFont(serif(40, true)); g.setColor(new Color(255, 255, 255, 200)); String s = "PAUSED"; g.drawString(s, getWidth() / 2 - g.getFontMetrics().stringWidth(s) / 2, getHeight() / 2); }
            if (fastForward) { g.setFont(sans(14, true)); g.setColor(new Color(255, 230, 160)); g.drawString("» Pub's closed — time passes quickly » " + Util.hhmm(S.min), 16, getHeight() - 14); }
            if (!tickerText.isEmpty() && S.match != null && !S.match.phase.equals("ft")) { g.setFont(sans(12, false)); g.setColor(new Color(220, 255, 220, 220)); g.drawString("🎙 " + tickerText, 16, getHeight() - 14); }
            g.dispose();
        }
    }

    void updateStatus() {
        if (S == null) return;
        String s = Sim.isOpen() ? "Open · " + Sim.crowd() + "/" + Sim.capacity() + " in the pub · " + S.queue.size() + " waiting at the bar" : "Closed";
        if (S.match != null) s += " · ⚽ " + Football.score(S.match) + " (" + (S.match.phase.equals("ft") ? "FT" : S.match.phase.equals("ht") ? "HT" : S.match.minute + "'") + ")";
        if (S.song != null && !S.song.isEmpty()) s += " · ♪ " + S.song;
        if (S.flags.containsKey("argue")) s += " · ⚠ ARGUMENT BREWING (select the pair and click Calm / Join)";
        if (S.powerCut) s += " · 🕯 POWER CUT";
        status.setText(s);
    }

    // =====================================================================
    //  SIDE PANEL
    // =====================================================================
    final class Side extends JPanel {
        final Card card = new Card(); final JTextPane feed = new JTextPane(); final StyledDocument doc = feed.getStyledDocument(); final JPanel regs = new JPanel(); JTabbedPane tabs;
        final Map<String, Color> cols = new HashMap<>();
        long lastRegs;
        Side() {
            super(new BorderLayout(0, 6)); setPreferredSize(new Dimension(380, 100)); setOpaque(false); setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 8));
            cols.put("sys", new Color(0xf2e6c8)); cols.put("arrive", new Color(0x9c8f72)); cols.put("drama", new Color(0xff8a65)); cols.put("event", new Color(0xffd54f)); cols.put("match", new Color(0x81c784)); cols.put("story", new Color(0xf48fb1));
            cols.put("music", new Color(0x90caf9)); cols.put("game", new Color(0xb0a58a)); cols.put("staff", new Color(0xd7b98e)); cols.put("ach", new Color(0xffd54f)); cols.put("player", new Color(0xffffff)); cols.put("news", new Color(0xcfd8dc)); cols.put("chat", new Color(0xc8c2aa));
            add(card, BorderLayout.NORTH);
            feed.setEditable(false); feed.setOpaque(true); feed.setBackground(new Color(0x1c2622)); feed.setFont(sans(12, false)); feed.setForeground(CREAM);
            JScrollPane sp = new JScrollPane(feed); sp.setBorder(BorderFactory.createLineBorder(BRASS, 1)); sp.getVerticalScrollBar().setUnitIncrement(14);
            JPanel box = new WoodPanel(new BorderLayout()); box.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4)); JLabel hd = new JLabel("  PUB LIFE"); hd.setFont(serif(13, true)); hd.setForeground(BRASS2); box.add(hd, BorderLayout.NORTH); box.add(sp);
            add(box, BorderLayout.CENTER);
        }
        void clear() { try { doc.remove(0, doc.getLength()); } catch (Exception e) { } }
        void addChat(Npc n, String text) { append(Sim.first(n) + ": " + text, "chat", n.name + ": "); }
        void addLine(String text, String cat) { append(Util.hhmm(S == null ? 0 : S.min) + "  " + text, cat, null); }
        void append(String text, String cat, String bold) {
            SwingUtilities.invokeLater(() -> {
                try {
                    SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setForeground(a, cols.getOrDefault(cat, CREAM)); StyleConstants.setFontFamily(a, "Segoe UI"); StyleConstants.setFontSize(a, 12);
                    if (cat.equals("ach") || cat.equals("event") || cat.equals("drama") || cat.equals("story")) StyleConstants.setBold(a, true);
                    doc.insertString(doc.getLength(), text + "\n", a);
                    if (doc.getLength() > 14000) { String t = doc.getText(0, 4000); int cut = t.lastIndexOf('\n') + 1; doc.remove(0, Math.max(cut, 1)); }
                    feed.setCaretPosition(doc.getLength());
                } catch (BadLocationException e) { }
            });
        }
        void refresh() { card.update(); }
    }

    final class Card extends JPanel {
        Npc cur; final JLabel name = new JLabel("Nobody selected"), sub = new JLabel(" "), where = new JLabel(" "), mem = new JLabel(" "), rel = new JLabel(" ");
        final Bar mood = new Bar(new Color(0xe8b64a)), trust = new Bar(new Color(0x4c9a54)), drunk = new Bar(new Color(0xb05a8a)), anger = new Bar(new Color(0xc0392b)); final JPanel buttons = new JPanel(new GridLayout(0, 3, 4, 4));
        final Map<String, BrassButton> bmap = new LinkedHashMap<>();
        Card() {
            super(new BorderLayout(0, 4)); setOpaque(false); setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRASS, 1), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            setBackground(WOOD2); setOpaque(true);
            JPanel top = new JPanel(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS)); top.setOpaque(false);
            name.setFont(serif(18, true)); name.setForeground(BRASS2); sub.setForeground(CREAM); sub.setFont(sans(11, false)); where.setForeground(new Color(0xa5d6a7)); where.setFont(sans(11, true));
            top.add(name); top.add(sub); top.add(where); top.add(Box.createVerticalStrut(4));
            JPanel bars = new JPanel(new GridLayout(4, 2, 6, 2)); bars.setOpaque(false);
            for (Object[] x : new Object[][]{{"Mood", mood}, {"Trust in you", trust}, {"Drunk", drunk}, {"Anger", anger}}) { JLabel l = new JLabel((String) x[0]); l.setForeground(CREAM); l.setFont(sans(10, false)); JPanel row = new JPanel(new BorderLayout(4, 0)); row.setOpaque(false); l.setPreferredSize(new Dimension(70, 12)); row.add(l, BorderLayout.WEST); row.add((Component) x[1]); bars.add(row); }
            // 4 rows in 2 columns
            top.add(bars);
            mem.setForeground(new Color(0xd9c9a0)); mem.setFont(sans(11, false)); rel.setForeground(new Color(0xd9c9a0)); rel.setFont(sans(11, false));
            top.add(Box.createVerticalStrut(4)); top.add(rel); top.add(mem);
            add(top, BorderLayout.CENTER);
            String[][] acts = {{"Talk", "talk"}, {"Serve", "serve"}, {"Buy drink", "buy"}, {"Gossip", "gossip"}, {"Join chat", "join"}, {"Calm row", "calm"}, {"Darts", "darts"}, {"Pool", "pool"}, {"Apologise", "apologise"}, {"Throw out", "leave"}, {"Book", "book"}};
            for (String[] a : acts) { BrassButton b = btn(a[0], () -> { if (cur == null) return; if (a[1].equals("book")) { open("Regulars Book", Screens.book()); return; } if (!overlayOpen) Player.act(cur.id, a[1]); }).small(); bmap.put(a[1], b); buttons.add(b); }
            buttons.setOpaque(false); add(buttons, BorderLayout.SOUTH);
            show(null);
        }
        void show(Npc n) { cur = n; update(); }
        void update() {
            if (S == null) return;
            if (cur != null && !S.npcs.contains(cur)) cur = null;
            if (cur == null) { name.setText("Click someone"); sub.setText("Select a customer to see how they feel."); where.setText("Amber ! = waiting to be served · red !! = angry"); mem.setText(" "); rel.setText(" "); for (BrassButton b : bmap.values()) b.setEnabled(false); mood.set(0, ""); trust.set(0, ""); drunk.set(0, ""); anger.set(0, ""); return; }
            Npc n = cur;
            name.setText(n.name); sub.setText(n.age + " · " + n.job + (Data.ti(n.team) >= 0 ? " · " + Data.TEAM_SHORT[Data.ti(n.team)] : "") + " · " + Mgmt.regLabel(n) + " · " + (n.favName.isEmpty() ? Data.DNAME[Data.di(n.fav)] : n.favName));
            where.setText((n.inPub ? "● " : "○ ") + UI.statusLine(n) + " — " + UI.moodWord(n) + (n.defected ? " (defected!)" : ""));
            mood.set(n.mood, (int) n.mood + ""); trust.set((n.trust + 100) / 2, (int) n.trust + ""); drunk.set(n.drunk, (int) n.drunk + ""); anger.set(n.anger, (int) n.anger + "");
            List<String> fr = new ArrayList<>(), rv = new ArrayList<>(); for (Npc o : S.npcs) if (o != n) { double r = Sim.rel(n.id, o.id); if (S.couples.contains(Sim.pair(n.id, o.id))) fr.add("♥" + Sim.first(o)); else if (r >= 35) fr.add(Sim.first(o)); else if (r <= -35) rv.add(Sim.first(o)); }
            rel.setText("<html><body style='width:330px'><b>Friends:</b> " + (fr.isEmpty() ? "—" : String.join(", ", fr)) + " &nbsp; <b>Rivals:</b> " + (rv.isEmpty() ? "—" : String.join(", ", rv)) + "</body></html>");
            StringBuilder sb = new StringBuilder("<html><body style='width:330px'><b>Remembers:</b> "); int c = 0; for (Mem m : n.mem) { if (c++ >= 2) break; sb.append(esc(m.t)).append(" (").append(Sim.dayName(m.day).substring(0, 3)).append("); "); } if (n.mem.isEmpty()) sb.append("nothing yet"); sb.append("</body></html>"); mem.setText(sb.toString());
            boolean here = n.inPub;
            for (Map.Entry<String, BrassButton> e : bmap.entrySet()) {
                boolean en = here; switch (e.getKey()) { case "serve" -> en = here && n.st.equals("waitBar") && n.order != null; case "join" -> en = here && n.conv != null && !n.conv.isEmpty(); case "calm" -> en = here && Social.convOf(n) != null && Social.convOf(n).flagged; case "darts" -> en = here && S.upgrades.contains("darts"); case "pool" -> en = here && S.upgrades.contains("pool"); case "book" -> en = true; default -> {} }
                e.getValue().setEnabled(en);
            }
        }
    }
}
