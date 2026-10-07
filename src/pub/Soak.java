package pub;

import java.util.*;
import pub.Model.*;

/** Developer stress test: random play for N days, exercising UI builders and player actions. */
final class Soak {
    static void run(int days) {
        GameWindow.soak = true;
        GameWindow w = new GameWindow();
        w.newGame("Soak Arms"); w.closeOverlay();
        String[] acts = {"talk", "serve", "buy", "leave", "apologise", "gossip", "join", "calm", "darts", "pool"};
        int errors = 0; long t0 = System.currentTimeMillis();
        for (int i = 0; i < days * 1440; i++) {
            try {
                Sim.advance(1.0, 1.0);
                if (Sim.S.min % 7 == 0) { List<Npc> in = Sim.inPub(); if (!in.isEmpty()) { Npc n = in.get(Util.ri(0, in.size() - 1)); Player.x = n.x; Player.y = n.y + 20; Player.act(n.id, acts[Util.ri(0, acts.length - 1)]); w.closeOverlay(); } }
                if (Sim.S.min % 90 == 0) {
                    Screens.manage(Util.ri(0, 5)); Screens.chronicle(); Screens.town(); Screens.book(); Screens.jukebox(); Screens.achievements(); Screens.football(); Screens.menu(); Screens.pickOpponent("darts");
                    if (Sim.S.min == 540) { for (int k = 0; k < 9; k++) Mgmt.order(k, 25); }
                    if (Util.chance(.05)) Mgmt.buyUp(Mgmt.UPS.get(Util.ri(0, Mgmt.UPS.size() - 1)));
                    if (Util.chance(.05)) Mgmt.buyAd(Mgmt.ADS.get(Util.ri(0, Mgmt.ADS.size() - 1)));
                    if (Util.chance(.03) && !Sim.S.applicants.isEmpty()) Mgmt.hireApplicant(Sim.S.applicants.get(0));
                    w.side.card.show(Sim.S.npcs.get(Util.ri(0, Sim.S.npcs.size() - 1))); w.side.refresh();
                }
                for (int f = 0; f < 3; f++) w.game.tick();
                Events.modalOpen = false;
            } catch (Throwable t) { if (errors++ < 10) { System.out.println("ERROR at day " + Sim.S.day + " " + Util.hhmm(Sim.S.min)); t.printStackTrace(System.out); } }
        }
        Save.disabled = false; Save.save(9); System.out.println("save ok: " + Save.load(9) + "  | ms=" + (System.currentTimeMillis() - t0) + " errors=" + errors + " day=" + Sim.S.day + " money=" + (int) Sim.S.money + " rep=" + (int) Sim.S.rep);
        System.out.println("stats=" + Sim.S.stats);
        try { java.nio.file.Files.deleteIfExists(Save.file(9)); } catch (Exception e) { }
        System.exit(errors > 0 ? 1 : 0);
    }
}
