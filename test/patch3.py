def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

def gw(s):
    s = s.replace('final JPanel glass = new JPanel(new GridBagLayout());', 'final JPanel glass = new JPanel(new GridBagLayout()) { @Override protected void paintComponent(Graphics g) { g.setColor(new Color(0, 0, 0, 120)); g.fillRect(0, 0, getWidth(), getHeight()); } };')
    return s
rw('src/pub/GameWindow.java', gw)

def gm(s):
    s = s.replace('JPanel qp = new JPanel(new BorderLayout()); qp.setOpaque(false); qp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); qp.add(qLabel, BorderLayout.NORTH);\n            JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10)); grid.setOpaque(false);',
                  'JPanel qp = new JPanel(new BorderLayout(0, 14)); qp.setOpaque(false); qp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); qp.add(qLabel, BorderLayout.NORTH);\n            JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10)); grid.setOpaque(false); grid.setPreferredSize(new Dimension(500, 170));')
    s = s.replace('qp.add(grid, BorderLayout.CENTER);', 'JPanel gw = new JPanel(new BorderLayout()); gw.setOpaque(false); gw.add(grid, BorderLayout.NORTH); qp.add(gw, BorderLayout.CENTER);')
    return s
rw('src/pub/Games.java', gm)

def so(s):
    s = s.replace('    static void speak(Npc n, String t) { say(n, t, 6); S.flags.put("last:" + n.id, t); }', '    static void speak(Npc n, String t) { say(n, t, 4.6); S.flags.put("last:" + n.id, t); }')
    s = s.replace('        speak(sp, Util.pick(Data.STORIES));', '        speak(sp, freshStory());')
    s = s.replace('    // ---------- gossip ----------', '''    static final Deque<String> recentStories = new ArrayDeque<>();
    static String freshStory() {
        String s = null;
        for (int i = 0; i < 12; i++) { s = Util.pick(Data.STORIES); if (!recentStories.contains(s)) break; }
        recentStories.addLast(s); while (recentStories.size() > 9) recentStories.pollFirst();
        return s;
    }

    // ---------- gossip ----------''', 1)
    return s
rw('src/pub/Social.java', so)

def ai(s):
    s = s.replace('        if (fan) p += 0.4;', '        if (fan) p += 0.4; else if (md != null && n.sport > .4) p += 0.15;')
    s = s.replace('Match m = S.match; if (m != null && !m.phase.equals("ft") && S.upgrades.contains("tv")) r *= 1.7;', 'Match m = S.match; if (m != null && !m.phase.equals("ft") && S.upgrades.contains("tv")) r *= (m.rovers ? 2.4 : 1.6) * (S.upgrades.contains("bigscreen") ? 1.25 : 1);')
    return s
rw('src/pub/Ai.java', ai)

def sim(s):
    s = s.replace('        S.news.add(new News("NEW LANDLORD TAKES OVER THE SPECKLED PIGEON"', '        S.archive.add(new News("NEW LANDLORD TAKES OVER THE SPECKLED PIGEON"').replace('        S.news.add(new News("WESTBRIDGE ROVERS PREPARE FOR SATURDAY"', '        S.archive.add(new News("WESTBRIDGE ROVERS PREPARE FOR SATURDAY"')
    s = s.replace('        log("Welcome to " + S.pubName', '        S.archive.add(new News("MYSTERY PIGEON CONTINUES TO VISIT LOCAL PUB", "Residents baffled as bird attends for another week. \'He seems to like the scampi fries,\' said one witness.", 1));\n        log("Welcome to " + S.pubName')
    return s
rw('src/pub/Sim.java', sim)
