package pub;

import java.awt.image.BufferedImage;
import javax.swing.*;

/** Entry point. Dev flags: --shot file.png [--day N] [--hour H] [--screen name] render a frame and exit. */
public final class Main {
    public static void main(String[] args) throws Exception {
        System.setProperty("sun.java2d.opengl", "false");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception e) { }
        UIManager.put("ToolTip.background", new java.awt.Color(0x2a1a12)); UIManager.put("ToolTip.foreground", UI.CREAM);
        String shot = null, screen = ""; int day = 5, hour = 20, soak = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--shot")) shot = args[++i]; else if (args[i].equals("--day")) day = Integer.parseInt(args[++i]); else if (args[i].equals("--hour")) hour = Integer.parseInt(args[++i]); else if (args[i].equals("--screen")) screen = args[++i]; else if (args[i].equals("--soak")) soak = Integer.parseInt(args[++i]);
        }
        if (shot != null || soak > 0) Save.disabled = true;
        final String shotF = shot, screenF = screen; final int soakF = soak; final int dayF = day, hourF = hour;
        SwingUtilities.invokeAndWait(() -> {
            if (soakF > 0) { Soak.run(soakF); return; }
            if (shotF == null) Audio.start();
            GameWindow w = new GameWindow();
            w.setVisible(true);
            if (shotF == null) { w.showTitle(); return; }
            w.newGame("The Speckled Pigeon"); w.closeOverlay();
            long target = (dayF - 1) * 1440L + hourF * 60L, now = 0;
            while (Sim.S.day < dayF || (Sim.S.day == dayF && Sim.S.min < hourF * 60)) { Sim.advance(1.0, 1.0); if (++now > 2_000_000) break; if (w.overlayOpen) { w.closeOverlay(); } Events.modalOpen = false; w.queue.clear(); }
            w.closeOverlay(); w.queue.clear(); Events.modalOpen = false;
            if (!Sim.S.npcs.isEmpty()) { Model.Npc n = null; for (Model.Npc x : Sim.S.npcs) if (x.inPub) { n = x; break; } if (n != null) { Gfx.selId = n.id; w.side.card.show(n); } }
            for (int i = 0; i < 20; i++) w.game.tick();
            switch (screenF) {
                case "manage" -> GameWindow.open("Manage the pub", Screens.manage(0));
                case "stock" -> GameWindow.open("Manage the pub", Screens.manage(1));
                case "staff" -> GameWindow.open("Manage the pub", Screens.manage(2));
                case "news" -> GameWindow.open("Chronicle", Screens.chronicle());
                case "town" -> GameWindow.open("Westbridge", Screens.town());
                case "book" -> GameWindow.open("Regulars Book", Screens.book());
                case "darts" -> w.startGame("darts", null);
                case "pool" -> w.startGame("pool", Sim.npc("dave"));
                case "quiz" -> w.startGame("quiz", null);
                case "karaoke" -> w.startGame("karaoke", null);
                case "juke" -> GameWindow.open("Jukebox", Screens.jukebox());
                case "football" -> GameWindow.open("Football", Screens.football());
                case "title" -> w.showTitle();
                case "fight" -> Events.nearFight(Sim.npc("dave"), Sim.npc("gaz"), "football");
                case "notice" -> GameWindow.open("Notice board", Screens.notice());
                case "talk" -> { Model.Npc t = Sim.npc("linda"); t.inPub = true; Player.x = t.x; Player.y = t.y; Player.act("linda", "talk"); }
                case "day" -> GameWindow.open("Closing time", Screens.daySummary(Sim.S.history.isEmpty() ? new Model.DayRec() : Sim.S.history.get(Sim.S.history.size() - 1)));
                default -> {}
            }
            w.getContentPane().doLayout(); w.validate();
            try { Thread.sleep(300); } catch (Exception e) { }
            BufferedImage img = new BufferedImage(w.getWidth(), w.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics(); w.getRootPane().paint(g); g.dispose();
            try { javax.imageio.ImageIO.write(img, "png", new java.io.File(shotF)); } catch (Exception e) { e.printStackTrace(); }
            System.exit(0);
        });
    }
}
