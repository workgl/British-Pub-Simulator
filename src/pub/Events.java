package pub;

import java.util.*;
import java.util.function.*;
import pub.Model.*;
import static pub.Sim.*;

/** Random events, nightly events (quiz, karaoke, bands) and storylines. */
public final class Events {
    private Events() {}

    public static boolean modalOpen;
    static final Map<String, Integer> cooldown = new HashMap<>();

    static final class Ev { String id; DoubleSupplier w; Runnable run; int cd; Ev(String i, DoubleSupplier w, Runnable r, int cd) { id = i; this.w = w; run = r; this.cd = cd; } }
    static final List<Ev> EVENTS = new ArrayList<>();

    // ---------- helpers ----------
    static List<Npc> here(Predicate<Npc> p) { List<Npc> l = new ArrayList<>(); for (Npc n : S.npcs) if (n.inPub && p.test(n)) l.add(n); return l; }
    static Npc anyIn(Predicate<Npc> p) { List<Npc> l = here(p); return l.isEmpty() ? null : Util.pick(l); }
    static Npc reg() { return anyIn(n -> !n.stranger && !n.st.equals("walk")); }
    static void done(String text) { log(text, "event"); toast(text); }
    static void ask(String title, String text, Opt... opts) {
        Choice c = new Choice(title, text); for (Opt o : opts) c.opts.add(o); modalOpen = true; Sim.ask(c);
    }
    static Opt opt(String l, Runnable r) { return new Opt(l, r); }
    static Opt opt(String l, String hint, Runnable r) { return new Opt(l, hint, r); }
    static void rep(double d) { S.rep = Util.clamp(S.rep + d); }
    static void allMood(double d) { for (Npc n : S.npcs) if (n.inPub) n.mood = Util.clamp(n.mood + d); }
    static void trust(Npc n, double d) { n.trust = Util.clamp(n.trust + d, -100, 100); }
    static Npc leaveToPlayer(Npc n) { return n; }
    static boolean busyNight() { return crowd() >= 6; }

    static {
        EVENTS.add(new Ev("loo", () -> crowd() >= 5 ? 1.0 : 0, Events::lockedLoo, 5));
        EVENTS.add(new Ev("footyRow", () -> S.match != null && !S.match.phase.equals("ft") && crowd() >= 6 ? 1.6 : 0, Events::footyRow, 3));
        EVENTS.add(new Ev("proposal", () -> !S.couples.isEmpty() && crowd() >= 8 ? .5 : 0, Events::proposal, 40));
        EVENTS.add(new Ev("lostWallet", () -> crowd() >= 6 ? .8 : 0, Events::lostWallet, 6));
        EVENTS.add(new Ev("foundWallet", () -> crowd() >= 6 ? .6 : 0, Events::foundWallet, 8));
        EVENTS.add(new Ev("celebrity", () -> crowd() >= 8 && S.rep > 30 ? .12 : 0, Events::celebrity, 60));
        EVENTS.add(new Ev("powerCut", () -> crowd() >= 6 ? .18 : 0, Events::powerCut, 30));
        EVENTS.add(new Ev("dartsChallenge", () -> hasUp("darts") && crowd() >= 5 ? .9 : 0, Events::dartsChallenge, 5));
        EVENTS.add(new Ev("karaokeImpromptu", () -> crowd() >= 8 && dow() >= 4 && S.min > 20 * 60 ? .7 : 0, Events::spontaneousSong, 6));
        EVENTS.add(new Ev("dog", () -> crowd() >= 5 ? .7 : 0, Events::dog, 6));
        EVENTS.add(new Ev("tvBreaks", () -> S.match != null && "1st2nd".contains(S.match.phase) && !S.tvBroken && S.upgrades.contains("tv") ? .9 : 0, Events::tvBreaks, 15));
        EVENTS.add(new Ev("longStory", () -> crowd() >= 6 ? .7 : 0, () -> longStory(anyIn(n -> n.id.equals("kev") || n.age > 55)), 6));
        EVENTS.add(new Ev("police", () -> crowd() >= 8 ? .15 : 0, Events::police, 40));
        EVENTS.add(new Ev("bet", () -> crowd() >= 8 ? .8 : 0, Events::bet, 7));
        EVENTS.add(new Ev("review", () -> S.day > 10 ? .2 : 0, Events::review, 35));
        EVENTS.add(new Ev("rivalOpens", () -> !S.royalOak && S.day > 20 && S.rep > 25 ? .1 : 0, Events::rivalOpens, 999));
        EVENTS.add(new Ev("stagDo", () -> (dow() == 4 || dow() == 5) && S.min > 18 * 60 && crowd() < 24 ? .7 : 0, Events::stagDo, 25));
        EVENTS.add(new Ev("birthday", () -> crowd() >= 6 ? .5 : 0, Events::birthday, 10));
        EVENTS.add(new Ev("lottery", () -> crowd() >= 7 ? .35 : 0, Events::lottery, 25));
        EVENTS.add(new Ev("tourists", () -> crowd() < 20 && season() >= 2 ? .5 : 0, Events::tourists, 20));
        EVENTS.add(new Ev("inspector", () -> S.day > 7 ? .2 : 0, Events::inspector, 40));
        EVENTS.add(new Ev("pipe", () -> S.day > 5 ? .2 : 0, Events::burstPipe, 30));
        EVENTS.add(new Ev("ghost", () -> S.min > 21 * 60 ? .08 : 0, Events::ghost, 90));
        EVENTS.add(new Ev("spark", () -> crowd() >= 6 ? .6 : 0, Events::spark, 8));
        EVENTS.add(new Ev("tab", () -> S.flags.containsKey("steveBroke") || (byId.get("steve") != null && byId.get("steve").flags.containsKey("broke")) ? .5 : 0, Events::tab, 12));
        EVENTS.add(new Ev("breweryRep", () -> S.day > 6 ? .25 : 0, Events::breweryRep, 30));
        EVENTS.add(new Ev("thief", () -> !hasUp("cctv") && crowd() >= 12 ? .3 : 0, Events::thief, 20));
        EVENTS.add(new Ev("staffTrouble", () -> S.staff.size() > 0 ? .5 : 0, Events::staffTrouble, 10));
        EVENTS.add(new Ev("mouse", () -> hasUp("kitchen") ? .2 : 0, Events::mouse, 40));
        EVENTS.add(new Ev("fridayMan", () -> S.fridayMan.isEmpty() && dow() == 4 && S.day > 5 ? 1.5 : 0, Events::fridayManArrives, 999));
    }

    // ---------- scheduling ----------
    public static void dayStart() {
        for (String k : new ArrayList<>(cooldown.keySet())) if (cooldown.get(k) < S.day) cooldown.remove(k);
        S.nextEventCheck = 0;
        if (Util.chance(.07)) { S.deliveryLate = true; }
        S.flags.put("pigeonAt", Util.chance(.28) ? "" + Util.ri(13 * 60, 21 * 60) : "-1");
        missingRegularCheck();
        storyDaily();
    }

    public static void maybeDailyDrama() {
        if (S.deliveryLate) { log("Heads up: the beer delivery looks like it's running late.", "event"); }
    }

    public static void minute() {
        // pigeon
        String pa = S.flags.getOrDefault("pigeonAt", "-1");
        if (!pa.equals("-1") && S.min == Integer.parseInt(pa) && isOpen()) pigeonArrives();
        if (S.pigeonHere && Util.chance(.07)) pigeonSteal();
        if (S.pigeonHere && S.min == (Integer.parseInt(pa.isEmpty() ? "0" : pa) + 45) % 1440) { S.pigeonHere = false; log("The pigeon has left. Majestically.", "event"); }
        // power/tv
        if (S.powerCut && S.powerBackAt >= 0 && Mgmt.stamp() >= S.powerBackAt) { S.powerCut = false; S.tvBroken = false; S.powerBackAt = -1; done("The lights come back on! Cheers all round."); allMood(5); }
        // nightly events
        if (S.quizOn && dow() == 2 && S.min == 20 * 60 && S.quizDay != S.day && isOpen()) { S.quizDay = S.day; quizNight(); }
        for (Booking b : S.bookings) if (b.day == S.day && S.min == 20 * 60 + 30) startNightEvent(b);
        if (S.karaokeDay == S.day) karaokeTick();
        if (S.flags.getOrDefault("band", "").equals("" + S.day) && S.min == 23 * 60) S.flags.remove("band");
        // random
        if (!isOpen() || modalOpen || crowd() < 3) return;
        if (S.min % 15 != 0) return;
        double p = .16 + crowd() * .004;
        if (!Util.chance(p)) return;
        List<Ev> ok = new ArrayList<>(); for (Ev e : EVENTS) if (!cooldown.containsKey(e.id)) ok.add(e);
        Ev e = Util.wpick(ok, x -> x.w.getAsDouble());
        if (e == null) return;
        cooldown.put(e.id, S.day + e.cd);
        try { e.run.run(); } catch (RuntimeException ex) { ex.printStackTrace(); }
    }

    // ---------- pigeon ----------
    static void pigeonArrives() {
        S.pigeonHere = true; S.pigeonDay = S.day; S.pgx = 480; S.pgy = 590;
        log("🐦 A pigeon has strolled in through the front door.", "event"); sfx("coo");
        for (Npc n : S.npcs) if (n.inPub && Util.chance(.5)) say(n, Util.pick("Is that a PIGEON?", "There's a bird in here!", "He's back!", "Nobody make eye contact."), 4);
        Npc kev = byId.get("kev");
        if (kev != null && kev.inPub && !kev.secretKnown && Util.chance(.5)) { say(kev, "Alright, " + (S.pigeonName.equals("the pigeon") ? "Gerald" : S.pigeonName) + "? Told you not to come in here."); Social.revealSecret(kev, "drunk", null); }
        if (S.pigeonName.equals("the pigeon") && !modalOpen) {
            ask("A Pigeon Has Entered The Pub", "It's looking at the crisps. Regulars are asking what you'll call it.",
                opt("Name him Gerald", () -> namePigeon("Gerald")), opt("Name him Sir Pecksalot", () -> namePigeon("Sir Pecksalot")), opt("Name her Beyoncé", () -> namePigeon("Beyoncé")), opt("Don't encourage it", () -> { done("The pigeon remains unnamed. It doesn't care."); }));
        }
    }
    static void namePigeon(String nm) { S.pigeonName = nm; done("The pub pigeon is now officially " + nm + "."); Mgmt.addJoke(nm + " the pigeon", "kev", "dave"); allMood(4); news("PUB PIGEON GIVEN NAME", "The bird, now known as " + nm + ", 'accepted it with no visible emotion'."); }
    static void pigeonSteal() {
        Npc n = anyIn(x -> x.glass >= 0 || x.eating);
        if (n == null) return;
        log("The pigeon swoops and nicks " + first(n) + "'s crisps!", "event"); say(n, Util.pick("OI! THAT'S MINE!", "My crisps! He's got my crisps!", "I'm being robbed by a bird!"), 4); sfx("flap");
        n.mood -= 3; stat("pigeonThefts"); for (Npc o : S.npcs) if (o.inPub && o != n && Util.chance(.3)) say(o, Util.pick("Ha! Respect.", "Fair play, pigeon.", "Get in!"), 3);
        if (Util.chance(.25)) Mgmt.addJoke("the Great Crisp Heist", n.id, "kev");
    }
    static double pgTx = 480, pgTy = 400;
    public static void frame(double dt) {
        if (!S.pigeonHere) return;
        if (Util.dist(S.pgx, S.pgy, pgTx, pgTy) < 6 || Util.chance(.005)) { Nav.Spot s = Util.pick(Nav.STAND); pgTx = s.x + Util.r(-10, 10); pgTy = s.y + Util.r(-10, 10); }
        double dx = pgTx - S.pgx, dy = pgTy - S.pgy, d = Math.hypot(dx, dy);
        if (d > 1) { S.pgx += dx / d * 38 * dt; S.pgy += dy / d * 38 * dt; }
    }

    // ---------- individual events ----------
    static void lockedLoo() {
        Npc n = anyIn(x -> x.st.equals("sit") || x.st.equals("stand") || x.st.equals("tv")); if (n == null) return;
        Ai.release(n); n.path.clear(); n.x = Nav.LOO[0]; n.y = Nav.LOO[1] + 6; n.st = "loo"; n.timer = 25; n.doing = "Locked in the toilet"; S.flags.put("lockedLoo", n.id); L.fx("shake", Nav.LOO[0], 100);
        sfx("knock");
        ask("Someone's Locked in the Loo", n.name + " has been in there a while. There is... knocking. Muffled shouting. The lock has gone.",
            opt("Break the door down (£25)", "Costs £25", () -> { spend(25); free(n, "You shoulder the door. It opens. So does the ceiling a bit.", 6); }),
            opt("Slide a butter knife in", "Risky", () -> { if (Util.chance(.55) || hasStaff("manager")) free(n, "The butter knife works! A triumph of cutlery.", 5); else { n.timer = 15; n.mood -= 8; done("The knife snapped. " + first(n) + " is going to be a while."); } }),
            opt("Leave them. They'll be fine.", () -> { n.timer = 12; n.anger += 25; n.mood -= 15; remember(n, "loo", -4, "player", "player", n.name + " was left locked in the loo by the landlord"); addRel(n.id, "player", 0); trust(n, -12); done("You wander off. Muffled swearing follows."); }),
            opt("Ask the regulars to help", () -> { Npc t = byId.get("tony"); if (t != null && t.inPub) { free(n, "Big Tony removes the whole door, politely.", 8); say(t, "Stand back, please."); } else { free(n, "Six people push at once. The door and two of them go over.", 3); allMood(2); } }));
    }
    static void free(Npc n, String msg, double trustGain) {
        n.timer = 0; n.mood += 5; trust(n, trustGain); n.embar += 40; n.st = "idle"; done(msg);
        Mgmt.addJoke("the Loo Incident", n.id, "player"); remember(n, "loo", 2, "player", "player", n.name + " got rescued from the loo by the landlord");
        for (Npc o : S.npcs) if (o.inPub && o != n && Util.chance(.4)) say(o, Util.pick("Rescued by the landlord!", "Never gonna live that down, " + first(n) + "!", "Ha ha HA!"), 4);
        news("MAN TRAPPED IN PUB TOILET FOR " + Util.ri(20, 55) + " MINUTES", "'It was fine,' said " + n.name + ", who was not fine. Staff 'apologised for the lock'.");
        S.flags.remove("lockedLoo");
    }

    static void footyRow() {
        List<Npc> fans = here(n -> !n.team.isEmpty() && Data.ti(n.team) >= 0 && (Data.ti(n.team) == S.match.home || Data.ti(n.team) == S.match.away));
        Npc a = null, b = null;
        for (Npc x : fans) for (Npc y : fans) if (x != y && !x.team.equals(y.team) && x.conv.isEmpty() && y.conv.isEmpty()) { a = x; b = y; }
        if (a == null) return;
        final Npc A = a, B = b;
        say(A, "Your lot are a JOKE, " + first(B) + "!"); say(B, "Say that to my face!");
        A.anger += 30; B.anger += 30;
        ask("Football Fans Squaring Up", A.name + " (" + Data.TEAM_SHORT[Data.ti(A.team)] + ") and " + B.name + " (" + Data.TEAM_SHORT[Data.ti(B.team)] + ") are nose to nose over the match.",
            opt("Calm them down", () -> { if (Util.chance(.65 + (trustOf(A) + trustOf(B)) / 300)) { A.anger = 10; B.anger = 10; trust(A, 4); trust(B, 4); done("You talk them down. Pride intact, mostly."); addRelBoth(A.id, B.id, 4); } else { A.anger += 10; done("They ignore you. This is going to get worse."); Events.nearFight(A, B, "football"); } }),
            opt("Buy them both a pint (£10)", "Costs £10", () -> { spend(10); A.anger = 0; B.anger = 0; addRelBoth(A.id, B.id, 6); trust(A, 5); trust(B, 5); done("Two free pints. Suddenly they're best friends."); Mgmt.addJoke(first(A) + " and " + first(B) + "'s football truce", A.id, B.id); }),
            opt("Throw them both out", () -> { done("Out you go, both of you."); for (Npc x : List.of(A, B)) { x.stayUntil = 0; trust(x, -8); x.anger += 20; remember(x, "kicked", -5, "player", "player", x.name + " got thrown out for arguing about football"); Ai.goLeave(x); } rep(.3); }),
            opt("Let them get on with it", () -> { Social.Conv c = new Social.Conv(); c.id = "r" + Util.ri(100, 999); c.a = A.id; c.b = B.id; c.stage = 2; c.topic = "football"; c.next = 1; c.ttl = 12; A.conv = c.id; B.conv = c.id; Social.convs.add(c); done("You polish a glass and look elsewhere."); }));
    }
    static double trustOf(Npc n) { return n.trust; }

    public static void nearFight(Npc a, Npc b, String topic) {
        if (modalOpen) { Social.convs.removeIf(x -> false); Ai.goLeave(a); Ai.goLeave(b); return; }
        ask("A FIGHT IS ABOUT TO BREAK OUT", a.name + " and " + b.name + " have squared up over " + topic + ". Somebody's thrown a beer mat.",
            opt("Step between them", "Brave", () -> { if (Util.chance(.7)) { a.anger = 0; b.anger = 0; trust(a, 6); trust(b, 6); done("You step in. They stand down, sheepishly."); stat("rowsDefused"); rep(.3); } else { done("You take a beer mat to the forehead. They stop, horrified."); S.sat -= 1; a.embar += 40; b.embar += 40; a.anger = 0; b.anger = 0; } }),
            opt("Get Big Tony", () -> { Npc t = byId.get("tony"); if (t != null && t.inPub) { say(t, "Gentlemen. Let's not."); a.anger = 0; b.anger = 0; trust(a, 3); trust(b, 3); done("Tony hugs them both apart."); stat("rowsDefused"); } else done("Tony isn't here. Oh dear."); }),
            opt("Call the police", () -> { done("Two officers arrive, take names, accept a tea, leave."); a.stayUntil = 0; b.stayUntil = 0; Ai.goLeave(a); Ai.goLeave(b); trust(a, -10); trust(b, -10); stat("policeVisits"); S.sat -= 1; }),
            opt("Shout \"SETTLE IT AT DARTS!\"", () -> { done("The pub erupts. They agree. They go to the oche."); Ai.endGameFor(a); Ai.endGameFor(b); Ai.Game g = new Ai.Game(); g.a = a; g.b = b; g.kind = "darts"; g.end = S.min + 8; Ai.games.add(g); Ai.goTo(a, Nav.OCHE[0], Nav.OCHE[1], "darts", 8); Ai.goTo(b, Nav.OCHE[0] - 28, Nav.OCHE[1] + 40, "darts", 8); allMood(3); rep(.5); }));
    }

    static void proposal() {
        Npc a = null, b = null;
        for (String p : S.couples) { String[] ids = p.split("\\|"); Npc x = byId.get(ids[0]), y = byId.get(ids[1]); if (x != null && y != null && x.inPub && y.inPub) { a = x; b = y; break; } }
        if (a == null) return;
        final Npc A = a, B = b;
        say(A, "Everyone... can I have your attention?", 5); A.embar += 30;
        ask("A Proposal!", A.name + " is down on one knee in front of " + B.name + ". The whole pub has gone silent. Even Dave.",
            opt("Bring out the bubbly (£30)", "Costs £30", () -> { spend(30); proposalOutcome(A, B, true, 1.2); }),
            opt("Start a drumroll on the bar", () -> proposalOutcome(A, B, true, 1.0)),
            opt("Stay out of it", () -> proposalOutcome(A, B, false, .8)));
    }
    static void proposalOutcome(Npc a, Npc b, boolean help, double bonus) {
        boolean yes = Util.chance(Util.clamp(.45 + (Sim.rel(b.id, a.id) / 200.0) + (help ? .15 : 0), .15, .95));
        if (yes) { say(b, "YES!!!", 6); allMood(10); done(a.name + " and " + b.name + " are engaged! The pub erupts."); stat("proposalYes"); addRelBoth(a.id, b.id, 15); rep(1.5); news("ENGAGEMENT ANNOUNCED AT THE SPECKLED PIGEON", a.name + " proposed to " + b.name + " near the dartboard. 'It was beautiful,' said Linda. 'And long.'");
            if (help) { for (Npc n : S.npcs) if (n.inPub) { n.thirst += 25; } sfx("cheer"); } }
        else { say(b, "I... I'm so sorry.", 6); done(b.name + " says no. Awkward silence. Somebody drops a glass."); Social.breakUp(a, b); allMood(-8); }
    }

    static void lostWallet() {
        Npc n = reg(); if (n == null) return;
        n.money = Math.max(0, n.money - 40); say(n, "My WALLET! It's gone! I had it five minutes ago!", 5);
        ask("Lost Wallet", n.name + " is frantically patting their pockets. " + Util.money0(40) + " and a loyalty card for Savemore, gone.",
            opt("Organise a search", () -> { if (Util.chance(.7)) { Npc f = anyIn(x -> x != n); if (f != null) { done(first(f) + " finds the wallet under the dartboard!"); n.money += 40; addRel(n.id, f.id, 8); trust(n, 5); remember(n, "wallet", 4, f.id, f.id, f.name + " found " + n.name + "'s wallet"); } else { n.money += 40; done("The wallet turns up in a coat pocket."); } } else { done("Nothing. The wallet is gone for good."); n.mood -= 12; } }),
            opt("Give them a free drink", () -> { Order(n); trust(n, 4); done("A consoling free pint for " + first(n) + "."); }),
            opt("Shrug", () -> { trust(n, -4); n.mood -= 6; done("You shrug. " + first(n) + " glares."); }));
    }
    static void Order(Npc n) { n.glass = Data.di(n.fav); n.glassLevel = 1; n.thirst = 0; n.mood += 8; }

    static void foundWallet() {
        Npc f = reg(); if (f == null) return;
        boolean honest = f.hum + (1 - f.frug) + (f.id.equals("liam") ? -.5 : .3) > 1 && Util.chance(.7);
        ask("Someone Found a Wallet", first(f) + " is holding up a battered wallet containing £60 and a photo of a ferret. Nobody's claimed it.",
            opt("Put it in lost property", () -> { done("Locked in the till drawer. A stranger returns for it tomorrow (probably)."); trust(f, 3); rep(.3); }),
            opt("Ask " + first(f) + " to hand it in", () -> { if (honest) { done(first(f) + " is honest. A tear is shed."); trust(f, 6); addRel(f.id, "player", 0); } else { done(first(f) + " sheepishly 'forgets' to hand it in. You keep your eyes on them."); f.embar += 20; f.money += 60; } }),
            opt("Count the cash. Aloud.", () -> { done("Everyone watches you count it. Nobody breathes."); allMood(-2); }));
    }

    static void celebrity() {
        String[] names = {"Gordon Plimsoll (TV chef)", "Jonny Quaver (boy-band member, 1998)", "Dame Judith Marlowe (actor)", "Marcus 'The Hammer' Bell (darts champion)"};
        String c = Util.pick(names); stat("celebs");
        sfx("murmur");
        ask("A Celebrity Walks In", c + " has entered the pub and is looking for 'somewhere quiet'.",
            opt("Treat them like anybody else", () -> { done(c.split(" \\(")[0] + " is delighted by the normality. Tips well."); rep(1.5); S.sat += 1; addMoney(35); S.pop += 2; news(c.split(" \\(")[0].toUpperCase() + " SPOTTED IN WESTBRIDGE PUB", "'Lovely pint,' the star reportedly said. The pigeon looked unimpressed."); }),
            opt("Give the VIP treatment (£20 free)", () -> { spend(20); done("You fawn. They love it. Photos with the staff!"); rep(2.5); S.pop += 3; addMoney(60); news("FAMOUS FACE LOVES THE SPECKLED PIGEON", "A star was seen 'having a lovely time' in the local. Gaz tried to tell them about his semi-pro days."); for (Npc n : S.npcs) if (n.inPub) { n.mood += 5; if (Util.chance(.2)) say(n, "Can I get a selfie?!"); } }),
            opt("Make them pay double", () -> { done("They pay. Grudgingly."); addMoney(15); rep(-1.5); }));
    }

    static void powerCut() {
        S.powerCut = true; S.tvBroken = true; S.powerBackAt = Mgmt.stamp() + Util.ri(30, 60); stat("powerCuts"); sfx("powerdown");
        allMood(-4);
        ask("POWER CUT!", "Everything goes black. The jukebox dies mid-chorus. Somebody screams, then apologises.",
            opt("Light candles — make it cosy", () -> { done("Candlelit pub! Surprisingly romantic."); allMood(8); S.sat += 1; for (Npc n : S.npcs) if (n.inPub) n.exp += 4; S.powerBackAt = Mgmt.stamp() + 40; }),
            opt("Fire up the generator (£60)", "Costs £60", () -> { spend(60); S.powerCut = false; S.tvBroken = false; S.powerBackAt = -1; done("The generator coughs into life. Back in business."); stat("powerCuts"); }),
            opt("Announce a lock-in by torchlight", () -> { done("Someone starts a ghost story. Dave cries."); for (Npc n : S.npcs) if (n.inPub) { n.stayUntil += 30; n.thirst += 20; } allMood(4); }));
    }

    static void dartsChallenge() {
        Npc n = anyIn(x -> x.darts >= 4 && !x.st.equals("walk") && x.drunk < 70); if (n == null) return;
        say(n, "Oi, landlord! Fancy a game of darts? A tenner says you can't beat me.", 6);
        ask("Darts Challenge!", n.name + " has challenged you to a game of 301 for a tenner.",
            opt("You're on!", () -> L.game("darts", n)),
            opt("Not tonight", () -> { say(n, "Scared, are we?", 4); trust(n, -1); }),
            opt("Challenge them to double or nothing", () -> { S.flags.put("dartsStake", "20"); L.game("darts", n); }));
    }

    static void spontaneousSong() {
        Npc n = anyIn(x -> x.sing >= 3 && x.drunk > 25 && x.soc > .5); if (n == null) return;
        perform(n, true);
    }

    static void perform(Npc n, boolean impromptu) {
        double quality = n.sing + Util.r(-2, 2) - (n.drunk > 55 ? (n.drunk - 55) * .06 : 0) + (hasUp("sound") ? .7 : 0);
        String song = Util.pick("Sweet Carolynn", "Dancing Queen-ish", "Don't Stop Believing (Probably)", "Wonderwall (Ish)", "Mr Brightside (Roughly)", "My Way");
        say(n, "♪ " + song + " ♪", 8); sfx("mic"); L.fx("spot", Nav.STAGE[0], Nav.STAGE[1]);
        if (impromptu) log(n.name + " has grabbed the mic and is singing '" + song + "'!", "event");
        if (n.id.equals("maggie") && !n.secretKnown && quality > 8) {
            log("Maggie's voice fills the room. It is extraordinary.", "story");
            allMood(10); Social.revealSecret(n, "drunk", null); n.mood += 15; rep(2); sfx("cheer"); stat("brilliantSingers");
            news("PENSIONER 'SUGAR LUMP SALLY' RETURNS", "Maggie Doyle, 78, revealed as the one-hit wonder behind 1968's 'Sugar Lump Sally'. 'I just wanted a stout,' she said.");
            return;
        }
        if (quality >= 7) { allMood(5); for (Npc o : S.npcs) if (o.inPub && Util.chance(.3)) say(o, Util.pick("WOW!", "Where did THAT come from?!", "Brilliant!", "Encore!"), 4); n.mood += 12; sfx("cheer"); stat("goodSinging"); }
        else if (quality <= 3) { allMood(-1); for (Npc o : S.npcs) if (o.inPub && Util.chance(.3)) say(o, Util.pick("My ears!", "Dear God.", "Is it supposed to sound like that?", "Somebody stop him/her.".replace("him/her", "them")), 4); n.embar += 25; sfx("groan"); Mgmt.addJoke(first(n) + "'s singing", n.id, "dave"); stat("badSinging"); }
        else { for (Npc o : S.npcs) if (o.inPub && Util.chance(.2)) say(o, Util.pick("Not bad!", "Go on, then!", "Fair play."), 3); }
        n.timer = 0;
    }

    static void dog() {
        Npc n = anyIn(x -> !x.id.equals("owen") && !x.stranger); if (n == null) return;
        String dog = Util.pick("Biscuit", "Tiny Terry", "Brian", "Colonel Mustard", "Reg");
        Npc owen = byId.get("owen");
        ask("Someone's Brought a Dog", n.name + " has turned up with " + dog + ", a very large and entirely unbothered dog.",
            opt("Dogs are welcome", () -> { done(dog + " is the star of the pub."); allMood(4); n.mood += 6; trust(n, 4); if (owen != null && owen.inPub) { say(owen, "Is that dog loose?! IS THAT DOG LOOSE?"); owen.mood -= 20; owen.anger += 15; owen.embar += 20; if (!owen.secretKnown && Util.chance(.5)) Social.revealSecret(owen, "drunk", null); } }),
            opt("Sorry, no dogs", () -> { done("You turn " + dog + " away. Heartbreak."); trust(n, -4); n.mood -= 8; allMood(-1); if (owen != null) owen.mood += 4; }),
            opt("Dogs welcome — Owen, you'll cope", () -> { if (owen != null && owen.inPub) { done("Owen climbs onto a stool. Stays there."); owen.embar += 40; } else done(dog + " is the star of the pub."); allMood(3); }));
    }

    static void tvBreaks() {
        S.tvBroken = true; sfx("static");
        ask("THE TV HAS BROKEN MID-MATCH!", "Static. Gasps. Dave says a word the vicar will have to forgive.",
            opt("Whack it", () -> { if (Util.chance(.5)) { S.tvBroken = false; done("It flickers back! Cheers all round."); allMood(5); } else { done("It makes a noise. A bad noise."); allMood(-3); } }),
            opt("Call Rob the TV man (£45)", "Costs £45", () -> { spend(45); S.tvBroken = false; done("Rob arrives in 4 minutes flat. A miracle."); allMood(3); }),
            opt("Put the commentary on the radio", () -> { done("Crackly radio commentary. Atmosphere: 11/10."); for (Npc n : S.npcs) if (n.inPub) { n.exp += 2; n.anger = Math.max(0, n.anger - 3); } S.tvBroken = false; }),
            opt("Everyone use your phones", () -> { done("Eight people squint at one phone. It works!"); S.tvBroken = false; }));
    }

    static void flatPint(Npc n) {
        if (modalOpen || n == null) return;
        say(n, "This pint is FLAT.", 5);
        ask("\"This Pint Is Flat\"", n.name + " is holding their drink up to the light like evidence.",
            opt("Replace it, free", () -> { Order(n); trust(n, 4); n.exp += 4; done("Replacement pint. " + first(n) + " is pacified."); }),
            opt("Taste it yourself", () -> { if (Util.chance(.5)) { Order(n); done("It IS flat. You apologise and replace it."); trust(n, 6); } else { done("It's fine. You taste it. They glare. You drink the rest."); trust(n, -4); n.embar += 20; } }),
            opt("Tell them it's fine", () -> { trust(n, -6); n.exp -= 8; n.mood -= 8; remember(n, "flat", -3, "player", "player", n.name + " was told a flat pint was 'fine' by the landlord"); done("They say they'll remember this."); }));
    }

    public static void longStory(Npc n) {
        if (n == null || modalOpen) return;
        ask("Someone Has Started A Long Story", n.name + ": \"Now, this was back in '94, and you'll want to sit down...\" Eyes are glazing over.",
            opt("Listen politely", () -> { n.mood += 10; trust(n, 3); allMood(-2); done("Twenty-two minutes later, there is a punchline. It is good. Probably."); n.stayUntil += 20; Mgmt.addJoke(first(n) + "'s very long story", n.id, "dave"); }),
            opt("Interrupt: 'Anyone for a drink?'", () -> { n.embar += 15; trust(n, -2); allMood(2); done("The pub exhales. " + first(n) + " sulks."); }),
            opt("Add your own, longer story", () -> { done("A story-off begins. Nobody wins. Nobody leaves."); allMood(-1); for (Npc o : S.npcs) if (o.inPub) o.stayUntil += 10; }));
    }

    static void police() {
        String why = Util.pick("a report of 'a hostage situation' (it was karaoke)", "reports of 'suspicious bird activity'", "a complaint that someone was 'being murdered' (Gaz, losing at pool)", "a noise complaint from a neighbour who lives 3 miles away");
        stat("policeVisits");
        ask("The Police Are Here", "Two officers stroll in following " + why + ".",
            opt("Offer them a cup of tea", () -> { done("They stay for tea. Gaz tells them about being semi-pro."); allMood(2); rep(.5); S.sat += .5; }),
            opt("Explain calmly", () -> { done("They nod, tut, and leave. Dave wanted to see handcuffs."); }),
            opt("Panic and hide the pigeon", () -> { done("There was no pigeon. There was never a pigeon."); allMood(1); Mgmt.addJoke("the day we hid the pigeon", "kev", "dave"); }));
        news("POLICE ATTEND PUB 'FOLLOWING MISUNDERSTANDING'", "Officers described the matter as 'resolved' and 'a bit embarrassing'.");
    }

    static void bet() {
        Npc a = anyIn(x -> x.gen > .3 && !x.st.equals("walk") && x.drunk > 10), b = anyIn(x -> x != a && !x.st.equals("walk")); if (a == null || b == null) return;
        String[] bets = {"eat a whole ghost-pepper pie", "do a full lap of the car park in a towel", "drink a pint in under 8 seconds", "name every Rovers manager since 1990", "balance a pint on their head for a full minute", "sing the entire alphabet backwards"};
        String bet = Util.pick(bets);
        say(a, "I bet " + first(b) + " a tenner they can't " + bet + "!", 6);
        ask("A Ridiculous Bet", a.name + " has bet " + b.name + " £10 that " + first(b) + " can't " + bet + ". They want you to hold the money.",
            opt("Hold the stakes (£10 rake)", () -> { boolean win = Util.chance(.45 + b.tol * .15); addMoney(10); done(win ? b.name + " completes the challenge! Pub goes wild." : b.name + " fails. Heroically."); allMood(win ? 5 : 3); (win ? b : a).mood += 12; (win ? a : b).embar += 25; addRelBoth(a.id, b.id, 2); Mgmt.addJoke("the " + bet.split(" ")[0] + " bet", a.id, b.id); stat("bets"); }),
            opt("Veto it — insurance", () -> { done("Killjoy! The crowd boos, lightly."); allMood(-2); trust(a, -2); }),
            opt("Raise the stakes (£50 each)", () -> { boolean win = Util.chance(.5); addMoney(win ? 30 : 20); done("A hush falls... " + first(win ? b : a) + " takes the pot."); allMood(8); rep(.5); }));
    }

    static void review() {
        double stars = 1 + Util.clamp((S.sat - 20) / 20 + (S.clean - 40) / 30 + Mgmt.ambience() / 40 - (S.price[0] > 5.8 ? 1 : 0), 0, 4) + Util.r(-.5, .5);
        int st = (int) Math.round(Util.clamp(stars, 1, 5));
        if (st == 5) stat("fiveStar");
        log("📰 The Chronicle's reviewer was in incognito! " + "★".repeat(st) + "☆".repeat(5 - st), "event");
        String quote = switch (st) { case 5 -> "'A gem. The pigeon is a triumph.'"; case 4 -> "'Hearty, honest, a little sticky.'"; case 3 -> "'Perfectly fine. Fine is the word.'"; case 2 -> "'Disappointing. The crisps were the highlight.'"; default -> "'I have eaten better crisps off the floor.'"; };
        news("REVIEW: THE SPECKLED PIGEON — " + st + "/5", quote + " The reviewer " + (st >= 4 ? "left a generous tip" : "left quietly") + ".");
        rep((st - 3) * 2.5); S.pop += (st - 3) * 2; S.prestige += st * 8;
        if (!modalOpen) done("The Chronicle review: " + st + "/5 stars");
    }

    static void rivalOpens() {
        S.royalOak = true; S.rivalStr = 55; stat("rivalOpened");
        ask("A RIVAL PUB HAS OPENED!", "The Royal Oak has opened on the high street. Half-price pints, a 'gastro' menu, and a bouncer who is somehow also a sommelier.",
            opt("Run a half-price night (£100)", "Costs £100", () -> { spend(100); S.rivalStr -= 12; S.pop += 4; for (Npc n : S.npcs) n.loyalty += 4; done("Half-price night! Packed — and Dave hasn't left once."); }),
            opt("Host a big event soon", () -> { S.rivalStr -= 6; S.rep += 1; done("You'll answer them with a good night out. Book something."); }),
            opt("Spread a rumour about the Oak's kitchen", () -> { if (Util.chance(.5)) { S.rivalStr -= 14; done("Linda spreads it for free. Within an hour, everyone knows."); } else { S.rep -= 3; S.rivalStr += 4; done("It backfires spectacularly. Everyone knows you did it."); } }),
            opt("Ignore them", () -> done("The Royal Oak will find its level. Probably.")));
        news("ROYAL OAK OPENS ON HIGH STREET", "A new pub opens in Westbridge. Locals describe it as 'nice but obviously not our local'.");
    }

    static void stagDo() {
        int n = Util.ri(6, 9);
        ask("A Stag Do Appears At The Door", n + " men in matching pink T-shirts reading 'DAVE'S LAST NIGHT OF FREEDOM'. Every single one is called Dave. (Not our Dave.)",
            opt("Welcome them in", () -> { for (int i = 0; i < n; i++) { Npc s = Chars.stranger(); Chars.fillDefaults(s); s.flags.put("gone", "1"); s.money = 90; s.drunk = Util.r(20, 40); s.stayA = 2; S.npcs.add(s); byId.put(s.id, s); s.planStay = 3; Ai.enterPub(s); s.thirst = 80; s.gen = .9; } done("The stags pile in. Chants. Chaos. Profit."); stat("stagDo"); rep(-.3); S.pop += 1; }),
            opt("Turn them away", () -> { done("You politely decline. They go to the Frog & Trumpet."); S.rivalStr += 1; }),
            opt("Cover charge £5 each", () -> { addMoney(5 * n); for (int i = 0; i < n; i++) { Npc s = Chars.stranger(); Chars.fillDefaults(s); s.flags.put("gone", "1"); s.money = 80; s.drunk = Util.r(10, 30); S.npcs.add(s); byId.put(s.id, s); s.planStay = 3; Ai.enterPub(s); s.thirst = 85; } done("Cover charge collected: " + Util.money0(5 * n) + ". They don't mind."); stat("stagDo"); }));
    }

    static void birthday() {
        Npc n = reg(); if (n == null) return;
        ask("It's " + first(n) + "'s Birthday!", n.name + " is turning " + (n.age + 1) + " and has told absolutely nobody (except everyone).",
            opt("Free pint and a song (£5)", () -> { spend(5); Order(n); allMood(4); n.mood += 15; trust(n, 6); remember(n, "birthday", 5, "player", "player", "The landlord made a fuss on " + n.name + "'s birthday"); done("The whole pub sings. Badly. " + first(n) + " is touched."); sfx("cheer"); rep(.4); }),
            opt("A quiet 'happy birthday'", () -> { trust(n, 2); n.mood += 4; done("A quiet nod. They appreciate it."); }),
            opt("Ignore it", () -> { trust(n, -3); n.mood -= 5; remember(n, "birthday", -3, "player", "player", "The landlord forgot " + n.name + "'s birthday"); done("Nobody says anything. Ouch."); }));
    }

    static void lottery() {
        Npc n = anyIn(x -> x.gen > .3 && !x.stranger); if (n == null) return;
        say(n, "I'VE WON! Two hundred quid on a scratchcard!", 6);
        n.money += 200; n.mood = 100;
        ask("A Scratchcard Win!", n.name + " just won £200 and wants to buy everyone a drink.",
            opt("Say yes — start pouring", () -> { int c = 0; for (Npc o : S.npcs) if (o.inPub && o != n) { int d = Data.di(o.fav); if (S.stock[d] > 0) { S.stock[d]--; addMoney(S.price[d]); n.money -= S.price[d]; o.glass = d; o.glassLevel = 1; o.thirst = 0; o.mood += 10; addRel(o.id, n.id, 3); c++; } } done(n.name + " buys " + c + " drinks. Legend."); allMood(5); sfx("cheer"); trust(n, 3); }),
            opt("Let them spend it wisely", () -> { n.mood += 5; done("Sensible. They buy chips instead."); }));
    }

    static void tourists() {
        ask("A Coach Party Arrives!", "A coach of visitors has stopped outside: nine tourists who want 'the authentic British pub experience'.",
            opt("Give them a proper welcome", () -> { for (int i = 0; i < 9; i++) { Npc s = Chars.stranger(); Chars.fillDefaults(s); s.flags.put("gone", "1"); s.money = 70; S.npcs.add(s); byId.put(s.id, s); s.planStay = 2; Ai.enterPub(s); s.thirst = 70; } done("Nine tourists photographing the pump clips."); rep(.5); S.pop += 1; }),
            opt("Charge them 'authentic' prices", () -> { addMoney(60); done("They pay it cheerfully. Cash."); rep(-.5); }));
    }

    static void inspector() {
        double score = S.clean + (hasStaff("cleaner") ? 10 : 0) + (hasUp("loos") ? 8 : 0) + (S.messes.size() > 4 ? -15 : 0) + Util.r(-10, 10);
        String r = score > 85 ? "Excellent! Five stars." : score > 65 ? "A solid four. 'Lovely, apart from the carpet.'" : score > 45 ? "Three. 'Could be worse. Smells of old lager.'" : "A grim two. A fine is issued.";
        if (score <= 45) { spend(80); rep(-2); S.sat -= 2; } else if (score > 85) { rep(1.5); S.pop += 2; }
        news("HYGIENE INSPECTOR VISITS LOCAL PUB", r);
        done("Surprise inspection: " + r);
    }

    static void burstPipe() {
        ask("Burst Pipe in the Cellar", "A pipe has burst. Water is lapping around the barrels. Wayne suggests 'maybe a bucket?'",
            opt("Call a plumber (£120)", "Costs £120", () -> { spend(120); done("Fixed in an hour. Stock is saved."); }),
            opt("Bucket brigade", () -> { if (Util.chance(.5)) done("The regulars form a human chain. Stock saved!"); else { S.stock[Util.ri(0, 5)] /= 2; done("Some stock is ruined, but the pub survives."); } allMood(2); }),
            opt("Ignore it", () -> { for (int i = 0; i < 6; i++) S.stock[i] = (int) (S.stock[i] * .6); done("You lose a lot of stock. The smell lingers."); S.clean -= 10; }));
    }

    static void ghost() {
        Npc m = byId.get("maggie");
        done("The lights flicker. A glass slides across the bar by itself.");
        for (Npc n : S.npcs) if (n.inPub && Util.chance(.5)) say(n, Util.pick("Did anyone else feel that?!", "That wasn't Wayne...", "I'm not drinking up here alone!"), 5);
        if (m != null && m.inPub) say(m, "That's just Reginald. Been here since 1974. Doesn't tip.", 6);
        allMood(2); S.pop += 1; stat("ghosts");
        news("IS THE SPECKLED PIGEON HAUNTED?", "Customers report 'cold spots', 'a smell of pipe tobacco' and 'Wayne, mostly'.");
    }

    static void spark() {
        List<Npc> c = here(n -> !n.stranger && n.age > 18);
        Collections.shuffle(c, Util.R);
        for (Npc a : c) for (Npc b : c) if (a != b && !S.crush.contains(a.id + "|" + b.id) && !S.couples.contains(pair(a.id, b.id)) && rel(a.id, b.id) > 15 && Math.abs(a.age - b.age) < 15 && !S.exes.contains(pair(a.id, b.id))) {
            S.crush.add(a.id + "|" + b.id); a.mood += 10; log("There's a spark between " + first(a) + " and " + first(b) + ". Nobody has noticed yet.", "story"); say(a, Util.fill(Util.pick("Is it me or is it hot in here?", "{n}, you've got something on your... never mind."), "n", first(b)), 5); return;
        }
    }

    static void tab() {
        Npc s = byId.get("steve"); if (s == null || !s.inPub) return;
        say(s, "Put that on my tab, would you? Just till Friday.", 5);
        ask("Steve's Tab", "Steve Whitlock says he's good for it. He's been saying that for three weeks. His card has apparently 'declined by itself'.",
            opt("Let him run a tab (£20)", () -> { spend(0); s.tab += 20; s.glass = Data.di(s.fav); s.glassLevel = 1; trust(s, 4); done("Steve's tab: " + Util.money0(s.tab) + ". He promises to settle it."); }),
            opt("Cash only, Steve", () -> { s.embar += 40; trust(s, -4); s.mood -= 10; done("Steve goes the colour of a conservatory. He pays in coins."); if (!s.secretKnown) Social.revealSecret(s, "drunk", null); }),
            opt("Quietly pay for him (£15)", () -> { spend(15); trust(s, 10); s.mood += 5; remember(s, "kind", 5, "player", "player", "The landlord quietly covered Steve's drink"); done("Steve will never forget this. He'll also never mention it."); }));
    }

    static void breweryRep() {
        ask("A Brewery Rep Drops By", "A cheerful man in a branded fleece offers a 'special arrangement' on stock.",
            opt("Take 40 free lager (they want exclusivity)", () -> { S.stock[0] = Math.min(Mgmt.stockCap(), S.stock[0] + 40); Mgmt.addJoke("Exclusive Lager Man", "dave", "gaz"); done("40 free pints. The rep leaves smiling. Ale drinkers are suspicious."); for (Npc n : S.npcs) if (n.fav.equals("ale")) n.mood -= 3; }),
            opt("Sample beers — free for the regulars", () -> { for (Npc n : S.npcs) if (n.inPub) { n.mood += 4; n.thirst += 10; } done("Free samples! Hands go up everywhere."); sfx("cheer"); }),
            opt("Decline politely", () -> done("The rep leaves a coaster and a lanyard.")));
    }

    static void thief() {
        ask("Something's Been Nicked", "A bag has vanished from beside the fruit machine. A stranger in a hoodie is leaving a bit quickly.",
            opt("Chase after them", () -> { if (Util.chance(.5)) { done("You catch them at the door! The bag is returned. Applause."); rep(.5); allMood(3); } else { done("They're gone. You have a stitch."); allMood(-2); } }),
            opt("Check the CCTV (buy it)", () -> { done("You should really install CCTV."); }),
            opt("Shrug", () -> { S.sat -= 1; done("Someone's bag is gone. That'll sting."); }));
    }

    static void staffTrouble() {
        List<Staff> p = new ArrayList<>(); for (Staff s : S.staff) if (s.present) p.add(s);
        if (p.isEmpty()) return; Staff s = Util.pick(p);
        double r = Util.R.nextDouble();
        if (r < .3) { log(s.name + " dropped a tray of pints. Nobody is hurt, except the pints.", "staff"); S.messes.add(mess(Nav.TABLES[1][0], Nav.TABLES[1][1] + 30)); s.mood -= 8; for (Npc n : S.npcs) if (n.inPub && Util.chance(.3)) say(n, "Ooooh!", 3); }
        else if (r < .55) { log(s.name + " is brilliant with an angry customer. Everyone claps.", "staff"); s.mood += 8; S.sat += .5; for (Npc n : S.npcs) if (n.inPub) n.anger = Math.max(0, n.anger - 5); }
        else if (r < .75 && s.role.equals("bartender")) { Npc c = anyIn(x -> true); if (c != null) { log(s.name + " gave " + first(c) + " the wrong change AND the wrong drink. Impressive.", "staff"); c.exp -= 6; s.mistakes++; } }
        else if (r < .9) { log(s.name + " has become very popular with the regulars.", "staff"); for (Npc n : S.npcs) if (n.inPub) s.rel.merge(n.id, 2.0, Double::sum); s.mood += 4; }
        else if (S.staff.size() > 1 && s.mood < 50 && Util.chance(.6)) {
            ask(s.name + " Wants a Word", s.name + " says: 'I've had an offer from the Frog & Trumpet. More money, no pigeon.'",
                opt("Give them a £10/day raise", () -> { s.wage += 10; s.mood += 25; done(s.name + " stays. Wages bill rises."); }),
                opt("Let them go", () -> { S.staff.remove(s); done(s.name + " leaves for the Frog & Trumpet."); S.rivalStr += 2; }),
                opt("Counter with free pizza", () -> { if (Util.chance(.5)) { s.mood += 20; done("Pizza works. It always works."); } else { S.staff.remove(s); done(s.name + " takes the pizza and leaves anyway."); } }));
        }
    }
    static Mess mess(double x, double y) { Mess m = new Mess(); m.x = x; m.y = y; m.made = S.min; return m; }

    static void mouse() {
        ask("A Mouse in the Kitchen", "The chef has seen a mouse. The mouse has seen the chef. They're at a standoff.",
            opt("Pest control (£80)", () -> { spend(80); done("Sorted. Quietly. No news at all."); }),
            opt("Get Wayne to deal with it", () -> { if (Util.chance(.3)) done("Wayne befriends the mouse. It's called Kevin now."); else { rep(-1); S.clean -= 8; done("Chaos. A customer sees. A review is likely."); } }),
            opt("Pretend you didn't see", () -> { S.clean -= 3; done("Out of sight, out of mind."); }));
    }

    // ---------- Friday man storyline ----------
    static void fridayManArrives() {
        Npc c = Chars.colin(); Chars.fillDefaults(c); c.loyalty = 55; c.lastVisit = S.day; S.npcs.add(c); byId.put(c.id, c);
        S.fridayMan = "colin"; S.storyFriday = 1;
        log("A quiet stranger in a grey coat orders a pint of best and sits at the end of the bar. He says nothing.", "story");
        Ai.enterPub(c);
    }

    static void storyDaily() {
        if (S.fridayMan.isEmpty()) return;
        Npc c = byId.get("colin"); if (c == null) return;
        if (dow() == 4) c.planGo = true;
        if (dow() == 4 && c.planGo) { c.planAt = 19 * 60 + 30; c.planStay = 3; }
        // hint progression: each Friday visited
        if (dow() == 5 && c.lastVisit == S.day - 1) {
            S.storyFriday++;
            if (S.storyFriday == 3) log("Colin has now been in three Fridays running. He always glances at the door before he sits.", "story");
            if (S.storyFriday >= 5 && !c.secretKnown) colinReveal(c);
        }
    }

    static void colinReveal(Npc c) {
        int which = Util.ri(0, 3);
        c.secretKnown = true; stat("secrets");
        String[] heads = {"MYSTERY FRIDAY REGULAR IS THE CHRONICLE'S FOOD CRITIC", "MYSTERY PUB REGULAR WON £1.2M ON LOTTERY", "MYSTERY REGULAR 'WAS GAZ MULLEN'S OLD COACH'", "MYSTERY PUB REGULAR 'ACTUALLY SEMI-PRO LEGEND'"};
        switch (which) {
            case 0 -> ask("Colin's Secret", "Colin slides a card across the bar: 'Colin Forsyth, Westbridge Chronicle — Food & Drink'. He's been reviewing you for five weeks.", opt("Offer him a free meal", () -> { rep(5); S.pop += 4; S.prestige += 40; done("A glowing review is coming."); news(heads[0], "Colin Forsyth's verdict: 'The scampi fries are a revelation.'"); }), opt("Ask for a good review", () -> { rep(2); done("He raises an eyebrow. The review is... fair."); }));
            case 1 -> ask("Colin's Secret", "Colin quietly admits he won £1.2 million on the lottery in March. He's been 'looking for somewhere normal'.", opt("Keep it a secret", () -> { addMoney(500); trust(c, 20); done("Colin presses £500 into your hand: 'Invest it in the pub'."); }), opt("Let slip to Linda", () -> { rep(-1); trust(c, -30); done("The whole town knows by teatime. Colin doesn't return.");  c.planGo = false; c.absentUntil = 9999; }));
            case 2 -> { Npc g = byId.get("gaz"); ask("Colin's Secret", "Colin turns out to be Ray Forsyth, who coached Ashford United's reserves. He recognises Gaz instantly.", opt("Let Colin speak", () -> { done("\"You were the KIT MAN!\" The pub goes silent. Gaz goes pale."); if (g != null) { g.embar += 60; g.mood -= 30; Social.revealSecret(g, "drunk", null); } }), opt("Warn Gaz first", () -> { if (g != null) { trust(g, 10); done("Gaz slips out the back. The truth will keep."); g.stayUntil = 0; } })); }
            default -> ask("Colin's Secret", "Colin confesses: he's the real semi-pro footballer Gaz has been describing for years. Gaz went a funny colour.", opt("Bring them together", () -> { Npc g = byId.get("gaz"); done("Colin and Gaz share a pint. It turns out Gaz did play. For eleven minutes."); if (g != null) { g.mood += 30; addRel(g.id, "colin", 20); } rep(2); }), opt("Keep it quiet", () -> done("Colin nods. Some stories are better as stories.")));
        }
        news(heads[which], "Friday regular Colin Forsyth's secret revealed. The pub 'did not see that coming'.");
    }

    // ---------- missing regular ----------
    static void missingRegularCheck() {
        for (Npc n : S.npcs) if (!n.stranger && n.loyalty > 60 && n.absentUntil >= S.day + 2 && !n.flags.containsKey("missing") && S.day - n.lastVisit >= 2 && Util.chance(.5)) {
            n.flags.put("missing", "" + S.day); log("Everyone's wondering where " + n.name + " has got to. Not seen for days.", "story");
            for (Npc o : S.npcs) if (o.inPub) {}
            S.flags.put("worry:" + n.id, "1");
        }
    }

    public static void onArrive(Npc n) {
        if (n.flags.containsKey("missing") && !n.stranger) {
            n.flags.remove("missing");
            String why = n.lifeNews.isEmpty() ? "had a family thing" : n.lifeNews.get(n.lifeNews.size() - 1);
            for (Npc o : S.npcs) if (o.inPub) { if (friends(o.id, n.id) || Util.chance(.3)) say(o, Util.pick("There he/she is!", "We thought you'd died!", "Where've you BEEN?!").replace("he/she", "they")); addRel(o.id, n.id, 2); }
            say(n, "Sorry! I " + why + ". Did I miss anything?", 6);
            log(n.name + " is back! Reason: " + why + ".", "story"); allMood(3);
            trust(n, 3);
        }
    }

    // ---------- quiz night ----------
    public static class QTeam { public String name; public List<Npc> members = new ArrayList<>(); public double skill; public int score; public boolean player; }
    public static List<QTeam> lastQuizTeams;

    public static List<QTeam> quizTeams() {
        List<Npc> pool = here(n -> n.intel > .35 || n.soc > .6);
        Collections.shuffle(pool, Util.R);
        List<QTeam> teams = new ArrayList<>();
        List<String> names = new ArrayList<>(Arrays.asList(Data.QUIZ_TEAMS)); Collections.shuffle(names, Util.R);
        int k = 0;
        while (!pool.isEmpty() && teams.size() < 4) {
            QTeam t = new QTeam(); t.name = names.get(k++);
            Npc lead = pool.remove(0); t.members.add(lead);
            for (int i = 0; i < 2 && !pool.isEmpty(); i++) {
                Npc best = null; double bs = -999; for (Npc p : pool) { double s = rel(lead.id, p.id) + Util.r(0, 20); if (s > bs) { bs = s; best = p; } }
                if (best != null) { pool.remove(best); t.members.add(best); }
            }
            double sk = 0; for (Npc m : t.members) sk += m.intel * .6 + m.quizSkill / 10.0 * .4; t.skill = sk / t.members.size();
            teams.add(t);
        }
        lastQuizTeams = teams;
        return teams;
    }

    static void quizNight() {
        if (crowd() < 3) { log("Quiz night — but hardly anyone's here. It's called off.", "event"); return; }
        ask("QUIZ NIGHT!", "It's 8 o'clock and the regulars are huddled around the answer sheets. Will you join in, or let them get on with it?",
            opt("Take part (play the quiz)", () -> L.game("quiz", null)),
            opt("Let them quiz without you", () -> autoQuiz()));
    }

    static void autoQuiz() {
        List<QTeam> teams = quizTeams(); if (teams.isEmpty()) return;
        for (int q = 0; q < 10; q++) for (QTeam t : teams) if (Util.chance(.2 + t.skill * .65)) t.score++;
        teams.sort((a, b) -> b.score - a.score);
        QTeam w = teams.get(0);
        quizAftermath(w, teams, false);
    }

    public static void quizAftermath(QTeam winner, List<QTeam> teams, boolean playerWon) {
        StringBuilder sb = new StringBuilder(); for (QTeam t : teams) sb.append(t.name).append(" ").append(t.score).append("  ");
        log("🏆 Quiz result: " + sb, "event");
        stat("quizzes");
        for (Npc n : S.npcs) if (n.inPub) {
            boolean inWin = winner != null && winner.members.contains(n);
            n.mood = Util.clamp(n.mood + (inWin ? 14 : 2)); n.exp += inWin ? 8 : 2;
            if (inWin) say(n, Util.pick("We DID it!", "Who's the quiz king?", "Pints on us!"), 5);
            else if (Util.chance(.15)) say(n, Util.pick("Question 7 was rigged.", "That wasn't even a real country.", "Priya's cheating again!"), 5);
        }
        Npc priya = byId.get("priya");
        if (winner != null && priya != null && winner.members.contains(priya)) news("PUB QUIZ WON BY " + winner.name.toUpperCase(), "Team captain Priya Nair said: 'Obviously.' Runners-up demanded a recount.");
        else if (winner != null) news("LOCAL PUB HOSTS SUCCESSFUL QUIZ NIGHT", winner.name + " took the title. 'We practised,' said the captain. Nobody believes this.");
        if (playerWon) { stat("quizWins"); }
        rep(.8); S.pop += .6; if (S.sat < 90) S.sat += .5;
        // Dave disputes answers
        Npc d = byId.get("dave");
        if (d != null && d.inPub && Util.chance(.5)) { say(d, "I'd like to challenge question 4!", 6); Mgmt.addJoke("Dave challenging the quiz", "dave", "priya"); addRel("priya", "dave", -1); }
    }

    // ---------- night events ----------
    static void startNightEvent(Booking b) {
        switch (b.type) {
            case "karaoke" -> {
                if (!hasUp("karaoke")) { log("Karaoke night booked, but you have no machine!", "event"); return; }
                S.karaokeDay = S.day; S.flags.put("karaokeSinger", "0"); stat("karaokeNights"); log("🎤 KARAOKE NIGHT begins! Who's brave enough?", "event"); sfx("mic");
                ask("Karaoke Night", "The machine's on. A hopeful hush. Somebody coughs. Do you take the mic first?", opt("Take the mic!", () -> L.game("karaoke", null)), opt("Let the customers go first", () -> {}));
            }
            case "band" -> { S.flags.put("band", "" + S.day); log("🎸 Live band night! The Mild Interest are tuning up.", "event"); S.pop += 3; for (Npc n : S.npcs) if (!n.inPub && n.soc > .6 && Util.chance(.3)) { n.planGo = true; n.planAt = S.min + Util.ri(5, 30); n.planVenue = "pub"; n.planStay = 2; } allMood(6); sfx("guitar"); S.playingGenre = "indie"; S.song = "Live: Mild Interest"; S.songUntil = Mgmt.stamp() + 120; news("LIVE MUSIC AT SPECKLED PIGEON", "A band played to 'a room'. The room 'tapped its foot'."); addMoney(0); }
            case "beerfest" -> { log("🍺 Beer festival! Everyone's thirsty.", "event"); for (Npc n : S.npcs) if (n.inPub) { n.thirst += 30; n.mood += 6; } S.pop += 3; }
            case "charity" -> { log("❤ Charity night — raising money for the Rovers kids' fund.", "event"); double r = crowd() * 12.0; addMoney(r * .3); rep(3); S.pop += 3; news("CHARITY NIGHT RAISES " + Util.money0(r) + " FOR LOCAL KIDS", "Dave auctioned his own shoe. It went for £40."); allMood(8); }
            default -> {}
        }
    }

    static void karaokeTick() {
        if (S.min < 20 * 60 + 40 || S.min > 23 * 60) return;
        if (S.min % 9 != 0 || !Util.chance(.8)) return;
        Npc n = Util.wpick(here(x -> x.st.equals("sit") || x.st.equals("stand") || x.st.equals("tv")), x -> .3 + x.soc + x.drunk / 40.0 + (x.id.equals("maggie") ? 1.5 : 0));
        if (n != null) perform(n, false);
    }

    public static void playerSang(int score) {
        stat("sang");
        if (score >= 70) { allMood(8); rep(1.5); S.pop += 1; done("You nail it! The pub is on its feet."); sfx("cheer"); }
        else if (score >= 35) { allMood(2); done("Not bad, landlord! Not bad at all."); }
        else { allMood(1); done("Truly awful. They love it. Nobody has ever been so fond of you."); for (Npc n : S.npcs) if (n.inPub) addRel(n.id, "player", 0); rep(.5); }
    }
}
