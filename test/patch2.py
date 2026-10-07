def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)
OLD = '''    static void endGameFor(Npc n) {
        for (Iterator<Game> it = games.iterator(); it.hasNext(); ) { Game g = it.next(); if (g.a == n || g.b == n) { it.remove(); Npc o = g.a == n ? g.b : g.a; if (o.inPub && (o.st.equals("darts") || o.st.equals("pool"))) { o.timer = 0; decide(o); } } }
    }'''
NEW = '''    static void endGameFor(Npc n) {
        List<Npc> others = new ArrayList<>();
        for (Iterator<Game> it = games.iterator(); it.hasNext(); ) { Game g = it.next(); if (g.a == n || g.b == n) { it.remove(); others.add(g.a == n ? g.b : g.a); } }
        for (Npc o : others) if (o.inPub && (o.st.equals("darts") || o.st.equals("pool"))) { o.timer = 0; decide(o); }
    }'''
rw('src/pub/Ai.java', lambda s: s.replace(OLD, NEW))
