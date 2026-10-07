def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

def ev(s):
    s = s.replace('''    public static String specialDay() {
        int m = Sim.monthIdx(), d = (S.day - 1) % 28 + 1;''', '''    public static String specialDay() { return specialDay(S.day); }
    public static String specialDay(int day) {
        int m = ((day - 1) / 28) % 12, d = (day - 1) % 28 + 1;''')
    return s
rw('src/pub/Events.java', ev)

NOTICE = r'''
    // ================================================================
    //  NOTICE BOARD
    // ================================================================
    public static JComponent notice() {
        JPanel p = vbox(); p.add(inkSerif("What's on at " + S.pubName, 20)); p.add(new JLabel(" "));
        for (int i = 0; i < 8; i++) {
            int d = S.day + i; List<String> items = new ArrayList<>();
            if (S.quizOn && Sim.dow(d) == 2) items.add("Pub Quiz, 20:00");
            if (S.roastOn && Sim.dow(d) == 6) items.add("Sunday Roast, 12:00-16:00");
            if (Sim.dow(d) == 4) items.add("Friday night - expect a crowd");
            for (Match m : S.fixtures) if (m.day == d) items.add(Util.hhmm(m.kickoff) + " " + Data.TEAM_SHORT[m.home] + " v " + Data.TEAM_SHORT[m.away] + (m.rovers ? "  *** ROVERS ***" : ""));
            for (Booking b : S.bookings) if (b.day == d) items.add(b.type.toUpperCase() + " NIGHT (booked)");
            String sp = Events.specialDay(d); if (!sp.isEmpty()) items.add(sp.toUpperCase());
            for (Delivery dv : S.deliveries) if (dv.arrives == d) items.add("Delivery: " + dv.qty + " x " + Data.DNAME[dv.item]);
            String head = (i == 0 ? "Today" : i == 1 ? "Tomorrow" : Sim.dayName(d)) + " (day " + d + ")";
            JLabel hl = ink(head, 14, true); p.add(hl);
            if (items.isEmpty()) p.add(ink("   Nothing special.", 12, false)); else for (String it : items) p.add(ink("   - " + it, 12, false));
        }
        p.add(new JLabel(" ")); p.add(hrow(btn("Book events & advertising", () -> { GameWindow.close(); GameWindow.open("Manage the pub", manage(4)); })));
        JScrollPane sp = scroll(p); sp.setPreferredSize(new Dimension(620, 480)); return sp;
    }
'''
def sc(s):
    s = s.replace('    // ================================================================\n    //  MENU / HELP / DAY SUMMARY', NOTICE + '\n    // ================================================================\n    //  MENU / HELP / DAY SUMMARY', 1)
    return s
rw('src/pub/Screens.java', sc)
rw('src/pub/GameWindow.java', lambda s: s.replace('case "notice" -> { open("Manage the pub", Screens.manage(4)); return true; }', 'case "notice" -> { open("Notice board", Screens.notice()); return true; }'))
