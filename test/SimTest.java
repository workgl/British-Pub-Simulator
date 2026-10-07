import pub.*;
import pub.Model.*;
import java.util.*;

public class SimTest {
    public static void main(String[] a) {
        int days = a.length > 0 ? Integer.parseInt(a[0]) : 14;
        boolean verbose = a.length > 1;
        Sim.newGame("The Speckled Pigeon");
        Map<String,Integer> cats = new TreeMap<>();
        Sim.L = new Sim.Listener() {
            public void log(String t, String c) { cats.merge(c, 1, Integer::sum); if (verbose && !c.equals("arrive")) System.out.println(" [" + c + "] " + t); }
            public void choice(Sim.Choice c) { Sim.Opt o = c.opts.get(Util.ri(0, c.opts.size() - 1)); if (verbose) System.out.println("   CHOICE: " + c.title + " -> " + o.label); Events.modalOpen = false; o.run.run(); }
            public void dayEnd(DayRec r) { State S = Sim.S; System.out.printf("Day %d %s: rev %.0f exp %.0f money %.0f rep %.1f pop %.1f sat %.1f peak %d served %d level %d%n", r.day, Sim.dayName(r.day), r.rev, r.exp, S.money, S.rep, S.pop, S.sat, r.crowd, r.served, S.level); }
        };
        long t0 = System.currentTimeMillis();
        int target = days * 1440;
        for (int i = 0; i < target; i++) {
            Player.x = 200; Player.y = 150; Sim.advance(1.0, 1.0);
            if (i % 5 == 0) Sim.advance(0.5, 0.0001);
            // keep stock topped up
            if (Sim.S.min == 9 * 60) for (int k = 0; k < 7; k++) if (Sim.S.stock[k] < 50) Mgmt.order(k, 40);
            if (Sim.S.min == 9 * 60 && Sim.S.stock[8] < 15) Mgmt.order(8, 20);
            if (Sim.S.min == 9 * 60 && Sim.S.stock[7] < 15) Mgmt.order(7, 20);
        }
        System.out.println("ms=" + (System.currentTimeMillis() - t0) + " cats=" + cats);
        System.out.println("stats=" + Sim.S.stats);
        System.out.println("ach=" + Sim.S.ach.keySet());
    }
}
