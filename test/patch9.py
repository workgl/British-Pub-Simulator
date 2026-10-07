def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

def ai(s):
    # meal-time hunger on arrival
    s = s.replace('        sfx("door");\n        if (n.stranger) {', '''        double hh = hour();
        if (hh >= 11.5 && hh < 14.8) n.hunger = Math.max(n.hunger, 55 + (dow() == 6 && S.roastOn ? 25 : 0) + Util.r(0, 20));
        else if (hh >= 17.3 && hh < 20) n.hunger = Math.max(n.hunger, 45 + Util.r(0, 25));
        sfx("door");
        if (n.stranger) {''', 1)
    # waiter table service in sit state
    s = s.replace('        else if (n.st.equals("sit") || n.st.equals("stand")) {\n            if (wantsLeave(n) && Util.chance(.2)) goLeave(n);',
                  '''        else if (n.st.equals("sit") || n.st.equals("stand")) {
            Staff wt = staffByRole("waiter");
            if (wt != null && n.st.equals("sit") && n.order == null && !n.flags.containsKey("tbl") && wantsDrink(n) && Util.chance(.3)) {
                int d = chooseDrink(n);
                if (d >= 0 && n.money >= S.price[d]) { Order o = new Order(); o.drink = d; o.since = S.min; n.order = o; n.flags.put("tbl", "" + (S.min + Util.ri(2, 5))); n.doing = "Waiting for the waiter"; }
            }
            if (n.flags.containsKey("tbl") && S.min >= Integer.parseInt(n.flags.get("tbl")) && n.order != null) { n.flags.remove("tbl"); Staff w2 = staffByRole("waiter"); serve(n, w2); return; }
            if (wantsLeave(n) && Util.chance(.2)) goLeave(n);''', 1)
    s = s.replace('        if (n.order != null) { S.queue.remove(n.id); n.order = null; }\n        endGameFor(n);', '        if (n.order != null) { S.queue.remove(n.id); n.order = null; }\n        n.flags.remove("tbl");\n        endGameFor(n);', 1)
    s = s.replace('    static void finishServe(Npc n) {\n        n.st = "idle";', '    static void finishServe(Npc n) {\n        if (n.st.equals("sit")) { n.doing = ""; return; }\n        n.st = "idle";', 1)
    # waiter speeds food
    s = s.replace('            n.foodT = Math.max(6, 22 - sk); n.nextDur = 0; n.exp += 2;', '            n.foodT = Math.max(5, 22 - sk - (hasStaff("waiter") ? 3 : 0) - (S.upgrades.contains("kitchen2") ? 4 : 0)); n.nextDur = 0; n.exp += 2;')
    return s
rw('src/pub/Ai.java', ai)
