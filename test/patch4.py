def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)
def gw(s):
    s = s.replace('p.add(Screens.inkSerif("🍺 British Pub Simulator", 28));', 'p.add(Screens.inkSerif("British Pub Simulator", 30));')
    s = s.replace('weatherIcon(g, x + 78 + 120, 26);', 'weatherIcon(g, x + 78 + 150, 30);')
    s = s.replace('x = 560; g.setFont(serif(22, true))', 'x = 620; g.setFont(serif(22, true))')
    s = s.replace('g.drawString(Sim.dateStr(), x + 78, 28); g.drawString(Data.SEASONS[Sim.season()]', 'g.drawString(Sim.dateStr(), x + 78, 28); g.drawString(Data.SEASONS[Sim.season()]')
    return s
rw('src/pub/GameWindow.java', gw)
