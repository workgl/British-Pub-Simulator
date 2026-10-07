package pub;

import java.util.*;
import pub.Model.*;
import static pub.Sim.*;

/** The landlord: movement and every way of interacting with customers. */
public final class Player {
    private Player() {}
    public static double x = 480, y = 520, phase;
    public static boolean walking;
    public static String bubble = ""; public static double bubbleUntil;
    static List<double[]> path = new ArrayList<>();
    static Runnable onArrive;

    public static void sync() { if (S != null) { x = S.px; y = S.py; } }
    public static void say(String t) { bubble = t; bubbleUntil = rt + 3.5; }

    public static void walkTo(double tx, double ty, Runnable then) {
        path = Nav.path(x, y, tx, ty); onArrive = then; walking = !path.isEmpty();
    }
    public static void stop() { path.clear(); onArrive = null; walking = false; }

    public static void update(double dt, double dx, double dy) {
        double sp = 120 * dt;
        if (dx != 0 || dy != 0) {
            path.clear(); onArrive = null;
            double len = Math.hypot(dx, dy); double nx = x + dx / len * sp, ny = y + dy / len * sp;
            if (Nav.free(nx, y)) x = nx; if (Nav.free(x, ny)) y = ny; walking = true; phase += dt * 10;
        } else if (!path.isEmpty()) {
            double[] wp = path.get(0); double ddx = wp[0] - x, ddy = wp[1] - y, d = Math.hypot(ddx, ddy);
            phase += dt * 10;
            if (d <= sp) { x = wp[0]; y = wp[1]; path.remove(0); if (path.isEmpty()) { walking = false; Runnable r = onArrive; onArrive = null; if (r != null) r.run(); } }
            else { x += ddx / d * sp; y += ddy / d * sp; }
        } else walking = false;
        if (S != null) { S.px = x; S.py = y; }
    }

    static boolean near(Npc n) { return Util.dist(x, y, n.x, n.y) < 70; }
    public static void goTo(Npc n, Runnable act) {
        if (near(n)) { act.run(); return; }
        double ang = Math.atan2(y - n.y, x - n.x);
        double tx = n.x + Math.cos(ang) * 34, ty = n.y + Math.sin(ang) * 34;
        if (!Nav.free(tx, ty)) { tx = n.x; ty = n.y + 30; }
        walkTo(tx, ty, act);
    }

    // ---------- actions ----------
    public static void act(String id, String action) {
        Npc n = npc(id); if (n == null || !n.inPub) { toast("They've gone."); return; }
        goTo(n, () -> perform(n, action));
    }

    static void perform(Npc n, String action) {
        if (!n.inPub) return;
        switch (action) {
            case "talk" -> talk(n);
            case "serve" -> serve(n);
            case "buy" -> buy(n);
            case "leave" -> askLeave(n);
            case "apologise" -> apologise(n);
            case "gossip" -> gossipWith(n);
            case "join" -> join(n);
            case "darts" -> L.game("darts", n);
            case "pool" -> L.game("pool", n);
            case "calm" -> { Social.Conv c = Social.convOf(n); if (c != null && c.flagged) intervene(c); else toast("Nothing to calm down."); }
            default -> {}
        }
    }

    static void serve(Npc n) {
        if (n.order == null || !n.st.equals("waitBar")) { toast(Sim.first(n) + " isn't waiting for a drink."); return; }
        Ai.serve(n, null); say("There you go.");
    }

    static void buy(Npc n) {
        int d = Data.di(n.fav); if (d >= 7) d = 0;
        if (S.stock[d] <= 0) { int alt = -1; for (int i = 0; i < 7; i++) if (S.stock[i] > 0) alt = i; if (alt < 0) { toast("Nothing left to pour!"); return; } d = alt; }
        S.stock[d]--; n.glass = d; n.glassLevel = 1; n.thirst = Math.max(0, n.thirst - 40);
        n.drunk = Util.clamp(n.drunk + Data.DABV[d] * 5 * (1 - n.tol * .45));
        int c = Integer.parseInt(n.flags.getOrDefault("bought:" + S.day, "0")) + 1; n.flags.put("bought:" + S.day, "" + c);
        stat("buyDrinks");
        if (c > 2) { n.trust += .3; Sim.say(n, Util.pick("You're not trying to get me drunk, are you?", "Steady on, landlord!", "Another? What do you want?")); }
        else { n.trust = Util.clamp(n.trust + 4, -100, 100); n.mood += 6; n.exp += 5; n.loyalty = Util.clamp(n.loyalty + 1.2, 0, 100); Sim.say(n, Util.pick("Cheers, landlord!", "You're a gent.", "Ooh, don't mind if I do.")); remember(n, "treat", 3, "player", "player", "The landlord bought " + n.name + " a drink"); }
        say("This one's on the house."); sfx("pour"); log("You bought " + Sim.first(n) + " a drink.", "player");
    }

    static void askLeave(Npc n) {
        boolean justified = n.drunk > 68 || n.anger > 60;
        Choice c = new Choice("Ask " + Sim.first(n) + " to leave", justified ? n.name + " is " + (n.drunk > 68 ? "extremely drunk" : "furious") + ". Most people would agree you're right." : n.name + " has done nothing wrong, as far as anyone can tell.");
        c.opts.add(new Opt("Politely ask them to leave", () -> {
            if (justified) { n.trust -= 3; n.embar += 30; n.stayUntil = 0; remember(n, "asked", -2, "player", "player", n.name + " was asked to leave when they'd had too much"); S.sat += .3; Sim.say(n, Util.pick("Fair enough. Sorry, landlord.", "I'm going, I'm going.")); log("You asked " + n.name + " to leave. They did, quietly.", "player"); }
            else { n.trust -= 15; n.loyalty -= 8; n.anger += 30; n.stayUntil = 0; remember(n, "kicked", -6, "player", "player", n.name + " was asked to leave for no good reason"); Sim.say(n, Util.pick("WHAT?! What did I do?!", "You're kidding. After all these years?", "Unbelievable.")); for (Npc o : S.npcs) if (o.inPub && o != n && rel(o.id, n.id) > 30) { o.trust -= 4; Sim.say(o, "That was a bit harsh, wasn't it?"); } log("You asked " + n.name + " to leave for no reason. People noticed.", "player"); }
            Ai.goLeave(n);
        }));
        c.opts.add(new Opt("Bar them for a week", () -> { n.bannedUntil = S.day + 7; n.trust -= 10; n.loyalty -= 10; n.stayUntil = 0; remember(n, "barred", -8, "player", "player", n.name + " was barred for a week"); Ai.goLeave(n); log(n.name + " is barred for a week.", "player"); if (!justified) S.rep -= 1; }));
        c.opts.add(new Opt("On second thoughts...", () -> {}));
        Sim.ask(c);
    }

    static void apologise(Npc n) {
        boolean need = n.trust < 0 || n.anger > 10 || n.mem.stream().anyMatch(m -> "player".equals(m.who) && m.w < 0 && S.day - m.day < 14);
        if (!need) { Sim.say(n, "Apologise for what?"); n.mood -= 1; return; }
        n.trust = Util.clamp(n.trust + 10, -100, 100); n.anger *= .3; n.mood += 6; n.loyalty = Util.clamp(n.loyalty + 3, 0, 100);
        for (Mem m : n.mem) if ("player".equals(m.who) && m.w < 0) m.w *= .4;
        remember(n, "apology", 3, "player", "player", "The landlord apologised to " + n.name);
        Sim.say(n, Util.pick("...Thanks. Appreciated.", "Alright. Water under the bridge.", "Well. That means something."));
        say("I'm sorry about that."); log("You apologised to " + n.name + ".", "player");
    }

    static void gossipWith(Npc n) {
        List<Mem> pool = new ArrayList<>(); for (Mem m : n.mem) if (!"player".equals(m.about) && !m.about.equals(n.id) && Math.abs(m.w) >= 1.5) pool.add(m);
        if (pool.isEmpty() || n.gos < .2 && Util.chance(.6)) { Sim.say(n, Util.pick("I don't really gossip.", "Nothing to tell.", "Ask Linda.")); return; }
        Mem m = Util.pick(pool);
        Choice c = new Choice(n.name + " leans in...", "\"" + m.t + ".\" (" + (m.src.equals("self") ? "saw it themselves" : m.src.equals("saw") ? "witnessed" : "heard it from " + Sim.name(m.src)) + ", " + Sim.dayName(m.day) + ")");
        Npc subj = npc(m.about);
        c.opts.add(new Opt("Fascinating. Tell no one.", () -> { n.trust = Util.clamp(n.trust + 3, -100, 100); log("You heard: " + m.t, "player"); }));
        c.opts.add(new Opt("Spread it around", () -> { int k = 0; for (Npc o : S.npcs) if (o.inPub && o != n && o != subj && Util.chance(.5)) { Mem nm = new Mem(S.day, m.kind, m.w * .5, m.about, m.who, m.t); nm.src = "player"; o.mem.add(0, nm); if (subj != null) addRel(o.id, subj.id, Math.signum(m.w) * 2); k++; } if (subj != null) { subj.trust -= 3; if (subj.inPub) subj.anger += 8; } log("You spread the word to " + k + " people.", "player"); stat("playerGossips"); }));
        c.opts.add(new Opt("Stop gossiping in my pub", () -> { n.trust = Util.clamp(n.trust - 2, -100, 100); Sim.say(n, "Spoilsport."); }));
        Sim.ask(c);
    }

    static void join(Npc n) {
        Social.Conv cv = Social.convOf(n); if (cv == null) { toast(Sim.first(n) + " isn't chatting to anyone."); return; }
        Npc a = npc(cv.a), b = npc(cv.b);
        Choice c = new Choice("Join the conversation", a.name + " and " + b.name + " are talking about " + cv.topic + ".");
        c.opts.add(new Opt("Back " + Sim.first(a), () -> { addRel(a.id, "player", 0); a.trust += 3; b.trust -= 2; addRel(b.id, a.id, -1); a.mood += 4; b.anger += 4; Sim.say(a, "See? The landlord agrees with me!"); }));
        c.opts.add(new Opt("Back " + Sim.first(b), () -> { b.trust += 3; a.trust -= 2; addRel(a.id, b.id, -1); b.mood += 4; a.anger += 4; Sim.say(b, "See? The landlord agrees with me!"); }));
        c.opts.add(new Opt("Tell a joke", () -> { if (Util.chance(.55 + .1 * (S.rep > 40 ? 1 : 0))) { a.mood += 6; b.mood += 6; addRelBoth(a.id, b.id, 2); Sim.say(a, "Ha! Good one, landlord."); stat("jokesTold"); if (cv.flagged) Social.settle(cv, "Your joke breaks the tension."); } else { a.mood -= 2; b.mood -= 2; Sim.say(b, "...That's not funny."); } }));
        c.opts.add(new Opt("Change the subject", () -> { cv.stage = 0; if (cv.flagged) Social.settle(cv, "You steer them onto safer ground."); cv.topic = Util.pick(Data.TOPIC_KEYS); Sim.say(a, "Right. Anyway... " + cv.topic + "?"); }));
        Sim.ask(c);
    }

    public static void intervene(Social.Conv cv) {
        Npc a = npc(cv.a), b = npc(cv.b); if (a == null || b == null) return;
        Choice c = new Choice("Argument: " + Sim.first(a) + " vs " + Sim.first(b), "Voices are raised over " + cv.topic + ". How do you handle it?");
        c.opts.add(new Opt("Calm both down", () -> { if (Util.chance(.7 + (a.trust + b.trust) / 400)) { Social.settle(cv, "You talk them down."); a.trust += 3; b.trust += 3; rep(.2); } else { log("They ignore you!", "drama"); } }));
        c.opts.add(new Opt("Take " + Sim.first(a) + "'s side", () -> { a.trust += 6; b.trust -= 8; b.anger += 15; Social.settle(cv, "You side with " + a.name + ". " + b.name + " is not impressed."); remember(b, "sided", -3, "player", "player", "The landlord took " + a.name + "'s side against " + b.name); }));
        c.opts.add(new Opt("Buy them both a pint", () -> { for (Npc x : List.of(a, b)) { int d = Data.di(x.fav); if (d < 7 && S.stock[d] > 0) { S.stock[d]--; x.glass = d; x.glassLevel = 1; } x.trust += 4; } Social.settle(cv, "Free pints work wonders."); addRelBoth(a.id, b.id, 4); }));
        c.opts.add(new Opt("Throw them both out", () -> { Social.convs.remove(cv); a.conv = ""; b.conv = ""; S.flags.remove("argue"); for (Npc x : List.of(a, b)) { x.trust -= 8; x.stayUntil = 0; remember(x, "kicked", -5, "player", "player", x.name + " was thrown out for arguing"); Ai.goLeave(x); } }));
        Sim.ask(c);
    }
    static void rep(double d) { S.rep = Util.clamp(S.rep + d); }

    // ---------- conversation ----------
    static void talk(Npc n) {
        Mem grudge = null;
        for (Mem m : n.mem) if ("player".equals(m.who) && m.w <= -3 && S.day - m.day < 21) { grudge = m; break; }
        Choice c;
        if (grudge != null && n.trust < 5) {
            final Mem g = grudge;
            c = new Choice(n.name, "\"I haven't forgotten, you know. " + g.t + " — " + Sim.dayName(g.day) + ". I remember.\"");
            c.opts.add(new Opt("Apologise", () -> apologise(n)));
            c.opts.add(new Opt("Buy them a pint to make it up", () -> buy(n)));
            c.opts.add(new Opt("Brush it off", () -> { n.trust -= 4; n.anger += 8; Sim.say(n, "...Right."); }));
            Sim.ask(c); return;
        }
        if (n.wantsJob && n.trust > -15 && !n.staff && S.staff.size() < 8) {
            c = new Choice(n.name, "\"Landlord... I don't suppose you're hiring? I'm skint, and I'm a quick learner. I've watched a LOT of bartending videos.\"");
            c.opts.add(new Opt("Hire them as a bartender", () -> hireNpc(n, "bartender")));
            c.opts.add(new Opt("Hire them as a waiter", () -> hireNpc(n, "waiter")));
            c.opts.add(new Opt("Maybe later", () -> { n.trust -= 2; Sim.say(n, "No worries... I'll keep asking."); }));
            Sim.ask(c); return;
        }
        if (n.trust > 45 && !n.secretKnown && !n.flags.containsKey("confided") && Util.chance(.55)) {
            n.flags.put("confided", "1");
            c = new Choice(n.name + " lowers their voice", "\"I've never told anyone this... " + n.secret + " Please don't tell anyone.\"");
            c.opts.add(new Opt("Your secret's safe with me", () -> { n.trust = Util.clamp(n.trust + 12, -100, 100); n.loyalty += 6; remember(n, "confide", 6, "player", "player", n.name + " confided in the landlord"); log("You now know " + Sim.first(n) + "'s secret: " + n.secret, "player"); toast("Secret kept: " + Sim.first(n)); }));
            c.opts.add(new Opt("Tell Linda. Immediately.", () -> { Social.revealSecret(n, "linda", null); n.trust -= 40; remember(n, "betrayed", -9, "player", "player", n.name + " was betrayed by the landlord"); }));
            Sim.ask(c); return;
        }
        if (n.exp < -10) {
            String why = S.price[Data.di(n.fav)] > Data.DPRICE[Data.di(n.fav)] * 1.2 ? "the prices in here" : S.clean < 45 ? "the state of the place" : "the service";
            c = new Choice(n.name + " has a complaint", "\"Can I have a word about " + why + "?\"");
            c.opts.add(new Opt("You're right. I'll do better.", () -> { n.trust += 4; n.exp += 8; n.mood += 4; }));
            c.opts.add(new Opt("Here's a free pint for your trouble", () -> buy(n)));
            c.opts.add(new Opt("It's the same for everyone", () -> { n.trust -= 5; n.anger += 12; n.loyalty -= 2; }));
            Sim.ask(c); return;
        }
        if (Util.chance(.3) && !n.lifeNews.isEmpty()) {
            String ln = n.lifeNews.get(n.lifeNews.size() - 1);
            c = new Choice(n.name, "\"Did I tell you? I " + ln.replace("has ", "have ").replace("just ", "") + ".\"");
            c.opts.add(new Opt("That's brilliant!", () -> { n.trust += 3; n.mood += 5; }));
            c.opts.add(new Opt("Tell me more", () -> { n.trust += 2; n.mood += 3; }));
            c.opts.add(new Opt("Hmm.", () -> { n.mood -= 2; }));
            Sim.ask(c); return;
        }
        // small talk topic
        String topic = Util.pick(Data.TOPIC_KEYS); double o = n.op.getOrDefault(topic, 0.0);
        String line = Util.fill(Util.pick(Data.TOPICS.get(topic)[o > .25 ? 0 : o < -.25 ? 1 : 2]), "team", Data.ti(n.team) >= 0 ? Data.TEAM_SHORT[Data.ti(n.team)] : "the lads", "other", "landlord");
        c = new Choice(n.name + " on " + topic, "\"" + line + "\"");
        c.opts.add(new Opt("Agree", () -> { double v = o > .25 || o < -.25 ? 3 : 1.5; n.trust = Util.clamp(n.trust + v, -100, 100); n.mood += 3; n.op.merge(topic, .05, Double::sum); }));
        c.opts.add(new Opt("Disagree", () -> { n.trust = Util.clamp(n.trust + (n.tmp > .5 ? -3 : 1), -100, 100); n.anger += n.tmp * 8; if (Math.abs(o) > .5) Sim.say(n, Util.pick("Oh, come on, landlord!", "You're joking.")); }));
        c.opts.add(new Opt("Crack a joke", () -> { if (Util.chance(.4 + n.hum * .4)) { n.mood += 8; n.trust += 3; Sim.say(n, "Ha! You're all right, you."); } else { n.trust -= 1; Sim.say(n, "...Right."); } }));
        c.opts.add(new Opt("Buy them a drink", () -> buy(n)));
        Sim.ask(c);
    }

    static void hireNpc(Npc n, String role) {
        Staff s = Mgmt.makeStaff(n.name, role, Math.max(3, n.barSkill), 6, 6, 8, 45, role.equals("bartender") ? "Hidden talent" : "Keen");
        s.fromNpc = n.id; s.rel.put(n.id, 50.0); s.shirt = n.shirt; s.hair = n.hair;
        Mgmt.hire(s); s.present = true;
        n.staff = true; n.wantsJob = false;
        if (n.inPub) { n.inPub = false; n.st = "away"; Ai.release(n); }
        n.planGo = false; n.loc = "pub"; n.trust += 15; n.loyalty = 100;
        for (Npc o : S.npcs) if (o.inPub) { if (rel(o.id, n.id) > 15) Sim.say(o, "Good on you, " + Sim.first(n) + "!"); }
        log(n.name + " now works behind the bar!", "story"); news("LOCAL " + n.job.toUpperCase() + " BECOMES BARTENDER", n.name + " has been hired at the Speckled Pigeon. 'Be nice to him', said the landlord.");
        toast("Hired " + n.name + "!");
        S.npcs.remove(n); byId.remove(n.id);
        S.flags.put("hired:" + n.id, "" + S.day);
    }
}
