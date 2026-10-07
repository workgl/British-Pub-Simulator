package pub;

import java.io.Serializable;
import java.util.*;

/** All saveable game state lives here (plain serialisable objects). */
public final class Model {
    private Model() {}

    public static class Mem implements Serializable {
        public int day; public String kind; public double w; public String about, who, t, src = "self";
        public Mem(int day, String kind, double w, String about, String who, String t) { this.day = day; this.kind = kind; this.w = w; this.about = about; this.who = who; this.t = t; }
    }

    public static class Order implements Serializable {
        public int drink; public int qty = 1; public List<String> round = new ArrayList<>(); public double since; public boolean food; public boolean wrong;
    }

    public static class Npc implements Serializable {
        // identity
        public String id, name, job, bio, g = "x", fav = "lager", favName = "", team = "", home = "houses";
        public int age;
        public double soc = .5, tmp = .4, hum = .5, gen = .4, gos = .3, frug = .5, intel = .5, sport = .4, tol = .6;
        public Map<String, Double> op = new HashMap<>();
        public Map<String, Double> music = new HashMap<>();
        public String[] quirk = new String[0]; public int quirkEvery = 60, quirkT = 60, quirkCount;
        public String secret = "", secretKind = ""; public boolean secretKnown, secretGood;
        public int dayMask = 127; public double arrA = 18, arrB = 20, stayA = 1.5, stayB = 3; public double income = 150;
        public int workFrom = -1, workTo = -1, workMask = 31; public String workplace = "";
        public int skin = 0xE0AC8C, hair = 0x3B2A1A, shirt = 0x3366AA, hairStyle = 0; public double scale = 1;
        public int darts = 4, pool = 4, sing = 4, quizSkill = 4, barSkill = 2;
        public boolean peace, stranger, wantsJob;
        // dynamic
        public double mood = 60, energy = 80, money = 40, thirst = 20, bladder = 10, hunger = 20, drunk, anger, embar, trust, loyalty = 50, exp;
        public boolean inPub; public String st = "away"; public String loc = "home"; public int locUntil;
        public double x, y; public List<double[]> path = new ArrayList<>(); public String nextSt = ""; public double nextDur;
        public String spot = ""; public double timer; public int glass = -1; public double glassLevel;
        public Order order; public String conv = ""; public String doing = "";
        public List<Mem> mem = new ArrayList<>();
        public List<String> lifeNews = new ArrayList<>();
        public int visits, daysSeen, lastVisit, bannedUntil, absentUntil, planAt = -1, planStay, arrivedAt, stayUntil, tab;
        public boolean planGo, defected, staff; public String planVenue = "pub";
        public int drinksToday; public double spentToday;
        public int foodT = -1; public boolean eating;
        public String bubble = ""; public double bubbleUntil; public boolean bubbleShout;
        public double walkPhase; public int arguedToday;
        public String pickedFriend = "";
        public int promotedDay;
        public Map<String, String> flags = new HashMap<>();
    }

    public static class Staff implements Serializable {
        public String id, name, role, tags = ""; public int skill, speed, rely, charm; public double mood = 70, wage;
        public double x, y, tx, ty; public boolean present, busy; public double busyT; public int hiredDay, mistakes, starBar;
        public String fromNpc = "";
        public Map<String, Double> rel = new HashMap<>();
        public int shirt = 0x884422, hair = 0x222222; public String note = "";
        public int sick;
    }

    public static class Joke implements Serializable {
        public String id, name; public int count = 1, day; public String a = "", b = "";
    }

    public static class Mess implements Serializable { public double x, y; public int made; public int kind; }

    public static class Delivery implements Serializable { public int item, qty, arrives; }

    public static class Booking implements Serializable { public String type; public int day; }

    public static class News implements Serializable {
        public String head, body; public int day;
        public News(String h, String b, int d) { head = h; body = b; day = d; }
    }

    public static class DayRec implements Serializable { public int day; public double rev, exp, sat; public int served, crowd; public String weather = ""; }

    public static class Goal implements Serializable { public int minute, team; public String scorer; public int hg, ag; public String kind; }

    public static class Match implements Serializable {
        public int home, away, hg, ag, minute, kickoff, day; public String phase = "pre"; public boolean rovers, big;
        public List<String> events = new ArrayList<>(); public double acc; public String label = "League";
        public int htLeft = 15; public double flash; public String lastEvent = ""; public int lastEventT;
    }

    public static class State implements Serializable {
        public String pubName = "The Speckled Pigeon";
        public int day = 1, min = 10 * 60;
        public double money = 2500, rep = 20, pop = 25, sat = 60, clean = 85, prestige;
        public int level = 1;
        public double[] price = Data.DPRICE.clone();
        public int[] stock = {60, 50, 40, 20, 24, 30, 40, 40, 20};
        public int[] autoTarget = {80, 60, 50, 30, 30, 40, 50, 40, 25};
        public boolean autoStock = true;
        public List<Delivery> deliveries = new ArrayList<>();
        public int openH = 11, closeH = 23;
        public List<Npc> npcs = new ArrayList<>(); public List<Staff> staff = new ArrayList<>(), applicants = new ArrayList<>();
        public Map<String, Double> rel = new HashMap<>();
        public Set<String> crush = new HashSet<>(), couples = new HashSet<>(), exes = new HashSet<>();
        public List<Joke> jokes = new ArrayList<>();
        public Set<String> upgrades = new HashSet<>(Arrays.asList("darts", "pool", "jukebox", "tv", "kitchen"));
        public Set<String> decor = new HashSet<>();
        public Map<String, Integer> ads = new HashMap<>();
        public Map<String, Integer> stats = new HashMap<>();
        public Map<String, Integer> ach = new LinkedHashMap<>();
        public Map<String, String> flags = new HashMap<>();
        public List<String> log = new ArrayList<>();
        public List<News> news = new ArrayList<>(), archive = new ArrayList<>();
        public List<Mess> messes = new ArrayList<>();
        public Match match; public List<Match> fixtures = new ArrayList<>();
        public int[] table = new int[8]; public int rovPlayed, rovW, rovD, rovL;
        public int weather; // 0 sun 1 cloud 2 rain 3 snow
        public double rev, exp; public int served, peakCrowd; public double satSum; public int satN;
        public double revToday, expToday; public int servedToday;
        public List<DayRec> history = new ArrayList<>();
        public boolean quizOn = true, sports = true, roastOn = true; public int karaokeDow = -1;
        public List<Booking> bookings = new ArrayList<>();
        public double rivalStr = 40; public boolean royalOak;
        public double px = 480, py = 520;
        public List<String> queue = new ArrayList<>();
        public Map<String, String> occ = new HashMap<>();
        public String pigeonName = "the pigeon"; public double pgx = 600, pgy = 300; public int pigeonDay = -99; public boolean pigeonHere;
        public boolean tvBroken, powerCut, deliveryLate, lastOrders, openedToday, dayEnded = true;
        public int tvFixAt = -1, powerBackAt = -1;
        public int quizDay = -1; public boolean quizRan;
        public Map<String, Integer> jukeQueueCount = new HashMap<>();
        public String song = ""; public int songUntil = -1, songGenreIdx = -1; public int songAt;
        public String playingGenre = "";
        public int storyFriday; public String fridayMan = ""; public int karaokeDay = -1;
        public int daysOpen, totalPints, totalMeals;
        public String lastNightSummary = "";
        public int tourAt;
        public String season = "Autumn";
        public int nextEventCheck;
        public int absTop;
    }
}
