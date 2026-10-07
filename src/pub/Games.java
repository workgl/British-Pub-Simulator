package pub;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import pub.Model.*;
import static pub.UI.*;

/** Playable mini-games: darts, pool, pub quiz and karaoke. */
public final class Games {
    private Games() {}

    // =====================================================================
    //  DARTS (301)
    // =====================================================================
    static final int[] ORDER = {20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5};

    /** Returns {score, multiplier} for a hit at (dx,dy) relative to the board centre, board radius R (double outer). */
    static int[] dartScore(double dx, double dy, double R) {
        double d = Math.hypot(dx, dy) / R;
        if (d > 1.0) return new int[]{0, 0};
        if (d < .0374) return new int[]{50, 2};
        if (d < .0935) return new int[]{25, 1};
        double ang = Math.toDegrees(Math.atan2(dx, -dy)); if (ang < 0) ang += 360;
        int seg = ORDER[(int) (((ang + 9) % 360) / 18)];
        if (d > .953) return new int[]{seg * 2, 2};
        if (d > .582 && d < .629) return new int[]{seg * 3, 3};
        return new int[]{seg, 1};
    }

    public static JPanel darts(Npc opp, Runnable close) {
        return new DartsPanel(opp, close);
    }

    static final class DartsPanel extends JPanel {
        final Npc opp; final Runnable close; final double CX = 300, CY = 270, R = 215;
        int pScore = 301, oScore = 301, turnStart, dartsThrown, turnTotal; boolean playerTurn = true, over, busy;
        List<double[]> stuck = new ArrayList<>(); double mx = CX, my = CY, t; String msg = "Your throw — click to throw. Reticle sways!";
        List<String> hist = new ArrayList<>(); javax.swing.Timer timer; int stake;
        DartsPanel(Npc opp, Runnable close) {
            this.opp = opp; this.close = close; setPreferredSize(new Dimension(780, 580)); setOpaque(false);
            stake = opp == null ? 0 : Integer.parseInt(Sim.S.flags.getOrDefault("dartsStake", "10")); Sim.S.flags.remove("dartsStake");
            if (opp != null) msg = "Playing " + opp.name + " for " + Util.money0(stake) + ". Click to throw!";
            addMouseMotionListener(new MouseMotionAdapter() { public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); } public void mouseDragged(MouseEvent e) { mouseMoved(e); } });
            addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { if (playerTurn && !busy && !over) throwDart(); } });
            timer = new javax.swing.Timer(16, e -> { t += .016; repaint(); }); timer.start();
        }
        double sway() { return 10 + 5 * Math.min(dartsThrown, 2); }
        double[] reticle() { double a = sway(); return new double[]{mx + Math.sin(t * 2.3) * a + Math.sin(t * 5.1) * a * .4, my + Math.cos(t * 1.9) * a + Math.cos(t * 4.3) * a * .4}; }
        void throwDart() {
            double[] r = reticle(); land(r[0] + Util.R.nextGaussian() * 3, r[1] + Util.R.nextGaussian() * 3, true);
        }
        void land(double x, double y, boolean player) {
            Audio.sfx("dart"); stuck.add(new double[]{x, y, player ? 1 : 0});
            int[] s = dartScore(x - CX, y - CY, R);
            int cur = player ? pScore : oScore;
            turnTotal += s[0]; dartsThrown++;
            int after = cur - s[0];
            String lab = s[1] == 3 ? "Treble " + s[0] / 3 : s[1] == 2 ? (s[0] == 50 ? "Bullseye" : "Double " + s[0] / 2) : String.valueOf(s[0]);
            msg = (player ? "You" : Sim.first(opp)) + ": " + (s[0] == 0 ? "missed the board!" : lab + " (" + s[0] + ")");
            if (after < 0 || after == 1) { bust(player); return; }
            if (player) pScore = after; else oScore = after;
            if (after == 0) { finish(player); return; }
            if (dartsThrown >= 3) endTurn(player);
        }
        void bust(boolean player) {
            if (player) pScore = turnStart; else oScore = turnStart;
            msg = (player ? "You" : Sim.first(opp)) + " went BUST! Score back to " + turnStart; Audio.sfx("wrong");
            endTurn(player);
        }
        void endTurn(boolean player) {
            if (turnTotal == 180) { if (player) { Sim.stat("d180"); msg = "ONE HUNDRED AND EIGHTYYYY!"; Audio.sfx("cheer"); } }
            hist.add((player ? "You" : Sim.first(opp)) + " scored " + turnTotal);
            dartsThrown = 0; turnTotal = 0; busy = true;
            javax.swing.Timer w = new javax.swing.Timer(1100, e -> { stuck.clear(); busy = false; playerTurn = !player; turnStart = playerTurn ? pScore : oScore; if (!playerTurn) npcTurn(); else if (opp == null) {} });
            w.setRepeats(false); w.start();
        }
        void npcTurn() {
            if (opp == null) { playerTurn = true; return; }
            busy = true; msg = Sim.first(opp) + " steps up to the oche...";
            final int[] n = {0};
            javax.swing.Timer w = new javax.swing.Timer(750, null);
            w.addActionListener(e -> {
                if (over) { w.stop(); return; }
                double sigma = Math.max(4, (11 - opp.darts) * 7.5 + opp.drunk * .25);
                int need = oScore;
                double tx = CX, ty = CY - R * .6;      // treble 20
                if (need <= 40 && need % 2 == 0 && need >= 2) { int seg = need / 2; tx = CX + Math.sin(Math.toRadians(indexOf(seg) * 18)) * R * .975; ty = CY - Math.cos(Math.toRadians(indexOf(seg) * 18)) * R * .975; }
                else if (need < 60 && need > 1) { int seg = Math.min(20, need); if (need > 20) seg = need - 40 > 0 ? 20 : 20; tx = CX + Math.sin(Math.toRadians(indexOf(seg) * 18)) * R * .8; ty = CY - Math.cos(Math.toRadians(indexOf(seg) * 18)) * R * .8; }
                busy = false; land(tx + Util.R.nextGaussian() * sigma, ty + Util.R.nextGaussian() * sigma, false); busy = true;
                n[0]++;
                if (over || n[0] >= 3 || dartsThrown == 0) { w.stop(); busy = false; }
            });
            w.setInitialDelay(900); w.start();
        }
        int indexOf(int seg) { for (int i = 0; i < 20; i++) if (ORDER[i] == seg) return i; return 0; }
        void finish(boolean player) {
            over = true; Audio.sfx(player ? "cheer" : "groan");
            msg = player ? "GAME SHOT! You win!" : Sim.first(opp) + " wins the leg.";
            if (player) Sim.stat("dartsWins");
            if (opp != null) Games.afterGame(opp, player, "darts", stake);
            JButton b = btn("Back to the pub", () -> { timer.stop(); close.run(); });
            setLayout(null); b.setBounds(270, 520, 220, 38); add(b); revalidate();
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g);
            g.setPaint(new GradientPaint(0, 0, WOOD3, 0, getHeight(), WOOD)); g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g.setColor(new Color(0x1d120c)); g.fillOval((int) (CX - R - 14), (int) (CY - R - 14), (int) (2 * R + 28), (int) (2 * R + 28));
            Gfx.dartboard(g, CX, CY, R);
            g.setFont(sans(14, true)); g.setColor(CREAM);
            for (int i = 0; i < 20; i++) { double a = Math.toRadians(i * 18); String s = "" + ORDER[i]; int w = g.getFontMetrics().stringWidth(s); g.drawString(s, (float) (CX + Math.sin(a) * (R + 2 - 0) - w / 2.0 + Math.sin(a) * 2), (float) (CY - Math.cos(a) * (R + 2) + 5 - Math.cos(a) * 2)); }
            for (double[] d : stuck) { g.setColor(d[2] == 1 ? new Color(0x2fa0ff) : new Color(0xff5a5a)); g.fill(new Ellipse2D.Double(d[0] - 3, d[1] - 3, 7, 7)); g.setColor(Color.WHITE); g.draw(new Ellipse2D.Double(d[0] - 3, d[1] - 3, 7, 7)); g.drawLine((int) d[0], (int) d[1], (int) d[0] + 10, (int) d[1] - 16); }
            if (playerTurn && !busy && !over) { double[] r = reticle(); g.setColor(new Color(255, 255, 255, 220)); g.setStroke(new BasicStroke(1.6f)); g.draw(new Ellipse2D.Double(r[0] - 12, r[1] - 12, 24, 24)); g.drawLine((int) r[0] - 18, (int) r[1], (int) r[0] + 18, (int) r[1]); g.drawLine((int) r[0], (int) r[1] - 18, (int) r[0], (int) r[1] + 18); }
            // scoreboard
            rr(g, 570, 24, 190, 190);
            g.setFont(serif(15, true)); g.setColor(BRASS2); g.drawString("301", 584, 48);
            g.setFont(sans(12, true)); g.setColor(CREAM); g.drawString("You", 584, 74); g.drawString(opp == null ? "Practice" : Sim.first(opp), 584, 128);
            g.setFont(serif(34, true)); g.setColor(playerTurn ? BRASS2 : CREAM); g.drawString("" + pScore, 584, 112); g.setColor(!playerTurn ? BRASS2 : CREAM); g.drawString("" + oScore, 584, 166);
            g.setFont(sans(11, false)); g.setColor(new Color(0xcfc3a0)); g.drawString("Darts this turn: " + dartsThrown + "/3", 584, 198);
            int y = 240; g.setFont(sans(11, false)); for (int i = Math.max(0, hist.size() - 8); i < hist.size(); i++) { g.drawString(hist.get(i), 576, y); y += 16; }
            g.setFont(sans(14, true)); g.setColor(Color.WHITE); g.drawString(msg, 24, 28);
            g.dispose();
        }
        void rr(Graphics2D g, int x, int y, int w, int h) { g.setColor(new Color(0, 0, 0, 120)); g.fillRoundRect(x, y, w, h, 14, 14); g.setColor(BRASS); g.drawRoundRect(x, y, w, h, 14, 14); }
    }

    static void afterGame(Npc opp, boolean playerWon, String kind, int stake) {
        Sim.S.px = Player.x;
        if (playerWon) { Sim.addMoney(0); opp.money = Math.max(0, opp.money - stake); Sim.S.money += stake; opp.mood -= 4; opp.anger += 4; opp.trust += 1; Sim.say(opp, Util.pick("Fair play. Good game.", "Lucky!", "Rematch next week.", "Right, who's next?"));
            Sim.remember(opp, "game", -1, "player", "player", opp.name + " lost at " + kind + " to the landlord"); Sim.log("You beat " + opp.name + " at " + kind + " (+" + Util.money0(stake) + ").", "player"); }
        else { Sim.S.money -= stake; opp.money += stake; opp.mood += 8; opp.trust += 2; Sim.say(opp, Util.pick("Easy!", "Better luck next time, landlord.", "Pay up!"));
            Sim.remember(opp, "game", 1, "player", "player", opp.name + " beat the landlord at " + kind); Sim.log(opp.name + " beat you at " + kind + " (-" + Util.money0(stake) + ").", "player");
            if (opp.id.equals("sarah") && kind.equals("darts") && !opp.secretKnown) { Sim.say(opp, "Oh. Was that... was that good?"); Social.revealSecret(opp, "drunk", null); }
            if (opp.id.equals("alan") && kind.equals("pool")) Sim.say(opp, "Praise the Lord! And pass the chalk.");
        }
    }

    // =====================================================================
    //  POOL
    // =====================================================================
    public static JPanel pool(Npc opp, Runnable close) { return new PoolPanel(opp, close); }

    static final class PoolPanel extends JPanel {
        static final double L = 60, T = 70, TW = 780, TH = 390, BR = 11;
        static final class Ball { double x, y, vx, vy; int id; boolean potted; Color col; }
        final List<Ball> balls = new ArrayList<>(); Ball cue;
        final double[][] pockets = {{L, T}, {L + TW / 2, T - 6}, {L + TW, T}, {L, T + TH}, {L + TW / 2, T + TH + 6}, {L + TW, T + TH}};
        final Npc opp; final Runnable close; boolean playerTurn = true, moving, over, placing, charging; double power, powDir = 1, mx, my, t;
        String msg = "Aim with the mouse; hold click to charge, release to shoot."; int firstHit = -1; boolean pottedOwn, pottedCue, pottedOther, pottedBlack; int[] group = {0};
        javax.swing.Timer timer; int stake = 5;
        PoolPanel(Npc opp, Runnable close) {
            this.opp = opp; this.close = close; setPreferredSize(new Dimension(920, 560)); setOpaque(false);
            rack();
            if (opp != null) msg = "You (reds) vs " + Sim.first(opp) + " (yellows) — " + Util.money0(stake) + " a frame. Your break!";
            addMouseMotionListener(new MouseMotionAdapter() { public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); } public void mouseDragged(MouseEvent e) { mouseMoved(e); } });
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { if (over || moving || !playerTurn) return; if (placing) { if (free(mx, my)) { cue.x = mx; cue.y = my; cue.potted = false; placing = false; msg = "Cue ball placed."; } return; } charging = true; power = 0; powDir = 1; }
                public void mouseReleased(MouseEvent e) { if (charging && playerTurn && !moving) { charging = false; shoot(Math.atan2(my - cue.y, mx - cue.x), Math.max(.08, power)); } }
            });
            timer = new javax.swing.Timer(16, e -> tick()); timer.start();
        }
        boolean free(double x, double y) { if (x < L + BR || x > L + TW - BR || y < T + BR || y > T + TH - BR) return false; for (Ball b : balls) if (!b.potted && b != cue && Math.hypot(b.x - x, b.y - y) < BR * 2.1) return false; return true; }
        void rack() {
            cue = new Ball(); cue.x = L + TW * .25; cue.y = T + TH / 2; cue.id = 0; cue.col = Color.WHITE; balls.add(cue);
            double rx = L + TW * .72, ry = T + TH / 2; int[] ids = {1, 9, 2, 8, 10, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15}; int k = 0;
            // order: alternate groups, black in the middle row 3
            int[] layout = {1, 9, 9, 1, 8, 1, 1, 9, 9, 1, 9, 9, 1, 9, 1};
            int[] ridOrder = new int[15]; int ri = 1, yi = 9; for (int i = 0; i < 15; i++) { if (layout[i] == 8) ridOrder[i] = 8; else if (layout[i] == 1) ridOrder[i] = ri++; else ridOrder[i] = yi++; }
            for (int c = 0; c < 5; c++) for (int r = 0; r <= c; r++) { Ball b = new Ball(); b.x = rx + c * BR * 1.75; b.y = ry + (r - c / 2.0) * BR * 2.05; b.id = ridOrder[k++]; b.col = b.id == 8 ? Color.BLACK : b.id < 8 ? new Color(0xd32f2f) : new Color(0xf6c524); balls.add(b); }
        }
        boolean mine(Ball b, boolean p) { return p ? (b.id >= 1 && b.id <= 7) : (b.id >= 9); }
        void shoot(double ang, double pw) {
            cue.vx = Math.cos(ang) * pw * 15; cue.vy = Math.sin(ang) * pw * 15; moving = true; firstHit = -1; pottedOwn = pottedCue = pottedOther = pottedBlack = false; Audio.sfx("pool");
        }
        void tick() {
            t += .016;
            if (charging) { power += powDir * .022; if (power >= 1) { power = 1; powDir = -1; } if (power <= 0) { power = 0; powDir = 1; } }
            if (moving) {
                for (int s = 0; s < 6; s++) step(1.0 / 6);
                boolean any = false; for (Ball b : balls) if (!b.potted && (Math.abs(b.vx) > .02 || Math.abs(b.vy) > .02)) any = true;
                if (!any) { moving = false; settle(); }
            }
            repaint();
        }
        void step(double dt) {
            for (Ball b : balls) if (!b.potted) {
                b.x += b.vx * dt; b.y += b.vy * dt; b.vx *= .9935; b.vy *= .9935;
                if (Math.hypot(b.vx, b.vy) < .03) { b.vx = 0; b.vy = 0; }
                if (b.x < L + BR) { b.x = L + BR; b.vx = Math.abs(b.vx) * .8; } if (b.x > L + TW - BR) { b.x = L + TW - BR; b.vx = -Math.abs(b.vx) * .8; }
                if (b.y < T + BR) { b.y = T + BR; b.vy = Math.abs(b.vy) * .8; } if (b.y > T + TH - BR) { b.y = T + TH - BR; b.vy = -Math.abs(b.vy) * .8; }
                for (double[] p : pockets) if (Math.hypot(b.x - p[0], b.y - p[1]) < 20) { pot(b); break; }
            }
            for (int i = 0; i < balls.size(); i++) for (int j = i + 1; j < balls.size(); j++) {
                Ball a = balls.get(i), b = balls.get(j); if (a.potted || b.potted) continue;
                double dx = b.x - a.x, dy = b.y - a.y, d = Math.hypot(dx, dy);
                if (d < BR * 2 && d > 0) {
                    double nx = dx / d, ny = dy / d, ov = BR * 2 - d; a.x -= nx * ov / 2; a.y -= ny * ov / 2; b.x += nx * ov / 2; b.y += ny * ov / 2;
                    double dvx = a.vx - b.vx, dvy = a.vy - b.vy, dp = dvx * nx + dvy * ny;
                    if (dp > 0) { a.vx -= dp * nx; a.vy -= dp * ny; b.vx += dp * nx; b.vy += dp * ny; if (dp > .8) Audio.sfx("pool"); }
                    if ((a == cue || b == cue) && firstHit < 0) firstHit = (a == cue ? b : a).id;
                }
            }
        }
        void pot(Ball b) {
            b.potted = true; b.vx = b.vy = 0; Audio.sfx("pot");
            if (b == cue) { pottedCue = true; return; }
            if (b.id == 8) { pottedBlack = true; return; }
            if (mine(b, playerTurn)) pottedOwn = true; else pottedOther = true;
        }
        boolean allOwnPotted(boolean p) { for (Ball b : balls) if (!b.potted && mine(b, p)) return false; return true; }
        void settle() {
            boolean p = playerTurn; boolean foul = pottedCue || firstHit < 0 || (firstHit == 8 ? !allOwnPotted(p) : !mine(ballById(firstHit), p));
            if (pottedBlack) { boolean clear = allOwnPotted(p) && !foul; end(clear ? p : !p, clear ? "pots the black to win!" : "pots the black too early and loses!"); return; }
            String who = p ? "You" : Sim.first(opp);
            if (foul) { msg = who + " fouled" + (pottedCue ? " (potted the white)" : firstHit < 0 ? " (missed everything)" : " (wrong ball first)") + " — ball in hand."; cue.potted = true; swap(true); }
            else if (pottedOwn) { msg = who + " potted one — go again!"; Audio.sfx("right"); continueTurn(); }
            else { msg = who + " missed. " + (p ? Sim.first(opp) : "Your") + (p ? "'s turn." : " turn."); swap(false); }
        }
        Ball ballById(int id) { for (Ball b : balls) if (b.id == id) return b; return cue; }
        void swap(boolean ballInHand) {
            playerTurn = !playerTurn; if (cue.potted) { if (playerTurn) { placing = true; msg += " Click to place the cue ball."; } else { cue.potted = false; for (int i = 0; i < 80; i++) { double x = L + TW * .2 + Util.r(0, 100), y = T + Util.r(30, TH - 30); if (free(x, y)) { cue.x = x; cue.y = y; break; } } cue.vx = cue.vy = 0; } }
            if (!playerTurn) npcShoot(); }
        void continueTurn() { if (!playerTurn) npcShoot(); else if (cue.potted) placing = true; }
        void npcShoot() {
            if (opp == null) { playerTurn = true; return; }
            javax.swing.Timer w = new javax.swing.Timer(1100, e -> {
                if (over) return;
                boolean black = allOwnPotted(false);
                Ball best = null; double[] bp = null; double bs = -1e9, bang = 0;
                for (Ball b : balls) if (!b.potted && b != cue && (black ? b.id == 8 : mine(b, false))) for (double[] p : pockets) {
                    double tx = p[0] - b.x, ty = p[1] - b.y, td = Math.hypot(tx, ty); if (td < 1) continue;
                    double gx = b.x - tx / td * BR * 2, gy = b.y - ty / td * BR * 2; double cx = gx - cue.x, cy = gy - cue.y, cd = Math.hypot(cx, cy); if (cd < 1) continue;
                    double cut = Math.abs(Math.atan2(cx * ty - cy * tx, cx * tx + cy * ty));
                    double sc = -cd * .5 - td * .3 - cut * 300; if (cut > 1.2) continue;
                    if (sc > bs) { bs = sc; best = b; bp = new double[]{gx, gy}; bang = Math.atan2(cy, cx); }
                }
                double ang, pw;
                double sigma = Math.toRadians((11 - opp.pool) * .55 + opp.drunk * .02);
                if (best == null) { Ball any = null; for (Ball b : balls) if (!b.potted && b != cue && mine(b, false)) any = b; if (any == null) any = ballById(8); ang = Math.atan2(any.y - cue.y, any.x - cue.x); pw = .35; }
                else { ang = bang; pw = Util.clamp(.25 + Math.hypot(bp[0] - cue.x, bp[1] - cue.y) / 700 + Math.hypot(best.x - 0, 0) * 0, .3, .85); }
                shoot(ang + Util.R.nextGaussian() * sigma, pw);
            });
            w.setRepeats(false); w.start();
        }
        void end(boolean playerWon, String text) {
            over = true; msg = (playerWon ? "You " : Sim.first(opp) + " ") + text; Audio.sfx(playerWon ? "cheer" : "groan");
            if (playerWon) Sim.stat("poolWins");
            if (opp != null) Games.afterGame(opp, playerWon, "pool", stake);
            setLayout(null); JButton b = btn("Back to the pub", () -> { timer.stop(); close.run(); }); b.setBounds(350, 505, 220, 38); add(b); revalidate();
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g);
            g.setPaint(new GradientPaint(0, 0, WOOD3, 0, getHeight(), WOOD)); g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g.setColor(new Color(0x5a3a22)); g.fillRoundRect((int) L - 36, (int) T - 36, (int) TW + 72, (int) TH + 72, 40, 40);
            g.setColor(new Color(0x1f7a4a)); g.fillRect((int) L - 6, (int) T - 6, (int) TW + 12, (int) TH + 12);
            g.setColor(new Color(0x17603a)); g.setStroke(new BasicStroke(2f)); g.drawLine((int) (L + TW * .25), (int) T, (int) (L + TW * .25), (int) (T + TH));
            g.setColor(Color.BLACK); for (double[] p : pockets) g.fill(new Ellipse2D.Double(p[0] - 20, p[1] - 20, 40, 40));
            for (Ball b : balls) if (!b.potted) {
                g.setColor(new Color(0, 0, 0, 80)); g.fill(new Ellipse2D.Double(b.x - BR + 2, b.y - BR + 3, BR * 2, BR * 2));
                g.setColor(b.col); g.fill(new Ellipse2D.Double(b.x - BR, b.y - BR, BR * 2, BR * 2));
                g.setColor(new Color(255, 255, 255, 140)); g.fill(new Ellipse2D.Double(b.x - 6, b.y - 7, 6, 5));
                if (b.id >= 9) { g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(b.x - 4, b.y - 3, 8, 8)); }
            }
            if (!moving && !over && playerTurn && !placing && !cue.potted) {
                double ang = Math.atan2(my - cue.y, mx - cue.x); double len = 180;
                g.setColor(new Color(255, 255, 255, 130)); g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[]{6, 6}, 0)); g.drawLine((int) cue.x, (int) cue.y, (int) (cue.x + Math.cos(ang) * len), (int) (cue.y + Math.sin(ang) * len));
                double pull = charging ? power * 70 : 0; g.setColor(new Color(0xd9b26a)); g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.drawLine((int) (cue.x - Math.cos(ang) * (BR + 6 + pull)), (int) (cue.y - Math.sin(ang) * (BR + 6 + pull)), (int) (cue.x - Math.cos(ang) * (BR + 220 + pull)), (int) (cue.y - Math.sin(ang) * (BR + 220 + pull)));
            }
            if (placing) { g.setColor(new Color(255, 255, 255, 120)); g.fill(new Ellipse2D.Double(mx - BR, my - BR, BR * 2, BR * 2)); }
            // power bar
            g.setColor(new Color(0, 0, 0, 140)); g.fillRoundRect(40, 515, 260, 18, 10, 10); g.setPaint(new GradientPaint(40, 0, new Color(0x4caf50), 300, 0, new Color(0xe53935))); g.fillRoundRect(40, 515, (int) (260 * (charging ? power : 0)), 18, 10, 10); g.setColor(CREAM); g.setFont(sans(11, true)); g.drawString("POWER", 46, 528);
            g.setFont(sans(14, true)); g.setColor(Color.WHITE); g.drawString(msg, 40, 32);
            g.setFont(sans(12, false)); g.setColor(new Color(0xcfc3a0)); int r = 0, y = 0; for (Ball b : balls) if (!b.potted) { if (b.id >= 1 && b.id <= 7) r++; if (b.id >= 9) y++; }
            g.drawString("Reds left: " + r + "    Yellows left: " + y + (opp != null ? "    Stake " + Util.money0(stake) : ""), 40, 52);
            g.dispose();
        }
    }

    // =====================================================================
    //  QUIZ
    // =====================================================================
    public static JPanel quiz(Runnable close) { return new QuizPanel(close); }

    static final class QuizPanel extends JPanel {
        final List<Object[]> qs = new ArrayList<>(); int qi = -1, time; final List<Events.QTeam> teams; final Events.QTeam me = new Events.QTeam(); javax.swing.Timer timer;
        final JLabel qLabel = new JLabel(), info = new JLabel(), score = new JLabel(); final JButton[] ans = new JButton[4]; boolean answered, hintUsed; final Runnable close; final JPanel results = new JPanel();
        QuizPanel(Runnable close) {
            super(new BorderLayout(10, 10)); this.close = close; setOpaque(false); setPreferredSize(new Dimension(780, 520));
            teams = new ArrayList<>(Events.quizTeams()); me.name = "The Landlord's Legends"; me.player = true; teams.add(me);
            List<Object[]> all = new ArrayList<>(Arrays.asList(Data.QUIZ)); Collections.shuffle(all, Util.R); qs.addAll(all.subList(0, 10));
            JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); JLabel h = title("PUB QUIZ NIGHT", 24, BRASS2); top.add(h, BorderLayout.WEST); info.setForeground(CREAM); info.setFont(sans(14, true)); top.add(info, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            qLabel.setFont(serif(22, true)); qLabel.setForeground(Color.WHITE); qLabel.setVerticalAlignment(SwingConstants.TOP);
            JPanel mid = new JPanel(new BorderLayout(10, 10)); mid.setOpaque(false); JPanel qp = new JPanel(new BorderLayout(0, 14)); qp.setOpaque(false); qp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); qp.add(qLabel, BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10)); grid.setOpaque(false); grid.setPreferredSize(new Dimension(500, 170));
            for (int i = 0; i < 4; i++) { final int k = i; ans[i] = btn("", () -> answer(k)); ans[i].setFont(sans(15, true)); grid.add(ans[i]); }
            JPanel gw = new JPanel(new BorderLayout()); gw.setOpaque(false); gw.add(grid, BorderLayout.NORTH); qp.add(gw, BorderLayout.CENTER);
            JPanel rt = new JPanel(); rt.setOpaque(false); rt.setLayout(new BoxLayout(rt, BoxLayout.Y_AXIS)); rt.setPreferredSize(new Dimension(230, 100)); score.setForeground(CREAM); score.setFont(sans(13, false)); rt.add(score);
            JButton hint = btn("Ask Priya for a hint", () -> hint()); rt.add(Box.createVerticalStrut(10)); rt.add(hint);
            mid.add(qp, BorderLayout.CENTER); mid.add(rt, BorderLayout.EAST); add(mid, BorderLayout.CENTER);
            timer = new javax.swing.Timer(1000, e -> { if (qi >= 0 && !answered && --time <= 0) answer(-1); else if (qi >= 0 && !answered) info.setText("Question " + (qi + 1) + "/10   ⏱ " + time + "s"); });
            next(); timer.start();
        }
        void hint() { Npc p = Sim.npc("priya"); if (hintUsed || answered || p == null || !p.inPub) { Sim.toast(p == null || !p.inPub ? "Priya isn't here!" : "Hint already used."); return; } hintUsed = true; int c = (Integer) qs.get(qi)[2]; int rem = 0; for (int i = 0; i < 4 && rem < 2; i++) if (i != c) { ans[i].setEnabled(false); rem++; } }
        void next() {
            qi++; answered = false; time = 15;
            if (qi >= 10) { finish(); return; }
            Object[] q = qs.get(qi); qLabel.setText("<html><body style='width:480px'>" + esc((String) q[0]) + "</body></html>"); String[] a = (String[]) q[1];
            for (int i = 0; i < 4; i++) { ans[i].setText((char) ('A' + i) + ". " + a[i]); ans[i].setEnabled(true); }
            info.setText("Question " + (qi + 1) + "/10   ⏱ " + time + "s"); updateScore();
        }
        void answer(int k) {
            if (answered) return; answered = true; int c = (Integer) qs.get(qi)[2];
            for (int i = 0; i < 4; i++) ans[i].setEnabled(false);
            boolean ok = k == c; if (ok) { me.score++; Audio.sfx("right"); } else Audio.sfx("wrong");
            String who = ""; for (Events.QTeam t : teams) if (!t.player && Util.chance(.2 + t.skill * .65)) t.score++;
            info.setText(ok ? "Correct!" : (k < 0 ? "Time's up! Answer: " : "Wrong! Answer: ") + (char) ('A' + c));
            Npc dave = Sim.npc("dave"); if (!ok && dave != null && dave.inPub && Util.chance(.15)) info.setText("Dave: \"I'd like to challenge that.\" (Answer: " + (char) ('A' + c) + ")");
            updateScore();
            javax.swing.Timer w = new javax.swing.Timer(1500, e -> next()); w.setRepeats(false); w.start();
        }
        void updateScore() { teams.sort((a, b) -> b.score - a.score); StringBuilder sb = new StringBuilder("<html><b>Scores</b><br>"); for (Events.QTeam t : teams) sb.append(t.player ? "<font color='#ffd27a'>" : "").append(esc(t.name)).append(": ").append(t.score).append(t.player ? "</font>" : "").append("<br>"); score.setText(sb.toString()); }
        void finish() {
            timer.stop(); for (JButton b : ans) b.setVisible(false); updateScore(); Events.QTeam w = teams.get(0); boolean won = w == me || (teams.size() > 1 && teams.get(0).score == me.score && Util.chance(.5));
            if (won) w = me; qLabel.setText("<html><body style='width:480px'>" + (won ? "YOUR TEAM WINS THE QUIZ! 🏆" : esc(w.name) + " win the quiz!") + "</body></html>"); info.setText("Final scores");
            Events.quizAftermath(w, new ArrayList<>(teams), won); if (won) { Sim.addMoney(0); Sim.S.money += 25; Sim.toast("You win the £25 prize pot!"); }
            Audio.sfx(won ? "cheer" : "groan");
            add(btn("Back to the pub", () -> { timer.stop(); close.run(); }), BorderLayout.SOUTH); revalidate();
        }
    }

    // =====================================================================
    //  KARAOKE
    // =====================================================================
    public static JPanel karaoke(Runnable close) { return new KaraokePanel(close); }

    static final class KaraokePanel extends JPanel {
        static final String[] LYRICS = {"Is this the real pub? ♪", "Is this just fantasy? ♪", "Caught in a landslide... of crisps ♪", "No escape from the lock-in ♪", "Open your eyes, look up to the pigeon ♪", "And see... ♪", "I'm just a poor landlord ♪", "Nobody loves me ♪"};
        final Runnable close; double t, startT; List<double[]> notes = new ArrayList<>(); int hits, misses, combo; String fb = "Press SPACE or click when a note hits the gold line!"; javax.swing.Timer timer; boolean over;
        KaraokePanel(Runnable close) {
            this.close = close; setPreferredSize(new Dimension(780, 420)); setOpaque(false); setFocusable(true);
            for (int i = 0; i < 22; i++) notes.add(new double[]{2.0 + i * .75 + (i % 3 == 0 ? .2 : 0), 0}); // time, state (0 pending,1 hit,2 miss)
            Audio.quietMode = false;
            addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { press(); } });
            addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_SPACE) press(); } });
            timer = new javax.swing.Timer(16, e -> { t += .016; for (double[] n : notes) if (n[1] == 0 && t - n[0] > .25) { n[1] = 2; misses++; combo = 0; fb = "Missed!"; } if (!over && t > notes.get(notes.size() - 1)[0] + 1) end(); repaint(); });
            timer.start(); SwingUtilities.invokeLater(this::requestFocusInWindow);
        }
        void press() {
            if (over) return; double best = 9; double[] bn = null;
            for (double[] n : notes) if (n[1] == 0 && Math.abs(t - n[0]) < best) { best = Math.abs(t - n[0]); bn = n; }
            if (bn != null && best < .22) { bn[1] = 1; hits++; combo++; fb = best < .07 ? "PERFECT!" : best < .14 ? "Great!" : "Good"; Audio.sfx("right"); } else { misses++; combo = 0; fb = "Off the beat!"; Audio.sfx("wrong"); }
        }
        void end() {
            over = true; int score = (int) (100.0 * hits / notes.size()); fb = "You scored " + score + "%!"; Events.playerSang(score);
            JButton b = btn("Bow and leave the stage", () -> { timer.stop(); close.run(); }); setLayout(null); b.setBounds(280, 360, 240, 38); add(b); revalidate();
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); AA(g);
            g.setPaint(new GradientPaint(0, 0, new Color(0x2b1b45), 0, getHeight(), new Color(0x0e0818))); g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            for (int i = 0; i < 6; i++) { g.setColor(Gfx.c(i % 2 == 0 ? 0xff4fa3 : 0x4fc3ff, 40 + (int) (30 * Math.sin(t * 3 + i)))); g.fillOval(60 + i * 120, 20, 100, 100); }
            g.setFont(serif(24, true)); g.setColor(BRASS2); g.drawString("KARAOKE NIGHT", 24, 40);
            g.setFont(sans(20, true)); g.setColor(Color.WHITE); String ly = LYRICS[((int) (t / 2.2)) % LYRICS.length]; int w = g.getFontMetrics().stringWidth(ly); g.drawString(ly, 390 - w / 2, 130);
            int hitX = 160, y = 220;
            g.setColor(new Color(255, 255, 255, 40)); g.fillRoundRect(40, y - 28, 700, 56, 28, 28);
            g.setColor(BRASS2); g.setStroke(new BasicStroke(3f)); g.drawLine(hitX, y - 36, hitX, y + 36);
            for (double[] n : notes) { double x = hitX + (n[0] - t) * 200; if (x < 30 || x > 750) continue; g.setColor(n[1] == 1 ? new Color(0x66ff99) : n[1] == 2 ? Gfx.c(0xff5a5a, 120) : new Color(0xff4fa3)); g.fill(new Ellipse2D.Double(x - 14, y - 14, 28, 28)); g.setColor(Color.WHITE); g.draw(new Ellipse2D.Double(x - 14, y - 14, 28, 28)); }
            g.setFont(sans(18, true)); g.setColor(Color.WHITE); g.drawString(fb, 40, 310);
            g.setFont(sans(14, false)); g.setColor(CREAM); g.drawString("Hits " + hits + "   Misses " + misses + "   Combo " + combo, 40, 340);
            g.dispose();
        }
    }
}
