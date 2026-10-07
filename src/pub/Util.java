package pub;

import java.util.*;

/** Small helpers shared by everything. */
public final class Util {
    public static final Random R = new Random();
    private Util() {}

    public static double r(double a, double b) { return a + R.nextDouble() * (b - a); }
    public static int ri(int a, int b) { return a + R.nextInt(b - a + 1); }
    public static boolean chance(double p) { return R.nextDouble() < p; }
    public static double clamp(double v, double a, double b) { return v < a ? a : Math.min(v, b); }
    public static double clamp(double v) { return clamp(v, 0, 100); }
    public static int clampi(int v, int a, int b) { return v < a ? a : Math.min(v, b); }

    @SafeVarargs public static <T> T pick(T... a) { return a[R.nextInt(a.length)]; }
    public static <T> T pick(List<T> a) { return a.get(R.nextInt(a.size())); }

    public static String money(double n) {
        String s = String.format(Locale.UK, "%,.2f", Math.abs(n));
        return (n < 0 ? "-" : "") + "£" + s;
    }
    public static String money0(double n) {
        return (n < 0 ? "-" : "") + "£" + String.format(Locale.UK, "%,d", Math.round(Math.abs(n)));
    }
    public static String hhmm(int m) {
        m = ((m % 1440) + 1440) % 1440;
        return String.format("%02d:%02d", m / 60, m % 60);
    }
    /** Replace {key} placeholders. */
    public static String fill(String s, String... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        return s;
    }
    public static <T> T wpick(List<T> items, java.util.function.ToDoubleFunction<T> w) {
        double tot = 0; double[] ws = new double[items.size()];
        for (int i = 0; i < ws.length; i++) { ws[i] = Math.max(0, w.applyAsDouble(items.get(i))); tot += ws[i]; }
        if (tot <= 0) return null;
        double x = R.nextDouble() * tot;
        for (int i = 0; i < ws.length; i++) { x -= ws[i]; if (x <= 0) return items.get(i); }
        return items.get(items.size() - 1);
    }
    public static double dist(double x1, double y1, double x2, double y2) { return Math.hypot(x1 - x2, y1 - y2); }
}
