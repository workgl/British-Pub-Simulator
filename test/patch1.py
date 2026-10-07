def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

EV_ADD = r'''    // ---------- staff storylines ----------
    static Staff twistCandidate() { for (Staff s : S.staff) if (!s.fromNpc.isEmpty() && S.day - s.hiredDay >= 10 && s.present && !S.flags.containsKey("twist:" + s.id)) return s; return null; }
    static void staffTwist() {
        Staff s = twistCandidate(); if (s == null) return; S.flags.put("twist:" + s.id, "1");
        int r = Util.ri(0, 2); String f = s.name.split(" ")[0];
        if (r == 0) {
            ask(f + " Has Invented Something", f + " is behind the bar with a shaker and a dangerous look of confidence: \"I've made a cocktail. It's called The Pigeon Drop. Try it.\"",
                opt("Taste it", () -> { if (Util.chance(.7)) { done("It's... incredible. Regulars are queueing for The Pigeon Drop."); rep(3); S.pop += 3; S.prestige += 30; news("PUB BARTENDER INVENTS 'THE PIGEON DROP'", f + " has created a drink 'with notes of surprise'. Demand is 'brisk'."); Mgmt.addJoke("The Pigeon Drop", "dave", "gaz"); } else { done("It tastes of cough medicine and regret. But " + f + " is so proud."); s.mood += 10; } }),
                opt("Put it on the menu (£20 ingredients)", () -> { spend(20); rep(2); S.pop += 2; done("The Pigeon Drop goes on the specials board."); }),
                opt("Politely decline", () -> { s.mood -= 10; done(f + " pours it down the sink, wounded."); }));
        } else if (r == 1) {
            ask(f + "'s Mates Drink Free", "You notice " + f + "'s friends have been getting a LOT of 'accidental' double measures. The stock count is down.",
                opt("Have a quiet word", () -> { s.mood -= 5; s.rely += 1; for (int i = 0; i < 6; i++) S.stock[i] = Math.max(0, S.stock[i] - 3); done(f + " blushes and promises to behave. Mostly."); }),
                opt("Dock their wages (£30)", () -> { addMoney(30); s.mood -= 20; done(f + " pays up, sulking."); }),
                opt("Sack them", () -> { Mgmt.fire(s); }),
                opt("Let it slide", () -> { for (int i = 0; i < 6; i++) S.stock[i] = Math.max(0, S.stock[i] - 8); s.mood += 10; for (Npc n : S.npcs) if (n.inPub) n.mood += 2; done("Free drinks for friends. The pub loves it. The accountant does not."); }));
        } else {
            ask(f + " Has A Job Offer", f + " has been offered a job at the Frog & Trumpet: 'Better hours, and a dental plan.'",
                opt("Counter-offer: +£15/day", () -> { s.wage += 15; s.mood += 30; done(f + " stays. Wages up."); }),
                opt("Wish them luck", () -> { Mgmt.fire(s); S.rivalStr += 3; done(f + " leaves for the Frog & Trumpet. The regulars grumble."); S.sat -= 1; }),
                opt("Promise a promotion (manager)", () -> { s.role = "manager"; s.wage += 25; s.mood += 25; done(f + " is now your manager."); }));
        }
    }

    // ---------- special days ----------
    public static String specialDay() {
        int m = Sim.monthIdx(), d = (S.day - 1) % 28 + 1;
        if (m == 1 && d == 28) return "Halloween"; if (m == 2 && d == 5) return "Bonfire Night"; if (m == 3 && d == 24) return "Christmas Eve"; if (m == 3 && d == 28) return "New Year's Eve";
        if (m == 5 && d == 14) return "Valentine's Day"; if (m == 7 && d == 23) return "St George's Day"; return "";
    }

    // ---------- Friday man storyline ----------'''

def ev(s):
    s = s.replace('        EVENTS.add(new Ev("fridayMan"', '        EVENTS.add(new Ev("staffTwist", () -> twistCandidate() != null && crowd() >= 5 ? 1.4 : 0, Events::staffTwist, 3));\n        EVENTS.add(new Ev("fridayMan"')
    s = s.replace('    // ---------- Friday man storyline ----------', EV_ADD)
    return s
rw('src/pub/Events.java', ev)

LIFELONG = r'''        if (n.defected && n.loyalty > 45) { n.defected = false; log(n.name + " is back!", "story"); }
        if (!n.stranger && n.visits >= 80 && n.loyalty >= 85 && !n.flags.containsKey("lifelong")) {
            n.flags.put("lifelong", "1"); stat("lifelongs");
            log(n.name + " has become a lifelong regular.", "story"); news("LIFELONG REGULAR: " + n.name.toUpperCase(), "'I'll be here till they carry me out,' said " + n.name + ". 'Preferably after closing.'");
        }'''
def ai(s):
    s = s.replace("        if (dw == 4 && n.money > 15) p += .12;", "        if (dw == 4 && n.money > 15) p += .12;\n        if (!Events.specialDay().isEmpty()) p += .25;")
    s = s.replace("        if (S.sat < 40) r *= .6;\n        return r;", "        if (S.sat < 40) r *= .6;\n        if (!Events.specialDay().isEmpty()) r *= 1.6;\n        return r;")
    s = s.replace('        if (n.defected && n.loyalty > 45) { n.defected = false; log(n.name + " is back!", "story"); }', LIFELONG)
    return s
rw('src/pub/Ai.java', ai)

TIPS = r'''    public static String regLabel(Npc n) {
        if (n.stranger) return "Newcomer"; if (n.visits >= 80 && n.loyalty >= 85) return "Lifelong regular"; if (n.loyalty >= 72) return "Pillar of the pub"; if (n.visits < 10) return "Occasional"; return "Regular";
    }
    static final String[] TIPS = {"Tip: stand behind the bar (top-left) and you'll serve customers automatically.", "Tip: click a person, then 'Talk' - they remember how you treat them.", "Tip: check Manage > Stock before busy nights. Empty taps make people furious.", "Tip: Friday, Saturday and match days are your big earners.", "Tip: click spills on the floor to mop them up.", "Tip: the TV shows live fictional football. Click it for the league table.", "Tip: Linda gossips. Do not tell Linda secrets.", "Tip: book a karaoke or band night in Manage > Events & Ads.", "Tip: some customers will ask you for a job. They might be brilliant."};
    static int tipIdx;

    static void minute() {
        if (S.day <= 6 && S.min % 150 == 5 && isOpen() && tipIdx < TIPS.length) toast(TIPS[tipIdx++]);
'''
def mg(s):
    s = s.replace('        if (S.quizOn && dow() == 2) log("Quiz night tonight at 20:00.", "event");',
                  '        String sp = Events.specialDay(); if (!sp.isEmpty()) { log("Tonight is " + sp + "! Expect a crowd.", "event"); news(sp.toUpperCase() + " IN WESTBRIDGE", "Pubs across town expect a busy night. The Speckled Pigeon is ready, ish."); }\n        if (S.quizOn && dow() == 2) log("Quiz night tonight at 20:00.", "event");')
    s = s.replace('    static void minute() {\n', TIPS, 1)
    return s
rw('src/pub/Mgmt.java', mg)

rw('src/pub/Gfx.java', lambda s: s.replace('        if (!S.decor.contains("fairy")) return;', '        if (!S.decor.contains("fairy") && Sim.monthIdx() != 3 && Events.specialDay().isEmpty()) return;'))
rw('src/pub/GameWindow.java', lambda s: s.replace('" · " + Screens.class.getSimpleName().substring(0, 0) + (n.favName.isEmpty()', '" · " + Mgmt.regLabel(n) + " · " + (n.favName.isEmpty()'))
