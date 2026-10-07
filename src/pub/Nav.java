package pub;

import java.util.*;

/** Pub floor layout, named spots and grid pathfinding. */
public final class Nav {
    private Nav() {}
    public static final int W = 960, H = 620, CS = 10, GW = W / CS, GH = H / CS;

    public static final class Spot {
        public final String id, kind; public final double x, y; public final String table;
        Spot(String id, String kind, double x, double y, String table) { this.id = id; this.kind = kind; this.x = x; this.y = y; this.table = table; }
    }
    public static final Map<String, Spot> SPOTS = new LinkedHashMap<>();
    public static final List<Spot> SEATS = new ArrayList<>();   // tables + armchairs + stools
    public static final List<Spot> STAND = new ArrayList<>();
    public static final List<Spot> TV = new ArrayList<>();
    public static final List<Spot> BARWAIT = new ArrayList<>();
    public static final double[][] TABLES = {{300, 310}, {450, 310}, {600, 310}, {300, 460}, {450, 460}, {600, 460}};
    public static final double[] DOOR = {480, 596};
    public static final double[] LOO = {460, 134};
    public static final double[] JUKE = {878, 326};
    public static final double[] FRUIT = {878, 232};
    public static final double[] OCHE = {138, 420};
    public static final double[] DARTBOARD = {34, 420};
    public static final double[] HATCH = {860, 140};
    public static final double[] STAGE = {560, 420};
    public static final double[][] BARTENDER = {{110, 150}, {200, 150}, {290, 150}};
    public static final double[][] POOL = {{760, 372}, {840, 372}, {760, 540}, {840, 540}, {682, 455}, {918, 455}};
    public static final int[] POOLRECT = {700, 400, 200, 110};
    public static final double[] NOTICE = {760, 140};
    public static final double[] TVPOS = {610, 160};

    private static final boolean[] blocked = new boolean[GW * GH];

    private static void block(double x, double y, double w, double h) {
        for (int cy = (int) (y / CS); cy <= (int) ((y + h) / CS); cy++)
            for (int cx = (int) (x / CS); cx <= (int) ((x + w) / CS); cx++)
                if (cx >= 0 && cy >= 0 && cx < GW && cy < GH) blocked[cy * GW + cx] = true;
    }
    private static void add(Spot s, List<Spot> l) { SPOTS.put(s.id, s); if (l != null) l.add(s); }

    static {
        for (int t = 0; t < TABLES.length; t++) {
            double x = TABLES[t][0], y = TABLES[t][1]; String tid = "T" + t;
            add(new Spot(tid + ".0", "seat", x - 40, y, tid), SEATS);
            add(new Spot(tid + ".1", "seat", x + 40, y, tid), SEATS);
            add(new Spot(tid + ".2", "seat", x, y - 38, tid), SEATS);
            add(new Spot(tid + ".3", "seat", x, y + 38, tid), SEATS);
            block(x - 27, y - 23, 54, 46);
        }
        add(new Spot("A0", "arm", 95, 520, "A"), SEATS);
        add(new Spot("A1", "arm", 95, 570, "A"), SEATS);
        for (int i = 0; i < 6; i++) add(new Spot("S" + i, "stool", 70 + i * 45, 222, "BAR"), SEATS);
        for (int i = 0; i < 7; i++) add(new Spot("W" + i, "barwait", 52 + i * 42, 248, "BAR"), BARWAIT);
        double[][] st = {{400, 235}, {440, 250}, {500, 238}, {560, 255}, {640, 245}, {700, 250}, {750, 290}, {790, 250}, {830, 300}, {200, 300}, {160, 340}, {240, 360}, {350, 390}, {400, 400},
            {520, 390}, {560, 400}, {650, 400}, {280, 400}, {200, 480}, {250, 540}, {350, 540}, {520, 540}, {660, 560}, {420, 395}};
        for (int i = 0; i < st.length; i++) add(new Spot("P" + i, "stand", st[i][0], st[i][1], "P"), STAND);
        double[][] tv = {{540, 200}, {600, 212}, {660, 200}, {480, 225}, {720, 222}, {570, 245}, {640, 245}, {520, 270}, {690, 262}};
        for (int i = 0; i < tv.length; i++) add(new Spot("V" + i, "tv", tv[i][0], tv[i][1], "V"), TV);

        // walls & furniture
        block(0, 0, W, 124);          // back wall
        block(0, 0, 22, H);           // left wall
        block(942, 0, 20, H);         // right wall
        block(0, 598, 438, 30);       // bottom wall left
        block(522, 598, 440, 30);     // bottom wall right
        block(0, 125, 36, 70);        // behind-bar left edge
        block(36, 168, 298, 40);      // counter
        block(POOLRECT[0] - 6, POOLRECT[1] - 6, POOLRECT[2] + 12, POOLRECT[3] + 12);
        block(902, 286, 50, 76);      // jukebox
        block(902, 196, 50, 68);      // fruit machine
        block(22, 395, 10, 50);       // dartboard
    }

    public static boolean free(double x, double y) {
        int cx = (int) (x / CS), cy = (int) (y / CS);
        return cx >= 0 && cy >= 0 && cx < GW && cy < GH && !blocked[cy * GW + cx];
    }

    private static int[] nearestFree(double x, double y) {
        int cx = Util.clampi((int) (x / CS), 0, GW - 1), cy = Util.clampi((int) (y / CS), 0, GH - 1);
        if (!blocked[cy * GW + cx]) return new int[]{cx, cy};
        for (int r = 1; r < 14; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx >= 0 && ny >= 0 && nx < GW && ny < GH && !blocked[ny * GW + nx]) return new int[]{nx, ny};
                }
        return new int[]{cx, cy};
    }

    private static boolean clear(double x0, double y0, double x1, double y1) {
        double d = Util.dist(x0, y0, x1, y1);
        int n = (int) (d / 4) + 1;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            if (!free(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t)) return false;
        }
        return true;
    }

    /** A* path as a list of waypoints (pixel coordinates). Never null. */
    public static List<double[]> path(double x0, double y0, double x1, double y1) {
        List<double[]> out = new ArrayList<>();
        if (clear(x0, y0, x1, y1)) { out.add(new double[]{x1, y1}); return out; }
        int[] s = nearestFree(x0, y0), g = nearestFree(x1, y1);
        int N = GW * GH, start = s[1] * GW + s[0], goal = g[1] * GW + g[0];
        double[] gs = new double[N]; Arrays.fill(gs, 1e18);
        int[] from = new int[N]; Arrays.fill(from, -1);
        boolean[] closed = new boolean[N];
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        gs[start] = 0; pq.add(new double[]{0, start});
        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1}, dy = {0, 0, 1, -1, 1, -1, 1, -1};
        boolean found = false;
        while (!pq.isEmpty()) {
            int cur = (int) pq.poll()[1];
            if (closed[cur]) continue; closed[cur] = true;
            if (cur == goal) { found = true; break; }
            int cx = cur % GW, cy = cur / GW;
            for (int k = 0; k < 8; k++) {
                int nx = cx + dx[k], ny = cy + dy[k];
                if (nx < 0 || ny < 0 || nx >= GW || ny >= GH) continue;
                int ni = ny * GW + nx;
                if (blocked[ni] || closed[ni]) continue;
                if (k >= 4 && (blocked[cy * GW + nx] || blocked[ny * GW + cx])) continue;
                double ng = gs[cur] + (k < 4 ? 1 : 1.414);
                if (ng < gs[ni]) {
                    gs[ni] = ng; from[ni] = cur;
                    double h = Math.hypot(nx - g[0], ny - g[1]);
                    pq.add(new double[]{ng + h, ni});
                }
            }
        }
        if (!found) { out.add(new double[]{x1, y1}); return out; }
        List<double[]> raw = new ArrayList<>();
        for (int c = goal; c != -1; c = from[c]) raw.add(new double[]{(c % GW) * CS + CS / 2.0, (c / GW) * CS + CS / 2.0});
        Collections.reverse(raw);
        // string-pull
        double cx = x0, cy = y0; int i = 0;
        while (i < raw.size()) {
            int best = i;
            for (int j = raw.size() - 1; j >= i; j--) if (clear(cx, cy, raw.get(j)[0], raw.get(j)[1])) { best = j; break; }
            out.add(raw.get(best)); cx = raw.get(best)[0]; cy = raw.get(best)[1]; i = best + 1;
        }
        out.add(new double[]{x1, y1});
        return out;
    }
}
