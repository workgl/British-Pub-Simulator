package pub;

import java.util.*;
import pub.Model.*;
import static pub.Sim.*;

/** Fictional league: fixtures, live match simulation and crowd reactions. */
public final class Football {
    private Football() {}

    static Match mk(int day, int kick, int h, int a, boolean big, String label) {
        Match m = new Match(); m.day = day; m.kickoff = kick; m.home = h; m.away = a; m.big = big; m.rovers = h == 0 || a == 0; m.label = label; return m;
    }

    public static void planWeek() {
        S.fixtures.removeIf(m -> m.day < S.day);
        int base = S.day - dow();
        if (base < S.day) base = S.day;
        int opp = Util.ri(1, 7);
        boolean home = Util.chance(.5);
        S.fixtures.add(mk(S.day + (5 - dow() < 0 ? 0 : 5 - dow()), 15 * 60, home ? 0 : opp, home ? opp : 0, false, "League"));
        int a = Util.ri(1, 7), b; do { b = Util.ri(1, 7); } while (b == a || b == opp);
        S.fixtures.add(mk(S.day + 5 - dow(), 17 * 60 + 30, a, b, true, "Saturday Night Football"));
        int c = Util.ri(1, 7), d; do { d = Util.ri(1, 7); } while (d == c);
        S.fixtures.add(mk(S.day + 6 - dow(), 16 * 60, c, d, true, "Super Sunday"));
        if ((S.day / 7) % 2 == 0) { int e = Util.ri(1, 7), f; do { f = Util.ri(1, 7); } while (f == e); S.fixtures.add(mk(S.day + 1 - dow() + (dow() > 1 ? 7 : 0), 19 * 60 + 45, e, f, true, "Midweek Cup")); }
        S.fixtures.removeIf(m -> m.day < S.day);
    }

    public static void dayStart() {
        S.fixtures.removeIf(m -> m.day < S.day);
        if (S.match != null && !S.match.phase.equals("ft")) S.match = null;
        else if (S.match != null) S.match = null;
    }

    static void comm(String t) { L.commentary(t); }

    public static void minute() {
        Match m = S.match;
        if (m == null) {
            for (Match f : S.fixtures) if (f.day == S.day && f.kickoff == S.min && f.phase.equals("pre")) { start(f); return; }
            return;
        }
        if (m.phase.equals("ft")) { if (++m.lastEventT > 25) S.match = null; return; }
        m.flash = Math.max(0, m.flash - 1);
        switch (m.phase) {
            case "pre" -> {}
            case "1st", "2nd" -> {
                m.minute++;
                tick(m);
                if (m.phase.equals("1st") && m.minute >= 45 + (m.acc > 0 ? 0 : 1)) { m.phase = "ht"; m.htLeft = 15; ev(m, "HT", "Half time: " + score(m)); comm("And that's half time. " + score(m)); }
                else if (m.phase.equals("2nd") && m.minute >= 90 + (int) m.acc) { finish(m); }
            }
            case "ht" -> { if (--m.htLeft <= 0) { m.phase = "2nd"; m.minute = 45; comm("We're back underway for the second half."); } }
            default -> {}
        }
        if (S.tvBroken && !m.phase.equals("ft")) for (Npc n : S.npcs) if (n.inPub && Ai.fanOfRaw(n, m)) { n.anger += .4; n.exp -= .5; }
    }

    static String score(Match m) { return Data.TEAM_SHORT[m.home] + " " + m.hg + "-" + m.ag + " " + Data.TEAM_SHORT[m.away]; }

    static void ev(Match m, String kind, String text) { m.events.add(m.minute + "' " + text); m.lastEvent = text; }

    static void start(Match m) {
        m.phase = "1st"; m.minute = 0; m.acc = Util.ri(2, 5); S.match = m;
        comm("We're off! " + Data.TEAM[m.home] + " versus " + Data.TEAM[m.away] + ".");
        if (S.upgrades.contains("tv") && !S.tvBroken && crowd() > 0) {
            log("KICK-OFF: " + Data.TEAM[m.home] + " v " + Data.TEAM[m.away] + (m.rovers ? "  — it's the Rovers!" : ""), "match");
            sfx("whistle");
            for (Npc n : S.npcs) if (n.inPub && Ai.fanOf(n) && !n.st.equals("tv") && n.conv.isEmpty() && Util.chance(.8)) { n.timer = 0; Ai.goTV(n); }
        }
    }

    static double lam(int a, int b) {
        double sa = Data.TEAM_STR[a], sb = Data.TEAM_STR[b];
        return .0125 * Math.pow(sa / 70.0, 1.6) / Math.pow(sb / 70.0, .8);
    }

    static void tick(Match m) {
        if (Util.chance(lam(m.home, m.away) * 1.08)) goal(m, true);
        else if (Util.chance(lam(m.away, m.home))) goal(m, false);
        else if (Util.chance(.018)) {
            int t = Util.chance(.5) ? m.home : m.away;
            String s = Util.pick("OFF THE POST!", "SAVED! Brilliant stop!", "Just wide!", "Over the bar!", "Cleared off the line!");
            ev(m, "near", s + " (" + Data.TEAM_SHORT[t] + ")"); comm(s + " " + Data.TEAM_SHORT[t] + " so close there.");
            nearMiss(t);
        } else if (Util.chance(.0014)) {
            int t = Util.chance(.5) ? m.home : m.away;
            ev(m, "red", "RED CARD for " + Data.TEAM_SHORT[t] + "!"); comm("And that's a straight red! " + Data.TEAM_SHORT[t] + " down to ten!");
            for (Npc n : S.npcs) if (n.inPub && Ai.fanOfRaw(n, m)) { boolean sup = Data.ti(n.team) == t; n.anger += sup ? 14 : -4; if (!sup && Util.chance(.3)) say(n, "Ha! Off you go!"); else if (sup) say(n, Util.pick("RIDICULOUS!", "That was never a red!", "REF!")); }
            sfx("groan");
        }
    }

    static void goal(Match m, boolean home) {
        if (home) m.hg++; else m.ag++;
        int t = home ? m.home : m.away;
        String who = Util.pick(Data.PLAYERS);
        ev(m, "goal", "GOAL! " + Data.TEAM_SHORT[t] + " — " + who + "  (" + m.hg + "-" + m.ag + ")");
        m.flash = 6;
        comm(Util.pick("GOAL! " + who + " scores for " + Data.TEAM_SHORT[t] + "!", "It's in! " + who + " with a cracker!", "What a finish from " + who + "!", "And " + who + " puts " + Data.TEAM_SHORT[t] + " in front!") + " " + score(m) + ".");
        if (!S.upgrades.contains("tv") || S.tvBroken) return;
        sfx("goal"); L.fx("goal", 610, 80);
        log("⚽ GOAL! " + score(m) + " (" + who + ")", "match");
        boolean bigScreen = S.upgrades.contains("bigscreen");
        List<Npc> cel = new ArrayList<>(), gut = new ArrayList<>();
        for (Npc n : S.npcs) {
            if (!n.inPub) continue;
            int mine = Data.ti(n.team);
            boolean watching = Ai.fanOfRaw(n, m) || n.sport > .45;
            if (!watching) continue;
            if (mine == t) { cel.add(n); n.mood = Util.clamp(n.mood + 14); n.anger = Math.max(0, n.anger - 8); n.exp += bigScreen ? 5 : 3;
                say(n, Util.pick("GOOOOAAAL!", "YESSSS!", "GET IN!!", "WHAT A GOAL!", "COME ON YOU " + Data.TEAM_SHORT[t].toUpperCase() + "!"), 4);
                if (Util.chance(.22) && n.glass >= 0) { n.glass = -1; n.glassLevel = 0; Mess ms = new Mess(); ms.x = n.x; ms.y = n.y + 12; ms.made = S.min; S.messes.add(ms); say(n, "MY PINT!", 4); stat("spilledDrinks"); }
            } else if (mine >= 0 && (mine == m.home || mine == m.away)) { gut.add(n); n.mood = Util.clamp(n.mood - 12); n.anger += 11; n.exp -= 2;
                say(n, Util.pick("Oh, you're joking!", "Come ON!", "Unbelievable.", "Defend! DEFEND!", "I can't watch this."), 4);
            } else if (n.sport > .6) { n.mood += 3; if (Util.chance(.3)) say(n, Util.pick("Ooh, nice one.", "Great goal, that.", "Well played.")); }
        }
        for (Npc a : cel) for (Npc b : cel) if (a != b && a.id.compareTo(b.id) < 0 && Util.dist(a.x, a.y, b.x, b.y) < 220) addRelBoth(a.id, b.id, 1.2);
        // gloating
        for (Npc a : cel) for (Npc b : gut) if (Util.dist(a.x, a.y, b.x, b.y) < 200 && Util.chance(.35) && a.conv.isEmpty() && b.conv.isEmpty()) {
            say(a, Util.fill(Util.pick("Not so clever now, {n}!", "Get in, {n}! How's THAT?", "{n}! Come and sing with us!"), "n", first(b)));
            addRel(b.id, a.id, -2.5); b.anger += 6; Mgmt.banter(a, b);
            if (b.tmp > .5 && Util.chance(.25)) { Social.Conv c = new Social.Conv(); c.id = "f" + Util.ri(100, 999); c.a = b.id; c.b = a.id; c.topic = "football"; c.stage = 1; c.next = 1; c.ttl = 12; a.conv = c.id; b.conv = c.id; Social.convs.add(c); }
            break;
        }
        if (cel.size() >= 3 && Util.chance(.35)) {
            Npc lead = null; for (Npc n : cel) if (n.sport > .7) lead = n;
            if (lead != null) { say(lead, Util.pick(Data.TEAM_SHORT[t] + "! " + Data.TEAM_SHORT[t] + "! " + Data.TEAM_SHORT[t] + "!", "♪ We're by far the greatest team, the world has ever seen! ♪", "♪ Ooh, aah, " + who.split(" ")[1] + "! ♪"), 6); log("A chant breaks out in the pub!", "match"); stat("chants"); sfx("chant"); }
        }
        if (S.messes.size() > 12) S.messes.remove(0);
    }

    static void nearMiss(int t) {
        for (Npc n : S.npcs) if (n.inPub && n.st.equals("tv") && Util.chance(.25)) { boolean sup = Data.ti(n.team) == t; say(n, sup ? Util.pick("ARGH!", "How did that not go in?!", "Oh come ON!") : Util.pick("Phew!", "Lucky!", "Ha!"), 3); }
        sfx("gasp");
    }

    static void finish(Match m) {
        m.phase = "ft"; m.lastEventT = 0;
        comm("Full time. " + score(m) + "."); ev(m, "FT", "Full time: " + score(m));
        sfx("whistle");
        int hp = m.hg > m.ag ? 3 : m.hg == m.ag ? 1 : 0, ap = m.hg < m.ag ? 3 : m.hg == m.ag ? 1 : 0;
        S.table[m.home] += hp; S.table[m.away] += ap;
        if (m.rovers) {
            int rg = m.home == 0 ? m.hg : m.ag, og = m.home == 0 ? m.ag : m.hg;
            S.rovPlayed++;
            if (rg > og) S.rovW++; else if (rg == og) S.rovD++; else S.rovL++;
            int opp = m.home == 0 ? m.away : m.home;
            String h, b;
            if (rg > og) { h = "ROVERS BEAT " + Data.TEAM_SHORT[opp].toUpperCase() + " " + rg + "-" + og; b = "Fans celebrate 'in a manner not suited to the bus stop'."; }
            else if (rg == og) { h = "ROVERS HOLD " + Data.TEAM_SHORT[opp].toUpperCase() + " IN " + rg + "-" + og + " DRAW"; b = "'We'll take it,' said everyone, in a tone of voice that said otherwise."; }
            else { h = "ROVERS BEATEN " + og + "-" + rg + " BY " + Data.TEAM_SHORT[opp].toUpperCase(); b = "Manager says he is 'gutted but proud'. Fans are just gutted."; }
            S.news.add(new News(h, b, S.day));
        }
        if (!S.upgrades.contains("tv") || S.tvBroken) return;
        log("FULL TIME: " + score(m), "match");
        int winner = m.hg > m.ag ? m.home : m.hg < m.ag ? m.away : -1;
        for (Npc n : S.npcs) {
            if (!n.inPub) continue;
            int mine = Data.ti(n.team);
            if (mine < 0 || (mine != m.home && mine != m.away)) continue;
            if (winner == mine) { n.mood = Util.clamp(n.mood + 12); n.exp += 8; n.stayUntil += 40; say(n, Util.pick("What a result!", "Told you! TOLD you!", "Drinks all round!"), 5); n.thirst += 25;
                if (n.gen > .6 && n.money > 25 && Util.chance(.25)) n.drunk += 0; }
            else if (winner >= 0) { n.mood = Util.clamp(n.mood - 14); n.exp -= 6; n.anger += 10; say(n, Util.pick("Rubbish. Absolute rubbish.", "I'm done. Done with football.", "Every week. EVERY week."), 5);
                if (Util.chance(.4 + n.tmp * .3)) { n.stayUntil = S.min; remember(n, "loss", -1, n.id, n.id, n.name + " left in a sulk after " + Data.TEAM_SHORT[mine] + " lost"); stat("leftAfterLoss"); log(n.name + " storms off after the result.", "match"); } }
            else n.mood -= 2;
        }
        if (m.rovers && (m.hg + m.ag) > 0 && crowd() >= 8) stat("matchNightsHosted");
        if (m.rovers) stat("roversMatchesShown");
    }
}
