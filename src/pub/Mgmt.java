package pub;

import java.util.*;
import pub.Model.*;
import static pub.Sim.*;

/** Business side: staff, stock, upgrades, money, reputation, news, achievements. */
public final class Mgmt {
    private Mgmt() {}

    public record Up(String id, String name, String desc, int cost, int lvl, double upkeep) {}
    public static final List<Up> UPS = List.of(
        new Up("bigscreen", "Big-Screen TV", "Bigger match-day crowds and louder goals.", 900, 1, 2),
        new Up("karaoke", "Karaoke Machine", "Lets you host karaoke nights.", 450, 1, 1),
        new Up("fruit", "Fruit Machine", "Steady passive income. Occasionally ruins lives.", 600, 1, 0),
        new Up("fire", "Open Fireplace", "Cosy. Big winter boost to mood and popularity.", 700, 1, 1),
        new Up("garden", "Beer Garden", "+10 capacity in spring and summer.", 1800, 2, 2),
        new Up("cellar", "Cellar Cooling", "Stock costs 6% less. Fewer flat pints.", 800, 1, 1),
        new Up("kitchen2", "Proper Kitchen", "Faster, better meals.", 1500, 2, 3),
        new Up("scoreboard", "Dart Scoreboard", "Darts nights are properly serious.", 250, 1, 0),
        new Up("sound", "Sound System", "Better music and karaoke.", 600, 1, 1),
        new Up("loos", "Refurbished Loos", "Satisfaction up. The loos stop being a rumour.", 500, 1, 1),
        new Up("furniture", "Comfy Furniture", "Ambience up. Maggie approves.", 1200, 2, 0),
        new Up("cctv", "CCTV", "Fewer fights and thefts.", 400, 1, 1),
        new Up("wifi", "Free WiFi", "Students stay longer. Liam will never leave.", 100, 1, 1),
        new Up("contactless", "Contactless Terminals", "Faster service at the bar.", 150, 1, 1),
        new Up("sign", "New Pub Sign", "A gleaming pigeon. Popularity up.", 300, 1, 0));
    public static Up up(String id) { for (Up u : UPS) if (u.id.equals(id)) return u; return null; }

    public static final String[][] DECOR = {
        {"plants", "Potted Plants", "60"}, {"fairy", "Fairy Lights", "80"}, {"shirt", "Framed Rovers Shirt", "90"}, {"ads", "Vintage Beer Adverts", "110"},
        {"brass", "Horse Brasses", "70"}, {"chalk", "Specials Chalkboard", "60"}, {"trophy", "Trophy Cabinet", "100"}};
    public static final String[] WALLS = {"Classic Red", "Racing Green", "Navy Blue", "Mustard"};
    public static final int[] WALLCOL = {0x7A2E2A, 0x2F5236, 0x25345A, 0x9A7A2C};

    public record Ad(String id, String name, int cost, int days, int boost, String desc) {}
    public static final List<Ad> ADS = List.of(
        new Ad("flyers", "Flyers on Every Lamppost", 40, 7, 4, "A few curious strangers."),
        new Ad("chronicle", "Chronicle Advert", 90, 7, 7, "Half-page. Misspells 'Pigeon'."),
        new Ad("social", "Social Media Push", 60, 4, 5, "Students and Jade's followers."),
        new Ad("radio", "Local Radio Spot", 220, 7, 12, "Read by someone who's never been to Westbridge."),
        new Ad("sponsor", "Sponsor the Rovers", 300, 14, 9, "Rovers fans love you. Everyone else shrugs."));

    // ================= staff =================
    static final String[] SF = {"Wayne", "Precious", "Nigel", "Shaz", "Bernard", "Courtney", "Trish", "Kyle", "Dilys", "Marco", "Tamsin", "Gordon", "Hayley", "Idris", "Mandy", "Rupert", "Sunil", "Bex"};
    static final String[] SL = {"Pocock", "Adeyemi", "Fenwick", "Boyle", "Whitcombe", "Haddad", "Rowntree", "Beesley", "Cartwright", "Oyelaran", "Tuck", "Prendergast"};
    static final String[] TAGS = {"Brilliant with angry customers", "Always late", "Dozes on shift", "Knows everyone in town", "Whispers to pigeons", "Says 'no worries' to everything", "Terrible, but everyone loves them", "Obsessively tidy", "Hums constantly", "Has a mysterious past"};

    public static Staff makeStaff(String name, String role, int skill, int speed, int rely, int charm, double wage, String tag) {
        Staff s = new Staff(); s.id = "st" + Util.ri(1000, 99999); s.name = name; s.role = role; s.skill = skill; s.speed = speed; s.rely = rely; s.charm = charm; s.wage = wage; s.tags = tag;
        s.shirt = Util.pick(0x884422, 0x225588, 0x558822, 0x772255, 0x444444); s.hair = Util.pick(0x222222, 0x5A3A1A, 0xAA7733, 0x999999);
        s.x = 480; s.y = 560; s.hiredDay = S == null ? 1 : S.day;
        return s;
    }
    public static void hire(Staff s) {
        s.hiredDay = S.day; S.staff.add(s); s.x = 340; s.y = 150;
        stat("hires");
    }
    public static void genApplicants() {
        S.applicants.clear();
        String[] roles = {"bartender", "bartender", "chef", "waiter", "cleaner", "manager", "bartender"};
        for (int i = 0; i < 4; i++) {
            String role = Util.pick(roles);
            int sk = Util.ri(2, 9);
            double w = 38 + sk * 4 + Util.ri(0, 12) + (role.equals("manager") ? 25 : 0) + (role.equals("chef") ? 8 : 0);
            Staff s = makeStaff(Util.pick(SF) + " " + Util.pick(SL), role, sk, Util.ri(2, 9), Util.ri(2, 9), Util.ri(2, 9), w, Util.pick(TAGS));
            if (s.tags.equals("Always late")) s.rely = Math.min(s.rely, 3);
            if (s.tags.startsWith("Terrible")) { s.skill = Math.min(s.skill, 3); s.charm = 10; }
            if (s.tags.startsWith("Brilliant with")) s.charm = Math.max(s.charm, 8);
            S.applicants.add(s);
        }
    }
    public static boolean hireApplicant(Staff s) {
        if (S.staff.size() >= 8) return false;
        S.applicants.remove(s); hire(s); log("Hired " + s.name + " as " + s.role + ".", "staff"); sfx("coin"); return true;
    }
    public static void fire(Staff s) {
        S.staff.remove(s); stat("fires");
        log("You sacked " + s.name + ".", "staff");
        for (Npc n : S.npcs) { double r = s.rel.getOrDefault(n.id, 0.0); if (r > 4 || s.charm >= 8 && Util.chance(.4)) { addRel(n.id, "player", 0); n.trust -= 6 + r; remember(n, "sacked", -4, "player", "player", n.name + " was upset the landlord sacked " + s.name); } }
        if (s.charm >= 8) { S.rep -= 1.5; news("BELOVED BARMAN SACKED", s.name + " was let go. Regulars are 'considering their position'."); }
    }

    // staff movement and shifts
    static void moveStaff(double dt) {
        for (Staff s : S.staff) {
            if (!s.present) continue;
            double dx = s.tx - s.x, dy = s.ty - s.y, d = Math.hypot(dx, dy);
            double sp = 60 * dt;
            if (d > sp) { s.x += dx / d * sp; s.y += dy / d * sp; } else { s.x = s.tx; s.y = s.ty; }
        }
    }
    static void staffMinute() {
        int bi = 0;
        for (Staff s : S.staff) {
            if (!s.present) continue;
            switch (s.role) {
                case "bartender" -> { s.tx = Nav.BARTENDER[Math.min(bi, 2)][0]; s.ty = Nav.BARTENDER[Math.min(bi, 2)][1]; bi++; }
                case "manager" -> { s.tx = 345; s.ty = 150; }
                case "chef" -> { s.tx = 850; s.ty = 88; }
                case "waiter" -> { if (Util.chance(.2)) { Nav.Spot sp = Nav.SEATS.get(Util.ri(0, 23)); s.tx = sp.x + 20; s.ty = sp.y + 20; if (!Nav.free(s.tx, s.ty)) { s.tx = sp.x; s.ty = sp.y; } } }
                case "cleaner" -> {
                    if (!S.messes.isEmpty()) { Mess m = S.messes.get(0); s.tx = m.x; s.ty = m.y; }
                    else if (Util.chance(.1)) { Nav.Spot sp = Util.pick(Nav.STAND); s.tx = sp.x; s.ty = sp.y; }
                }
                default -> {}
            }
            // mood
            if (isOpen()) { s.mood += (65 + (S.sat - 50) * .1 - crowd() * .3 - s.mood) * .01; }
            if (s.tags.startsWith("Dozes") && Util.chance(.003) && s.role.equals("bartender")) { s.busy = true; s.busyT = 8; log(s.name + " has dozed off behind the bar.", "staff"); }
        }
    }

    // ================= daily cycle =================
    static void opening() {
        S.openedToday = true; S.daysOpen++;
        sfx("bell");
        log("Doors open! " + dayName() + (Ai.matchToday() ? " — MATCH DAY" : ""), "sys");
        // attendance
        for (Staff s : S.staff) {
            double p = .55 + s.rely * .048; s.present = Util.chance(Math.min(.99, p)) && s.sick <= 0;
            if (s.sick > 0) s.sick--;
            if (!s.present) { log(s.name + " didn't turn up" + (s.rely < 4 ? " (again)." : "."), "staff"); s.x = 480; s.y = 560; }
            else { s.x = 480; s.y = 590; }
            if (s.tags.equals("Always late") && s.present && Util.chance(.4)) { s.present = false; log(s.name + " is running late. Very late.", "staff"); }
        }
        if (staffByRole("bartender") == null && staffByRole("manager") == null) L.alert("Nobody on the bar today — you'll have to serve!");
        String sp = Events.specialDay(); if (!sp.isEmpty()) { log("Tonight is " + sp + "! Expect a crowd.", "event"); news(sp.toUpperCase() + " IN WESTBRIDGE", "Pubs across town expect a busy night. The Speckled Pigeon is ready, ish."); }
        if (S.quizOn && dow() == 2) log("Quiz night tonight at 20:00.", "event");
        for (Booking b : S.bookings) if (b.day == S.day) log("Tonight: " + b.type + " night!", "event");
        Events.maybeDailyDrama();
    }

    static void closing() {
        sfx("bell"); log("Time, gentlemen, please! The pub is closed.", "sys");
        for (Npc n : S.npcs) if (n.inPub) n.stayUntil = Math.min(n.stayUntil, S.min + Util.ri(0, 18));
        L.closed();
    }

    static double ambience() {
        double a = 30 + S.decor.size() * 4 + (S.upgrades.contains("fire") ? 5 + (season() == 1 ? 5 : 0) : 0) + (S.upgrades.contains("furniture") ? 8 : 0) + (S.upgrades.contains("loos") ? 4 : 0) + (S.upgrades.contains("sign") ? 3 : 0)
            + (S.upgrades.contains("sound") ? 3 : 0) + (S.upgrades.contains("bigscreen") ? 3 : 0) + (S.clean - 55) * .25;
        return Util.clamp(a);
    }

    static void endOfDay() {
        S.dayEnded = true;
        double wages = 0; for (Staff s : S.staff) if (s.present) wages += s.wage;
        double rent = 85 + 10 * S.level, util = 22 + (season() == 1 ? 14 : 0) + S.upgrades.size() * 2.0;
        double upkeep = 0; for (String u : S.upgrades) { Up x = up(u); if (x != null) upkeep += x.upkeep; }
        double lic = dow() == 6 ? 70 : 0, sports = (dow() == 0 && S.sports) ? 60 : 0;
        double fruit = S.upgrades.contains("fruit") ? 8 + S.pop * .2 : 0;
        if (fruit > 0) addMoney(fruit);
        double total = wages + rent + util + upkeep + lic + sports;
        S.money -= total; S.expToday += total;
        double rev = S.revToday, exp = S.expToday;
        DayRec r = new DayRec(); r.day = S.day; r.rev = rev; r.exp = exp; r.sat = S.sat; r.served = S.servedToday; r.crowd = S.peakCrowd; r.weather = new String[]{"Sunny", "Cloudy", "Rain", "Snow"}[S.weather];
        S.history.add(r); while (S.history.size() > 60) S.history.remove(0);
        S.lastNightSummary = String.format("Takings %s · Wages %s · Rent %s · Utilities %s · Upkeep %s%s%s", Util.money0(rev), Util.money0(wages), Util.money0(rent), Util.money0(util), Util.money0(upkeep), lic > 0 ? " · Licence " + Util.money0(lic) : "", sports > 0 ? " · Sports pkg " + Util.money0(sports) : "");
        // reputation
        double amb = ambience();
        double target = .55 * S.sat + .25 * amb + 8 + (S.upgrades.contains("loos") ? 2 : 0) - (S.price[0] > 5.6 ? 5 : 0);
        double avgCharm = 0; int ns = 0; for (Staff s : S.staff) if (s.present) { avgCharm += s.charm; ns++; } if (ns > 0) target += avgCharm / ns * .8;
        S.rep = Util.clamp(S.rep + (target - S.rep) * .05);
        double adBoost = 0; for (Ad a : ADS) if (S.ads.getOrDefault(a.id, -1) >= S.day) adBoost += a.boost;
        double facil = (S.upgrades.size() - 5) * .8 + S.decor.size() * .5;
        double tp = .6 * S.rep + adBoost + facil + 4 * S.level - (S.royalOak ? S.rivalStr * .15 : 0) + 8;
        S.pop = Util.clamp(S.pop + (tp - S.pop) * .2);
        S.prestige += Math.max(0, (rev - exp) / 25.0) + S.rep / 12.0 + S.sat / 40.0;
        // satisfaction drifts to neutral if nobody came
        // level
        int lv = S.level;
        if (S.rep >= 85 && S.day >= 160 && S.level < 5) lv = 5; else if (S.rep >= 68 && S.day >= 80 && S.level < 4) lv = 4; else if (S.rep >= 48 && S.day >= 30 && S.level < 3) lv = 3; else if (S.rep >= 30 && S.day >= 8 && S.level < 2) lv = 2;
        if (lv > S.level) { S.level = lv; L.levelUp(lv); stat("levelups"); news("PUB REACHES NEW HEIGHTS: " + levelName(lv).toUpperCase(), "The Speckled Pigeon is officially a '" + levelName(lv) + "'. The pigeon declined to comment."); }
        if (dow() == 4 && S.money > 0) S.flags.put("survivedFriday", "1");
        Social.daily();
        checkAch();
        L.dayEnd(r);
        S.peakCrowd = 0; S.satSum = 0; S.satN = 0;
        L.autosave();
    }

    public static String levelName(int l) { return new String[]{"", "Struggling Local", "Popular Pub", "Community Favourite", "Legendary Pub", "Local Institution"}[l]; }

    static void newDay() {
        for (Npc n : S.npcs) { n.drunk = 0; n.energy = 85; n.thirst = 25; n.bladder = 5; n.hunger = 30; n.anger *= .3; n.embar = 0; n.mood += (60 - n.mood) * .3; n.daysSeen++; if (n.bannedUntil < S.day) n.bannedUntil = 0; }
        // archive news -> today's edition
        for (News nw : S.news) { nw.day = S.day; S.archive.add(nw); }
        S.news.clear();
        List<String[]> pool = new ArrayList<>(Arrays.asList(Data.NEWS_FILLER)); Collections.shuffle(pool, Util.R);
        for (int i = 0; i < 3; i++) S.archive.add(new News(pool.get(i)[0], pool.get(i)[1], S.day));
        if (S.pigeonDay == S.day - 1) S.archive.add(new News("MYSTERY PIGEON CONTINUES TO VISIT LOCAL PUB", "The bird was seen again at the Speckled Pigeon. " + (S.pigeonName.equals("the pigeon") ? "It remains unnamed." : "Regulars call him " + S.pigeonName + "."), S.day));
        while (S.archive.size() > 120) S.archive.remove(0);
        S.ads.entrySet().removeIf(e -> e.getValue() < S.day);
        S.bookings.removeIf(b -> b.day < S.day);
        if (dow() == 0) genApplicants();
        for (Staff s : S.staff) { s.mood = Util.clamp(s.mood + (60 - s.mood) * .2); s.present = false; }
        S.clean = Math.min(100, S.clean + (staffByRole("cleaner") != null ? 8 : 3));
        S.satSum = 0; S.satN = 0; S.peakCrowd = 0;
        S.tvFixAt = S.tvBroken ? S.day : -1;
        if (S.tvBroken && Util.chance(.5)) S.tvBroken = false;
        S.powerCut = false; S.pigeonHere = false; Ai.games.clear();
    }

    // ================= stock =================
    public static int stockCap() { return S.upgrades.contains("cellar") ? 400 : 250; }
    public static double unitCost(int i) { return Data.DCOST[i] * (S.upgrades.contains("cellar") ? .94 : 1); }
    public static boolean order(int item, int qty) {
        double cost = unitCost(item) * qty;
        int pending = 0; for (Delivery d : S.deliveries) if (d.item == item) pending += d.qty;
        if (S.stock[item] + pending + qty > stockCap()) { toast("That's more than the cellar can hold."); return false; }
        if (S.money < cost) { toast("Not enough money."); return false; }
        spend(cost);
        Delivery d = new Delivery(); d.item = item; d.qty = qty; d.arrives = S.day + (S.min < 17 * 60 ? 1 : 2) + (S.deliveryLate ? 1 : 0); S.deliveries.add(d);
        sfx("coin"); return true;
    }
    static void deliveries() {
        if (S.deliveryLate) { S.deliveryLate = false; log("The delivery driver has NOT turned up. Typical.", "event"); return; }
        List<Delivery> done = new ArrayList<>();
        for (Delivery d : S.deliveries) if (d.arrives <= S.day) { S.stock[d.item] = Math.min(stockCap(), S.stock[d.item] + d.qty); done.add(d); }
        S.deliveries.removeAll(done);
        if (!done.isEmpty()) log("📦 Delivery arrived (" + done.size() + " consignment" + (done.size() > 1 ? "s" : "") + ").", "sys");
    }

    public static boolean buyUp(Up u) {
        if (S.upgrades.contains(u.id)) return false;
        if (S.level < u.lvl) { toast("Reach level " + u.lvl + " first."); return false; }
        if (S.money < u.cost) { toast("Can't afford that."); return false; }
        spend(u.cost); S.upgrades.add(u.id); stat("upgrades");
        log("Installed: " + u.name, "sys"); sfx("coin"); news("SPECKLED PIGEON INSTALLS " + u.name.toUpperCase(), "'It's about time,' said Dave. 'Mind you, it's not as good as the one at the Frog.' (It is.)");
        return true;
    }
    public static boolean buyDecor(String id, int cost) {
        if (S.decor.contains(id) || S.money < cost) return false;
        spend(cost); S.decor.add(id); sfx("coin"); log("Added decor: " + id, "sys"); return true;
    }
    public static boolean buyAd(Ad a) {
        if (S.money < a.cost) { toast("Can't afford that."); return false; }
        spend(a.cost); S.ads.put(a.id, S.day + a.days); sfx("coin"); log("Advertising: " + a.name, "sys"); stat("ads"); return true;
    }
    public static boolean book(String type, int day, int cost) {
        if (S.money < cost) { toast("Can't afford that."); return false; }
        for (Booking b : S.bookings) if (b.day == day) { toast("Something's already booked that day."); return false; }
        spend(cost); Booking b = new Booking(); b.type = type; b.day = day; S.bookings.add(b); sfx("coin");
        log("Booked " + type + " night for " + dayName(day) + ".", "event"); return true;
    }

    // ================= songs / jokes / small helpers =================
    public static int stamp() { return S.day * 1440 + S.min; }
    public static boolean songPlaying() { return S.songUntil > stamp(); }
    public static void playSong(int idx, Npc by) {
        String[] s = Data.SONGS[idx];
        S.song = s[0] + " — " + s[1]; S.playingGenre = s[2]; S.songUntil = stamp() + 4; S.songGenreIdx = idx;
        L.sfx("song:" + idx);
        log("♪ Now playing: " + S.song + (by != null ? " (chosen by " + first(by) + ")" : ""), "music");
        stat("songs");
        if (by != null) for (Npc n : S.npcs) if (n.inPub && n != by) { double a = n.music.getOrDefault(s[2], 0.0); if (Math.abs(a) > .5 && Util.chance(.4)) say(n, a > 0 ? Util.pick("Oh, good choice, " + first(by) + "!", "Yes! Turn it up!") : Util.pick("Who chose THIS?!", "Not this again, " + first(by) + ".")); if (a < -.5) addRel(n.id, by.id, -.8); else if (a > .5) addRel(n.id, by.id, .6); }
        if (s[2].equals("singalong") && crowd() > 6) { for (Npc n : S.npcs) if (n.inPub && n.soc > .6 && Util.chance(.35)) say(n, "♪ Sweet Carolyyyn! BA BA BAAAA! ♪", 6); log("The whole pub sings along!", "music"); stat("singalongs"); }
    }
    public static void addJoke(String name, String a, String b) {
        for (Joke j : S.jokes) if (j.name.equals(name)) return;
        Joke j = new Joke(); j.id = Util.fill("j{n}", "n", "" + S.jokes.size()); j.name = name; j.a = a; j.b = b; j.day = S.day; S.jokes.add(j);
        log("New running joke: \"" + name + "\"", "story"); stat("jokes");
    }
    static void banter(Npc a, Npc b) { stat("banter"); }
    static void bumpLaugh(Npc a, Npc b) { addRelBoth(a.id, b.id, 1); }

    public static String regLabel(Npc n) {
        if (n.stranger) return "Newcomer"; if (n.visits >= 80 && n.loyalty >= 85) return "Lifelong regular"; if (n.loyalty >= 72) return "Pillar of the pub"; if (n.visits < 10) return "Occasional"; return "Regular";
    }
    static final String[] TIPS = {"Tip: stand behind the bar (top-left) and you'll serve customers automatically.", "Tip: click a person, then 'Talk' - they remember how you treat them.", "Tip: check Manage > Stock before busy nights. Empty taps make people furious.", "Tip: Friday, Saturday and match days are your big earners.", "Tip: click spills on the floor to mop them up.", "Tip: the TV shows live fictional football. Click it for the league table.", "Tip: Linda gossips. Do not tell Linda secrets.", "Tip: book a karaoke or band night in Manage > Events & Ads.", "Tip: some customers will ask you for a job. They might be brilliant."};
    static int tipIdx;

    static void minute() {
        if (S.day <= 6 && S.min % 150 == 5 && isOpen() && tipIdx < TIPS.length) toast(TIPS[tipIdx++]);
        if (S.songUntil != 0 && S.songUntil <= stamp()) { S.playingGenre = ""; S.song = ""; S.songUntil = 0; }
        if (S.min % 60 == 30) checkAch();
        // pub level crowd effects
        int c = crowd();
        if (c >= 25) { stat("fullPub"); }
        if (S.tvFixAt >= 0 && S.min == 8 * 60 && S.tvBroken) { S.tvBroken = false; }
    }

    // ================= achievements =================
    public static final String[][] ACH = {
        {"p100", "Pint Pioneer", "Serve 100 pints."}, {"p1000", "Pint Legend", "Serve 1,000 pints."}, {"p5000", "Pint Colossus", "Serve 5,000 pints."},
        {"friday", "Friday Survivor", "Survive your first Friday night."}, {"quiz", "Quiz Master", "Win the pub quiz."}, {"friends", "Friend to All", "Win the trust of every regular."},
        {"rivalry", "Legendary Rivalry", "Start a legendary pub rivalry."}, {"year", "Year-Round Landlord", "Keep the pub open for 365 days."}, {"lifelong", "Lifelong Regular", "Have a customer become a lifelong regular."},
        {"profit", "In the Black", "Make a profit in a single day."}, {"big", "Big Night", "Take over £800 in one day."}, {"rich", "Pub Tycoon", "Have £10,000 in the bank."},
        {"rep50", "Local Hero", "Reach 50 reputation."}, {"rep90", "Beloved", "Reach 90 reputation."},
        {"lvl2", "Popular Pub", "Reach level 2."}, {"lvl3", "Community Favourite", "Reach level 3."}, {"lvl4", "Legendary Pub", "Reach level 4."}, {"lvl5", "Local Institution", "Reach level 5."},
        {"hire", "The Boss", "Hire your first employee."}, {"liam", "Liam Gets a Job", "Hire Liam to work the bar."}, {"fire", "Cold Hearted", "Sack someone."},
        {"karaoke", "Mic Drop", "Host a karaoke night."}, {"sing", "Star in a Pub", "Sing at karaoke yourself."}, {"darts", "Arrows!", "Win a game of darts."}, {"pool", "Pool Shark", "Win a game of pool."}, {"d180", "One Hundred And Eighty!", "Hit a maximum in darts."},
        {"juke", "DJ Landlord", "Play 25 jukebox songs."}, {"crowd", "Standing Room Only", "Have 28 people in the pub at once."}, {"secrets", "Secret Keeper", "Uncover 3 secrets."}, {"gossip", "Pub Telegraph", "Witness 20 gossips."},
        {"peace", "Peacemaker", "Defuse 5 arguments."}, {"couple", "Cupid", "See a couple form in your pub."}, {"hatchet", "Bury the Hatchet", "Turn a rivalry into a friendship."}, {"power", "Candlelit", "Survive a power cut."},
        {"pigeon", "Pigeon Fancier", "Name the pigeon."}, {"police", "Helping With Enquiries", "Have the police visit."}, {"celeb", "Famous (Ish)", "Serve a celebrity."}, {"review", "Five Stars", "Get a five-star review."},
        {"dry", "Dry Run", "Run out of beer. Shame on you."}, {"joke", "Running Gag", "Have 5 running jokes in the pub."}, {"treat", "Generous Soul", "Buy 20 drinks for customers."}, {"stag", "Stag Do Survivor", "Host a stag do."},
        {"proposal", "She Said Yes", "Host a successful proposal."}, {"newreg", "Welcome to the Family", "Turn 3 strangers into regulars."}, {"match", "Match Day Maestro", "Host 5 Rovers matches with 8+ in the pub."}};

    public static void unlock(String id) {
        if (S.ach.containsKey(id)) return;
        S.ach.put(id, S.day);
        for (String[] a : ACH) if (a[0].equals(id)) { log("🏆 Achievement: " + a[1] + " — " + a[2], "ach"); L.ach(a[1] + " — " + a[2]); S.prestige += 25; }
        sfx("ach");
    }

    static void checkAch() {
        int pints = stat0("pints");
        if (pints >= 100) unlock("p100"); if (pints >= 1000) unlock("p1000"); if (pints >= 5000) unlock("p5000");
        if (S.flags.containsKey("survivedFriday") && S.day > 5) unlock("friday");
        if (stat0("quizWins") > 0) unlock("quiz");
        boolean all = true; int regs = 0;
        for (Npc n : S.npcs) if (!n.stranger) { regs++; if (n.trust < 40) all = false; }
        if (all && regs >= 14) unlock("friends");
        if (stat0("legendaryRivalries") > 0) unlock("rivalry");
        if (S.daysOpen >= 365) unlock("year");
        for (Npc n : S.npcs) if (n.visits >= 80 && n.loyalty >= 85) unlock("lifelong");
        if (!S.history.isEmpty()) { DayRec r = S.history.get(S.history.size() - 1); if (r.rev - r.exp > 0) unlock("profit"); if (r.rev >= 800) unlock("big"); }
        if (S.money >= 10000) unlock("rich");
        if (S.rep >= 50) unlock("rep50"); if (S.rep >= 90) unlock("rep90");
        for (int i = 2; i <= 5; i++) if (S.level >= i) unlock("lvl" + i);
        if (stat0("hires") > 1) unlock("hire"); if (stat0("fires") > 0) unlock("fire");
        for (Staff s : S.staff) if (s.fromNpc.equals("liam")) unlock("liam");
        if (stat0("karaokeNights") > 0) unlock("karaoke"); if (stat0("sang") > 0) unlock("sing");
        if (stat0("dartsWins") > 0) unlock("darts"); if (stat0("poolWins") > 0) unlock("pool"); if (stat0("d180") > 0) unlock("d180");
        if (stat0("songs") >= 25) unlock("juke");
        if (crowd() >= 28) unlock("crowd");
        if (stat0("secrets") >= 3) unlock("secrets"); if (stat0("gossips") >= 20) unlock("gossip");
        if (stat0("rowsDefused") >= 5) unlock("peace"); if (stat0("couples") > 0) unlock("couple"); if (stat0("rivalsToFriends") > 0) unlock("hatchet");
        if (stat0("powerCuts") > 0 && !S.powerCut) unlock("power");
        if (!S.pigeonName.equals("the pigeon")) unlock("pigeon");
        if (stat0("policeVisits") > 0) unlock("police"); if (stat0("celebs") > 0) unlock("celeb"); if (stat0("fiveStar") > 0) unlock("review");
        if (stat0("stockouts") >= 5) unlock("dry");
        if (S.jokes.size() >= 5) unlock("joke"); if (stat0("buyDrinks") >= 20) unlock("treat");
        if (stat0("stagDo") > 0) unlock("stag"); if (stat0("proposalYes") > 0) unlock("proposal");
        if (stat0("newRegulars") >= 3) unlock("newreg"); if (stat0("matchNightsHosted") >= 5) unlock("match");
    }
}
