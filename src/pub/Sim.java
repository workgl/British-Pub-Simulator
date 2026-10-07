package pub;

import java.util.*;
import pub.Model.*;

/** Simulation core: state, clock, day cycle, arrivals, economy plumbing. */
public final class Sim {
    private Sim() {}
    public static State S;
    public static double rt, acc;
    public static final Map<String, Npc> byId = new HashMap<>();
    public static boolean paused;

    // ---------- UI bridge ----------
    public static class Opt {
        public String label, hint = ""; public Runnable run;
        public Opt(String l, Runnable r) { label = l; run = r; }
        public Opt(String l, String h, Runnable r) { label = l; hint = h; run = r; }
    }
    public static class Choice {
        public String title, text; public List<Opt> opts = new ArrayList<>();
        public Choice(String t, String x) { title = t; text = x; }
    }
    public static class Listener {
        public void log(String text, String cat) {}
        public void toast(String text) {}
        public void sfx(String name) {}
        public void say(Npc n, String text) {}
        public void choice(Choice c) {}
        public void news(News n) {}
        public void ach(String t) {}
        public void fx(String type, double x, double y) {}
        public void commentary(String t) {}
        public void levelUp(int l) {}
        public void dayEnd(DayRec r) {}
        public void alert(String text) {}
        public void morning() {}
        public void closed() {}
        public void autosave() {}
        public void game(String kind, Npc opponent) {}
    }
    public static Listener L = new Listener();

    // ---------- helpers ----------
    public static int dow() { return (S.day - 1) % 7; }
    public static int dow(int day) { return (day - 1) % 7; }
    public static int monthIdx() { return ((S.day - 1) / 28) % 12; }
    public static int season() { return monthIdx() / 3; }
    public static int year() { return (S.day - 1) / 336 + 1; }
    public static String dayName() { return Data.DAYS[dow()]; }
    public static String dayName(int d) { return Data.DAYS[dow(d)]; }
    public static String dateStr() { return dayName() + " " + ((S.day - 1) % 28 + 1) + " " + Data.MONTHS[monthIdx()]; }
    public static double hour() { return S.min / 60.0; }
    public static Npc npc(String id) { return byId.get(id); }
    public static String name(String id) { if ("player".equals(id)) return "you"; Npc n = byId.get(id); if (n != null) return n.name.split(" ")[0].replace("Rev.", "Alan"); return id; }
    public static String full(String id) { Npc n = byId.get(id); return n == null ? id : n.name; }
    public static String first(Npc n) { String s = n.name.split(" ")[0]; if (s.equals("Rev.")) return "Alan"; if (s.equals("Big")) return "Tony"; if (s.equals("Dr")) return "Fiona"; return s; }
    public static int openMin() { return S.openH * 60; }
    public static int closeMin() { return S.closeH * 60; }
    public static boolean isOpen() {
        int m = S.min;
        if (S.closeH <= 24) return m >= openMin() && m < closeMin();
        return m >= openMin() || m < (S.closeH - 24) * 60;
    }
    public static boolean afterClose() { return !isOpen(); }
    public static List<Npc> inPub() { List<Npc> l = new ArrayList<>(); for (Npc n : S.npcs) if (n.inPub) l.add(n); return l; }
    public static int crowd() { int c = 0; for (Npc n : S.npcs) if (n.inPub) c++; return c; }
    public static int capacity() { return 34 + (S.upgrades.contains("garden") && season() >= 2 ? 10 : 0); }
    public static void log(String t, String cat) {
        S.log.add(Util.hhmm(S.min) + "  " + t);
        if (S.log.size() > 300) S.log.remove(0);
        L.log(t, cat);
    }
    public static void toast(String t) { L.toast(t); }
    public static void sfx(String s) { L.sfx(s); }
    public static void stat(String k) { S.stats.merge(k, 1, Integer::sum); }
    public static void stat(String k, int v) { S.stats.merge(k, v, Integer::sum); }
    public static int stat0(String k) { return S.stats.getOrDefault(k, 0); }
    public static void say(Npc n, String text) { say(n, text, 5.5); }
    public static void say(Npc n, String text, double secs) {
        n.bubble = text; n.bubbleUntil = rt + secs; L.say(n, text);
    }
    public static void news(String h, String b) {
        News nw = new News(h, b, S.day); S.news.add(nw); L.news(nw);
    }
    public static void ask(Choice c) { L.choice(c); }
    public static void addMoney(double v) { S.money += v; if (v > 0) S.revToday += v; }
    public static void spend(double v) { S.money -= v; S.expToday += v; }
    public static Staff staffByRole(String role) { for (Staff s : S.staff) if (s.role.equals(role) && s.present) return s; return null; }
    public static boolean hasStaff(String role) { return staffByRole(role) != null; }
    public static boolean hasUp(String id) { return S.upgrades.contains(id); }

    // ---------- relationships ----------
    public static double rel(String a, String b) { return S.rel.getOrDefault(a + "|" + b, 0.0); }
    public static void addRel(String a, String b, double d) {
        if (a.equals(b)) return;
        double old = rel(a, b), nv = Util.clamp(old + d, -100, 100);
        S.rel.put(a + "|" + b, nv);
        Social.bondCheck(a, b);
    }
    public static void addRelBoth(String a, String b, double d) { addRel(a, b, d); addRel(b, a, d); }
    public static boolean friends(String a, String b) { return rel(a, b) >= 35 && rel(b, a) >= 35; }
    public static boolean rivals(String a, String b) { return rel(a, b) <= -35 && rel(b, a) <= -35; }
    public static String pair(String a, String b) { return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a; }

    // ---------- memory ----------
    public static void remember(Npc n, String kind, double w, String about, String who, String text) {
        for (Mem m : n.mem) if (m.t.equals(text) && m.day == S.day) return;
        n.mem.add(0, new Mem(S.day, kind, w, about, who, text));
        while (n.mem.size() > 40) n.mem.remove(n.mem.size() - 1);
    }
    public static void rememberAll(String kind, double w, String about, String who, String text, Npc... ns) { for (Npc n : ns) remember(n, kind, w, about, who, text); }

    // ---------- new game / load ----------
    public static void newGame(String pubName) {
        S = new State();
        if (pubName != null && !pubName.isBlank()) S.pubName = pubName;
        S.npcs = new ArrayList<>(Chars.cast());
        for (Npc n : S.npcs) { Chars.fillDefaults(n); n.loyalty = Util.r(45, 65); n.mood = Util.r(50, 70); n.home = "houses"; n.lastVisit = -5; n.visits = Util.ri(15, 40); }
        for (Object[] r : Chars.INITIAL_REL) { S.rel.put(r[0] + "|" + r[1], ((Number) r[2]).doubleValue()); S.rel.put(r[1] + "|" + r[0], ((Number) r[3]).doubleValue()); }
        Mgmt.hire(Mgmt.makeStaff("Wayne Pocock", "bartender", 3, 6, 7, 9, 55, "Slow, forgetful, adored by everyone."));
        S.staff.get(0).present = true;
        Mgmt.genApplicants();
        rebuild();
        Football.planWeek();
        prepareDay(true);
        S.weather = Util.ri(0, 1);
        S.archive.add(new News("NEW LANDLORD TAKES OVER THE SPECKLED PIGEON", "Locals describe the new owner as 'probably fine' and 'not Barry'. The pigeon was unavailable for comment.", 1));
        S.archive.add(new News("WESTBRIDGE ROVERS PREPARE FOR SATURDAY", "Fans optimistic, in the way people are optimistic about parking.", 1));
        S.archive.add(new News("MYSTERY PIGEON CONTINUES TO VISIT LOCAL PUB", "Residents baffled as bird attends for another week. 'He seems to like the scampi fries,' said one witness.", 1));
        log("Welcome to " + S.pubName + ". Doors open at " + S.openH + ":00. Click the floor to walk, click people to talk.", "sys");
    }

    public static void load(State s) { S = s; rebuild(); }

    public static void rebuild() {
        byId.clear();
        for (Npc n : S.npcs) byId.put(n.id, n);
        S.occ.clear();
        for (Npc n : S.npcs) if (n.inPub && !n.spot.isEmpty()) S.occ.put(n.spot, n.id);
    }

    // ---------- day cycle ----------
    static void prepareDay(boolean first) {
        for (Npc n : S.npcs) Ai.planDay(n);
    }

    static void newDay() {
        S.day++;
        S.dayEnded = false; S.openedToday = false; S.lastOrders = false;
        S.revToday = 0; S.expToday = 0; S.servedToday = 0;
        S.pigeonHere = false;
        // clean up strangers
        List<Npc> drop = new ArrayList<>();
        for (Npc n : S.npcs) {
            if (n.stranger && !n.inPub && ("1".equals(n.flags.get("gone")) || (n.visits <= 1 && S.day - n.lastVisit > 4))) drop.add(n);
        }
        S.npcs.removeAll(drop); for (Npc n : drop) byId.remove(n.id);
        Mgmt.newDay();
        Football.dayStart();
        Ai.lifeEvents();
        prepareDay(false);
        Events.dayStart();
        if (dow() == 0) Football.planWeek();
        S.weather = weatherNext();
        L.morning();
    }

    static int weatherNext() {
        int s = season();
        double r = Util.R.nextDouble();
        int w = S.weather;
        if (r < .55) return w; // persistence
        if (s == 1 && r > .85) return 3;
        if (s == 3) return r < .75 ? 0 : (r < .9 ? 1 : 2);
        return r < .3 ? 0 : (r < .65 ? 1 : 2);
    }

    // ---------- time loop ----------
    /** Called every frame. dt seconds, speed multiplier (game minutes per real second). */
    public static void advance(double dt, double speed) {
        if (S == null) return;
        rt += dt;
        double mv = Math.min(speed, 3.0);
        Ai.move(dt * mv);
        Mgmt.moveStaff(dt * mv);
        Events.frame(dt);
        acc += dt * speed;
        int guard = 0;
        while (acc >= 1 && guard++ < 80) { acc -= 1; minute(); }
        if (acc > 3) acc = 0;
    }

    public static boolean canFastForward() { return !isOpen() && crowd() == 0; }

    public static void minute() {
        S.min++;
        if (S.min >= 1440) S.min = 0;
        int m = S.min;
        if (m == 300) newDay();
        if (m % 60 == 0) { hourly(); }
        if (m == 480) Mgmt.deliveries();
        if (m == openMin() % 1440) Mgmt.opening();
        int lo = (closeMin() - 15) % 1440; if (lo < 0) lo += 1440;
        if (m == lo && !S.lastOrders) { S.lastOrders = true; sfx("bell"); log("Last orders! Ring the bell.", "sys"); for (Npc n : S.npcs) if (n.inPub && Util.chance(.5)) say(n, Util.pick("Last orders, is it? Same again!", "Already?", "One for the road, then.", "Time flies...")); }
        if (m == closeMin() % 1440) { Mgmt.closing(); }
        Football.minute();
        Events.minute();
        Ai.arrivals();
        for (Npc n : new ArrayList<>(S.npcs)) Ai.minute(n);
        Mgmt.staffMinute();
        Ai.barService();
        Social.minute();
        Mgmt.minute();
        Ai.petsAndMess();
        if (!S.dayEnded && S.openedToday && !isOpen() && S.min != 0 && (crowd() == 0 || minutesSinceClose() > 75) && minutesSinceClose() >= 20) Mgmt.endOfDay();
    }

    public static int minutesSinceClose() {
        int c = closeMin() % 1440, d = S.min - c; if (d < 0) d += 1440; return d;
    }

    static void hourly() {
        if (Util.chance(.12)) S.weather = weatherNext();
        L.autosave();
    }
}
