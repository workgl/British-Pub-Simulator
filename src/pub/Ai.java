package pub;

import java.util.*;
import pub.Model.*;
import static pub.Sim.*;

/** Customer behaviour: planning, arrival, movement, ordering, activities, leaving. */
public final class Ai {
    private Ai() {}

    static final class Game { Npc a, b; String kind; int end; double sa, sb; int nextShout; }
    static final List<Game> games = new ArrayList<>();

    static final double[] DOW_MULT = {.55, .7, .85, .8, 1.4, 1.3, 1.0};

    // ================= planning =================
    static boolean matchToday() { for (Match m : S.fixtures) if (m.day == S.day && (m.rovers || m.big)) return true; return false; }
    static Match matchOfDay() { for (Match m : S.fixtures) if (m.day == S.day && m.rovers) return m; for (Match m : S.fixtures) if (m.day == S.day) return m; return null; }

    static boolean fanOf(Npc n) {
        Match m = S.match;
        if (m == null || m.phase.equals("ft") || S.tvBroken || !S.upgrades.contains("tv")) return false;
        int t = Data.ti(n.team);
        if (t == m.home || t == m.away) return true;
        if (n.sport > .75 && (m.rovers || m.big)) return true;
        return false;
    }

    static boolean fanOfRaw(Npc n, Match m) {
        if (m == null) return false;
        int t = Data.ti(n.team);
        return t == m.home || t == m.away || (n.sport > .75 && (m.rovers || m.big));
    }

    static void planDay(Npc n) {
        n.planGo = false; n.drinksToday = 0; n.spentToday = 0; n.arguedToday = 0; n.planVenue = "pub";
        n.money += n.income / 7.0; if (dow() == 4 && !n.stranger) n.money += n.income / 7.0 * 0.0;
        if (n.absentUntil >= S.day || n.bannedUntil >= S.day) return;
        int dw = dow();
        double p = 0.28 + 0.55 * n.loyalty / 100.0;
        if (((n.dayMask >> dw) & 1) == 0) p *= 0.3;
        if (n.stranger) p = n.visits > 0 ? 0.22 : 0;
        Match md = matchOfDay();
        boolean fan = md != null && (Data.ti(n.team) == md.home || Data.ti(n.team) == md.away || (n.sport > .75 && md.rovers));
        if (fan) p += 0.4; else if (md != null && n.sport > .4) p += 0.15;
        if (dw == 2 && S.quizOn && n.intel > .6) p += 0.4;
        if (dw == 4 && n.money > 15) p += .12;
        if (!Events.specialDay().isEmpty()) p += .25;
        if (n.money < 8) p *= 0.2;
        if (S.weather == 2) p *= .92;
        p *= 0.6 + S.rep / 100.0 * 0.8;
        for (Booking b : S.bookings) if (b.day == S.day && n.soc > .6) p += .15;
        if (n.defected) p *= .25;
        if (p > 0.92) p = .92;
        if (!Util.chance(p)) return;
        n.planGo = true;
        double hr = Util.r(n.arrA, n.arrB);
        if (dw >= 5 && n.workFrom >= 0) hr -= 3;
        if (md != null && fan) { hr = md.kickoff / 60.0 - Util.r(.4, 1.2); n.planStay = (int) (Util.r(2.8, 4)); }
        else if (dw == 2 && S.quizOn && n.intel > .6) hr = Util.r(19, 19.7);
        else if (dw == 6 && S.roastOn) hr = Util.r(12.2, 13.5);
        int at = (int) (hr * 60);
        at = Math.max(at, openMin() + 5);
        if (S.closeH <= 24) at = Math.min(at, closeMin() - 75);
        n.planAt = at;
        n.planStay = (int) Util.r(n.stayA, n.stayB);
        double rivalChance = 0.05 + (S.royalOak ? .08 : 0) + (n.loyalty < 35 ? .3 : 0) + (100 - S.rep) / 700.0 + (n.defected ? .6 : 0) + (S.price[0] > 6 ? .15 : 0);
        if (Util.chance(rivalChance)) {
            n.planVenue = S.royalOak && Util.chance(.5) ? "oak" : "frog";
            stat("rivalVisits");
        }
        if (fan && md != null && md.rovers && Util.chance(.2) && n.money > 20 && md.home == 0) { n.planVenue = "stadium"; n.planAt = md.kickoff + 125; n.mood = 60; }
    }

    static void lifeEvents() {
        for (Npc n : S.npcs) {
            if (n.stranger || !Util.chance(.045)) continue;
            double r = Util.R.nextDouble();
            String f = first(n);
            if (r < .14) { n.income *= 1.12; n.mood += 12; n.lifeNews.add("just got a promotion"); newsFor(n, "LOCAL " + n.job.toUpperCase() + " GETS PROMOTION", f + " reportedly 'a bit insufferable about it'."); }
            else if (r < .26) { n.mood -= 18; n.lifeNews.add("has split up with a partner"); }
            else if (r < .38) { int d = Util.ri(3, 8); n.absentUntil = S.day + d; n.lifeNews.add("just got back from a holiday in " + Util.pick("Tenerife", "Skegness", "Benidorm", "Rhyl", "Cornwall")); n.loc = "station"; }
            else if (r < .50) { n.absentUntil = S.day + Util.ri(2, 4); n.lifeNews.add("had the worst flu of their life"); n.mood -= 8; }
            else if (r < .62) { n.money += 50; n.lifeNews.add("won £50 on a scratchcard"); n.mood += 8; }
            else if (r < .72) { n.money = Math.max(0, n.money - 60); n.lifeNews.add("got a parking fine outside Savemore"); n.mood -= 6; newsFor(n, "PARKING WARDEN 'DOING HIS JOB'", "A fine was issued outside Savemore. Local " + n.job + " described it as 'a scandal'."); }
            else if (r < .82) { n.lifeNews.add("adopted a rescue " + Util.pick("greyhound", "cat", "ferret", "tortoise") + " named " + Util.pick("Gerald", "Biscuit", "Nigel", "Dennis")); n.mood += 10; }
            else if (r < .90) { n.lifeNews.add("has taken up " + Util.pick("pickleball", "cold-water swimming", "sourdough", "amateur dramatics")); }
            else { n.lifeNews.add("got locked out of their house for six hours"); n.mood -= 5; }
            while (n.lifeNews.size() > 4) n.lifeNews.remove(0);
        }
    }
    static void newsFor(Npc n, String h, String b) { if (Util.chance(.5)) S.news.add(new News(h, b, S.day)); }

    // ================= arrivals =================
    static double strangerRate() {
        double h = hour();
        double hc = h < 14 ? .7 : h < 17 ? .35 : h < 19 ? 1.0 : h < 22 ? 1.35 : .6;
        double r = 0.055 * (S.pop / 40.0) * DOW_MULT[dow()] * hc;
        if (S.weather == 2) r *= .85;
        if (dow() == 6 && S.roastOn && h >= 12 && h < 15) r *= 2.2;
        Match m = S.match; if (m != null && !m.phase.equals("ft") && S.upgrades.contains("tv")) r *= (m.rovers ? 2.4 : 1.6) * (S.upgrades.contains("bigscreen") ? 1.25 : 1);
        for (String ad : S.ads.keySet()) if (S.ads.get(ad) >= S.day) r *= 1.15;
        if (S.sat < 40) r *= .6;
        if (!Events.specialDay().isEmpty()) r *= 1.6;
        return r;
    }

    static void arrivals() {
        if (!isOpen()) return;
        for (Npc n : S.npcs) if (!n.inPub && n.planGo && n.planVenue.equals("pub") && S.min == n.planAt) { n.planGo = false; enterPub(n); }
        if (Util.chance(strangerRate())) {
            if (crowd() >= capacity()) { stat("turnedAway"); return; }
            Npc s = Chars.stranger(); Chars.fillDefaults(s);
            s.loyalty = 40; s.lastVisit = S.day; s.planStay = Util.ri(1, 3);
            S.npcs.add(s); byId.put(s.id, s);
            enterPub(s);
        }
    }

    static void enterPub(Npc n) {
        if (n.inPub) return;
        if (crowd() >= capacity()) { stat("turnedAway"); return; }
        if (n.bannedUntil >= S.day) return;
        n.inPub = true; n.st = "walk"; n.x = Nav.DOOR[0] + Util.r(-12, 12); n.y = Nav.DOOR[1] + 14; n.loc = "pub";
        n.arrivedAt = S.min; n.visits++; n.exp = 0; n.conv = "";
        double stay = n.planStay > 0 ? n.planStay : Util.r(n.stayA, n.stayB);
        n.stayUntil = S.min + (int) (stay * 60 * Util.r(.8, 1.2));
        n.timer = 0; n.eating = false; n.order = null; n.glass = -1; n.spot = ""; n.foodT = -1;
        Match m = S.match;
        if (m != null && !m.phase.equals("ft") && fanOf(n)) n.stayUntil = Math.max(n.stayUntil, S.min + (m.phase.equals("pre") ? 120 : 60));
        n.stayUntil = Math.max(n.stayUntil, S.min + 40);
        sfx("door");
        if (n.stranger) { if (Util.chance(.5)) say(n, Util.pick("Evening. What's good here?", "Nice place. Bit... cosy.", "Is that a pigeon on your sign?")); }
        else {
            // greeting
            String line = Util.pick("Evening all!", "Alright, landlord!", "Usual please!", "Is that Wayne on the pumps? Lord help us.", "Packed in here tonight!");
            if (crowd() < 5) line = Util.pick("Quiet in here...", "Where is everybody?", "Just us, then.");
            say(n, line, 3.5);
            if (S.min % 7 == 0) log(n.name + " arrives" + (n.visits > 40 ? " (regular)" : "") + ".", "arrive");
        }
                S.peakCrowd = Math.max(S.peakCrowd, crowd());
        Events.onArrive(n);
        goTo(n, Nav.BARWAIT.get(Util.ri(0, Nav.BARWAIT.size() - 1)).x, 262, "idle", 0);
    }

    // ================= pathing =================
    static void goTo(Npc n, double x, double y, String st, double dur) {
        release(n);
        n.path = Nav.path(n.x, n.y, x, y); n.st = "walk"; n.nextSt = st; n.nextDur = dur;
    }
    static void goSpot(Npc n, Nav.Spot s, String st, double dur) {
        S.occ.put(s.id, n.id); n.path = Nav.path(n.x, n.y, s.x, s.y); n.st = "walk"; n.nextSt = st; n.nextDur = dur;
        String old = n.spot; if (!old.isEmpty() && !old.equals(s.id) && n.id.equals(S.occ.get(old))) S.occ.remove(old);
        n.spot = s.id;
    }
    static void release(Npc n) {
        if (!n.spot.isEmpty()) { if (n.id.equals(S.occ.get(n.spot))) S.occ.remove(n.spot); n.spot = ""; }
    }

    static void move(double dt) {
        for (Npc n : S.npcs) {
            if (!n.inPub) continue;
            if (n.st.equals("walk") && !n.path.isEmpty()) {
                double[] wp = n.path.get(0);
                double dx = wp[0] - n.x, dy = wp[1] - n.y, d = Math.hypot(dx, dy);
                double sp = (68 + n.energy * .25) * (1 - n.drunk * .0025) * dt;
                n.walkPhase += dt * 9;
                if (d <= sp) { n.x = wp[0]; n.y = wp[1]; n.path.remove(0); if (n.path.isEmpty()) arrive(n); }
                else { n.x += dx / d * sp; n.y += dy / d * sp; if (n.drunk > 40) n.x += Math.sin(rt * 5 + n.walkPhase) * n.drunk * .006; }
            }
        }
        // player & pigeon handled elsewhere
    }

    static void arrive(Npc n) {
        n.st = n.nextSt; n.timer = n.nextDur;
        switch (n.st) {
            case "idle" -> decide(n);
            case "waitBar" -> { if (n.order != null && !S.queue.contains(n.id)) S.queue.add(n.id); n.order.since = S.min; }
            case "leave" -> exit(n);
            case "juke" -> jukeUse(n);
            case "loo" -> { n.bladder = 0; }
            default -> {}
        }
    }

    // ================= minute behaviour =================
    static String loc(Npc n) { return n.loc; }

    static void minute(Npc n) {
        if (!n.inPub) { awayStep(n); return; }
        n.thirst = Math.min(100, n.thirst + .35);
        n.hunger = Math.min(100, n.hunger + .12);
        n.drunk = Math.max(0, n.drunk - .08);
        n.energy = Math.max(0, n.energy - .03);
        n.anger = Math.max(0, n.anger - .25); n.embar = Math.max(0, n.embar - .3);
        double target = 52 + n.soc * 6 + (S.clean - 60) * .1 + (S.powerCut ? -6 : 0) - (n.thirst > 80 ? 8 : 0) - n.anger * .25 + (n.drunk > 20 && n.drunk < 60 ? 8 : 0) + (S.sat - 50) * .05;
        if (n.eating) target += 4;
        n.mood += (target - n.mood) * .04; n.mood = Util.clamp(n.mood);
        if (n.glass >= 0 && !n.st.equals("walk")) {
            n.glassLevel -= n.st.equals("sit") ? .045 : .03;
            if (n.glassLevel <= 0) { n.glass = -1; n.bladder += 6; }
        } else if (n.glass >= 0) n.glassLevel -= .01;
        if (n.glass >= 0 && n.glassLevel <= 0) { n.glass = -1; n.bladder += 6; }
        // music taste
        if (!S.playingGenre.isEmpty() && Util.chance(.06)) {
            double a = n.music.getOrDefault(S.playingGenre, 0.0);
            n.mood += a * 3; n.exp += a * .6;
            if (Util.chance(.12) && Math.abs(a) > .5) say(n, a > 0 ? Util.pick("Oh, I LOVE this one!", "Turn it up!", "Ooh, banger.") : Util.pick("Who put this on?!", "Is this a joke?", "Make it stop."));
        }
        if (n.st.equals("waitBar") && n.order != null) {
            double w = S.min - n.order.since;
            if (w > 6) { n.exp -= .7; n.mood -= .4; n.anger += .5; }
            if (w == 10 && Util.chance(.5)) say(n, Util.pick("Any chance of a drink today?", "I've aged waiting here.", "Is anyone working?", "Oi! Landlord!"));
            if (w > 20) { S.queue.remove(n.id); n.order = null; n.exp -= 15; n.anger += 20; remember(n, "wait", -3, "player", "player", n.name + " gave up waiting to be served"); stat("walkouts"); say(n, "Forget it, I'm going elsewhere!", 4); goLeave(n); return; }
        }
        // quirk
        if (n.quirk.length > 0 && !n.st.equals("walk") && --n.quirkT <= 0) {
            n.quirkT = Math.max(8, (int) (n.quirkEvery * Util.r(.75, 1.25)));
            Social.quirk(n);
        }
        // food
        if (n.foodT > 0 && --n.foodT == 0) { n.eating = true; n.hunger = 0; n.timer = Math.max(n.timer, 12); say(n, Util.pick("Ooh, that looks good.", "Finally!", "Lovely.")); sfx("plate"); }
        if (n.eating && n.foodT == 0) { if (--n.nextDur <= -14) { n.eating = false; n.foodT = -1; n.nextDur = 0; } }
        // closing
        if (!isOpen() && n.stayUntil > S.min + 25) n.stayUntil = S.min + Util.ri(0, 20);
        // timers
        if (n.st.equals("walk")) return;
        if (n.timer > 0) n.timer -= 1;
        if (n.timer <= 0) { finishState(n); }
        else if (n.st.equals("sit") || n.st.equals("stand")) {
            if (wantsLeave(n) && Util.chance(.2)) goLeave(n);
            else if (n.glass < 0 && wantsDrink(n) && Util.chance(.25)) goBar(n);
        }
        // karaoke/quiz hooks etc handled by Events
    }

    static void finishState(Npc n) {
        switch (n.st) {
            case "darts", "pool" -> endGame(n);
            case "waitBar" -> {}
            case "leave" -> {}
            default -> decide(n);
        }
    }

    // ================= decisions =================
    static boolean wantsLeave(Npc n) {
        if (!isOpen() && S.min > n.stayUntil) return true;
        if (S.lastOrders && n.glass < 0 && n.order == null) return true;
        if (S.min >= n.stayUntil && !(fanOf(n) && S.match != null) && n.conv.isEmpty()) return true;
        if (n.money < 3 && n.glass < 0) return true;
        if (n.drunk > 92) return true;
        if (n.anger > 82) return true;
        if (n.energy < 8) return true;
        return false;
    }

    static boolean wantsDrink(Npc n) {
        if (n.order != null) return false;
        if (n.glass >= 0 && n.glassLevel > .2) return false;
        if (n.drinksToday >= 8 || n.drunk > 88) return false;
        if (S.lastOrders && S.min > closeMin() - 4) return false;
        double minPrice = 2.6;
        if (n.money < minPrice) return false;
        return n.thirst > 38 || (n.glass < 0 && n.soc > .55 && Util.chance(.3));
    }

    static void decide(Npc n) {
        if (!n.inPub) return;
        if (wantsLeave(n)) { goLeave(n); return; }
        if (n.bladder > 72 && Util.chance(.85)) {
            goTo(n, Nav.LOO[0] + Util.r(-10, 10), Nav.LOO[1] + 6, "loo", Util.r(2, 5)); n.doing = "In the loos"; return;
        }
        if (wantsDrink(n)) { goBar(n); return; }
        if (n.hunger > 72 && foodOn() && n.money > 13 && !n.eating && n.foodT < 0 && Util.chance(.5)) { orderFood(n); return; }
        if (fanOf(n) && S.match != null) { goTV(n); return; }
        // weighted activity choice
        String[] k = {"sit", "stand", "darts", "pool", "juke", "stool", "wander"};
        double[] w = new double[k.length];
        w[0] = 3 + (1 - n.soc) * 2;
        w[1] = 1.2 + n.soc;
        w[2] = S.upgrades.contains("darts") && freeGame("darts") && n.drunk < 75 ? .9 + n.darts * .12 : 0;
        w[3] = S.upgrades.contains("pool") && freeGame("pool") && n.drunk < 75 && n.money > 1 ? .9 + n.pool * .12 : 0;
        w[4] = S.upgrades.contains("jukebox") && n.money > 2 && !Mgmt.songPlaying() ? .5 + n.gen * .8 : 0;
        w[5] = 1 + n.soc;
        w[6] = .6;
        double tot = 0; for (double x : w) tot += x;
        double r = Util.r(0, tot); int c = 0;
        for (; c < w.length - 1; c++) { r -= w[c]; if (r <= 0) break; }
        switch (k[c]) {
            case "sit" -> { Nav.Spot s = findSeat(n, false); if (s != null) { goSpot(n, s, "sit", Util.r(10, 26)); n.doing = "Sitting at " + (s.table.equals("A") ? "the fireside" : "a table"); } else standSomewhere(n); }
            case "stool" -> { Nav.Spot s = findSeat(n, true); if (s != null) { goSpot(n, s, "sit", Util.r(10, 26)); n.doing = "On a bar stool"; } else standSomewhere(n); }
            case "stand" -> standSomewhere(n);
            case "darts" -> startGame(n, "darts");
            case "pool" -> startGame(n, "pool");
            case "juke" -> { goTo(n, Nav.JUKE[0], Nav.JUKE[1], "juke", 1); n.doing = "At the jukebox"; }
            default -> { Nav.Spot s = Nav.STAND.get(Util.ri(0, Nav.STAND.size() - 1)); goTo(n, s.x, s.y, "idle", 0); n.doing = "Wandering about"; }
        }
    }

    static void standSomewhere(Npc n) {
        List<Nav.Spot> fr = new ArrayList<>(); for (Nav.Spot s : Nav.STAND) if (!S.occ.containsKey(s.id)) fr.add(s);
        if (fr.isEmpty()) { n.timer = 3; n.st = "stand"; return; }
        Nav.Spot s = Util.wpick(fr, sp -> { double w = 1; for (Npc o : S.npcs) if (o.inPub && o != n && Util.dist(o.x, o.y, sp.x, sp.y) < 90) { double r = rel(n.id, o.id); w += r > 25 ? 2 : r < -30 ? -.7 : .2; } return Math.max(.1, w); });
        goSpot(n, s, "stand", Util.r(6, 18)); n.doing = "Standing around chatting";
    }

    static Nav.Spot findSeat(Npc n, boolean stool) {
        List<Nav.Spot> c = new ArrayList<>();
        for (Nav.Spot s : Nav.SEATS) if (!S.occ.containsKey(s.id) && s.kind.equals("stool") == stool) c.add(s);
        if (c.isEmpty()) return null;
        return Util.wpick(c, s -> {
            double w = 1;
            for (Npc o : S.npcs) if (o.inPub && o != n && o.spot.startsWith(s.table + ".")) { double r = rel(n.id, o.id); w += r > 30 ? 3 : r < -30 ? -1.5 : .4; }
            if (n.id.equals("steve") && s.table.equals("T1")) w *= 8;
            if (n.id.equals("maggie") && s.id.equals("A0")) w *= 12;
            if (n.id.equals("dave") && s.id.equals("S2")) w *= 12;
            if (S.match != null && fanOf(n) && s.table.matches("T[012]")) w *= 2;
            return Math.max(.05, w);
        });
    }

    static boolean foodOn() { return S.upgrades.contains("kitchen") && hasStaff("chef") && S.stock[8] > 0; }

    static void goTV(Npc n) {
        List<Nav.Spot> fr = new ArrayList<>(); for (Nav.Spot s : Nav.TV) if (!S.occ.containsKey(s.id)) fr.add(s);
        if (fr.isEmpty()) { Nav.Spot s = findSeat(n, false); if (s != null) goSpot(n, s, "tv", 8); else { n.st = "tv"; n.timer = 6; } n.doing = "Watching the match"; return; }
        // sit near same-team fans
        Nav.Spot s = Util.wpick(fr, sp -> { double w = 1; for (Npc o : S.npcs) if (o.inPub && o != n && Util.dist(o.x, o.y, sp.x, sp.y) < 80) { if (!o.team.isEmpty() && o.team.equals(n.team)) w += 2; else if (rel(n.id, o.id) < -25) w += .1; } return w; });
        goSpot(n, s, "tv", Util.r(8, 14)); n.doing = "Watching the match";
    }

    // ================= bar =================
    static int chooseDrink(Npc n) {
        int fav = Data.di(n.fav);
        int pick = fav;
        if (n.drunk > 70 && Util.chance(.5)) pick = 6;
        if (n.frug > .85 && n.money < 20) { int best = 0; for (int i = 0; i < Data.BEVS - 1; i++) if (S.price[i] < S.price[best] && S.stock[i] > 0 && Data.DABV[i] > 0) best = i; pick = best; }
        if (S.stock[pick] <= 0) {
            List<Integer> alts = new ArrayList<>(); for (int i = 0; i < Data.BEVS; i++) if (S.stock[i] > 0 && (i != 6 || n.id.equals("tony"))) alts.add(i);
            if (alts.isEmpty()) return -1;
            n.exp -= 5; pick = Util.pick(alts); stat("stockSwaps");
        }
        return pick;
    }

    static void goBar(Npc n) {
        int d = chooseDrink(n);
        if (d < 0) { n.exp -= 15; n.anger += 12; say(n, Util.pick("You've got NOTHING?!", "No beer at all? What kind of pub is this?")); stat("stockouts"); remember(n, "stockout", -3, "player", "player", n.name + " was furious the pub had run out of everything"); n.stayUntil = S.min; return; }
        double price = S.price[d], fair = Data.DPRICE[d];
        double tol = 1.1 + (1 - n.frug) * .35;
        double ratio = price / fair;
        if (ratio > tol * 1.25) {
            n.exp -= 10; n.loyalty -= 1.5; n.mood -= 6;
            say(n, Util.fill(Util.pick("{p} for a pint?! You're having a laugh.", "{p}?! Daylight robbery."), "p", Util.money(price)));
            remember(n, "ripoff", -3, "player", "player", n.name + " thought the prices were daylight robbery");
            stat("priceWalk"); n.stayUntil = Math.min(n.stayUntil, S.min + 10); n.thirst -= 15; return;
        }
        if (ratio > tol) { n.exp -= 4; n.mood -= 3; if (Util.chance(.5)) say(n, Util.pick("Bit steep, this.", "It's not cheap, is it?", "Prices... wow.")); }
        else if (ratio < .85) { n.exp += 3; if (Util.chance(.3)) say(n, "Cheap pints! I love this place."); }
        if (n.money < price) { n.thirst -= 10; return; }
        Order o = new Order(); o.drink = d; o.since = S.min;
        // round buying
        if (n.gen > .55 && n.frug < .5 && n.money > price * 3 && Util.chance(.1) && d < 6) {
            for (Npc f : S.npcs) if (f.inPub && f != n && friends(n.id, f.id) && f.glass < 0 && f.order == null && o.round.size() < 3 && Util.dist(n.x, n.y, f.x, f.y) < 260) o.round.add(f.id);
            o.qty = 1 + o.round.size();
            if (o.qty > 1) say(n, Util.pick("I'm getting a round in!", "Same again for everyone — on me.", "Put your wallet away, I'm buying."));
        }
        n.order = o;
        List<Nav.Spot> fr = new ArrayList<>(); for (Nav.Spot s : Nav.BARWAIT) if (!S.occ.containsKey(s.id)) fr.add(s);
        n.doing = "Waiting at the bar";
        if (fr.isEmpty()) { goTo(n, Util.r(60, 330), 262 + Util.r(0, 14), "waitBar", 0); }
        else goSpot(n, Util.pick(fr), "waitBar", 0);
    }

    static void orderFood(Npc n) {
        Order o = new Order(); o.drink = 8; o.food = true; o.since = S.min; n.order = o;
        Nav.Spot s = Util.pick(Nav.BARWAIT); n.doing = "Ordering food";
        goTo(n, s.x, s.y, "waitBar", 0);
    }

    static boolean behindBar() { return Player.x > 40 && Player.x < 400 && Player.y > 120 && Player.y < 170; }

    static void barService() {
        if (behindBar() && !S.queue.isEmpty() && S.min % 2 == 0) {
            for (String id : new ArrayList<>(S.queue)) { Npc n = byId.get(id); if (n != null && n.inPub && n.st.equals("waitBar") && n.order != null) { serve(n, null); Player.say(Util.pick("Next!", "What can I get you?", "Pint? Coming up.")); break; } }
        }
        for (Staff st : S.staff) {
            if (!st.present) continue;
            if (!(st.role.equals("bartender") || st.role.equals("manager"))) continue;
            if (st.busy) {
                st.busyT -= 1;
                if (st.busyT <= 0) { st.busy = false; Npc n = byId.get(st.note); st.note = ""; if (n != null && n.order != null && n.inPub) serve(n, st); }
                continue;
            }
            for (Iterator<String> it = S.queue.iterator(); it.hasNext(); ) {
                String id = it.next(); Npc n = byId.get(id);
                if (n == null || !n.inPub || n.order == null) { it.remove(); continue; }
                if (!n.st.equals("waitBar")) continue;
                boolean taken = false; for (Staff o : S.staff) if (o.busy && id.equals(o.note)) taken = true;
                if (taken) continue;
                it.remove();
                st.busy = true; st.note = id;
                st.busyT = Math.max(1, 3.6 - st.skill * .27 - st.speed * .15 + (crowd() > 22 ? 1 : 0));
                break;
            }
        }
    }

    public static void serve(Npc n, Staff by) {
        Order o = n.order; if (o == null) return;
        S.queue.remove(n.id);
        int d = o.drink, q = o.qty;
        if (S.stock[d] < q) { // partial stockout
            q = Math.max(0, S.stock[d]);
            if (q == 0) { n.order = null; n.exp -= 14; n.anger += 10; say(n, Util.pick("What do you mean you've run out?!", "No " + Data.DNAME[d].toLowerCase() + "? Seriously?")); stat("stockouts"); remember(n, "stockout", -3, "player", "player", n.name + " was annoyed the pub ran out of " + Data.DNAME[d].toLowerCase()); L.alert("Out of " + Data.DNAME[d] + "!"); finishServe(n); return; }
        }
        double price = S.price[d] * q;
        n.money -= price; addMoney(price); S.stock[d] -= q; S.servedToday += q; S.served += q; n.spentToday += price; n.spot = n.spot;
        stat("served", q); if (d < 7 && Data.DABV[d] > 0) { stat("pints", q); S.totalPints += q; }
        sfx(by == null ? "till" : "pour");
        if (by != null) { double ch = (by.charm - 5) * .5; n.mood += ch; n.exp += by.charm * .3; by.rel.merge(n.id, 1.0, Double::sum); }
        else { n.trust = Util.clamp(n.trust + 1.5, -100, 100); n.mood += 2; n.exp += 3; }
        double mistake = by == null ? .01 : Math.max(0, 10 - by.skill) * .011 + (crowd() > 22 ? .05 : 0) + (by.mood < 40 ? .04 : 0);
        if (Util.chance(mistake) && !o.food) {
            n.exp -= 8; n.mood -= 5; say(n, Util.pick("That's not what I ordered!", "Is this even my drink?", "I asked for " + Data.DNAME[d] + ", not whatever this is.")); if (by != null) { by.mistakes++; by.mood -= 3; }
            stat("mistakes");
        }
        if (o.food) {
            S.totalMeals += q; stat("meals", q);
            Staff chef = staffByRole("chef"); int sk = chef == null ? 3 : chef.skill;
            n.foodT = Math.max(6, 22 - sk); n.nextDur = 0; n.exp += 2;
            if (chef != null && Util.chance(Math.max(0, 6 - sk) * .06)) { n.exp -= 10; n.mood -= 8; stat("badMeals"); n.flags.put("badMeal", "1"); }
        } else {
            n.glass = d; n.glassLevel = 1; n.thirst = Math.max(0, n.thirst - 45); n.drinksToday++;
            n.drunk = Util.clamp(n.drunk + Data.DABV[d] * 5 * (1 - n.tol * .45), 0, 100);
            if (d == Data.di(n.fav)) n.mood += 3;
            for (String id : o.round) {
                Npc f = byId.get(id); if (f == null || !f.inPub) continue;
                f.glass = d; f.glassLevel = 1; f.thirst = Math.max(0, f.thirst - 45); f.drinksToday++; f.drunk = Util.clamp(f.drunk + Data.DABV[d] * 5 * (1 - f.tol * .45), 0, 100);
                addRel(f.id, n.id, 4); f.mood += 6; remember(f, "treat", 3, n.id, n.id, n.name + " bought " + f.name + " a drink"); say(f, Util.pick("Cheers, " + first(n) + "!", "You're a gent.", "Ooh, don't mind if I do!"));
            }
            if (n.mood > 55 && Util.chance(.06)) { Mess ms = new Mess(); ms.x = n.x + Util.r(-10, 10); ms.y = n.y + 16; ms.made = S.min; S.messes.add(ms); }
            if (Util.chance(.015)) Events.flatPint(n);
        }
        n.order = null;
        finishServe(n);
    }
    static void finishServe(Npc n) {
        n.st = "idle"; n.timer = 0; n.doing = "";
        decide(n);
    }

    // ================= games =================
    static boolean freeGame(String kind) { for (Game g : games) if (g.kind.equals(kind)) return false; return true; }

    static void startGame(Npc n, String kind) {
        List<Npc> cand = new ArrayList<>();
        for (Npc o : S.npcs) if (o.inPub && o != n && (o.st.equals("sit") || o.st.equals("stand") || o.st.equals("idle")) && o.drunk < 80 && o.order == null && rel(n.id, o.id) > -80 && o.conv.isEmpty()) cand.add(o);
        if (cand.isEmpty()) { standSomewhere(n); return; }
        Npc b = Util.wpick(cand, o -> 1 + Math.max(0, rel(n.id, o.id) + 20) / 25 + (rivals(n.id, o.id) ? 2 : 0));
        Game g = new Game(); g.a = n; g.b = b; g.kind = kind; g.end = S.min + Util.ri(12, 22); g.nextShout = S.min + 3;
        games.add(g);
        if (kind.equals("darts")) { goTo(n, Nav.OCHE[0], Nav.OCHE[1], "darts", g.end - S.min); goTo(b, Nav.OCHE[0] - 28, Nav.OCHE[1] + 40, "darts", g.end - S.min); }
        else { goTo(n, Nav.POOL[0][0], Nav.POOL[0][1], "pool", g.end - S.min); goTo(b, Nav.POOL[2][0], Nav.POOL[2][1], "pool", g.end - S.min); }
        n.doing = "Playing " + kind + " vs " + first(b); b.doing = "Playing " + kind + " vs " + first(n);
        say(n, Util.pick("Fancy a game, " + first(b) + "?", "Rack 'em up!", "Loser buys.")); say(b, Util.pick("You're on.", "Prepare to be destroyed.", "Easy money."), 5);
        if (kind.equals("pool")) { addMoney(1); sfx("coin"); }
        log(first(n) + " challenges " + first(b) + " at " + kind + ".", "game");
    }

    static void endGame(Npc n) {
        for (Iterator<Game> it = games.iterator(); it.hasNext(); ) {
            Game g = it.next();
            if (g.a != n && g.b != n) continue;
            it.remove();
            int ska = g.kind.equals("darts") ? g.a.darts : g.a.pool, skb = g.kind.equals("darts") ? g.b.darts : g.b.pool;
            double sa = ska + Util.r(-3, 3) - g.a.drunk * .02, sb = skb + Util.r(-3, 3) - g.b.drunk * .02;
            Npc w = sa >= sb ? g.a : g.b, l = w == g.a ? g.b : g.a;
            w.mood += 8; l.mood -= 5; l.anger += 6 + l.tmp * 8;
            say(w, Util.pick("Easy!", "Get in!", "That's how it's done!", "Pay up, " + first(l) + ".")); say(l, Util.pick("Fluke.", "Best of three?", "Table's wonky.", "I was distracted."), 5);
            addRel(w.id, l.id, 1.5); addRel(l.id, w.id, l.tmp > .5 ? -2 : 0.5);
            remember(w, "game", 1, l.id, l.id, w.name + " beat " + l.name + " at " + g.kind);
            remember(l, "game", -1, w.id, w.id, l.name + " lost at " + g.kind + " to " + w.name);
            if (g.kind.equals("darts") && (w.id.equals("sarah") && g.b.darts < 6 || g.a.id.equals("sarah") && g.b.darts < 6)) { /* hidden talent hint */ if (w.id.equals("sarah") && Util.chance(.35)) say(w, "Oh... was that good? Beginner's luck."); }
            log(w.name + " beat " + l.name + " at " + g.kind + ".", "game"); stat("npcGames");
            Social.gameAftermath(w, l, g.kind);
            decide(g.a); decide(g.b);
            return;
        }
        decide(n);
    }

    // ================= jukebox =================
    static void jukeUse(Npc n) {
        if (n.money < 1 || Mgmt.songPlaying()) return;
        List<Integer> idx = new ArrayList<>(); for (int i = 0; i < Data.SONGS.length; i++) idx.add(i);
        Integer p = Util.wpick(idx, i -> 1 + 2 * Math.max(0, n.music.getOrDefault(Data.SONGS[i][2], 0.0)) + 1);
        if (p == null) return;
        n.money -= 1; addMoney(1);
        Mgmt.playSong(p, n);
    }

    // ================= leaving =================
    static void goLeave(Npc n) {
        if (n.st.equals("leave")) return;
        if (n.order != null) { S.queue.remove(n.id); n.order = null; }
        endGameFor(n);
        n.conv = ""; n.doing = "Heading out";
        goTo(n, Nav.DOOR[0], Nav.DOOR[1] + 10, "leave", 0);
        if (!isOpen() && Util.chance(.4)) say(n, Util.pick("Night, all!", "Right, that's me.", "Cheers, landlord!"));
        else if (n.anger > 50) say(n, Util.pick("I'm off. Rubbish tonight.", "Forget this.", "I've had enough."));
        else if (Util.chance(.3)) say(n, Util.pick("Right, I'm off.", "See you tomorrow.", "Better get home.", "Same time next week."));
    }
    static void endGameFor(Npc n) {
        List<Npc> others = new ArrayList<>();
        for (Iterator<Game> it = games.iterator(); it.hasNext(); ) { Game g = it.next(); if (g.a == n || g.b == n) { it.remove(); others.add(g.a == n ? g.b : g.a); } }
        for (Npc o : others) if (o.inPub && (o.st.equals("darts") || o.st.equals("pool"))) { o.timer = 0; decide(o); }
    }

    static void exit(Npc n) {
        n.inPub = false; n.st = "away"; release(n); n.glass = -1; n.eating = false; n.foodT = -1; n.conv = ""; n.path.clear();
        n.loc = Util.chance(.15) ? "takeaway" : (Util.chance(.1) ? "station" : "houses");
        n.locUntil = S.day * 1440 + S.min + 60;
        Social.leaveHook(n);
        visitEnd(n);
        sfx("doorclose");
    }

    static void visitEnd(Npc n) {
        double score = Util.clamp(50 + n.exp + (n.mood - 50) * .6, 0, 100);
        S.satSum += score; S.satN++;
        S.sat = S.sat * .94 + score * .06;
        n.loyalty = Util.clamp(n.loyalty + (score - 55) * .05, 0, 100);
        n.lastVisit = S.day; n.timer = 0;
        stat("customers");
        if (n.stranger) {
            if (n.visits >= 3 && n.loyalty > 55) {
                n.stranger = false; n.promotedDay = S.day;
                news("NEW REGULAR AT THE SPECKLED PIGEON", n.name + ", a local " + n.job + ", is now 'pretty much part of the furniture'.");
                log(n.name + " is now a regular.", "story"); stat("newRegulars");
            } else if (n.visits == 1 && score < 55 && Util.chance(.7)) n.flags.put("gone", "1");
        }
        if (!n.stranger && n.loyalty < 15 && !n.defected) {
            n.defected = true; n.planGo = false;
            log(n.name + " has stopped coming. Seen at the Frog & Trumpet.", "story");
            news("REGULAR DEFECTS TO FROG & TRUMPET", n.name + " is understood to have 'had enough'. Frog & Trumpet declined to gloat. Slightly.");
            stat("defections");
        }
        if (n.defected && n.loyalty > 45) { n.defected = false; log(n.name + " is back!", "story"); }
        if (!n.stranger && n.visits >= 80 && n.loyalty >= 85 && !n.flags.containsKey("lifelong")) {
            n.flags.put("lifelong", "1"); stat("lifelongs");
            log(n.name + " has become a lifelong regular.", "story"); news("LIFELONG REGULAR: " + n.name.toUpperCase(), "'I'll be here till they carry me out,' said " + n.name + ". 'Preferably after closing.'");
        }
    }

    // ================= offstage =================
    static void awayStep(Npc n) {
        int stamp = S.day * 1440 + S.min;
        double h = S.min / 60.0;
        if (n.planGo && !n.planVenue.equals("pub") && S.min == n.planAt) {
            n.planGo = false;
            n.loc = n.planVenue.equals("stadium") ? "stadium" : n.planVenue; n.locUntil = stamp + n.planStay * 60;
            if (n.planVenue.equals("stadium")) { n.loc = "houses"; Match m = matchOfDay(); if (m != null) { n.mood += m.hg > m.ag == (m.home == Data.ti(n.team)) ? 14 : -10; } if (isOpen()) { n.planAt = S.min + 5; n.planGo = true; n.planVenue = "pub"; } }
            else if (Util.chance(.12)) news("LOCAL SPOTTED AT " + (n.planVenue.equals("oak") ? "THE ROYAL OAK" : "THE FROG & TRUMPET"), n.name + " was seen there. 'Just for the one,' they said.");
            return;
        }
        if (stamp < n.locUntil) return;
        boolean workDay = n.workFrom >= 0 && ((n.workMask >> dow()) & 1) == 1;
        if (h < 6.5 || h >= 23.5) { n.loc = "houses"; n.locUntil = stamp + 120; }
        else if (workDay && h >= n.workFrom && h < n.workTo) { n.loc = workLoc(n); n.locUntil = stamp + 60; }
        else {
            String[] hang = n.id.equals("alan") ? new String[]{"church", "church", "houses", "shop"} : n.id.equals("mo") ? new String[]{"takeaway", "takeaway", "cafe"} : n.id.equals("kev") ? new String[]{"station", "cafe", "park", "houses"}
                : new String[]{"houses", "park", "shop", "cafe", "super", "station", "houses"};
            n.loc = hang[Util.ri(0, hang.length - 1)]; n.locUntil = stamp + Util.ri(45, 120);
            if (n.absentUntil >= S.day && n.loc.equals("station")) n.loc = "houses";
        }
    }
    static String workLoc(Npc n) {
        String w = n.workplace;
        if (w.contains("Savemore")) return "super"; if (w.contains("General")) return "hospital"; if (w.contains("Uni")) return "uni"; if (w.contains("Council")) return "council";
        if (n.id.equals("mo")) return "takeaway"; return "work";
    }

    // ================= pigeon & mess =================
    static void petsAndMess() {
        // mess aging
        S.messes.removeIf(m -> S.min - m.made > 600 && Util.chance(.01));
        double crowdPenalty = crowd() * .0016 + S.messes.size() * .006;
        S.clean = Util.clamp(S.clean - crowdPenalty, 0, 100);
        Staff c = staffByRole("cleaner");
        if (c != null && isOpen() && S.clean < 96) { S.clean = Util.clamp(S.clean + .03 + c.skill * .006, 0, 100); if (!S.messes.isEmpty() && Util.chance(.08 + c.skill * .01)) S.messes.remove(0); }
        for (Npc n : S.npcs) if (n.inPub && n.drunk > 55 && Util.chance(.0016 * n.drunk / 50)) { Mess m = new Mess(); m.x = n.x; m.y = n.y + 14; m.made = S.min; S.messes.add(m); if (Util.chance(.3)) say(n, Util.pick("Oops.", "Whoops, sorry!", "That wasn't me.")); }
        if (S.messes.size() > 14) S.messes.remove(0);
    }
}
