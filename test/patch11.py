def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

SPECIAL = r'''
    // ---------- character-specific lines (id|topic) ----------
    public static final Map<String, String[]> SPECIAL = new HashMap<>();
    static {
        SPECIAL.put("dave|politics", new String[]{"If I was Prime Minister, first thing I'd do is ban roundabouts. Second thing: more roundabouts, but better.", "Taxes! Everyone's got a view on taxes. Mine is: fewer.", "The council couldn't organise a pint in a brewery, and I'll tell them to their faces. Again."});
        SPECIAL.put("dave|football", new String[]{"Twenty years I've supported the Rovers. Twenty years of pain. It's practically a hobby.", "That offside decision was a crime against humanity.", "I'd manage Rovers better. I've got a clipboard."});
        SPECIAL.put("sarah|work", new String[]{"Twelve hours on A&E, and someone asked me if I do 'private'. I said: it's the NHS, love.", "You wouldn't believe what people put up their noses. Actually... you would.", "Night shifts do something to your sense of humour. It becomes mostly dark."});
        SPECIAL.put("sarah|health", new String[]{"Drink water. That's all. It's always water. Please.", "I see so many people who 'just had a funny turn'. It's the pies."});
        SPECIAL.put("liam|money", new String[]{"I've lived on toast for nine days. I'm basically a minimalist now.", "Student loans are just a monthly reminder that I'm an adult.", "I found 40p in a coat pocket. I nearly cried."});
        SPECIAL.put("liam|food", new String[]{"Pasta, pesto, and more pasta. The student diet.", "I once lived on stolen ketchup for a week. Be kind."});
        SPECIAL.put("steve|money", new String[]{"Business is booming. Booming! Don't look at the bank statements.", "I've got a lot of capital tied up. In the business. Which is thriving.", "It's all about cash flow. Mine's... flowing. Elsewhere."});
        SPECIAL.put("steve|cars", new String[]{"New Audi. Leased. Which is basically owning. Basically.", "I only drive German. It's a quality thing."});
        SPECIAL.put("linda|relationships", new String[]{"Have you seen the way those two look at each other? Mark my words.", "Of course I'm not interfering. I'm just... noticing. Loudly."});
        SPECIAL.put("linda|pub", new String[]{"This pub's got more secrets than the Vatican, and I know every single one.", "Everyone talks in here. Nobody listens. Except me."});
        SPECIAL.put("gaz|football", new String[]{"You know, if my knee hadn't gone, I'd have been playing in the Premier League by now. Probably.", "I played centre-half. Obviously. I was a wall.", "The beautiful game was made for people like me. Not for people like Owen."});
        SPECIAL.put("gaz|work", new String[]{"Plastering's an art, mate. I've got a gift. A trowel-shaped gift.", "Did a ceiling today. Smooth as a snooker table. You could play on it."});
        SPECIAL.put("maggie|television", new String[]{"They don't make soaps like they used to. In my day, someone was murdered every Tuesday.", "That chef on telly is far too thin. You can't trust a thin chef."});
        SPECIAL.put("maggie|music", new String[]{"I once knew a man who played the trombone at Buckingham Palace. Never stopped talking about it.", "Sixties music. That's proper. All this modern nonsense is just noises."});
        SPECIAL.put("maggie|cars", new String[]{"I never learned to drive. I've been all over the world on the 43 bus.", "A car is a cupboard on wheels. Why would you."});
        SPECIAL.put("priya|television", new String[]{"I've watched every quiz show on television. Twice. Without the answers.", "Question: why do they never ask about cheese? Cheese is important."});
        SPECIAL.put("priya|work", new String[]{"I spend all day in spreadsheets. By night I fight. Well... knowledge-fight.", "Council meeting minutes at nine a.m. I think I've died inside, a little."});
        SPECIAL.put("tony|food", new String[]{"A good sponge needs patience. And butter. Mostly butter.", "I've been working on a lemon drizzle. It's emotional.", "Do not fear the buttercream, my friend."});
        SPECIAL.put("tony|work", new String[]{"People think door work is all fighting. It's mostly saying 'evening' and 'trainers, sorry'.", "Best fights are the ones that don't happen."});
        SPECIAL.put("kev|cars", new String[]{"Thirty-one years on the 43. Never had a single crash. Just one low bridge.", "You can't beat a proper diesel engine. Listen to that. Beautiful.", "Roundabouts are the root of all evil."});
        SPECIAL.put("kev|weather", new String[]{"In '87 we had snow up to the windows. The 43 still ran. Late, mind.", "You can tell a lot from a sky. This one says: 'rain, then more rain'."});
        SPECIAL.put("jade|relationships", new String[]{"I'm not saying I'm single, but my cat has a better social life.", "He liked my story at 2am. Do you think that means something?", "Love is just good lighting and a ring light."});
        SPECIAL.put("jade|music", new String[]{"I'm making a playlist for the pub. It's mostly banging tunes and one sad song.", "This is my favourite song. It's also everyone's favourite song. That's how you know."});
        SPECIAL.put("alan|football", new String[]{"For heaven's sake, ref! ...Forgive me.", "Rovers are the Lord's own team. That second goal was a miracle. And a fluke.", "I pray for a clean sheet. Sometimes it works."});
        SPECIAL.put("alan|weather", new String[]{"The Lord giveth sun, and the Lord taketh it away in October.", "Rain on a Sunday keeps the congregation honest. Or at home."});
        SPECIAL.put("mo|food", new String[]{"A good kebab is a bridge between people. A bad kebab is a bridge too far.", "Chilli sauce? I admire your courage.", "I've fed half this town at 2am. I know your secrets."});
        SPECIAL.put("mo|football", new String[]{"United are on fire. I'm not even kidding. Don't tell Dave.", "Football and food — the only two things worth caring about."});
        SPECIAL.put("debbie|money", new String[]{"Alimony: the gift that keeps on giving. I mean — er — things are fine.", "I run a supermarket. I know the price of everything and the value of... also everything."});
        SPECIAL.put("debbie|relationships", new String[]{"My ex-husband thinks he's charming. He's like a conservatory: pricey, draughty, and nobody asked for it.", "Marriage is a long conversation in which you mostly don't say what you mean."});
        SPECIAL.put("owen|football", new String[]{"22 grown men chasing a bag of wind. Give me rugby. Give me a scrum. Give me a proper sport.", "Football is just rugby for people who cry a lot.", "If they'd just pick the ball up, it'd be over in ten minutes."});
        SPECIAL.put("owen|work", new String[]{"Another day, another 'turn it off and on again'.", "Freelancing: the art of being unemployed in a nice shirt."});
        SPECIAL.put("fiona|health", new String[]{"I'm not allowed to say who, but half this pub is on tablets.", "A glass of wine a day is fine. A bottle isn't. Everybody says 'it was a small bottle'.", "Walk more. Eat vegetables. Ignore Dave."});
        SPECIAL.put("fiona|holidays", new String[]{"I fly a plane at the weekends. It's a hobby. A very quiet hobby.", "Best holiday I ever had was a three-hour solo flight. No one to talk to."});
    }
    public static String[] special(String id, String topic) { return SPECIAL.get(id + "|" + topic); }
'''

def da(s):
    s = s.replace('    public static final String[] TOPIC_KEYS = TOPICS.keySet().toArray(new String[0]);', '    public static final String[] TOPIC_KEYS = TOPICS.keySet().toArray(new String[0]);\n' + SPECIAL)
    return s
rw('src/pub/Data.java', da)

def so(s):
    s = s.replace('        String line = Util.pick(bank[o > .25 ? 0 : o < -.25 ? 1 : 2]);', '        String[] spc = Data.special(sp.id, topic);\n        String line = spc != null && Util.chance(.6) ? Util.pick(spc) : Util.pick(bank[o > .25 ? 0 : o < -.25 ? 1 : 2]);')
    return s
rw('src/pub/Social.java', so)
