def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

def ch(s):
    s = s.replace('''    public static Npc stranger() {
        String fn = Util.pick(Data.STRANGER_FIRST), ln = Util.pick(Data.STRANGER_LAST);''', '''    public static Npc stranger() { return stranger(new HashSet<>()); }
    public static Npc stranger(Set<String> used) {
        String fn = Util.pick(Data.STRANGER_FIRST); for (int i = 0; i < 20 && used.contains(fn); i++) fn = Util.pick(Data.STRANGER_FIRST);
        String ln = Util.pick(Data.STRANGER_LAST);''')
    return s
rw('src/pub/Chars.java', ch)

def ai(s):
    s = s.replace('Npc s = Chars.stranger(); Chars.fillDefaults(s);\n            s.loyalty = 40;', 'Set<String> used = new HashSet<>(); for (Npc o : S.npcs) used.add(o.name.split(" ")[0]);\n            Npc s = Chars.stranger(used); Chars.fillDefaults(s);\n            s.loyalty = 40;')
    s = s.replace('log(first(n) + " challenges " + first(b) + " at " + kind + ".", "game");', 'if (!n.stranger || !b.stranger) log(first(n) + " challenges " + first(b) + " at " + kind + ".", "game");')
    s = s.replace('log(w.name + " beat " + l.name + " at " + g.kind + ".", "game"); stat("npcGames");', 'if (!w.stranger || !l.stranger) log(w.name + " beat " + l.name + " at " + g.kind + ".", "game"); stat("npcGames");')
    s = s.replace('if (crowd() < 5) line = Util.pick("Quiet in here...", "Where is everybody?", "Just us, then.");', 'if (crowd() < 4 && Util.chance(.3)) line = Util.pick("Quiet in here...", "Where is everybody?", "Just us, then.");\n            Mem pm = null; for (Mem m : n.mem) if ("player".equals(m.who) && Math.abs(m.w) >= 2 && S.day - m.day <= 10) { pm = m; break; }\n            if (pm != null && Util.chance(.5)) line = pm.w > 0 ? Util.pick("Still thinking about that kindness, landlord!", "Evening, landlord. I haven\'t forgotten the other day — thanks.") : Util.pick("Evening, landlord. I haven\'t forgotten, you know.", "Hmph. Landlord.");')
    return s
rw('src/pub/Ai.java', ai)

def so(s):
    s = s.replace('        if (r < .13 && !S.jokes.isEmpty()) { jokeCallback(sp, li); return; }', '        if (r < .13 && !S.jokes.isEmpty()) { jokeCallback(sp, li); return; }\n        if (r < .24 && memoryCallback(sp, li)) return;')
    s = s.replace('    static void jokeCallback(Npc sp, Npc li) {', '''    static boolean memoryCallback(Npc sp, Npc li) {
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

    static void jokeCallback(Npc sp, Npc li) {''', 1)
    return s
rw('src/pub/Social.java', so)

def pl(s):
    s = s.replace('        if (n.wantsJob && n.trust > -15 && !n.staff && S.staff.size() < 8) {', '        if (n.wantsJob && n.trust > -15 && !n.staff && S.staff.size() < 8 && S.day - Integer.parseInt(n.flags.getOrDefault("jobAsked", "-9")) >= 3) {\n            n.flags.put("jobAsked", "" + S.day);')
    return s
rw('src/pub/Player.java', pl)
