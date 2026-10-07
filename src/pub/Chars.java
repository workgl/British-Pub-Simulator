package pub;

import java.util.*;
import pub.Model.Npc;

/** The cast. */
public final class Chars {
    private Chars() {}

    private static Npc mk(String id, String name, int age, String g, String job, String bio, String fav, String favName, String team) {
        Npc n = new Npc();
        n.id = id; n.name = name; n.age = age; n.g = g; n.job = job; n.bio = bio; n.fav = fav; n.favName = favName; n.team = team;
        return n;
    }
    private static Npc t(Npc n, double soc, double tmp, double hum, double gen, double gos, double frug, double intel, double sport, double tol) {
        n.soc = soc; n.tmp = tmp; n.hum = hum; n.gen = gen; n.gos = gos; n.frug = frug; n.intel = intel; n.sport = sport; n.tol = tol; return n;
    }
    private static Npc sc(Npc n, int days, double a, double b, double sa, double sb, double inc, double cash) {
        n.dayMask = days; n.arrA = a; n.arrB = b; n.stayA = sa; n.stayB = sb; n.income = inc; n.money = cash; return n;
    }
    private static Npc work(Npc n, String place, int from, int to, int mask) { n.workplace = place; n.workFrom = from; n.workTo = to; n.workMask = mask; return n; }
    private static Npc look(Npc n, int skin, int hair, int shirt, int style, double scale) { n.skin = skin; n.hair = hair; n.shirt = shirt; n.hairStyle = style; n.scale = scale; return n; }
    private static Npc skills(Npc n, int darts, int pool, int sing, int quiz, int bar) { n.darts = darts; n.pool = pool; n.sing = sing; n.quizSkill = quiz; n.barSkill = bar; return n; }
    private static Npc secret(Npc n, String text, boolean good) { n.secret = text; n.secretGood = good; return n; }
    private static Npc quirk(Npc n, int every, String... lines) { n.quirk = lines; n.quirkEvery = every; n.quirkT = Util.ri(every / 2, every); return n; }
    private static Npc op(Npc n, Object... kv) { for (int i = 0; i < kv.length; i += 2) n.op.put((String) kv[i], ((Number) kv[i + 1]).doubleValue()); return n; }
    private static Npc mus(Npc n, Object... kv) { for (int i = 0; i < kv.length; i += 2) n.music.put((String) kv[i], ((Number) kv[i + 1]).doubleValue()); return n; }

    public static List<Npc> cast() {
        List<Npc> l = new ArrayList<>();
        l.add(skills(look(work(sc(t(quirk(secret(op(mus(mk("dave", "Dave Hargreaves", 52, "m", "electrician", "Twenty years on the same stool. Knows everyone. Has opinions. About everything. Mostly football.", "ale", "Old Hen Bitter", "ROV"),
            "britpop", .8, "chant", .9, "folk", .3, "dance", -.6), "football", 1, "politics", .95, "work", .3, "television", -.3, "weather", -.2), "Secretly takes ballroom dancing lessons on Tuesday nights at the church hall.", true), 70,
            "Twenty years I've been drinking in here. Twenty years.", "I'm not saying I know everything about football. I'm saying I do.", "Mark my words — and I'm never wrong — it's all going downhill.", "I was in here before they put the carpet down."),
            .85, .6, .55, .45, .4, .4, .5, 1, .8), 127, 17.5, 19.5, 3, 5, 520, 60), "Hargreaves Electrics", 8, 17, 31), 0xE0AC8C, 0x777777, 0x2E5E8C, 1, 1.08), 6, 5, 4, 4, 1));
        l.add(skills(look(work(sc(t(secret(op(mus(mk("sarah", "Sarah Okafor", 34, "f", "A&E nurse", "Does twelve-hour shifts and still shows up for a quiz. Friendly, until politics comes up.", "wine", "Large white wine, ice", "ROV"),
            "indie", .7, "ballad", .5, "dance", .6), "politics", -.9, "work", .8, "television", .5, "football", .2), "Former county darts champion who stopped telling people after it got her banned from the Frog & Trumpet.", true),
            .7, .35, .7, .6, .3, .5, .7, .4, .6), 31 | 32, 16.5, 18.5, 2, 3.5, 410, 70), "Westbridge General", 7, 16, 15), 0x8D5524, 0x1B1B1B, 0xE85D75, 3, .98), 9, 4, 5, 6, 2));
        Npc liam = skills(look(work(sc(t(secret(op(mus(mk("liam", "Liam Pritchard", 20, "m", "university student", "Studying biochemistry. Allegedly. Always looking for the cheapest drink and a job.", "lager", "Whatever's cheapest", "ASH"),
            "indie", .8, "dance", .7, "folk", -.4), "music", .8, "money", -1, "football", .6, "television", .7), "Writes other students' essays for cash.", false),
            .75, .3, .8, .1, .5, .95, .6, .5, .5), 127, 19, 20.5, 2, 4, 55, 12), "Uni of Westbridge", -1, -1, 0), 0xF1C27D, 0x5A3A1A, 0x2F8F4E, 0, .96), 3, 6, 5, 4, 7);
        liam.wantsJob = true; l.add(liam);
        l.add(skills(look(sc(t(quirk(secret(op(mus(mk("steve", "Steve Whitlock", 47, "m", "conservatory salesman", "Runs Whitlock Windows & Conservatories. Believes he practically owns this pub.", "spirit", "Large whisky, no ice", "ASH"),
            "swing", .8, "singalong", .5, "metal", -.7), "money", .8, "politics", .5, "cars", .9, "football", .5), "Business is secretly going under, and the alimony isn't helping.", false), 60,
            "Between you and me, I practically run this place.", "When I'm doing the books for this place...", "I could buy this pub. Don't think I haven't thought about it.", "That's MY table, by the way."),
            .9, .55, .3, .8, .3, .1, .45, .5, .7), 127, 18, 20, 2.5, 4.5, 700, 300), 0xE0AC8C, 0x2B2B2B, 0x1A237E, 0, 1.0), 4, 5, 3, 3, 1));
        l.add(skills(look(sc(t(quirk(secret(op(mus(mk("linda", "Linda Moss", 61, "f", "retired school secretary", "Knows everyone's business. Will tell you hers, yours and theirs.", "wine", "Small white wine (large)", "ROV"),
            "singalong", .7, "ballad", .6, "metal", -.8), "politics", .1, "money", .4, "television", .5), "Writes the anonymous 'Overheard in Westbridge' column for the Chronicle.", false), 55,
            "I'm not one to gossip, but...", "You didn't hear it from me.", "I couldn't possibly say. (I'm going to.)", "Have you heard? No? Sit down."),
            1.0, .3, .6, .4, 1.0, .6, .6, .1, .4), 127, 12, 14, 3, 5, 280, 40), 0xF1C27D, 0xC9C9C9, 0x8E24AA, 4, .96), 2, 2, 4, 5, 1));
        l.add(skills(look(sc(t(quirk(secret(op(mus(mk("gaz", "Gaz Mullen", 38, "m", "plasterer", "Claims he 'used to play semi-pro'. Approximately every seventeen minutes.", "lager", "Pint of lager", "ASH"),
            "britpop", .5, "dance", .7, "chant", .8), "football", 1, "cars", .6, "holidays", .5), "Was never semi-pro. Was the kit man. For one season.", false), 17,
            "I used to play semi-pro, you know.", "When I was semi-pro, we trained twice a week. Twice.", "Had a trial at Ashford once. Semi-pro level, that was.", "Scouts came to watch me. Probably. Someone had a clipboard.",
            "I could've gone pro. Knee. Weather. Politics.", "Bergkamp once told me 'good touch'. I think.", "Arsenal wanted me. I said no. Long story.", "Semi-pro. Just saying."),
            .8, .55, .6, .5, .4, .5, .3, .9, .9), 127, 18, 20, 2.5, 4, 480, 45), 0xE0AC8C, 0x6B4A2A, 0xD32F2F, 2, 1.04), 5, 4, 2, 3, 2));
        l.add(skills(look(sc(t(secret(op(mus(mk("maggie", "Maggie Doyle", 78, "f", "retired", "Sharp tongue, sharper memory. Has sat in the same chair since 1974.", "stout", "Milk stout", "ROV"),
            "swing", .9, "ballad", .8, "singalong", .7, "metal", -1, "dance", -.5), "television", .8, "music", .9, "cars", -.5, "weather", .6), "Had a one-hit wonder in 1968 as 'Sugar Lump Sally'. Nobody knows. She sings like an angel.", true),
            .6, .7, .85, .2, .7, .8, .7, .1, .9), 127, 14, 16, 3.5, 5, 190, 30), 0xF1C27D, 0xEEEEEE, 0x6D4C41, 4, .9), 2, 4, 10, 6, 0));
        l.add(skills(look(work(sc(t(secret(op(mus(mk("priya", "Priya Nair", 29, "f", "council data analyst", "Quiz champion. Banned from three other pubs' quizzes for 'unfair knowledge'.", "cider", "Dry cider", "RED"),
            "indie", .7, "synthpop", .6, "chant", -.3), "television", .6, "work", .4, "food", .7), "Moonlights as a masked wrestler called 'The Nair Hair Raiser'.", true),
            .6, .4, .7, .5, .3, .5, .98, .3, .5), 4 | 16 | 32, 19, 20, 2.5, 3.5, 380, 60), "Westbridge Council", 9, 17, 31), 0xB07A4F, 0x111111, 0x00897B, 3, .94), 3, 7, 4, 10, 2));
        l.add(skills(look(sc(t(secret(op(mus(mk("tony", "Big Tony Russo", 45, "m", "nightclub doorman", "A gentle giant who breaks up fights by asking nicely. Drinks lime and soda.", "soft", "Lime & soda", "ROV"),
            "ballad", .6, "dance", .3, "metal", .4), "food", .9, "pets", .8), "Bakes spectacular cakes and sells them online as 'Tony's Teacakes'.", true),
            .5, .1, .4, .6, .1, .5, .5, .6, 1), 16 | 32 | 64 | 8, 17, 19, 2, 4, 340, 50), 0xC68642, 0x000000, 0x212121, 2, 1.25), 5, 5, 3, 4, 3));
        Npc tony = l.get(l.size() - 1); tony.peace = true;
        l.add(skills(look(sc(t(quirk(secret(op(mus(mk("kev", "Kev Bagshaw", 66, "m", "retired bus driver", "Drove the 43 for thirty-one years. Will tell you about it.", "ale", "Half of mild", "ROV"),
            "folk", .8, "singalong", .5, "dance", -.7), "cars", .9, "weather", .6, "holidays", .5, "health", .5), "Is the mystery man who feeds the pub pigeon. Has given it a name.", true), 100,
            "Did I ever tell you about the 43 to Kingsfield?", "Thirty-one years on the 43. Never missed a stop.", "Now, you'd think the roundabout was the problem. It wasn't.", "Where was I? Oh yes. The 43."),
            .7, .2, .4, .4, .4, .8, .5, .3, .6), 127, 12.5, 14, 4, 6, 260, 35), 0xF1C27D, 0xBBBBBB, 0x5D6D7E, 1, .98), 3, 3, 3, 5, 0));
        l.add(skills(look(sc(t(secret(op(mus(mk("jade", "Jade Whitaker", 24, "f", "hairdresser & influencer", "Photographs every drink. Has 412 followers and a dream.", "wine", "Prosecco, obviously", "KNG"),
            "dance", .9, "synthpop", .8, "folk", -.5, "swing", -.2), "music", .7, "relationships", .8, "television", .7), "Is the admin of the 'Westbridge Gossip & Grumbles' Facebook group.", false),
            .95, .45, .6, .5, .8, .4, .4, .1, .5), 16 | 32, 19.5, 21, 2, 3.5, 310, 45), 0xF1C27D, 0xC2185B, 0xAD1457, 5, .95), 3, 3, 6, 3, 3));
        l.add(skills(look(sc(t(secret(op(mus(mk("alan", "Rev. Alan Pike", 58, "m", "vicar of St Wulfstan's", "Competitive at pool. Swears at football. Immediately asks forgiveness.", "spirit", "Single malt, a small one", "ROV"),
            "folk", .6, "singalong", .6, "blues", .5), "football", .9, "politics", .1, "weather", .5), "Has a very secret and entirely unholy horse-racing habit.", false),
            .6, .5, .75, .6, .3, .5, .7, .8, .6), 1 | 2 | 8 | 32 | 64, 16.5, 18.5, 2, 3.5, 300, 80), 0xE0AC8C, 0x9E9E9E, 0x263238, 1, 1.0), 4, 8, 4, 6, 1));
        l.add(skills(look(sc(t(secret(op(mus(mk("mo", "Mo Rahman", 41, "m", "owner of Kebabylon takeaway", "Runs the best takeaway in Westbridge. Quick drinks on his break. Remembers everything you ever ordered.", "soft", "Cola, no ice", "ASH"),
            "britpop", .4, "dance", .6, "swing", .3), "football", .9, "food", 1, "money", .5), "The takeaway is up for sale — a mystery buyer is circling.", false),
            .8, .25, .8, .7, .6, .5, .6, .8, .3), 127, 15.5, 17.5, 1, 2, 560, 80), 0xA9744F, 0x111111, 0xFBC02D, 0, 1.0), 3, 3, 4, 5, 3));
        l.add(skills(look(work(sc(t(secret(op(mus(mk("debbie", "Debbie Crump", 45, "f", "supermarket manager", "Manages Savemore. Dry as a bone. Steve's ex-wife.", "wine", "Prosecco with ice", "NOR"),
            "disco", .8, "swing", .5, "metal", -.8), "money", .8, "relationships", -.5, "work", .6, "food", -.4), "Steve's ex-wife — she has never told a soul how much she enjoys the alimony payments.", false),
            .65, .5, .85, .4, .5, .6, .6, .2, .7), 32 | 16 | 64, 18.5, 20.5, 2, 3.5, 520, 70), "Savemore", 8, 17, 31), 0xF1C27D, 0x7B3F00, 0x6A1B9A, 4, 1.0), 3, 3, 4, 5, 2));
        l.add(skills(look(work(sc(t(secret(op(mus(mk("owen", "Owen Price", 31, "m", "IT contractor", "Welsh. Rugby-obsessed. Considers football an elaborate hoax.", "ale", "Craft IPA", "SUN"),
            "folk", .5, "indie", .6, "chant", -.9), "football", -.8, "work", .6, "television", .3), "Terrified of dogs. Absolutely terrified.", false),
            .6, .35, .7, .5, .3, .5, .85, .6, .6), 4 | 16 | 32, 18.5, 20.5, 2, 3.5, 450, 70), "Freelance (mostly at home)", 9, 17, 31), 0xF1C27D, 0x8B5A2B, 0xB71C1C, 0, 1.0), 4, 5, 5, 8, 2));
        l.add(skills(look(sc(t(secret(op(mus(mk("fiona", "Dr Fiona Blake", 55, "f", "GP", "Knows exactly how much you're drinking. Keeps it entirely to herself.", "wine", "Gin & tonic", "KNG"),
            "swing", .7, "folk", .4, "blues", .6), "health", .8, "work", .5, "holidays", .6), "Flies a vintage stunt plane at weekends.", true),
            .5, .2, .7, .7, .05, .3, .95, .2, .5), 2 | 8 | 32, 18, 19.5, 1.5, 3, 640, 120), 0xF1C27D, 0xB0A090, 0x00695C, 4, .98), 3, 3, 4, 8, 2));

        // initial relationships: a, b, a->b, b->a
        Object[][] rel = {
            {"dave", "gaz", -25, -25}, {"dave", "sarah", 10, -10}, {"dave", "steve", -30, -35}, {"dave", "tony", 40, 40}, {"dave", "kev", 35, 35}, {"dave", "linda", 15, 10},
            {"dave", "alan", 25, 25}, {"dave", "mo", 35, 35}, {"dave", "owen", -20, -20}, {"sarah", "priya", 45, 45}, {"sarah", "fiona", 35, 35}, {"sarah", "liam", 15, 15},
            {"sarah", "jade", 20, 20}, {"liam", "jade", 20, 30}, {"liam", "owen", 10, 10}, {"liam", "gaz", 35, 35}, {"steve", "debbie", -50, -60}, {"steve", "gaz", 10, 0},
            {"steve", "linda", 0, -10}, {"linda", "maggie", 55, 55}, {"linda", "kev", 20, 20}, {"linda", "jade", -15, -15}, {"priya", "owen", 50, 50}, {"priya", "debbie", 20, 20},
            {"tony", "mo", 55, 55}, {"tony", "maggie", 30, 30}, {"tony", "liam", 20, 20}, {"kev", "maggie", 25, 25}, {"kev", "alan", 30, 30}, {"alan", "gaz", -5, -5},
            {"alan", "steve", 5, -10}, {"owen", "gaz", -10, -10}, {"fiona", "debbie", 40, 40}, {"fiona", "alan", 35, 35}, {"debbie", "linda", 10, 5}};
        INITIAL_REL = rel;
        return l;
    }
    public static Object[][] INITIAL_REL;

    public static Npc colin() {
        Npc n = skills(look(sc(t(mk("colin", "Colin Forsyth", 49, "m", "'consultant'", "Appears every Friday. Says very little. Has a way of watching the room.", "ale", "Pint of best", "NOR"),
            .35, .15, .45, .6, .1, .3, .8, .5, .7), 16, 20, 21, 2, 3, 400, 250), 0xE0AC8C, 0x444444, 0x37474F, 0, 1.02), 6, 6, 3, 8, 3);
        n.dayMask = 16; return n;
    }

    private static int[] SHIRTS = {0x3366AA, 0xAA3333, 0x338855, 0x886622, 0x774488, 0x555555, 0xCC7722, 0x2299AA, 0xBB5577};
    private static int[] SKINS = {0xF1C27D, 0xE0AC8C, 0xC68642, 0x8D5524, 0xB07A4F, 0xF5D0B0};
    private static int[] HAIRS = {0x222222, 0x5A3A1A, 0x8B5A2B, 0xB0A090, 0xD4A94A, 0x777777, 0x7B3F00};

    public static Npc stranger() { return stranger(new HashSet<>()); }
    public static Npc stranger(Set<String> used) {
        String fn = Util.pick(Data.STRANGER_FIRST); for (int i = 0; i < 20 && used.contains(fn); i++) fn = Util.pick(Data.STRANGER_FIRST);
        String ln = Util.pick(Data.STRANGER_LAST);
        Npc n = mk("s" + Util.ri(1000, 99999), fn + " " + ln, Util.ri(19, 74), Util.chance(.5) ? "m" : "f", Util.pick(Data.STRANGER_JOB), "A newcomer to the pub.", Util.pick(Data.DRINKS[0], Data.DRINKS[1], Data.DRINKS[2], Data.DRINKS[4], Data.DRINKS[5], Data.DRINKS[0], Data.DRINKS[6]), "", Util.chance(.55) ? Util.pick(Data.TEAM_ID) : "");
        n.stranger = true;
        t(n, Util.r(.3, .9), Util.r(.1, .7), Util.r(.3, .8), Util.r(.2, .7), Util.r(.1, .7), Util.r(.3, .9), Util.r(.3, .9), Util.r(.1, .9), Util.r(.3, .9));
        n.money = Util.r(20, 90); n.income = Util.r(150, 450);
        n.skin = Util.pick(Arrays.stream(SKINS).boxed().toList()); n.hair = Util.pick(Arrays.stream(HAIRS).boxed().toList());
        n.shirt = Util.pick(Arrays.stream(SHIRTS).boxed().toList()); n.hairStyle = Util.ri(0, 5);
        n.darts = Util.ri(1, 7); n.pool = Util.ri(1, 7); n.sing = Util.ri(1, 8); n.quizSkill = Util.ri(2, 8);
        n.stayA = 1; n.stayB = 3; n.arrA = 18; n.arrB = 21;
        for (String g : Data.GENRES) n.music.put(g, Util.r(-.7, .9));
        n.secret = Util.pick("Has never actually watched a full football match.", "Is writing a novel. It is 900 pages and about a lighthouse.", "Once won a regional pie-eating competition.", "Is secretly a very good singer.", "Used to be in a ska band.", "Has a pet tortoise named Brian.");
        n.secretGood = true;
        n.peace = Util.chance(.1);
        return n;
    }

    /** Fill unset opinions & music tastes with seeded randomness so stances vary. */
    public static void fillDefaults(Npc n) {
        for (String t : Data.TOPIC_KEYS) n.op.putIfAbsent(t, Util.r(-.5, .5));
        for (String g : Data.GENRES) n.music.putIfAbsent(g, Util.r(-.6, .6));
        n.quirkT = Math.max(5, n.quirkT);
        if (n.workFrom >= 0 && n.workplace.isEmpty()) n.workplace = "work";
    }
}
