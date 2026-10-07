package pub;

import java.io.*;
import java.nio.file.*;
import pub.Model.State;

/** Saving and loading via Java serialisation. */
public final class Save {
    private Save() {}
    public static final Path DIR = Paths.get(System.getProperty("user.home"), ".speckled-pigeon");
    public static Path file(int slot) { return DIR.resolve("save" + slot + ".dat"); }
    public static boolean exists(int slot) { return Files.exists(file(slot)); }

    public static boolean disabled;

    public static boolean save(int slot) {
        if (Sim.S == null || disabled) return false;
        try {
            Files.createDirectories(DIR);
            Path tmp = DIR.resolve("save" + slot + ".tmp");
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                o.writeInt(1); o.writeObject(Sim.S);
            }
            Files.move(tmp, file(slot), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean load(int slot) {
        if (!exists(slot)) return false;
        try (ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file(slot))))) {
            i.readInt(); State s = (State) i.readObject();
            Sim.load(s);
            Social.convs.clear(); Ai.games.clear(); Events.modalOpen = false; Events.cooldown.clear();
            for (Model.Npc n : s.npcs) { n.conv = ""; if (n.st.equals("darts") || n.st.equals("pool")) { n.st = "stand"; n.timer = 1; } }
            Player.sync();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static String describe(int slot) {
        if (!exists(slot)) return "empty";
        try (ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file(slot))))) {
            i.readInt(); State s = (State) i.readObject();
            return s.pubName + " — day " + s.day + ", " + Util.money0(s.money) + ", rep " + (int) s.rep;
        } catch (Exception e) { return "unreadable"; }
    }
}
