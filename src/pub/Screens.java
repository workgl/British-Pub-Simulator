package pub;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import pub.Model.*;
import static pub.UI.*;
import static pub.Sim.S;

/** Management and information screens shown as overlays. */
public final class Screens {
    private Screens() {}

    static JLabel ink(String t, float size, boolean bold) { JLabel l = new JLabel(t); l.setFont(sans(size, bold)); l.setForeground(INK); return l; }
    static JLabel inkSerif(String t, float size) { JLabel l = new JLabel(t); l.setFont(serif(size, true)); l.setForeground(INK); return l; }
    static JPanel hrow(Component... cs) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2)); p.setOpaque(false); for (Component c : cs) p.add(c); return p; }
    static Bar bar(String label, double v, Color c) { Bar b = new Bar(c); b.set(v, label + " " + (int) v); b.setPreferredSize(new Dimension(300, 16)); b.setMaximumSize(new Dimension(300, 16)); b.setAlignmentX(Component.LEFT_ALIGNMENT); return b; }
    static JPanel padded(JComponent c, int pad) { JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad)); p.add(c); return p; }
    static JPanel card(JComponent... cs) { JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false); for (JComponent c : cs) { c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); } return p; }

    /** A tab bar over a card area. The builders are re-run when the tab is selected or refresh() is called. */
    static final class Tabs extends JPanel {
        final JPanel content = new JPanel(new BorderLayout()); final List<JToggleButton> btns = new ArrayList<>(); final List<Supplier<JComponent>> builders = new ArrayList<>(); int cur;
        Tabs(String[] names, List<Supplier<JComponent>> b, int start) {
            super(new BorderLayout(0, 8)); setOpaque(false); builders.addAll(b); content.setOpaque(false);
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); bar.setOpaque(false); ButtonGroup bg = new ButtonGroup();
            for (int i = 0; i < names.length; i++) {
                final int k = i; JToggleButton t = new JToggleButton(names[i]) {
                    @Override protected void paintComponent(Graphics g0) { Graphics2D g = (Graphics2D) g0.create(); AA(g); g.setColor(isSelected() ? INK : new Color(0x8a6a3a)); g.fillRoundRect(0, 0, getWidth(), getHeight() + 8, 10, 10); g.setFont(sans(13, true)); g.setColor(isSelected() ? BRASS2 : PAPER); FontMetrics fm = g.getFontMetrics(); g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent()) / 2 - 3); g.dispose(); }
                };
                t.setContentAreaFilled(false); t.setBorderPainted(false); t.setFocusPainted(false); t.setPreferredSize(new Dimension(Math.max(70, names[i].length() * 9 + 22), 30)); t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); t.setFocusable(false);
                t.addActionListener(e -> select(k)); bg.add(t); btns.add(t); bar.add(t);
            }
            add(bar, BorderLayout.NORTH); add(content, BorderLayout.CENTER); btns.get(start).setSelected(true); select(start);
        }
        void select(int i) { cur = i; content.removeAll(); content.add(builders.get(i).get()); content.revalidate(); content.repaint(); }
        void refresh() { select(cur); }
    }

    // ================================================================
    //  MANAGE
    // ================================================================
    public static JComponent manage(int start) {
        final Tabs[] ref = new Tabs[1];
        Runnable rf = () -> { ref[0].refresh(); GameWindow.I.hud.repaint(); };
        List<Supplier<JComponent>> b = List.of(() -> overview(), () -> stock(rf), () -> staff(rf), () -> upgrades(rf), () -> eventsTab(rf), () -> hoursTab(rf));
        ref[0] = new Tabs(new String[]{"Overview", "Stock & Prices", "Staff", "Upgrades", "Events & Ads", "Opening Hours"}, b, start);
        ref[0].setPreferredSize(new Dimension(900, 560));
        return ref[0];
    }

    static JComponent overview() {
        double profit = S.revToday - S.expToday;
        JPanel left = card(inkSerif("The Books", 20), Box.createVerticalStrut(6) instanceof JComponent c ? c : new JLabel(),
            ink("Cash in hand: " + Util.money(S.money), 16, true), ink("Taken today: " + Util.money(S.revToday) + "   Spent today: " + Util.money(S.expToday), 13, false),
            ink("Last night: " + (S.lastNightSummary.isEmpty() ? "—" : S.lastNightSummary), 12, false), new JLabel(" "),
            bar("Reputation", S.rep, new Color(0xc9973f)), Box.createVerticalStrut(4) instanceof JComponent c2 ? c2 : new JLabel(), bar("Popularity", S.pop, new Color(0x4c9a54)), bar("Customer satisfaction", S.sat, new Color(0x4a8fc9)), bar("Cleanliness", S.clean, new Color(0x7ec8c8)), bar("Ambience", Mgmt.ambience(), new Color(0xb05a8a)));
        left.add(new JLabel(" ")); left.add(inkSerif(Mgmt.levelName(S.level) + " (Level " + S.level + ")", 16));
        String nxt = S.level >= 5 ? "You've reached the top. Local institution!" : S.level == 1 ? "Level 2 needs: reputation 30 and 8 days open." : S.level == 2 ? "Level 3 needs: reputation 48 and 30 days." : S.level == 3 ? "Level 4 needs: reputation 68 and 80 days." : "Level 5 needs: reputation 85 and 160 days.";
        left.add(ink(nxt, 12, false));
        left.add(ink("Day " + S.day + " · Days open: " + S.daysOpen + " · Pints served: " + Sim.stat0("pints") + " · Regulars: " + (int) S.npcs.stream().filter(n -> !n.stranger).count(), 12, false));
        JComponent chart = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create(); AA(g); int w = getWidth(), h = getHeight();
                g.setColor(new Color(0, 0, 0, 25)); g.fillRoundRect(0, 0, w, h, 12, 12); g.setColor(INK); g.setFont(sans(12, true)); g.drawString("Last 14 days — takings vs costs", 10, 18);
                List<DayRec> hs = S.history.subList(Math.max(0, S.history.size() - 14), S.history.size()); double mx = 100; for (DayRec r : hs) mx = Math.max(mx, Math.max(r.rev, r.exp));
                int bw = Math.max(8, (w - 30) / Math.max(14, hs.size()) / 2 - 2);
                for (int i = 0; i < hs.size(); i++) { DayRec r = hs.get(i); int x = 14 + i * ((w - 30) / 14); int hh1 = (int) ((h - 50) * r.rev / mx), hh2 = (int) ((h - 50) * r.exp / mx);
                    g.setColor(new Color(0x4c9a54)); g.fillRect(x, h - 22 - hh1, bw, hh1); g.setColor(new Color(0xb5473a)); g.fillRect(x + bw + 1, h - 22 - hh2, bw, hh2);
                    g.setColor(INK); g.setFont(sans(9, false)); g.drawString(Data.DAYS[Sim.dow(r.day)].substring(0, 2), x, h - 8); }
                if (hs.isEmpty()) { g.setFont(sans(12, false)); g.drawString("Nothing yet — play a day!", 20, h / 2); }
                g.dispose();
            }
        };
        chart.setPreferredSize(new Dimension(420, 200));
        JPanel p = new JPanel(new BorderLayout(14, 10)); p.setOpaque(false); p.add(left, BorderLayout.WEST);
        JPanel r = card(chart, new JLabel(" "), inkSerif("Tips", 15), ink("<html><body style='width:360px'>• Weekends and match days are your cash cows.<br>• Keep the stock up — dry taps mean angry regulars.<br>• Click people, remember their grudges, and apologise.<br>• Check the Regulars Book for rivalries and romances.</body></html>", 12, false)); p.add(r, BorderLayout.CENTER);
        return p;
    }

    static JComponent stock(Runnable rf) {
        JPanel list = vbox();
        JPanel head = hrow(ink("Product", 12, true), Box.createHorizontalStrut(110), ink("Stock", 12, true), Box.createHorizontalStrut(30), ink("Price", 12, true)); list.add(head);
        for (int i = 0; i < Data.DRINKS.length; i++) {
            final int k = i; String nm = Data.DNAME[i] + (i == 8 ? " (ingredients)" : "");
            JLabel n = ink(nm, 13, true); n.setPreferredSize(new Dimension(150, 24));
            int pend = 0; for (Delivery d : S.deliveries) if (d.item == k) pend += d.qty;
            JLabel st = ink(S.stock[i] + (pend > 0 ? " (+" + pend + ")" : ""), 13, S.stock[i] < 15); st.setForeground(S.stock[i] < 15 ? new Color(0xa02020) : INK); st.setPreferredSize(new Dimension(90, 24));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(Math.round(S.price[i] * 10) / 10.0, 0.5, 30.0, 0.1)); sp.setPreferredSize(new Dimension(80, 24));
            sp.addChangeListener(e -> { S.price[k] = ((Number) sp.getValue()).doubleValue(); });
            JLabel mg = ink(String.format("cost %s · fair %s", Util.money(Mgmt.unitCost(i)), Util.money(Data.DPRICE[i])), 11, false); mg.setPreferredSize(new Dimension(160, 24));
            JPanel row = hrow(n, st, sp, mg);
            for (int q : new int[]{10, 25, 50}) row.add(sbtn("+" + q + " (" + Util.money0(Mgmt.unitCost(i) * q) + ")", () -> { if (Mgmt.order(k, q)) rf.run(); }));
            list.add(row);
        }
        list.add(new JLabel(" ")); list.add(ink("<html><body style='width:760px'>Orders placed before 17:00 arrive at 08:00 tomorrow (a day later if you order late). Prices far above the 'fair' price make customers grumble, walk out or defect to the Frog & Trumpet. Cheap pints make friends — and lose money.</body></html>", 12, false));
        return scroll(list);
    }

    static JComponent staff(Runnable rf) {
        JPanel root = new JPanel(new GridLayout(1, 2, 12, 0)); root.setOpaque(false);
        JPanel cur = vbox(); cur.add(inkSerif("Your staff (" + S.staff.size() + "/8)", 17));
        if (S.staff.isEmpty()) cur.add(ink("Nobody. It's just you and the pigeon.", 12, false));
        for (Staff s : S.staff) {
            JPanel c = card(ink("<html><b>" + esc(s.name) + "</b> — " + s.role + (s.present ? "" : " <i>(off today)</i>") + "</html>", 13, false),
                ink("Skill " + s.skill + " · Speed " + s.speed + " · Reliability " + s.rely + " · Charm " + s.charm, 11, false), ink("Wage " + Util.money0(s.wage) + "/day · mood " + (int) s.mood + " · mistakes " + s.mistakes, 11, false), ink("“" + esc(s.tags) + "”", 11, false));
            JPanel br = hrow(sbtn("Give raise +£5", () -> { s.wage += 5; s.mood = Math.min(100, s.mood + 10); rf.run(); }), sbtn("Sack", () -> { Mgmt.fire(s); rf.run(); }));
            c.add(br); c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x8a6a3a)), BorderFactory.createEmptyBorder(4, 0, 6, 0))); cur.add(c);
        }
        JPanel app = vbox(); app.add(inkSerif("Applicants this week", 17));
        for (Staff s : new ArrayList<>(S.applicants)) {
            JPanel c = card(ink("<html><b>" + esc(s.name) + "</b> — wants to be a <b>" + s.role + "</b></html>", 13, false), ink("Skill " + s.skill + " · Speed " + s.speed + " · Reliability " + s.rely + " · Charm " + s.charm, 11, false), ink("“" + esc(s.tags) + "” — asks " + Util.money0(s.wage) + "/day", 11, false));
            c.add(hrow(sbtn("Hire", () -> { if (!Mgmt.hireApplicant(s)) Sim.toast("Too many staff."); rf.run(); }))); c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x8a6a3a)), BorderFactory.createEmptyBorder(4, 0, 6, 0))); app.add(c);
        }
        app.add(ink("<html><body style='width:380px'>Bartenders serve drinks. Chefs make meals (needed for food). Cleaners fight mess. Waiters speed up food. Managers defuse arguments. Some customers ask you for a job in conversation — they might be brilliant.</body></html>", 11, false));
        root.add(scroll(cur)); root.add(scroll(app));
        return root;
    }

    static JComponent upgrades(Runnable rf) {
        JPanel list = vbox(); list.add(inkSerif("Facilities & Equipment", 17));
        for (Mgmt.Up u : Mgmt.UPS) {
            boolean has = S.upgrades.contains(u.id()); boolean locked = S.level < u.lvl();
            JPanel row = hrow(); JLabel nm = ink("<html><b>" + esc(u.name()) + "</b> — " + esc(u.desc()) + "</html>", 12, false); nm.setPreferredSize(new Dimension(520, 34)); row.add(nm);
            if (has) row.add(ink("Installed ✔", 12, true)); else row.add(sbtn(locked ? "Level " + u.lvl() : "Buy " + Util.money0(u.cost()), () -> { if (Mgmt.buyUp(u)) rf.run(); }));
            list.add(row);
        }
        list.add(new JLabel(" ")); list.add(inkSerif("Decoration", 17));
        for (String[] d : Mgmt.DECOR) { boolean has = S.decor.contains(d[0]); JPanel row = hrow(); JLabel l = ink(d[1] + " (ambience +4)", 12, false); l.setPreferredSize(new Dimension(300, 24)); row.add(l); if (has) row.add(ink("Hung ✔", 12, true)); else row.add(sbtn("Buy £" + d[2], () -> { if (Mgmt.buyDecor(d[0], Integer.parseInt(d[2]))) rf.run(); else Sim.toast("Can't afford it."); })); list.add(row); }
        JPanel wall = hrow(ink("Wallpaper:", 12, true));
        for (int i = 0; i < Mgmt.WALLS.length; i++) { final int k = i; wall.add(sbtn(Mgmt.WALLS[i] + (Integer.parseInt(S.flags.getOrDefault("wall", "0")) == i ? " ✔" : " (£60)"), () -> { if (Integer.parseInt(S.flags.getOrDefault("wall", "0")) != k && S.money >= 60) { Sim.spend(60); S.flags.put("wall", "" + k); rf.run(); } })); }
        list.add(wall);
        return scroll(list);
    }

    static JComponent eventsTab(Runnable rf) {
        JPanel root = new JPanel(new GridLayout(1, 2, 14, 0)); root.setOpaque(false);
        JPanel l = vbox(); l.add(inkSerif("Regular nights", 17));
        JCheckBox quiz = new JCheckBox("Weekly Pub Quiz (Wednesday 20:00)", S.quizOn); quiz.setOpaque(false); quiz.setFont(sans(13, false)); quiz.addActionListener(e -> S.quizOn = quiz.isSelected()); l.add(quiz);
        JCheckBox roast = new JCheckBox("Sunday Roast (needs a chef)", S.roastOn); roast.setOpaque(false); roast.setFont(sans(13, false)); roast.addActionListener(e -> S.roastOn = roast.isSelected()); l.add(roast);
        JCheckBox sports = new JCheckBox("Sports TV package (£60/week; no package = no football on the telly)", S.sports); sports.setOpaque(false); sports.setFont(sans(13, false)); sports.addActionListener(e -> { S.sports = sports.isSelected(); if (!S.sports) S.upgrades.remove("tv"); else S.upgrades.add("tv"); rf.run(); }); l.add(sports);
        l.add(new JLabel(" ")); l.add(inkSerif("Book a special night", 17));
        JComboBox<String> type = new JComboBox<>(new String[]{"karaoke (£80, needs machine)", "band (£150)", "beerfest (£300)", "charity (£50)"});
        String[] days = new String[7]; int[] dnum = new int[7]; for (int i = 0; i < 7; i++) { dnum[i] = S.day + i; days[i] = (i == 0 ? "Today" : i == 1 ? "Tomorrow" : Sim.dayName(S.day + i)) + " (" + Sim.dayName(S.day + i).substring(0, 3) + ")"; }
        JComboBox<String> day = new JComboBox<>(days);
        l.add(hrow(type, day, sbtn("Book it", () -> { String[] ty = {"karaoke", "band", "beerfest", "charity"}; int[] cost = {80, 150, 300, 50}; int ti = type.getSelectedIndex(); if (ty[ti].equals("karaoke") && !S.upgrades.contains("karaoke")) { Sim.toast("Buy a karaoke machine first."); return; } if (Mgmt.book(ty[ti], dnum[day.getSelectedIndex()], cost[ti])) rf.run(); })));
        l.add(new JLabel(" ")); l.add(inkSerif("Booked", 15));
        if (S.bookings.isEmpty()) l.add(ink("Nothing booked.", 12, false));
        for (Booking b : new ArrayList<>(S.bookings)) l.add(hrow(ink(Sim.dayName(b.day) + " (day " + b.day + "): " + b.type + " night", 12, false), sbtn("Cancel", () -> { S.bookings.remove(b); S.money += 30; rf.run(); })));
        JPanel r = vbox(); r.add(inkSerif("Advertising", 17));
        for (Mgmt.Ad a : Mgmt.ADS) {
            boolean active = S.ads.getOrDefault(a.id(), -1) >= S.day;
            r.add(card(ink("<html><b>" + esc(a.name()) + "</b> — " + esc(a.desc()) + "<br>+" + a.boost() + " popularity for " + a.days() + " days" + (active ? " <font color='#206020'>(ACTIVE until day " + S.ads.get(a.id()) + ")</font>" : "") + "</html>", 12, false), hrow(sbtn("Buy " + Util.money0(a.cost()), () -> { if (Mgmt.buyAd(a)) rf.run(); }))));
        }
        root.add(scroll(l)); root.add(scroll(r));
        return root;
    }

    static JComponent hoursTab(Runnable rf) {
        JPanel p = vbox(); p.add(inkSerif("Opening hours", 18));
        JSpinner open = new JSpinner(new SpinnerNumberModel(S.openH, 9, 16, 1)), close = new JSpinner(new SpinnerNumberModel(S.closeH, 20, 26, 1));
        open.addChangeListener(e -> { S.openH = (Integer) open.getValue(); rf.run(); }); close.addChangeListener(e -> { S.closeH = (Integer) close.getValue(); rf.run(); });
        p.add(hrow(ink("Open at (hour):", 13, true), open, ink("   Close at (hour, 24 = midnight, 26 = 2am):", 13, true), close));
        double extra = Math.max(0, (S.closeH - S.openH) - 12) * 20;
        p.add(ink("Currently open " + S.openH + ":00 to " + Util.hhmm(S.closeH * 60) + ". Overtime cost per day: " + Util.money0(extra), 13, false));
        p.add(new JLabel(" ")); p.add(ink("<html><body style='width:700px'>Longer hours catch the late crowd and Friday/Saturday spill-over, but every hour beyond 12 costs £20 in staff overtime and electricity. Licensing is strict: nobody stays after closing without an argument. Changes take effect tomorrow's opening.</body></html>", 12, false));
        return p;
    }

    // ================================================================
    //  CHRONICLE
    // ================================================================
    static int issueDay;
    public static JComponent chronicle() {
        issueDay = S.day;
        JPanel root = new JPanel(new BorderLayout(0, 6)); root.setOpaque(false);
        JEditorPane pane = new JEditorPane(); pane.setContentType("text/html"); pane.setEditable(false); pane.setOpaque(false);
        JScrollPane sc = scroll(pane); sc.setPreferredSize(new Dimension(780, 520)); sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Runnable[] show = new Runnable[1];
        show[0] = () -> { pane.setText(newspaperHtml(issueDay)); pane.setCaretPosition(0); };
        JPanel nav = hrow(sbtn("< Older edition", () -> { for (int d = issueDay - 1; d >= 1; d--) if (hasIssue(d)) { issueDay = d; show[0].run(); return; } }), sbtn("Newer edition >", () -> { for (int d = issueDay + 1; d <= S.day; d++) if (hasIssue(d)) { issueDay = d; show[0].run(); return; } }));
        show[0].run(); root.add(sc); root.add(nav, BorderLayout.SOUTH); return root;
    }
    static boolean hasIssue(int d) { for (News n : S.archive) if (n.day == d) return true; return false; }
    static String newspaperHtml(int day) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:Georgia,serif;color:#2b2118;margin:4px'>");
        sb.append("<div style='text-align:center;border-bottom:4px double #2b2118;padding-bottom:4px'><span style='font-size:28px;font-weight:bold'>THE WESTBRIDGE CHRONICLE</span><br><span style='font-size:12px'>Est. 1897 · Price: 80p · ").append(Sim.dayName(day)).append(" ").append((day - 1) % 28 + 1).append(" ").append(Data.MONTHS[((day - 1) / 28) % 12]).append(" · \"Serving Westbridge and slightly beyond\"</span></div>");
        List<News> items = new ArrayList<>(); for (News n : S.archive) if (n.day == day) items.add(n);
        // pub items first
        items.sort((a, b) -> Integer.compare(score(b), score(a)));
        if (items.isEmpty()) sb.append("<p style='font-size:16px'>No edition today — the printers are on strike (again).</p>");
        int i = 0; for (News n : items) {
            if (i == 0) sb.append("<h1 style='font-size:26px;margin-bottom:2px'>").append(esc(n.head)).append("</h1><p style='font-size:14px;margin-top:0'>").append(esc(n.body)).append("</p><hr>");
            else sb.append("<h3 style='font-size:17px;margin-bottom:1px'>").append(esc(n.head)).append("</h3><p style='font-size:13px;margin-top:0'>").append(esc(n.body)).append("</p>");
            i++;
        }
        Npc linda = Sim.npc("linda"); sb.append("<hr><p style='font-size:12px'><i>OVERHEARD IN WESTBRIDGE:</i> “").append(esc(linda != null && linda.secretKnown ? "The column is, apparently, by someone we all know." : "Whoever took Brenda's gnome — we know where you drink.")).append("”</p>");
        sb.append("<p style='font-size:12px'>WEATHER: ").append(new String[]{"Sunny spells", "Cloudy", "Rain, naturally", "Snow showers"}[S.weather]).append(" · FOOTBALL: Rovers P").append(S.rovPlayed).append(" W").append(S.rovW).append(" D").append(S.rovD).append(" L").append(S.rovL).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
    static int score(News n) { String h = n.head; int s = 0; if (h.contains("PUB") || h.contains("SPECKLED") || h.contains("PIGEON")) s += 3; if (h.contains("ROVERS")) s += 2; if (n.day == S.day) s += 0; return s + (h.hashCode() & 1); }

    // ================================================================
    //  TOWN
    // ================================================================
    public static JComponent town() {
        JPanel root = new JPanel(new BorderLayout(0, 6)); root.setOpaque(false);
        JLabel info = ink(" ", 13, true);
        final String[] hover = {""};
        JComponent map = new JComponent() {
            @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); Gfx.drawTown(g2, getWidth(), getHeight(), hover[0]); g2.dispose(); }
        };
        map.setPreferredSize(new Dimension(960, 520));
        map.addMouseMotionListener(new MouseMotionAdapter() { public void mouseMoved(MouseEvent e) { String h = ""; for (Gfx.Loc l : Gfx.TOWN) if (e.getX() >= l.x() && e.getX() <= l.x() + l.w() && e.getY() >= l.y() && e.getY() <= l.y() + l.h()) h = l.id();
            if (!h.equals(hover[0])) { hover[0] = h; StringBuilder sb = new StringBuilder(); for (Npc n : S.npcs) if ((n.inPub ? "pub" : n.loc).equals(h)) sb.append(n.name).append(", "); String nm = ""; for (Gfx.Loc l : Gfx.TOWN) if (l.id().equals(h)) nm = l.name(); info.setText(h.isEmpty() ? "Hover over a building to see who's there. Westbridge: population ~ 31,000 (and one pigeon)." : nm + ": " + (sb.length() == 0 ? "nobody you know" : sb.substring(0, sb.length() - 2))); map.repaint(); } } });
        root.add(map); root.add(padded(info, 4), BorderLayout.SOUTH);
        return root;
    }

    // ================================================================
    //  JUKEBOX
    // ================================================================
    public static JComponent jukebox() {
        JPanel root = new JPanel(new BorderLayout(0, 8)); root.setOpaque(false);
        JLabel now = inkSerif(Mgmt.songPlaying() ? "♪ " + S.song : "Silence. Wonderful, terrible silence.", 16); root.add(now, BorderLayout.NORTH);
        JPanel list = vbox();
        for (int i = 0; i < Data.SONGS.length; i++) {
            final int k = i; String[] s = Data.SONGS[i]; int likes = 0, dislikes = 0; for (Npc n : S.npcs) if (n.inPub) { double a = n.music.getOrDefault(s[2], 0.0); if (a > .5) likes++; else if (a < -.5) dislikes++; }
            JPanel row = hrow(); JLabel l = ink("<html><b>" + esc(s[0]) + "</b> — " + esc(s[1]) + " <i>(" + s[2] + ")</i> &nbsp; in the pub now: 👍 " + likes + " &nbsp; 👎 " + dislikes + "</html>", 12, false); l.setPreferredSize(new Dimension(560, 26)); row.add(l);
            row.add(sbtn("Play", () -> { Mgmt.playSong(k, null); for (Npc n : S.npcs) if (n.inPub && Util.chance(.25)) { double a = n.music.getOrDefault(s[2], 0.0); if (Math.abs(a) > .45) Sim.say(n, a > 0 ? Util.pick("Ooh, good choice, landlord!", "Yes! Turn it up!") : Util.pick("Landlord, WHY?", "Is this a joke?")); } now.setText("♪ " + S.song); }));
            list.add(row);
        }
        list.add(sbtn("Stop the music", () -> { S.songUntil = 0; S.song = ""; S.playingGenre = ""; Audio.stopSong(); now.setText("Silence."); }));
        root.add(scroll(list)); root.setPreferredSize(new Dimension(780, 440)); return root;
    }

    // ================================================================
    //  REGULARS BOOK
    // ================================================================
    public static JComponent book() {
        JPanel root = new JPanel(new BorderLayout(10, 0)); root.setOpaque(false);
        DefaultListModel<Npc> model = new DefaultListModel<>(); List<Npc> sorted = new ArrayList<>(S.npcs); sorted.sort(Comparator.comparing((Npc n) -> n.stranger).thenComparing(n -> n.name)); for (Npc n : sorted) model.addElement(n);
        JList<Npc> list = new JList<>(model); list.setOpaque(false); list.setFont(sans(13, false));
        list.setCellRenderer((l, n, i, sel, f) -> { JLabel lb = new JLabel((n.inPub ? "● " : "○ ") + n.name + (n.stranger ? "  (newcomer)" : "")); lb.setOpaque(true); lb.setBackground(sel ? new Color(0xd8c48a) : new Color(0, 0, 0, 0)); lb.setForeground(n.inPub ? new Color(0x1b5e20) : INK); lb.setFont(sans(13, n.inPub)); lb.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6)); return lb; });
        JEditorPane detail = new JEditorPane(); detail.setContentType("text/html"); detail.setEditable(false); detail.setOpaque(false);
        list.addListSelectionListener(e -> { if (list.getSelectedValue() != null) { detail.setText(npcHtml(list.getSelectedValue())); detail.setCaretPosition(0); } });
        JScrollPane ls = scroll(list); ls.setPreferredSize(new Dimension(240, 480));
        JTextPane story = new JTextPane(); story.setContentType("text/html"); story.setEditable(false); story.setOpaque(false); story.setText(storyHtml());
        JPanel right = new JPanel(new GridLayout(2, 1, 0, 6)); right.setOpaque(false); right.add(scroll(detail)); right.add(scroll(story));
        root.add(ls, BorderLayout.WEST); root.add(right); root.setPreferredSize(new Dimension(880, 520));
        if (model.size() > 0) list.setSelectedIndex(0);
        return root;
    }
    static String npcHtml(Npc n) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:Segoe UI;color:#2b2118;font-size:12px'>");
        sb.append("<h2 style='margin:0;font-family:Georgia'>").append(esc(n.name)).append("</h2>");
        sb.append(n.age).append(", ").append(esc(n.job)).append(" · supports ").append(Data.ti(n.team) >= 0 ? Data.TEAM[Data.ti(n.team)] : "nobody in particular").append(" · usual: ").append(esc(n.favName.isEmpty() ? Data.DNAME[Data.di(n.fav)] : n.favName)).append("<br><i>").append(esc(n.bio)).append("</i><br>");
        sb.append("Trust in you: <b>").append((int) n.trust).append("</b> · Loyalty: <b>").append((int) n.loyalty).append("</b> · Visits: ").append(n.visits).append(n.defected ? " · <font color='red'>DEFECTED</font>" : "").append(n.bannedUntil >= S.day ? " · BARRED" : "").append("<br>");
        sb.append("Mood: ").append(moodWord(n)).append(" · money ").append(Util.money0(n.money)).append(" · ").append(statusLine(n)).append("<br>");
        sb.append("<b>Relationships:</b> ");
        List<String> fr = new ArrayList<>(), rv = new ArrayList<>(); for (Npc o : S.npcs) if (o != n) { double r = Sim.rel(n.id, o.id); String cr = S.crush.contains(n.id + "|" + o.id) ? " ♥" : ""; if (S.couples.contains(Sim.pair(n.id, o.id))) fr.add("<b>" + Sim.first(o) + " (partner)</b>"); else if (r >= 30) fr.add(Sim.first(o) + " " + (int) r + cr); else if (r <= -30) rv.add(Sim.first(o) + " " + (int) r); else if (!cr.isEmpty()) fr.add(Sim.first(o) + cr); }
        sb.append("friends: ").append(fr.isEmpty() ? "—" : String.join(", ", fr)).append(" · rivals: ").append(rv.isEmpty() ? "—" : String.join(", ", rv)).append("<br>");
        if (n.secretKnown || "1".equals(n.flags.get("confided"))) sb.append("<b>Secret:</b> ").append(esc(n.secret)).append("<br>"); else sb.append("<b>Secret:</b> <i>unknown</i><br>");
        sb.append("<b>Memories:</b><ul style='margin-top:0'>"); int c = 0; for (Mem m : n.mem) { if (c++ >= 10) break; sb.append("<li>").append(Sim.dayName(m.day).substring(0, 3)).append(": ").append(esc(m.t)).append(m.w < 0 ? " <font color='#a02020'>(−)</font>" : m.w > 0 ? " <font color='#206020'>(+)</font>" : "").append("</li>"); }
        if (n.mem.isEmpty()) sb.append("<li>Nothing noteworthy yet.</li>");
        sb.append("</ul></body></html>"); return sb.toString();
    }
    static String storyHtml() {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:Segoe UI;color:#2b2118;font-size:12px'><h3 style='margin:0;font-family:Georgia'>The pub's stories</h3>");
        if (!S.couples.isEmpty()) { sb.append("<b>Couples:</b> "); for (String p : S.couples) { String[] x = p.split("\\|"); sb.append(Sim.name(x[0])).append(" &amp; ").append(Sim.name(x[1])).append("; "); } sb.append("<br>"); }
        if (!S.exes.isEmpty()) { sb.append("<b>Exes:</b> "); for (String p : S.exes) { String[] x = p.split("\\|"); sb.append(Sim.name(x[0])).append(" &amp; ").append(Sim.name(x[1])).append("; "); } sb.append("<br>"); }
        List<String> riv = new ArrayList<>(), fri = new ArrayList<>(); for (Map.Entry<String, String> e : S.flags.entrySet()) if (e.getKey().startsWith("bond:")) { String[] x = e.getKey().substring(5).split("\\|"); String s = Sim.name(x[0]) + " &amp; " + Sim.name(x[1]); if (e.getValue().equals("rival")) riv.add(s); else if (!e.getValue().isEmpty()) fri.add(s + (e.getValue().equals("best") ? " (best mates)" : "")); }
        sb.append("<b>Friendships:</b> ").append(fri.isEmpty() ? "—" : String.join("; ", fri)).append("<br><b>Rivalries:</b> ").append(riv.isEmpty() ? "—" : String.join("; ", riv)).append("<br>");
        sb.append("<b>Running jokes:</b> "); if (S.jokes.isEmpty()) sb.append("—"); else for (Joke j : S.jokes) sb.append("“").append(esc(j.name)).append("” ×").append(j.count).append("; "); sb.append("<br>");
        sb.append("<b>Pub pigeon:</b> ").append(esc(S.pigeonName)).append("<br>"); sb.append("</body></html>"); return sb.toString();
    }

    // ================================================================
    //  ACHIEVEMENTS
    // ================================================================
    public static JComponent achievements() {
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 6)); grid.setOpaque(false);
        for (String[] a : Mgmt.ACH) { boolean got = S.ach.containsKey(a[0]); JPanel c = card(ink("<html><b>" + (got ? "🏆 " : "🔒 ") + esc(a[1]) + "</b><br>" + esc(a[2]) + (got ? "  <font color='#206020'>(day " + S.ach.get(a[0]) + ")</font>" : "") + "</html>", 12, false)); c.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6)); if (!got) for (Component x : c.getComponents()) x.setEnabled(false); grid.add(c); }
        JPanel root = new JPanel(new BorderLayout()); root.setOpaque(false); root.add(inkSerif("Achievements " + S.ach.size() + "/" + Mgmt.ACH.length, 18), BorderLayout.NORTH); JScrollPane sp = scroll(grid); sp.setPreferredSize(new Dimension(820, 480)); root.add(sp); return root;
    }

    // ================================================================
    //  FOOTBALL
    // ================================================================
    public static JComponent football() {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:Segoe UI;color:#2b2118;font-size:13px;width:620px'>");
        Match m = S.match;
        if (!S.upgrades.contains("tv")) sb.append("<h2>No TV package</h2>Renew it in Manage → Events & Ads.");
        else if (m != null) { sb.append("<h2 style='font-family:Georgia'>").append(Football.score(m)).append(" (").append(m.phase.equals("ft") ? "FT" : m.phase.equals("ht") ? "HT" : m.minute + "'").append(")</h2><b>").append(m.label).append("</b><br>"); for (int i = Math.max(0, m.events.size() - 10); i < m.events.size(); i++) sb.append(esc(m.events.get(i))).append("<br>"); }
        else sb.append("<h2 style='font-family:Georgia'>No match on right now</h2>");
        sb.append("<h3>Upcoming</h3>"); List<Match> fx = new ArrayList<>(S.fixtures); fx.sort(Comparator.comparingInt((Match x) -> x.day * 1440 + x.kickoff));
        for (Match f : fx) if (f.day >= S.day && f.phase.equals("pre")) sb.append(Sim.dayName(f.day)).append(" ").append(Util.hhmm(f.kickoff)).append(": ").append(Data.TEAM[f.home]).append(" v ").append(Data.TEAM[f.away]).append(" <i>(").append(f.label).append(")</i><br>");
        sb.append("<h3>League table</h3><table cellpadding=2>"); Integer[] idx = new Integer[8]; for (int i = 0; i < 8; i++) idx[i] = i; Arrays.sort(idx, (a, b) -> S.table[b] - S.table[a]);
        for (int i = 0; i < 8; i++) sb.append("<tr><td>").append(i + 1).append("</td><td>").append(idx[i] == 0 ? "<b>" : "").append(Data.TEAM[idx[i]]).append(idx[i] == 0 ? "</b>" : "").append("</td><td>").append(S.table[idx[i]]).append(" pts</td></tr>");
        sb.append("</table><br>Rovers this season: P").append(S.rovPlayed).append(" W").append(S.rovW).append(" D").append(S.rovD).append(" L").append(S.rovL).append("</body></html>");
        JTextPane tp = new JTextPane(); tp.setContentType("text/html"); tp.setEditable(false); tp.setOpaque(false); tp.setText(sb.toString());
        JScrollPane sp = scroll(tp); sp.setPreferredSize(new Dimension(700, 460)); return sp;
    }

    // ================================================================
    //  OPPONENT PICKER
    // ================================================================
    public static JComponent pickOpponent(String kind) {
        JPanel list = vbox(); list.add(inkSerif("Who fancies a game of " + kind + "?", 18)); list.add(new JLabel(" "));
        boolean any = false;
        for (Npc n : S.npcs) if (n.inPub && !n.st.equals("walk") && n.drunk < 85 && !n.st.equals("darts") && !n.st.equals("pool")) {
            any = true; int sk = kind.equals("darts") ? n.darts : n.pool;
            list.add(hrow(ink(n.name + " — drinking " + (n.glass >= 0 ? Data.DNAME[n.glass].toLowerCase() : "nothing") + ", " + moodWord(n).toLowerCase(), 13, false), sbtn("Challenge", () -> { GameWindow.close(); GameWindow.I.startGame(kind, n); })));
        }
        if (!any) list.add(ink("Nobody's free right now.", 13, false));
        if (kind.equals("darts")) { list.add(new JLabel(" ")); list.add(sbtn("Practice alone", () -> { GameWindow.close(); GameWindow.I.startGame("darts", null); })); }
        list.add(new JLabel(" ")); list.add(ink("Stakes: darts £10 · pool £5. You can lose. Ask Sarah. (Actually, don't.)", 11, false));
        JScrollPane sp = scroll(list); sp.setPreferredSize(new Dimension(560, 380)); return sp;
    }


    // ================================================================
    //  NOTICE BOARD
    // ================================================================
    public static JComponent notice() {
        JPanel p = vbox(); p.add(inkSerif("What's on at " + S.pubName, 20)); p.add(new JLabel(" "));
        for (int i = 0; i < 8; i++) {
            int d = S.day + i; List<String> items = new ArrayList<>();
            if (S.quizOn && Sim.dow(d) == 2) items.add("Pub Quiz, 20:00");
            if (S.roastOn && Sim.dow(d) == 6) items.add("Sunday Roast, 12:00-16:00");
            if (Sim.dow(d) == 4) items.add("Friday night - expect a crowd");
            for (Match m : S.fixtures) if (m.day == d) items.add(Util.hhmm(m.kickoff) + " " + Data.TEAM_SHORT[m.home] + " v " + Data.TEAM_SHORT[m.away] + (m.rovers ? "  *** ROVERS ***" : ""));
            for (Booking b : S.bookings) if (b.day == d) items.add(b.type.toUpperCase() + " NIGHT (booked)");
            String sp = Events.specialDay(d); if (!sp.isEmpty()) items.add(sp.toUpperCase());
            for (Delivery dv : S.deliveries) if (dv.arrives == d) items.add("Delivery: " + dv.qty + " x " + Data.DNAME[dv.item]);
            String head = (i == 0 ? "Today" : i == 1 ? "Tomorrow" : Sim.dayName(d)) + " (day " + d + ")";
            JLabel hl = ink(head, 14, true); p.add(hl);
            if (items.isEmpty()) p.add(ink("   Nothing special.", 12, false)); else for (String it : items) p.add(ink("   - " + it, 12, false));
        }
        p.add(new JLabel(" ")); p.add(hrow(btn("Book events & advertising", () -> { GameWindow.close(); GameWindow.open("Manage the pub", manage(4)); })));
        JScrollPane sp = scroll(p); sp.setPreferredSize(new Dimension(620, 480)); return sp;
    }

    // ================================================================
    //  MENU / HELP / DAY SUMMARY
    // ================================================================
    public static JComponent menu() {
        JPanel p = vbox(); p.add(inkSerif(S.pubName, 22)); p.add(new JLabel(" "));
        p.add(hrow(btn("Save game", () -> { GameWindow.close(); Sim.toast(Save.save(1) ? "Game saved." : "Save failed!"); }), btn("Load game", () -> { GameWindow.close(); if (Save.load(1)) { GameWindow.I.afterLoad(); Sim.toast("Game loaded."); } else Sim.toast("No save found."); })));
        p.add(hrow(btn("New game", () -> { GameWindow.close(); GameWindow.I.confirmNew(); }), btn("How to play", () -> { GameWindow.close(); GameWindow.open("How to play", help()); }), btn("Quit", () -> { Save.save(1); System.exit(0); })));
        p.add(new JLabel(" ")); p.add(inkSerif("Sound", 16));
        p.add(check("Sound effects", Audio.sfxOn, v -> Audio.sfxOn = v)); p.add(check("Crowd ambience", Audio.ambOn, v -> Audio.ambOn = v)); p.add(check("Jukebox music", Audio.musicOn, v -> Audio.musicOn = v)); p.add(check("Spoken match commentary (Windows)", Audio.speechOn, v -> Audio.speechOn = v));
        p.add(check("Show name tags", Gfx.showNames, v -> Gfx.showNames = v)); p.add(check("Show chatter in the feed", GameWindow.chatter, v -> GameWindow.chatter = v));
        JSlider vol = new JSlider(0, 100, (int) (Audio.master * 100)); vol.setOpaque(false); vol.addChangeListener(e -> Audio.master = vol.getValue() / 100.0); p.add(hrow(ink("Volume", 12, true), vol));
        p.add(ink("Autosaves every in-game hour and at the end of each day.", 11, false));
        return p;
    }
    static JCheckBox check(String t, boolean v, Consumer<Boolean> f) { JCheckBox c = new JCheckBox(t, v); c.setOpaque(false); c.setFont(sans(13, false)); c.setForeground(INK); c.addActionListener(e -> f.accept(c.isSelected())); return c; }

    public static JComponent help() {
        JTextPane tp = new JTextPane(); tp.setContentType("text/html"); tp.setEditable(false); tp.setOpaque(false);
        tp.setText("<html><body style='font-family:Segoe UI;color:#2b2118;font-size:13px;width:640px'><h2 style='font-family:Georgia'>Running The Speckled Pigeon</h2>"
            + "<b>Move:</b> click the floor, or WASD / arrow keys. <b>Click a person</b> to select them; use the buttons on the side card (Talk, Serve, Buy a drink, Gossip, Join chat, Darts, Pool, Apologise, Ask to leave).<br>"
            + "<b>Serve:</b> customers with an amber <b>!</b> are waiting at the bar. Staff serve them automatically — or you can click and serve instantly.<br>"
            + "<b>Click things:</b> the TV (match &amp; table), jukebox, dartboard, pool table, notice board (events), kitchen, the bar.<br>"
            + "<b>Time:</b> Space pauses; 1/2/3 changes speed. The pub skips ahead overnight.<br>"
            + "<b>Hotkeys:</b> M manage · N newspaper · T town · B regulars book · J jukebox · F5 save.<br><br>"
            + "<b>Goal:</b> make money, keep people happy, and let the stories happen. Customers remember what you do. Arguments flare, couples form, secrets leak, a pigeon visits. Keep stock up, prices fair, and the loos open.<br><br>"
            + "<b>Tips:</b> Match days and Fridays are huge. Book events. Hire a cleaner. Big Tony calms fights. Never, ever, tell Linda a secret.</body></html>");
        JScrollPane sp = scroll(tp); sp.setPreferredSize(new Dimension(720, 440)); return sp;
    }

    public static JComponent daySummary(DayRec r) {
        JPanel p = vbox(); double profit = r.rev - r.exp;
        p.add(inkSerif("Closing time — " + Sim.dayName(r.day), 22));
        p.add(ink("Takings " + Util.money(r.rev) + " · Costs " + Util.money(r.exp) + " · " + (profit >= 0 ? "PROFIT " : "LOSS ") + Util.money(profit), 17, true));
        p.add(ink(S.lastNightSummary, 12, false)); p.add(ink("Served " + r.served + " · Busiest moment: " + r.crowd + " customers · Weather: " + r.weather, 13, false)); p.add(new JLabel(" "));
        p.add(bar("Reputation", S.rep, new Color(0xc9973f))); p.add(bar("Satisfaction", S.sat, new Color(0x4a8fc9))); p.add(bar("Cleanliness", S.clean, new Color(0x7ec8c8)));
        p.add(new JLabel(" ")); p.add(inkSerif("Tonight's moments", 15));
        List<String> ls = new ArrayList<>(); for (int i = S.log.size() - 1; i >= 0 && ls.size() < 7; i--) { String l = S.log.get(i); if (l.contains("🏆") || l.contains("SECRET") || l.contains("rivalry") || l.contains("couple") || l.contains("Quiz") || l.contains("GOAL") || l.contains("FULL TIME") || l.contains("storms") || l.contains("overheard") || l.contains("running joke")) ls.add(l); }
        for (String l : ls) p.add(ink("• " + esc(l), 12, false)); if (ls.isEmpty()) p.add(ink("A quiet one. The pigeon enjoyed it.", 12, false));
        List<String> warn = new ArrayList<>(); for (int i = 0; i < 7; i++) if (S.stock[i] < 25) warn.add(Data.DNAME[i] + " (" + S.stock[i] + ")"); if (!warn.isEmpty()) { p.add(new JLabel(" ")); JLabel w = ink("Running low: " + String.join(", ", warn), 13, true); w.setForeground(new Color(0xa02020)); p.add(w); p.add(hrow(sbtn("Open stock screen", () -> { GameWindow.close(); GameWindow.open("Manage the pub", manage(1)); }))); }
        p.setPreferredSize(new Dimension(640, 460)); return p;
    }
}
