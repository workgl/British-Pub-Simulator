package pub;

import java.util.*;
import pub.Model.*;
import static pub.Sim.*;

/** Conversations, memory, gossip, secrets, relationships and storylines. */
public final class Social {
    private Social() {}

    public static final class Conv { public String id, a, b, topic = "football"; public int stage, turn, next, ttl, fightT; public boolean flagged; public String lastLine = ""; }
    public static final List<Conv> convs = new ArrayList<>();

    public static Conv convOf(Npc n) { for (Conv c : convs) if (c.a.equals(n.id) || c.b.equals(n.id)) return c; return null; }
    public static Npc other(Conv c, Npc n) { return byId.get(c.a.equals(n.id) ? c.b : c.a); }
    static String ts(Npc n) { int t = Data.ti(n.team); return t < 0 ? "the lads" : Data.TEAM_SHORT[t]; }
    static String tn(String topic) { return switch (topic) { case "football" -> "football"; case "politics" -> "politics"; default -> topic; }; }

    // ---------- main tick ----------
    static boolean chatty(Npc n) {
        if (!n.inPub || !n.conv.isEmpty()) return false;
        return n.st.equals("sit") || n.st.equals("stand") || n.st.equals("tv") || n.st.equals("idle");
    }

    static void minute() {
        // update running convs
        for (Conv c : new ArrayList<>(convs)) {
            Npc a = byId.get(c.a), b = byId.get(c.b);
            if (a == null || b == null || !a.inPub || !b.inPub || a.st.equals("walk") && c.turn > 0 && Util.dist(a.x, a.y, b.x, b.y) > 200) { end(c, false); continue; }
            if (c.flagged) {
                c.fightT--;
                for (Npc p : S.npcs) if (p.inPub && p != a && p != b && (p.peace || p.id.equals("tony")) && Util.dist(p.x, p.y, a.x, a.y) < 220 && Util.chance(.18)) { peacemake(c, p); break; }
                Staff mgr = staffByRole("manager"); if (mgr != null && c.flagged && Util.chance(.04 + mgr.skill * .01)) { settle(c, "The manager steps in and defuses it."); continue; }
                if (c.flagged && c.fightT <= 0) { blowup(c); continue; }
            }
            if (--c.next <= 0) turn(c);
            if (--c.ttl <= 0 && !c.flagged) end(c, true);
        }
        // start new ones
        List<Npc> pool = new ArrayList<>(); for (Npc n : S.npcs) if (chatty(n)) pool.add(n);
        Collections.shuffle(pool, Util.R);
        for (Npc n : pool) {
            if (!n.conv.isEmpty()) continue;
            if (!Util.chance(.04 + .08 * n.soc)) continue;
            List<Npc> cand = new ArrayList<>();
            for (Npc o : pool) if (o != n && o.conv.isEmpty() && Util.dist(n.x, n.y, o.x, o.y) < 150) cand.add(o);
            if (cand.isEmpty()) continue;
            Npc p = Util.wpick(cand, o -> 1 + Math.max(0, rel(n.id, o.id) + 25) / 25.0 + (S.crush.contains(n.id + "|" + o.id) ? 3 : 0) + (rivals(n.id, o.id) ? n.tmp * 2 : 0) + n.gos * o.gos * 2);
            if (p != null) start(n, p);
        }
    }

    static void start(Npc a, Npc b) {
        Conv c = new Conv(); c.id = Util.pick("c") + Util.ri(1000, 9999); c.a = a.id; c.b = b.id; c.next = 0; c.ttl = Util.ri(10, 26);
        a.conv = c.id; b.conv = c.id; convs.add(c);
        turn(c);
    }

    static void end(Conv c, boolean graceful) {
        convs.remove(c);
        Npc a = byId.get(c.a), b = byId.get(c.b);
        if (a != null) a.conv = ""; if (b != null) b.conv = "";
        if (c.flagged) { S.flags.remove("argue"); }
        if (a != null && b != null && c.stage >= 2) {
            String t = a.name + " and " + b.name + " had a heated row about " + c.topic;
            remember(a, "argument", -3, a.id, b.id, t); remember(b, "argument", -3, b.id, a.id, t);
            addRelBoth(a.id, b.id, -2);
            witnesses(a, b, "argument", -2, t);
            stat("arguments"); a.arguedToday++; b.arguedToday++;
            String k = "arg:" + pair(a.id, b.id); int cnt = Integer.parseInt(S.flags.getOrDefault(k, "0")) + 1; S.flags.put(k, "" + cnt);
            if (cnt >= 3 && rel(a.id, b.id) <= -45 && rel(b.id, a.id) <= -45 && !S.flags.containsKey("legendary:" + pair(a.id, b.id))) {
                S.flags.put("legendary:" + pair(a.id, b.id), "1"); stat("legendaryRivalries");
                news("A LEGENDARY RIVALRY IS BORN", a.name + " and " + b.name + " have now fallen out " + cnt + " times. 'It's personal,' said a witness. 'It's always personal.'");
                log("A legendary rivalry: " + first(a) + " vs " + first(b) + ".", "story");
                Mgmt.addJoke(first(a) + " vs " + first(b), a.id, b.id);
            }
            if (S.couples.contains(pair(a.id, b.id)) && Util.chance(.4)) breakUp(a, b);
        }
        if (graceful && a != null && b != null && c.stage == 0 && Util.chance(.4)) {
            if (Util.chance(.5)) say(a, Util.pick("Anyway, I'll let you get on.", "Right. Another?", "Good chat.", "Anyway. Pint?"));
        }
    }

    static void witnesses(Npc a, Npc b, String kind, double w, String text) {
        for (Npc n : S.npcs) if (n.inPub && n != a && n != b && Util.dist(n.x, n.y, a.x, a.y) < 180 && Util.chance(.7)) {
            Mem m = new Mem(S.day, kind, w * .6, a.id, b.id, text); m.src = "saw"; n.mem.add(0, m); while (n.mem.size() > 40) n.mem.remove(n.mem.size() - 1);
        }
    }

    // ---------- turn ----------
    static void turn(Conv c) {
        Npc a = byId.get(c.a), b = byId.get(c.b);
        if (a == null || b == null) { end(c, false); return; }
        Npc sp = c.turn % 2 == 0 ? a : b, li = sp == a ? b : a; c.turn++;
        c.next = Util.ri(3, 5);
        if (Util.chance(.08)) { if (interject(c, sp, li)) return; }
        if (S.crush.contains(sp.id + "|" + li.id) && c.stage == 0 && Util.chance(.3)) { flirt(sp, li, c); return; }
        if (c.stage >= 1) { argueTurn(c, sp, li); return; }
        double r = Util.R.nextDouble();
        if (r < .04 + .0008 * (sp.drunk + li.drunk)) { mishear(sp, li); return; }
        if (r < .13 && !S.jokes.isEmpty()) { jokeCallback(sp, li); return; }
        if (r < .24 && memoryCallback(sp, li)) return;
        if (sp.gos > .45 && r < .13 + .3 * sp.gos && gossip(sp, li, c)) return;
        if (r < .5 && Util.chance(.3)) { story(sp, li, c); return; }
        statement(c, sp, li);
    }

    static String pickTopic(Npc sp, Npc li) {
        List<String> ks = Arrays.asList(Data.TOPIC_KEYS);
        return Util.wpick(ks, t -> {
            double w = .5 + 1.8 * Math.abs(sp.op.getOrDefault(t, 0.0)) + .8 * Math.abs(li.op.getOrDefault(t, 0.0));
            if (t.equals("football") && (S.match != null || Ai.matchToday())) w *= 3;
            if (t.equals("weather") && S.weather >= 2) w *= 1.6;
            if (t.equals("money") && (sp.money < 15 || li.money < 15)) w *= 1.8;
            if (t.equals("pub") && (S.sat < 45 || S.clean < 50)) w *= 2;
            if (t.equals("politics") && sp.id.equals("dave")) w *= 2.5;
            return w;
        });
    }

    static void speak(Npc n, String t) { say(n, t, 4.6); S.flags.put("last:" + n.id, t); }

    static void statement(Conv c, Npc sp, Npc li) {
        String topic = c.turn <= 1 || Util.chance(.35) ? pickTopic(sp, li) : c.topic;
        boolean switched = !topic.equals(c.topic) && c.turn > 1;
        c.topic = topic;
        double o = sp.op.getOrDefault(topic, 0.0);
        if (topic.equals("pub")) o = Util.clamp((sp.exp / 18.0) + (S.clean - 55) / 60.0 - (S.price[0] > 5.4 ? .6 : 0), -1, 1);
        if (topic.equals("weather")) o = S.weather == 0 ? .8 : S.weather == 1 ? 0 : -.8;
        String[][] bank = Data.TOPICS.get(topic);
        String[] spc = Data.special(sp.id, topic);
        String line = spc != null && Util.chance(.6) ? Util.pick(spc) : Util.pick(bank[o > .25 ? 0 : o < -.25 ? 1 : 2]);
        line = Util.fill(line, "team", ts(sp), "other", first(li));
        if (switched && Util.chance(.5)) line = Util.pick(Data.CHANGE_TOPIC) + " " + line;
        speak(sp, line);
        if (topic.equals("football") && sp.id.equals("owen")) speak(sp, "Football? It's just 22 grown men crying about a bag of wind.");
        double lo = li.op.getOrDefault(topic, 0.0);
        if (topic.equals("pub")) lo = o;
        boolean teamClash = topic.equals("football") && !sp.team.isEmpty() && !li.team.isEmpty() && !sp.team.equals(li.team);
        double agr = o * lo;
        String n = first(sp);
        if (lo < -.6 && o > .3 && !topic.equals("pub")) { // hates the topic
            li.anger += 9; li.mood -= 2; addRel(li.id, sp.id, -1.2);
            speak(li, Util.fill(Util.pick(Data.R_ANNOYED), "n", n));
            if (sp.id.equals("dave") && li.id.equals("sarah")) { c.stage = Util.chance(.5) ? 1 : 0; if (c.stage == 1) speak(li, "Dave. Do NOT start on the government again."); }
        } else if (agr < -.1 || teamClash && Util.chance(.65)) {
            double pArg = Util.clamp(.22 + .5 * li.tmp + .004 * li.anger + .003 * li.drunk - .004 * Math.max(0, rel(li.id, sp.id)) + (teamClash ? .15 : 0), .05, .9);
            if (Util.chance(pArg)) {
                c.stage = 1; li.anger += 12; sp.anger += 6; speak(li, Util.fill(Util.pick(Data.R_HEATED), "n", n)); addRelBoth(sp.id, li.id, -1.2);
            } else {
                speak(li, Util.fill(Util.pick(Util.chance(li.hum) ? Data.R_LAUGH : Data.R_DISAGREE), "n", n)); addRel(li.id, sp.id, -.3); addRel(sp.id, li.id, .3);
                if (teamClash) { li.mood += 1; Mgmt.banter(sp, li); }
            }
        } else if (agr > .05) {
            speak(li, Util.fill(Util.pick(Data.R_AGREE), "n", n));
            addRelBoth(sp.id, li.id, .8); sp.mood += 1.5; li.mood += 1.5; li.exp += .3; sp.exp += .3;
            double conv = .03 * Math.max(0, rel(li.id, sp.id)) / 100.0; li.op.merge(topic, (o - lo) * conv, Double::sum);
            if (Util.chance(.1 + .15 * sp.hum)) {
                speak(li, Util.fill(Util.pick(Data.R_LAUGH), "n", n)); sp.mood += 3; li.mood += 3;
                String k = "lj:" + pair(sp.id, li.id) + topic; int cnt = Integer.parseInt(S.flags.getOrDefault(k, "0")) + 1; S.flags.put(k, "" + cnt);
                if (cnt == 3) Mgmt.addJoke(first(sp) + " & " + first(li) + "'s " + topic + " thing", sp.id, li.id);
                if (cnt == 2 && Util.chance(.4)) { String t = sp.name + " and " + li.name + " had a proper laugh about " + topic; remember(sp, "laugh", 2, sp.id, li.id, t); remember(li, "laugh", 2, li.id, sp.id, t); }
            }
        } else {
            speak(li, Util.pick(Util.chance(.5) ? Data.R_BORED : Data.R_AGREE).replace("{n}", n));
            addRelBoth(sp.id, li.id, .15);
        }
    }

    static void argueTurn(Conv c, Npc sp, Npc li) {
        double deesc = .25 + (1 - sp.tmp) * .35 + (sp.peace ? .4 : 0) - sp.anger * .004 - sp.drunk * .003 + (rel(sp.id, li.id) > 20 ? .15 : 0);
        if (Util.chance(deesc)) {
            speak(sp, Util.fill(Util.pick(Data.R_CALM), "n", first(li)));
            if (c.flagged) settle(c, null);
            c.stage = 0;
            if (Util.chance(.5)) { speak(li, Util.pick("...Yeah. Fair enough.", "Alright. Truce.", "Your round, then.")); addRelBoth(sp.id, li.id, 2); }
            return;
        }
        c.stage = Math.min(3, c.stage + 1);
        speak(sp, Util.fill(Util.pick(c.stage >= 3 ? Data.R_SHOUT : Data.R_HEATED), "n", first(li)));
        sp.anger += 8; li.anger += 6; addRelBoth(sp.id, li.id, -2.2);
        if (c.stage >= 3 && !c.flagged) {
            c.flagged = true; c.fightT = Util.ri(7, 12); S.flags.put("argue", c.id);
            sfx("shout"); L.alert("ARGUMENT: " + first(sp) + " vs " + first(li) + " — step in?");
            log("A row is brewing between " + sp.name + " and " + li.name + " (" + c.topic + ").", "drama");
            if (Util.chance(.5)) { Mess m = new Mess(); m.x = sp.x; m.y = sp.y + 12; m.made = S.min; S.messes.add(m); say(li, "Mind my pint!"); }
        }
    }

    static void settle(Conv c, String msg) {
        c.flagged = false; c.stage = 0; S.flags.remove("argue");
        Npc a = byId.get(c.a), b = byId.get(c.b);
        if (a != null) { a.anger *= .5; a.embar += 15; } if (b != null) { b.anger *= .5; b.embar += 15; }
        if (msg != null) log(msg, "drama");
        stat("rowsDefused");
    }

    static void peacemake(Conv c, Npc p) {
        say(p, Util.pick("Come on, lads. Not in front of the pigeon.", "Easy, easy. Let's all have a nice sit down.", "Right, that's enough.", "Gentlemen, please. And ladies."));
        Npc a = byId.get(c.a), b = byId.get(c.b);
        settle(c, p.name + " calms things down between " + first(a) + " and " + first(b) + ".");
        addRel(a.id, p.id, 2); addRel(b.id, p.id, 2);
        remember(p, "peace", 1, p.id, a.id, p.name + " stopped " + a.name + " and " + b.name + " coming to blows");
        stat("peacemade");
    }

    static void blowup(Conv c) {
        Npc a = byId.get(c.a), b = byId.get(c.b);
        c.flagged = false; S.flags.remove("argue");
        if (a == null || b == null) return;
        stat("blowups");
        double r = Util.R.nextDouble();
        boolean cctv = hasUp("cctv");
        if (r < .5 || cctv && r < .65) {
            log(first(a) + " and " + first(b) + " both storm out!", "drama");
            addRelBoth(a.id, b.id, -8);
            remember(a, "fight", -6, a.id, b.id, a.name + " stormed out after a bust-up with " + b.name); remember(b, "fight", -6, b.id, a.id, b.name + " stormed out after a bust-up with " + a.name);
            S.sat = Math.max(0, S.sat - 1.5);
            end(c, false); a.exp -= 8; b.exp -= 8; a.stayUntil = 0; b.stayUntil = 0; Ai.goLeave(a); Ai.goLeave(b);
            news("PUB ROW: TWO CUSTOMERS 'VOCALLY DISAGREE'", "Witnesses describe the scene as 'loud, but mostly about " + c.topic + "'.");
        } else if (r < .8) {
            c.stage = 0; end(c, false);
            Events.nearFight(a, b, c.topic);
        } else {
            log(first(a) + " and " + first(b) + " go quiet, embarrassed.", "drama");
            a.embar += 30; b.embar += 30; end(c, false);
        }
    }

    // ---------- special turns ----------
    static boolean interject(Conv c, Npc sp, Npc li) {
        Npc x = null;
        for (Npc n : S.npcs) if (n.inPub && n != sp && n != li && n.conv.isEmpty() && Util.dist(n.x, n.y, sp.x, sp.y) < 120) { x = n; break; }
        if (x == null) return false;
        speak(x, Util.pick(Data.INTERRUPT));
        if (rel(x.id, sp.id) < 0 && Util.chance(.5)) { speak(sp, "Nobody asked you, " + first(x) + "."); addRel(sp.id, x.id, -1.5); sp.anger += 4; }
        else { speak(sp, Util.pick("Oh, here he/she goes.", "Fine. Go on, then.", "Pull up a chair, " + first(x) + ".").replace("he/she", "we")); addRelBoth(x.id, sp.id, .6); }
        return true;
    }

    static void mishear(Npc sp, Npc li) {
        String[] ex = Util.pick(Data.MISHEAR);
        speak(sp, ex[0]); speak(li, ex[1]);
        if (Util.chance(.6)) { sp.mood += 2; li.embar += 6; Mgmt.bumpLaugh(sp, li); }
        stat("misunderstandings");
        String k = "misheard:" + pair(sp.id, li.id); int cnt = Integer.parseInt(S.flags.getOrDefault(k, "0")) + 1; S.flags.put(k, "" + cnt);
        if (cnt == 2) Mgmt.addJoke(first(li) + "'s selective hearing", li.id, sp.id);
    }

    static boolean memoryCallback(Npc sp, Npc li) {
        Mem pick = null;
        for (Mem m : sp.mem) if (li.id.equals(m.who) && !sp.id.equals(m.who) && S.day - m.day <= 14 && Math.abs(m.w) >= 2 && !m.kind.equals("saw")) { pick = m; break; }
        if (pick == null) return false;
        String when = Sim.dayName(pick.day), n = first(li);
        switch (pick.kind) {
            case "argument", "fight" -> { speak(sp, Util.pick("I haven't forgotten our row on " + when + ", " + n + ".", "Still sore about " + when + ", " + n + ". Just saying.")); sp.anger += 4; addRel(sp.id, li.id, -.8);
                speak(li, Util.pick("Oh, give it a rest.", "That was days ago!", "...Fair. I overreacted.")); if (Util.chance(.3)) { addRelBoth(sp.id, li.id, 2); speak(sp, "Pint to forget it?"); } }
            case "laugh" -> { speak(sp, "Still laughing about " + when + ", " + n + "."); speak(li, Util.pick("Ha! Classic.", "Don't start me off.")); addRelBoth(sp.id, li.id, 1.5); sp.mood += 3; }
            case "game" -> { speak(sp, "I still want that rematch from " + when + ", " + n + "."); speak(li, Util.pick("Name the time.", "Any day. Bring money.")); addRelBoth(sp.id, li.id, .6); }
            case "treat" -> { speak(sp, "Thanks again for the drink on " + when + ", " + n + "."); speak(li, Util.pick("Don't mention it.", "You'd do the same.")); addRelBoth(sp.id, li.id, 1.5); }
            default -> { return false; }
        }
        stat("memoryCallbacks"); return true;
    }

    static void jokeCallback(Npc sp, Npc li) {
        Joke j = Util.wpick(S.jokes, x -> 1 + x.count);
        if (j == null) return;
        j.count++; j.day = S.day;
        speak(sp, Util.fill(Util.pick("This is like {j} all over again.", "Not as bad as {j}, eh?", "Don't start on {j} again!", "Ha! {j}. Never gets old."), "j", j.name));
        speak(li, Util.fill(Util.pick(Data.R_LAUGH), "n", first(sp)));
        addRelBoth(sp.id, li.id, 1.2); sp.mood += 3; li.mood += 3;
        stat("jokeCallbacks");
    }

    static void story(Npc sp, Npc li, Conv c) {
        if (!sp.lifeNews.isEmpty() && Util.chance(.6)) {
            String ln = sp.lifeNews.get(Util.ri(0, sp.lifeNews.size() - 1));
            speak(sp, "Did I tell you? I " + ln.replace("has ", "have ").replace("just ", "").replace("is ", "am ").replace("was ", "was ") + ".");
            speak(li, Util.pick("No way!", "Good for you, I think?", "Blimey.", "Tell me everything."));
            addRelBoth(sp.id, li.id, .8);
            return;
        }
        speak(sp, freshStory());
        double belief = li.intel < .5 ? .8 : .45;
        if (Util.chance(belief)) { speak(li, Util.pick("That never happened.", "Stop it. That's not true.", "Pull the other one.")); sp.embar += 3; }
        else { speak(li, Util.pick(Data.R_LAUGH).replace("{n}", first(sp))); sp.mood += 3; li.mood += 3; }
        addRelBoth(sp.id, li.id, .7);
        if (sp.id.equals("kev") && Util.chance(.5)) Events.longStory(sp);
    }

    static void flirt(Npc sp, Npc li, Conv c) {
        speak(sp, Util.fill(Util.pick(Data.R_FLIRT), "n", first(li)));
        boolean ok = rel(li.id, sp.id) > 25 || S.crush.contains(li.id + "|" + sp.id);
        sp.embar += 10;
        if (ok) {
            speak(li, Util.pick(Data.R_FLIRT_OK)); addRelBoth(sp.id, li.id, 6); sp.mood += 8; li.mood += 6;
            S.crush.add(li.id + "|" + sp.id);
            if (S.crush.contains(sp.id + "|" + li.id) && S.crush.contains(li.id + "|" + sp.id) && !S.couples.contains(pair(sp.id, li.id)) && Util.chance(.35)) makeCouple(sp, li);
        } else { speak(li, Util.fill(Util.pick(Data.R_FLIRT_NO), "n", first(sp))); sp.mood -= 6; sp.embar += 25; addRel(sp.id, li.id, -3); addRel(li.id, sp.id, -.5); S.crush.remove(sp.id + "|" + li.id); }
    }

    static void makeCouple(Npc a, Npc b) {
        S.couples.add(pair(a.id, b.id)); S.exes.remove(pair(a.id, b.id));
        addRelBoth(a.id, b.id, 15); stat("couples");
        log("💕 " + a.name + " and " + b.name + " are officially a couple!", "story");
        news("LOVE IS IN THE AIR AT THE SPECKLED PIGEON", a.name + " and " + b.name + " were seen 'holding hands near the crisps'. Linda Moss said she 'knew it first'.");
        Mgmt.addJoke(first(a) + " and " + first(b) + " getting together", a.id, b.id);
        remember(a, "love", 6, a.id, b.id, a.name + " and " + b.name + " got together"); remember(b, "love", 6, b.id, a.id, a.name + " and " + b.name + " got together");
    }

    static void breakUp(Npc a, Npc b) {
        S.couples.remove(pair(a.id, b.id)); S.exes.add(pair(a.id, b.id));
        S.crush.remove(a.id + "|" + b.id); S.crush.remove(b.id + "|" + a.id);
        addRelBoth(a.id, b.id, -25); a.mood -= 25; b.mood -= 25;
        log("💔 " + a.name + " and " + b.name + " have broken up — in public.", "story");
        news("COUPLE SPLIT IN PUB: 'IT WAS LOUD'", "Onlookers say the whole thing took less time than a pint.");
        remember(a, "breakup", -6, a.id, b.id, a.name + " and " + b.name + " broke up"); remember(b, "breakup", -6, b.id, a.id, b.name + " and " + a.name + " broke up");
    }

    static final Deque<String> recentStories = new ArrayDeque<>();
    static String freshStory() {
        String s = null;
        for (int i = 0; i < 12; i++) { s = Util.pick(Data.STORIES); if (!recentStories.contains(s)) break; }
        recentStories.addLast(s); while (recentStories.size() > 9) recentStories.pollFirst();
        return s;
    }

    // ---------- gossip ----------
    static boolean gossip(Npc sp, Npc li, Conv c) {
        List<Mem> pool = new ArrayList<>();
        for (Mem m : sp.mem) {
            if (m.about.equals(sp.id) || m.about.equals(li.id) || Math.abs(m.w) < 2) continue;
            boolean known = false; for (Mem x : li.mem) if (x.t.equals(m.t)) known = true;
            if (!known) pool.add(m);
        }
        List<String> secrets = new ArrayList<>();
        for (Npc o : S.npcs) if (!o.secretKnown && !o.secret.isEmpty() && "1".equals(sp.flags.get("knows:" + o.id)) && o != li && o != sp) secrets.add(o.id);
        if (!secrets.isEmpty() && Util.chance(.35)) {
            Npc o = byId.get(Util.pick(secrets));
            speak(sp, "I really shouldn't say... but " + o.name + ": " + o.secret);
            speak(li, Util.pick("NO. Absolutely not.", "I always wondered!", "Stop. Tell me more.", "I'm never looking at " + first(o) + " the same way again."));
            revealSecret(o, sp.id.equals("linda") ? "linda" : "gossip", sp);
            return true;
        }
        if (pool.isEmpty()) return false;
        Mem m = pool.get(Util.ri(0, pool.size() - 1));
        String t = m.t;
        boolean exag = Util.chance(.2);
        speak(sp, Util.pick("Did you hear? ", "Between us — ", "You didn't hear it from me, but ", "I shouldn't say, but ") + t.substring(0, 1).toLowerCase().replace(t.substring(0, 1).toLowerCase(), t.substring(0, 1)) + t.substring(1) + (exag ? " ...and I heard it was much worse." : "."));
        speak(li, Util.pick("No!", "Get out.", "Shut UP.", "I knew it.", "Well, that explains a lot."));
        Mem nm = new Mem(S.day, m.kind, m.w * (exag ? .9 : .6), m.about, m.who, exag ? t + " (allegedly worse)" : t); nm.src = sp.id; li.mem.add(0, nm);
        Npc subj = byId.get(m.about);
        if (subj != null) {
            addRel(li.id, subj.id, Math.signum(m.w) * 2);
            if (subj.inPub && Util.dist(subj.x, subj.y, sp.x, sp.y) < 170 && Util.chance(.6)) {
                say(subj, Util.pick("I can HEAR you, you know.", "Excuse me?! I'm right here!", "Who's been telling stories?"));
                subj.anger += 14; subj.embar += 18; addRel(subj.id, sp.id, -7);
                remember(subj, "gossip", -3, subj.id, sp.id, subj.name + " overheard " + sp.name + " gossiping about them");
                stat("overheard"); log(subj.name + " overheard " + first(sp) + " gossiping!", "drama");
            }
        }
        addRelBoth(sp.id, li.id, .7);
        stat("gossips");
        return true;
    }

    public static void revealSecret(Npc o, String how, Npc teller) {
        if (o.secretKnown) return;
        o.secretKnown = true; stat("secrets");
        o.embar += 30;
        log("SECRET OUT: " + o.name + " — " + o.secret, "story");
        String headline = switch (how) { case "linda" -> "OVERHEARD IN WESTBRIDGE: " + o.name.toUpperCase() + "'S SECRET"; case "drunk" -> "LOOSE LIPS: " + o.name.toUpperCase() + " SPILLS ALL"; default -> o.name.toUpperCase() + ": THE TRUTH EMERGES"; };
        news(headline, o.secret);
        for (Npc n : S.npcs) if (n != o && n.inPub) { addRel(n.id, o.id, o.secretGood ? 3 : (n.tmp > .5 ? -1 : 1)); remember(n, "secret", o.secretGood ? 2 : -1, o.id, o.id, "everyone learned that " + o.name + ": " + o.secret.substring(0, 1).toLowerCase() + o.secret.substring(1)); }
        Mgmt.addJoke(first(o) + "'s secret", o.id, teller == null ? o.id : teller.id);
        switch (o.id) {
            case "steve" -> { o.money = 8; o.income *= .5; o.flags.put("broke", "1"); }
            case "gaz" -> { o.mood -= 20; }
            case "mo" -> { if (!S.royalOak) { S.flags.put("moSells", "1"); } }
            case "liam" -> { o.wantsJob = true; }
            default -> {}
        }
        if (how.equals("linda")) for (Npc n : S.npcs) if (n.id.equals("linda")) { addRel(o.id, "linda", -8); }
    }

    // ---------- quirks ----------
    static void quirk(Npc n) {
        String line;
        if (n.id.equals("gaz")) { line = n.quirk[Math.min(n.quirkCount, n.quirk.length - 1)]; if (n.quirkCount >= n.quirk.length - 1) line = Util.pick(n.quirk); n.quirkCount++; }
        else line = Util.pick(n.quirk);
        speak(n, line); n.quirkCount += n.id.equals("gaz") ? 0 : 1;
        List<Npc> near = new ArrayList<>();
        for (Npc o : S.npcs) if (o.inPub && o != n && Util.dist(o.x, o.y, n.x, n.y) < 170) near.add(o);
        if (!near.isEmpty() && Util.chance(.7)) {
            Npc o = Util.pick(near);
            if (rel(o.id, n.id) < 15 && Util.chance(.6)) { speak(o, Util.fill(Util.pick("Here we go again...", "Not this one again.", "{n}, we KNOW."), "n", first(n))); addRel(o.id, n.id, -.4); }
            else { speak(o, Util.fill(Util.pick(Data.R_LAUGH), "n", first(n))); addRel(o.id, n.id, .5); }
        }
        if (n.quirkCount == 5 || n.quirkCount == 15) {
            String jn = switch (n.id) { case "gaz" -> "Gaz's semi-pro career"; case "dave" -> "Dave's twenty years"; case "steve" -> "Steve 'owning' the pub"; case "linda" -> "Linda's 'you didn't hear it from me'"; case "kev" -> "the 43 to Kingsfield"; default -> first(n) + "'s catchphrase"; };
            Mgmt.addJoke(jn, n.id, n.id);
        }
    }

    // ---------- misc hooks ----------
    static void leaveHook(Npc n) {
        Conv c = convOf(n);
        if (c != null) { if (c.flagged) S.flags.remove("argue"); convs.remove(c); Npc o = other(c, n); if (o != null) { o.conv = ""; if (Util.chance(.5)) say(o, Util.pick("Well, that was rude.", "Was it something I said?", "Where's he/she going?").replace("he/she", "they")); } }
        n.conv = "";
    }

    static void gameAftermath(Npc w, Npc l, String kind) {
        if (rivals(w.id, l.id) && l.tmp > .45 && Util.chance(.5)) { Conv c = new Conv(); c.id = "g" + Util.ri(100, 999); c.a = l.id; c.b = w.id; c.next = 1; c.ttl = 12; c.stage = 1; c.topic = "football"; w.conv = c.id; l.conv = c.id; convs.add(c); speak(l, "That's not how I remember it. Rematch. NOW."); }
        for (Npc n : S.npcs) if (n.inPub && n != w && n != l && Util.dist(n.x, n.y, w.x, w.y) < 200 && Util.chance(.2)) say(n, Util.pick("Good game!", "Ooh, close.", "Bet on the winner!", "Rematch! Rematch!"));
    }

    // ---------- daily relationship drift (crushes, friendships) ----------
    static void daily() {
        List<Npc> reg = new ArrayList<>(); for (Npc n : S.npcs) if (!n.stranger) reg.add(n);
        for (Npc a : reg) for (Npc b : reg) {
            if (a == b || a.id.compareTo(b.id) > 0) continue;
            String pr = pair(a.id, b.id);
            if (S.couples.contains(pr)) { addRelBoth(a.id, b.id, .6); continue; }
            double r = rel(a.id, b.id);
            if (r >= 40 && Math.abs(a.age - b.age) < 14 && !S.exes.contains(pr) && !S.crush.contains(a.id + "|" + b.id) && Util.chance(.01 + a.soc * .008)) {
                if (rel(b.id, a.id) >= 25) { S.crush.add(a.id + "|" + b.id); log(first(a) + " has a crush on " + first(b) + "... (nobody knows yet)", "story"); }
            }
            // slow decay toward zero
            if (Math.abs(r) > 5 && Math.abs(r) < 90 && !S.couples.contains(pr)) { double dec = r * -.004; addRel(a.id, b.id, dec); }
            // mutual long-term friendship through hanging out
            if (a.lastVisit == S.day - 1 && b.lastVisit == S.day - 1) addRelBoth(a.id, b.id, .15);
        }
        // Linda & Jade pick up secrets
        for (Npc g : reg) if (g.gos >= .8 && Util.chance(.18)) {
            List<Npc> un = new ArrayList<>(); for (Npc o : reg) if (o != g && !o.secretKnown && !o.secret.isEmpty() && !"1".equals(g.flags.get("knows:" + o.id))) un.add(o);
            if (!un.isEmpty()) { Npc o = Util.pick(un); g.flags.put("knows:" + o.id, "1"); }
        }
        // opinion drift among friends is in statement()
    }

    // ---------- relationship milestones ----------
    static void bondCheck(String a, String b) {
        String pr = pair(a, b);
        double ab = rel(a, b), ba = rel(b, a);
        String cur = S.flags.getOrDefault("bond:" + pr, "");
        String nw = (ab >= 40 && ba >= 40) ? "friend" : (ab <= -40 && ba <= -40) ? "rival" : "";
        if (ab >= 75 && ba >= 75) nw = "best";
        if (cur.equals(nw) || nw.isEmpty() && !cur.isEmpty() && (ab > -25 && ab < 25)) { if (nw.isEmpty() && !cur.isEmpty()) S.flags.put("bond:" + pr, ""); return; }
        S.flags.put("bond:" + pr, nw);
        Npc A = byId.get(a), B = byId.get(b); if (A == null || B == null || nw.isEmpty()) return;
        if (nw.equals("friend") || nw.equals("best")) {
            if (cur.equals("rival")) { stat("rivalsToFriends"); log("🤝 " + A.name + " and " + B.name + " have buried the hatchet!", "story"); news("UNLIKELY FRIENDSHIP BLOSSOMS", A.name + " and " + B.name + " 'actually get on now'. Locals suspect witchcraft."); }
            else if (nw.equals("friend")) { stat("friendships"); log(A.name + " and " + B.name + " are now friends.", "story"); }
            else { log(A.name + " and " + B.name + " are best mates now.", "story"); }
        } else if (nw.equals("rival")) { log("⚔ " + A.name + " and " + B.name + " are now proper rivals.", "story"); stat("rivalries"); }
    }
}
