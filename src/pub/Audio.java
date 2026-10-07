package pub;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Fully synthesised sound: SFX, crowd ambience, jukebox music and optional spoken commentary. */
public final class Audio {
    private Audio() {}
    static final int SR = 22050;
    public static volatile boolean sfxOn = true, ambOn = true, musicOn = true, speechOn = true;
    public static volatile double master = .8;
    public static volatile double crowd = 0;      // 0..1
    public static volatile int songIdx = -1;      // -1 none
    public static volatile boolean quietMode;     // minigames etc

    abstract static class V { boolean done; abstract double next(); }
    static final ConcurrentLinkedQueue<V> incoming = new ConcurrentLinkedQueue<>();
    static final List<V> voices = new ArrayList<>();
    static SourceDataLine line;
    static Thread thread;
    static final Random rnd = new Random();

    static V tone(double f0, double f1, double dur, int wave, double vol, double attack) {
        return new V() {
            double ph, t; final int n = (int) (dur * SR);
            double next() {
                if (t >= n) { done = true; return 0; }
                double p = t / n, f = f0 + (f1 - f0) * p;
                ph += f / SR; ph -= Math.floor(ph);
                double s = switch (wave) { case 0 -> Math.sin(ph * 6.2832); case 1 -> ph < .5 ? 1 : -1; case 2 -> ph * 2 - 1; default -> Math.abs(ph * 4 - 2) - 1; };
                double env = Math.min(1, t / (attack * SR + 1)) * Math.pow(1 - p, 1.6);
                t++; return s * env * vol;
            }
        };
    }
    static V noise(double dur, double lp0, double lp1, double vol, double attack) {
        return new V() {
            double t, y; final int n = (int) (dur * SR);
            double next() {
                if (t >= n) { done = true; return 0; }
                double p = t / n, lp = lp0 + (lp1 - lp0) * p, a = Math.min(1, 2 * Math.PI * lp / SR);
                y += a * ((rnd.nextDouble() * 2 - 1) - y);
                double env = Math.min(1, t / (attack * SR + 1)) * Math.pow(1 - p, 1.3);
                t++; return y * env * vol;
            }
        };
    }
    static void add(V v) { incoming.add(v); }

    // ---------- SFX ----------
    public static void sfx(String name) {
        if (line == null) return;
        if (name.startsWith("song:")) { try { songIdx = Integer.parseInt(name.substring(5)); startSong(songIdx); } catch (Exception e) { } return; }
        if (!sfxOn) return;
        switch (name) {
            case "door" -> { add(tone(880, 880, .12, 0, .25, .002)); add(tone(1318, 1318, .3, 0, .22, .01)); }
            case "doorclose" -> add(noise(.12, 600, 200, .3, .001));
            case "bell" -> { for (int i = 0; i < 4; i++) add(delayed(i * .09, tone(1568, 1568, .6, 0, .25, .001))); add(tone(2093, 2093, 1.0, 0, .18, .001)); }
            case "till" -> { add(tone(1400, 1400, .05, 1, .15, .001)); add(delayed(.07, tone(2000, 2000, .25, 0, .2, .001))); }
            case "coin" -> { add(tone(1800, 1800, .06, 1, .15, .001)); add(delayed(.06, tone(2400, 2400, .3, 0, .18, .001))); }
            case "pour" -> add(noise(.5, 2500, 1200, .22, .05));
            case "ach" -> { double[] f = {523, 659, 784, 1046}; for (int i = 0; i < 4; i++) add(delayed(i * .1, tone(f[i], f[i], .3, 1, .18, .005))); }
            case "goal" -> { add(noise(2.4, 1800, 700, .6, .4)); add(delayed(.15, noise(2.0, 1200, 500, .4, .3))); }
            case "whistle" -> { add(tone(3000, 3050, .5, 0, .25, .01)); add(delayed(.55, tone(3000, 3000, .4, 0, .25, .01))); }
            case "groan" -> add(tone(220, 110, .9, 2, .12, .05));
            case "gasp" -> add(noise(.5, 3000, 1500, .3, .1));
            case "cheer" -> add(noise(1.6, 2000, 900, .45, .2));
            case "chant" -> { add(tone(196, 196, .4, 1, .1, .02)); add(delayed(.45, tone(196, 196, .4, 1, .1, .02))); add(delayed(.9, tone(247, 247, .6, 1, .1, .02))); }
            case "shout" -> { add(tone(300, 450, .35, 2, .14, .01)); add(noise(.35, 2500, 1500, .25, .01)); }
            case "mic" -> { add(tone(1000, 1000, .15, 0, .2, .01)); add(noise(.2, 5000, 5000, .2, .01)); }
            case "guitar" -> { double[] f = {164, 196, 247, 329}; for (int i = 0; i < 4; i++) add(delayed(i * .12, tone(f[i], f[i], 1.2, 2, .12, .002))); }
            case "static" -> add(noise(1.2, 8000, 8000, .35, .01));
            case "powerdown" -> add(tone(600, 40, 1.2, 2, .25, .01));
            case "knock" -> { for (int i = 0; i < 3; i++) add(delayed(i * .2, noise(.08, 400, 200, .6, .001))); }
            case "flap" -> { for (int i = 0; i < 6; i++) add(delayed(i * .05, noise(.05, 3000, 1000, .25, .001))); }
            case "coo" -> { add(tone(420, 320, .3, 0, .15, .02)); add(delayed(.3, tone(400, 300, .35, 0, .15, .02))); }
            case "plate" -> add(tone(2800, 2400, .1, 0, .1, .001));
            case "dart" -> add(noise(.06, 900, 300, .5, .001));
            case "pool" -> add(tone(1900, 900, .08, 0, .35, .001));
            case "pot" -> { add(noise(.2, 500, 200, .4, .001)); add(delayed(.05, tone(180, 120, .2, 0, .3, .001))); }
            case "wrong" -> add(tone(180, 140, .5, 1, .2, .005));
            case "right" -> { add(tone(660, 660, .1, 1, .14, .002)); add(delayed(.1, tone(880, 880, .25, 1, .14, .002))); }
            case "murmur" -> add(noise(1.0, 700, 500, .2, .3));
            default -> {}
        }
    }
    static V delayed(double d, V v) {
        return new V() { int t, n = (int) (d * SR); double next() { if (t++ < n) return 0; double s = v.next(); if (v.done) done = true; return s; } };
    }

    // ---------- music ----------
    static final int[] MAJ = {0, 2, 4, 5, 7, 9, 11, 12}, MIN = {0, 2, 3, 5, 7, 8, 10, 12}, PENT = {0, 2, 4, 7, 9, 12, 14, 16};
    static volatile int[] melody; static volatile int rootMidi = 57, bpm = 120; static volatile String genre = "";
    static int step, stepSamples, stepCount; static int[] prog = {0, 5, 3, 4};
    static long songStart;
    static void startSong(int idx) {
        String[] s = Data.SONGS[idx];
        genre = s[2]; bpm = Integer.parseInt(s[3]); rootMidi = Integer.parseInt(s[4]);
        Random r = new Random(s[0].hashCode());
        int[] sc = genre.equals("blues") || genre.equals("ballad") || genre.equals("metal") || genre.equals("indie") ? MIN : (genre.equals("folk") || genre.equals("singalong") ? PENT : MAJ);
        int[] m = new int[64]; int cur = 4;
        for (int i = 0; i < 64; i++) { cur = Util.clampi(cur + r.nextInt(5) - 2, 0, 7); m[i] = r.nextInt(100) < 28 ? -1 : sc[cur]; }
        melody = m; prog = switch (genre) { case "blues" -> new int[]{0, 0, 3, 4}; case "ballad" -> new int[]{0, 5, 3, 4}; default -> new int[]{0, 4, 5, 3}; };
        step = 0; stepCount = 0; songStart = System.nanoTime();
    }
    public static void stopSong() { songIdx = -1; }
    static double mtof(int m) { return 440 * Math.pow(2, (m - 69) / 12.0); }
    static void musicStep() {
        int i = step++ % 64;
        int chord = prog[(i / 16) % 4];
        int base = rootMidi + MAJ[chord % 8];
        int w = switch (genre) { case "blues", "ballad", "swing" -> 0; case "folk" -> 3; case "chant" -> 1; case "metal" -> 1; default -> 2; };
        double lv = .09;
        int mn = melody[i];
        if (mn >= 0 && (i % 2 == 0 || !genre.equals("ballad"))) add(tone(mtof(rootMidi + 12 + mn), mtof(rootMidi + 12 + mn), 60.0 / bpm * (genre.equals("ballad") ? 1.6 : .45), w, lv, .004));
        if (i % 4 == 0) add(tone(mtof(base - 12), mtof(base - 12), 60.0 / bpm * .9, genre.equals("disco") ? 1 : 0, .14, .004));
        if (i % 8 == 0 || genre.equals("dance") && i % 4 == 0) add(tone(120, 45, .14, 0, .3, .001));
        if (i % 8 == 4 && !genre.equals("ballad")) add(noise(.1, 4000, 1500, .13, .001));
        if (!genre.equals("ballad") && !genre.equals("blues") && i % 2 == 1) add(noise(.03, 9000, 9000, .05, .001));
        if (genre.equals("chant") && i % 8 == 2) add(noise(.12, 1200, 600, .25, .001));
    }

    // ---------- main thread ----------
    public static void start() {
        try {
            AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(fmt); line.open(fmt, SR / 5 * 2); line.start();
        } catch (Throwable t) { line = null; System.err.println("Audio unavailable: " + t); return; }
        thread = new Thread(Audio::run, "audio"); thread.setDaemon(true); thread.start();
        startSpeech();
    }

    static void run() {
        byte[] buf = new byte[441 * 2];
        double ny = 0, amp = 0, lfo = 0; int nextLaugh = 0; double y2 = 0;
        int sampleCounter = 0;
        while (true) {
            for (int i = 0; i < 441; i++) {
                V v; while ((v = incoming.poll()) != null) voices.add(v);
                double s = 0;
                // ambient murmur
                if (ambOn && !quietMode) {
                    lfo += .00007; double wob = .5 + .5 * Math.sin(lfo * 3.1) * Math.sin(lfo * 1.7 + 1);
                    ny += .08 * ((rnd.nextDouble() * 2 - 1) - ny); y2 += .02 * (ny - y2);
                    s += (ny - y2) * (.09 + .22 * wob) * crowd * 1.2;
                    if (crowd > .15 && --nextLaugh <= 0) { nextLaugh = SR / 2 + rnd.nextInt(SR * 3); if (rnd.nextDouble() < crowd) add(tone(260 + rnd.nextInt(200), 200 + rnd.nextInt(150), .22, 2, .028 * crowd, .02)); }
                }
                // music sequencer
                if (musicOn && songIdx >= 0) {
                    if (--stepSamples <= 0) { stepSamples = (int) (SR * 60.0 / bpm / 2 * (genre.equals("swing") && step % 2 == 1 ? 1.25 : genre.equals("swing") ? .75 : 1)); musicStep(); }
                }
                for (int k = voices.size() - 1; k >= 0; k--) { V x = voices.get(k); s += x.next(); if (x.done) voices.remove(k); }
                s *= master; s = Math.max(-1, Math.min(1, s * .9));
                short sv = (short) (s * 30000);
                buf[2 * i] = (byte) sv; buf[2 * i + 1] = (byte) (sv >> 8);
            }
            line.write(buf, 0, buf.length);
        }
    }

    // ---------- speech (Windows only, optional) ----------
    static Process speech; static Writer speechIn; static long lastSpeak;
    static void startSpeech() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                "Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Rate=2; $s.Volume=70; try{$s.SelectVoice('Microsoft Hazel Desktop')}catch{}; while(($l=[Console]::In.ReadLine()) -ne $null){ $s.Speak($l) }");
            pb.redirectErrorStream(true); pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            speech = pb.start(); speechIn = new OutputStreamWriter(speech.getOutputStream());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { if (speech != null) speech.destroyForcibly(); }));
        } catch (Throwable t) { speech = null; }
    }
    public static void speak(String text) {
        if (!speechOn || speech == null || quietMode) return;
        long now = System.currentTimeMillis(); if (now - lastSpeak < 4500) return; lastSpeak = now;
        try { speechIn.write(text.replace("\n", " ").replaceAll("[^A-Za-z0-9 ,.!'\\-]", "") + "\n"); speechIn.flush(); } catch (Throwable t) { speech = null; }
    }
}
