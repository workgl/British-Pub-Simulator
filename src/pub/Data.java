package pub;

import java.util.*;

/** Static game data and text banks. */
public final class Data {
    private Data() {}

    // ---------- Drinks ----------
    public static final String[] DRINKS = {"lager", "ale", "cider", "stout", "wine", "spirit", "soft", "snack", "food"};
    public static final String[] DNAME = {"Lager", "Ale", "Cider", "Stout", "Wine", "Spirits", "Soft drink", "Snacks", "Pub meals"};
    public static final double[] DPRICE = {4.8, 4.6, 4.5, 5.0, 5.6, 4.9, 2.6, 1.6, 11.5};
    public static final double[] DCOST = {1.55, 1.45, 1.45, 1.7, 1.7, 1.3, 0.45, 0.45, 3.8};
    public static final double[] DABV = {1.0, 1.1, 1.1, 1.0, 1.4, 2.2, 0, 0, 0};
    public static final int[] DCOL = {0xF4C542, 0xB8651B, 0xD9B13B, 0x2A1A12, 0x9D2A4D, 0xC97B2A, 0x7ECBFF, 0xD9A066, 0xE8C07D};
    public static final int BEVS = 7; // first 7 are drinks
    public static int di(String k) { for (int i = 0; i < DRINKS.length; i++) if (DRINKS[i].equals(k)) return i; return 0; }

    // ---------- Calendar ----------
    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    public static final String[] MONTHS = {"September", "October", "November", "December", "January", "February", "March", "April", "May", "June", "July", "August"};
    public static final String[] SEASONS = {"Autumn", "Winter", "Spring", "Summer"};

    // ---------- Football ----------
    public static final String[] TEAM_ID = {"ROV", "NOR", "ASH", "BLK", "KNG", "RED", "SUN", "HAR"};
    public static final String[] TEAM = {"Westbridge Rovers", "Northcester City", "Ashford United", "Blackmoor Athletic", "Kingsfield Town", "Redcliffe Wanderers", "Sunderby Rangers", "Harlow Albion"};
    public static final String[] TEAM_SHORT = {"Rovers", "City", "United", "Athletic", "Town", "Wanderers", "Rangers", "Albion"};
    public static final int[] TEAM_STR = {62, 78, 82, 66, 60, 70, 64, 58};
    public static final int[] TEAM_COL = {0xC62828, 0x4FC3F7, 0xD32F2F, 0x212121, 0x1565C0, 0x8E24AA, 0x1B5E20, 0xF9A825};
    public static int ti(String id) { for (int i = 0; i < TEAM_ID.length; i++) if (TEAM_ID[i].equals(id)) return i; return -1; }
    public static final String[] PLAYERS = {"Jonno Pickles", "Dwayne Fothergill", "Mikey Strutt", "Big Ade", "Callum Bridge", "Tomasz Wojcik", "Reece Hollis", "Danny Kerrigan", "Ozzy Brannigan", "Fabio Pellow", "Stevie Marsh", "Ryan Toogood"};

    // ---------- Topics: {pos, neg, neu}. Placeholders {team} {other} ----------
    public static final Map<String, String[][]> TOPICS = new LinkedHashMap<>();
    static {
        TOPICS.put("football", new String[][]{
            {"{team} are going all the way this season. I can feel it in my bones.", "That goal on Saturday? Poetry. Pure poetry, {other}.", "Our midfield is the best in the division and I'll hear no arguments.", "Pie, pint, and {team} in front. That's heaven, that is."},
            {"{team} couldn't score in a brothel. Sorry, {other}, but it's true.", "That referee should be banned. From football. From Westbridge.", "The manager's got to go. Said it in August, saying it again.", "I'd stop watching but then who'd be miserable on Saturdays?"},
            {"Who's on the telly this weekend?", "Fantasy league's destroyed me again this week.", "What time's kick-off, anyway?"}});
        TOPICS.put("work", new String[][]{
            {"Got a good one on tomorrow. Actual interesting work, for once.", "Boss said I'm doing well. I nearly fainted.", "Honestly? I love my job. Don't tell anyone."},
            {"My boss is a nightmare. Emails at 9pm. NINE PM.", "If one more person says 'per my last email' I'm moving to a cabin.", "Work's been mental. Everything's on fire. Metaphorically. Mostly."},
            {"Busy week. Same as every week.", "They're bringing in 'hot desking'. Whatever that is."}});
        TOPICS.put("relationships", new String[][]{
            {"Honestly, I think I'm in love. Or it's the pie.", "Date went brilliantly. They laughed at my joke. Twice.", "Been together thirty years. Best thirty years. Mostly."},
            {"Dating apps are a hellscape, {other}.", "Another ghosting. Starting to think it's me.", "My ex keeps posting about their new partner. Every. Day."},
            {"Anyone seeing anyone? Purely for information.", "What's a normal amount of texts per day?"}});
        TOPICS.put("weather", new String[][]{
            {"Lovely day. Proper British summer. Seventeen degrees.", "Bit of sun on the face. Makes you feel human."},
            {"Rain again! Like living in a gutter.", "Freezing out there. My fingers have gone.", "Cold, wet, grey. Classic Westbridge."},
            {"Is it me or has the weather gone mad?", "They say snow's coming. They always say that."}});
        TOPICS.put("money", new String[][]{
            {"Bit of overtime came through. I'm basically a millionaire.", "Got a refund from the council. First time in history."},
            {"Everything costs a fortune. Look at the price of cheese.", "Gas bill came in. I had to sit down.", "Money doesn't grow on trees, {other}. I've checked."},
            {"Anyone know a good accountant?", "Is it worth switching energy supplier, d'you think?"}});
        TOPICS.put("holidays", new String[][]{
            {"Off to Spain in March. Two weeks of nothing.", "Booked Cornwall. Pasties for breakfast. Heaven.", "Camping this summer. Proper camping. With a kettle."},
            {"Never again with that airline. Charged me for breathing.", "Last holiday I got sunburnt in places I won't discuss.", "Holidays are just moving your stress somewhere warmer."},
            {"Where's good for a long weekend?", "Anyone been to Wales recently?"}});
        TOPICS.put("cars", new String[][]{
            {"New motor! Heated seats. Never getting out.", "Passed the MOT first time. Mechanic nearly cried."},
            {"Car's making a noise. A new noise. A meaningful noise.", "Parking in this town is a crime. I've got the fines to prove it.", "Petrol's through the roof."},
            {"Anyone know a decent garage?", "Diesel or petrol these days? I've lost track."}});
        TOPICS.put("music", new String[][]{
            {"Saw a band at the Corn Exchange. Absolutely incredible.", "They don't make songs like they used to. Seriously.", "I could listen to that jukebox all night."},
            {"That song's been on loop for three weeks and I'm losing it.", "Modern music is just noises with a beat.", "Who keeps putting the same song on?"},
            {"What's everyone listening to lately?", "Anyone going to the festival this year?"}});
        TOPICS.put("politics", new String[][]{
            {"Now, if I ran the country, you'd see changes, {other}.", "I've got a solution to the entire housing crisis. Hear me out.", "Politicians? All mad. Except my ideas, which are good."},
            {"Don't get me started on the council. Don't. I'll start.", "Gave up voting. They're all the same.", "Bins collected every three weeks now. THREE."},
            {"Did you see the news today?", "Feels like there's always an election on."}});
        TOPICS.put("television", new String[][]{
            {"Binged the entire series in a weekend. Zero regrets.", "Best thing on telly since forever, {other}. Seriously.", "Love a bit of quiz telly. Teaches you things."},
            {"Four hundred channels and nothing on.", "They've ruined it. That show used to be good.", "Reality TV is rotting the nation's brain."},
            {"Anyone watching that new drama?", "Does anyone actually understand the plot?"}});
        TOPICS.put("food", new String[][]{
            {"Best chip butty I've ever had was from Brenda's caff. Fight me.", "Sunday roast is the pinnacle of civilisation.", "Nothing beats a proper pie."},
            {"Tried that new vegan place. Still hungry. And angry.", "Pineapple on pizza is a crime.", "Whoever decided Savemore's portion sizes should apologise."},
            {"What's everyone having for tea?", "Have you tried the scampi fries? Be honest."}});
        TOPICS.put("pets", new String[][]{
            {"My dog is the best listener I've ever met.", "The cat has learned to open the fridge. We're doomed."},
            {"The neighbour's dog barks all night.", "Foxes in the bins again."},
            {"Anyone seen a lost ginger cat?", "Would you trust a parrot with your secrets?"}});
        TOPICS.put("health", new String[][]{
            {"Doing Dry January. Day four. Changed man. Shut up, I know.", "Started running. Only to the bus stop. Baby steps."},
            {"Back's gone again. I just sneezed.", "Doctor said cut down on salt. I said that's a lie."},
            {"Anyone know a good physio?", "Is it normal for knees to sound like that?"}});
        TOPICS.put("pub", new String[][]{
            {"I like it here. It's proper, this place.", "Best pint in Westbridge, no question.", "Good atmosphere tonight, innit?"},
            {"Bit sticky underfoot, isn't it?", "Prices have gone up again. Ridiculous.", "Place could do with a lick of paint, frankly."},
            {"Is that a new beer mat?", "The pigeon's been in again, apparently."}});
    }
    public static final String[] TOPIC_KEYS = TOPICS.keySet().toArray(new String[0]);

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


    public static final String[] STORIES = {
        "My cousin got stuck in a revolving door at Debenhams for three hours. They brought him sandwiches.",
        "I once got chased by a swan down the A40. Doing forty. The swan, not me.",
        "My mate Gary won a tractor in a raffle. Nobody knows where he keeps it.",
        "I accidentally joined a Morris dancing troupe in 2009. Still don't know how. Bells, mostly.",
        "Fell asleep on the 43 and woke up in Wales. Had a lovely day, actually.",
        "I once sold a lawnmower to a man who didn't have a lawn. Best salesman in Westbridge.",
        "My nan once beat a burglar with a frozen trout. Never let go of it.",
        "I got mistaken for a famous chef at Savemore. Signed forty autographs. Lied about the recipe.",
        "Me and my brother built a boat in the garden. Lived in Staffordshire. No water for miles.",
        "I found a ten-pound note in a Greggs sausage roll. Never told Greggs.",
        "I once got locked in a B&Q overnight. Best sleep of my life. Bed department.",
        "I'm banned from a bowling alley in Rhyl. For reasons I can't legally discuss.",
        "I once sat next to someone off the telly on a train. Told him I'd never heard of him. He was gutted.",
        "A seagull took my whole pasty in Skegness. Looked me dead in the eye. Never forgiven.",
        "I once accidentally ordered forty kilos of sprouts. In July. Don't ask.",
        "My uncle tried to smuggle a ferret through customs in a thermos. Worked, too. Ferret never forgave him.",
        "I got a speeding ticket on a mobility scooter. Twelve miles an hour in a four. I'm proud.",
        "My sister married a man she met in a car park. They've been together thirty years. The car's long gone.",
        "I once won a hamper at the church raffle. It was just tins of mushy peas. Eleven of them.",
        "A fella on the bus once tried to sell me a parrot. I said no. He said the parrot would be offended. It was.",
        "Got stuck in a lift at the Savemore with the area manager. We're friends now. Mostly.",
        "I once judged a village fete's vegetable competition and got threatened by a marrow enthusiast.",
        "Our cat figured out the doorbell. Now it rings it at 3am and gives me a look.",
        "I borrowed a hedge-trimmer from a neighbour in 2011. I've been avoiding him ever since. Hedge is gone."};

    public static final String[][] MISHEAR = {
        {"I've just bought a new Vauxhall.", "A new vacuum? Ooh, a Dyson?"},
        {"I'm off to Malaga next week.", "Malarkey? What sort of malarkey?"},
        {"Got my MOT done today.", "Got your MOTTO done? What's it say?"},
        {"We've got a new boiler.", "A new BOWLER? Like a hat? Lovely."},
        {"I fancy a pasty.", "A party? Who's invited? I'm not invited, am I."},
        {"Is that a Fiat outside?", "A flat outside? Did you check the tyres?"},
        {"We're having lasagne for tea.", "Lazy knees? See a physio."},
        {"I've booked Barcelona.", "Barking? You've booked a barking? Is it a dog show?"},
        {"They said sunny Thursday.", "Sonny Thursday? Who's Sonny?"},
        {"Did you hear about the council tax?", "The counsel tax? Who's been in court?"},
        {"My brother's a plumber.", "A plumber? Did you say a PLUMBER or a plum-grower? Big difference."},
        {"I'm learning Spanish.", "Learning to be a spaniel? Bit late in life, mate."}};

    public static final String[] INTERRUPT = {
        "Sorry to butt in, but that's rubbish.", "Hang on, hang on — who's this about?", "Oi! That's not what I heard!",
        "Can I just say something?", "That's not how it happened and you know it.", "Are we talking about Saturday? Because I was THERE.",
        "Sorry, wasn't listening. Go again?", "Is this the one about the... no. Never mind."};

    public static final String[] R_AGREE = {"Too right.", "Couldn't agree more, {n}.", "Exactly what I said!", "Nail on the head.", "Hear, hear.", "Well said."};
    public static final String[] R_DISAGREE = {"Oh, come off it, {n}.", "Daftest thing I've heard today.", "I'm sorry, {n}, but you're wrong.", "Absolute nonsense.", "Have you been drinking? Don't answer that."};
    public static final String[] R_LAUGH = {"Ha! Brilliant.", "Stop it, I'm crying.", "Classic {n}.", "You're a nightmare, you.", "Oh that's good."};
    public static final String[] R_ANNOYED = {"Not this again.", "Can we talk about literally anything else?", "I came here to relax, {n}.", "Here we go..."};
    public static final String[] R_HEATED = {"Don't you start with me, {n}!", "Say that again. Go on.", "You know NOTHING about it!", "Are you having a laugh?!"};
    public static final String[] R_SHOUT = {"OI! SIT DOWN!", "I'll tell you where to stick it, {n}!", "YOU'RE TALKING RUBBISH!", "I've HAD enough of you!"};
    public static final String[] R_CALM = {"Alright, alright. Let's not do this.", "Sorry, got carried away.", "Pint? My treat.", "Forget it. Water under the bridge."};
    public static final String[] R_BORED = {"Mm.", "Right.", "Is that so.", "Fascinating.", "Yeah... yeah."};
    public static final String[] R_FLIRT = {"You're looking well tonight, {n}.", "Is that a new shirt? Suits you.", "Can I get you a drink? Not for any reason.", "You've got a lovely laugh, {n}."};
    public static final String[] R_FLIRT_OK = {"Ooh, stop it. Don't stop.", "You're not so bad yourself.", "Go on then. One drink."};
    public static final String[] R_FLIRT_NO = {"Er... thanks. I'm just going to... be over there.", "You're lovely, {n}, but no.", "Is that the beer talking?"};
    public static final String[] CHANGE_TOPIC = {"Anyway. Did you see...", "Right, on a different note...", "Totally unrelated, but...", "Anyway, changing the subject..."};

    // ---------- News ----------
    public static final String[][] NEWS_FILLER = {
        {"MYSTERY PIGEON CONTINUES TO VISIT LOCAL PUB", "Residents baffled as bird attends for another week. 'He seems to like the scampi fries,' said one witness."},
        {"COUNCIL PROMISES TO LOOK INTO POTHOLE 'SOON-ISH'", "The pothole on Station Road, now 14 months old, has reportedly developed its own ecosystem."},
        {"MAN CLAIMS HE COULD HAVE PLAYED FOR ARSENAL", "Local pub customer insists the trial went 'really well, probably'. Club unavailable for comment."},
        {"SAVEMORE INTRODUCES SELF-SERVICE TILLS; PEOPLE STILL CROSS", "Staff report an 'unexpected assistance' queue forming at the machines."},
        {"VICTORIA PARK DUCK REPORTED 'WAY TOO CONFIDENT'", "Park wardens advise visitors not to make eye contact with Gerald."},
        {"BIN COLLECTION MOVES TO EVERY THREE WEEKS", "Residents describe the situation as 'ripe'."},
        {"WESTBRIDGE ROVERS 'CONFIDENT' AHEAD OF SATURDAY", "The manager said, 'We'll give it 110%' — a figure experts say is mathematically unavailable."},
        {"KEBABYLON VOTED BEST TAKEAWAY FOR THIRD YEAR", "Owner Mo Rahman said he was 'humbled, and a bit tired'."},
        {"BRENDA'S CAFF CHIP BUTTY HAILED 'LIFE-CHANGING'", "Local man claims to have cried. Brenda said 'don't tell anyone'."},
        {"TRAIN TO NORTHCESTER 'ONLY 12 MINUTES LATE'", "Commuters celebrated with a small round of applause on platform two."},
        {"LOCAL WOMAN'S TOMATO WEIGHS AS MUCH AS A SMALL CAT", "Judges at the Allotment Show described it as 'alarming'."},
        {"POLICE ASK PUBLIC TO STOP CALLING ABOUT 'SUSPICIOUS SEAGULL'", "Officers say the seagull is 'known to us'."},
        {"NEW ROUNDABOUT ALREADY HATED BY EVERYONE", "Consultation period will begin shortly after the roundabout is built."},
        {"FROG & TRUMPET INTRODUCES 'ARTISAN CRISPS'", "Prices described as 'brave'. Crisps described as 'crisps'."},
        {"VICAR SPOTTED CELEBRATING GOAL 'IN A NON-CHURCH MANNER'", "The Reverend later apologised to a nearby pensioner and a stained-glass window."},
        {"DOG THAT BARKS AT POSTMAN NOW BARKS AT POSTMAN'S CAR", "Experts say it's 'progress'."},
        {"SUDDEN SPIKE IN SALE OF FLASKS AMID COLD SNAP", "Retailer 'baffled but delighted'."},
        {"WESTBRIDGE BUS 43 'ARRIVES ON TIME FOR FIRST TIME SINCE 1998'", "Retired driver Kev Bagshaw said, 'Suspicious.'"},
        {"LOCAL MAN BUILDS FULL-SIZE SHED IN LIVING ROOM", "Wife 'has questions'. Shed 'is very nice'."},
        {"SUPERMARKET INTRODUCES 'SILENT HOUR'; STAFF 'LOVING IT'", "Customers reported feeling 'unnervingly calm'."},
        {"WESTBRIDGE ALLOTMENT SOCIETY IN MARROW 'SCANDAL'", "Judges are 'looking into' the unusually shiny vegetable."},
        {"TOWN CLOCK NOW SHOWS 'ROUGHLY' CORRECT TIME", "A council spokesperson called this 'progress, spiritually'."},
        {"SEAGULL STEALS BACON ROLL FROM MAYOR", "'It was a calculated attack,' said the mayor, who has not eaten a bacon roll since."},
        {"LOCAL DOG OWNER INSISTS PET 'ISN'T LIKE THAT'", "Dog, which is exactly like that, declined to comment."}};

    // ---------- Quiz ----------
    public static final Object[][] QUIZ = {
        {"What is the capital of Scotland?", new String[]{"Glasgow", "Edinburgh", "Aberdeen", "Dundee"}, 1},
        {"Which river runs through London?", new String[]{"Severn", "Mersey", "Thames", "Trent"}, 2},
        {"In which year was the Great Fire of London?", new String[]{"1666", "1588", "1815", "1745"}, 0},
        {"Who wrote 'Pride and Prejudice'?", new String[]{"Charlotte Bronte", "Jane Austen", "George Eliot", "Mary Shelley"}, 1},
        {"Cockney rhyming slang: 'apples and pears' means...?", new String[]{"Stairs", "Chairs", "Prayers", "Hairs"}, 0},
        {"What is the highest mountain in the UK?", new String[]{"Snowdon", "Scafell Pike", "Ben Nevis", "Cairn Gorm"}, 2},
        {"What is the maximum score with three darts?", new String[]{"160", "170", "180", "200"}, 2},
        {"How many sides does a 50p coin have?", new String[]{"5", "6", "7", "8"}, 2},
        {"What is the longest river in the UK?", new String[]{"Thames", "Severn", "Tyne", "Clyde"}, 1},
        {"Who painted the Mona Lisa?", new String[]{"Michelangelo", "Raphael", "Leonardo da Vinci", "Botticelli"}, 2},
        {"Which sport is played at Wimbledon?", new String[]{"Cricket", "Tennis", "Golf", "Croquet"}, 1},
        {"A group of owls is called a...?", new String[]{"Parliament", "Murder", "Flock", "Clutch"}, 0},
        {"Which planet is the Red Planet?", new String[]{"Venus", "Jupiter", "Mars", "Mercury"}, 2},
        {"How many points is a try worth in rugby union?", new String[]{"3", "4", "5", "7"}, 2},
        {"Who lives at 221B Baker Street?", new String[]{"Poirot", "Sherlock Holmes", "Miss Marple", "James Bond"}, 1},
        {"What is the chemical symbol for gold?", new String[]{"Go", "Gd", "Au", "Ag"}, 2},
        {"Which city is home to the Cavern Club?", new String[]{"Manchester", "Liverpool", "Leeds", "Bristol"}, 1},
        {"In what year did England win the men's World Cup?", new String[]{"1958", "1966", "1970", "1990"}, 1},
        {"What is the most expensive spice by weight?", new String[]{"Vanilla", "Cardamom", "Saffron", "Cinnamon"}, 2},
        {"How many pints are in a gallon?", new String[]{"6", "8", "10", "12"}, 1},
        {"Who is the patron saint of England?", new String[]{"St Andrew", "St David", "St Patrick", "St George"}, 3},
        {"Traditional Cornish pasty filling?", new String[]{"Beef, potato, swede, onion", "Chicken and leek", "Cheese and ham", "Lamb and mint"}, 0},
        {"Most expensive property on the UK Monopoly board?", new String[]{"Park Lane", "Mayfair", "Bond Street", "Old Kent Road"}, 1},
        {"Largest planet in the solar system?", new String[]{"Saturn", "Neptune", "Jupiter", "Uranus"}, 2},
        {"What do Americans call crisps?", new String[]{"Chips", "Fries", "Snaps", "Wafers"}, 0},
        {"Animal on the flag of Wales?", new String[]{"Lion", "Dragon", "Eagle", "Stag"}, 1},
        {"How many strings on a standard violin?", new String[]{"3", "4", "5", "6"}, 1},
        {"What is the capital of Wales?", new String[]{"Swansea", "Newport", "Cardiff", "Wrexham"}, 2},
        {"Bank of England is on which street?", new String[]{"Threadneedle Street", "Oxford Street", "Fleet Street", "Baker Street"}, 0},
        {"LOCAL: What is this pub called?", new String[]{"The Speckled Pigeon", "The Dog & Duck", "The Red Lion", "The Pigeon Hole"}, 0},
        {"LOCAL: Who claims to have played semi-pro football?", new String[]{"Dave", "Gaz", "Steve", "Owen"}, 1},
        {"LOCAL: Which team plays at Westbridge's stadium?", new String[]{"Westbridge Rovers", "Ashford United", "Harlow Albion", "Sunderby Rangers"}, 0}};
    public static final String[] QUIZ_TEAMS = {"Quiz Team Aguilera", "Les Quizerables", "Hot Fuzz Busters", "The Quizzy Rascals", "Quiz Pro Quo", "Universally Challenged", "Agatha Quiztie", "Bill & Quiz Adventure"};

    // ---------- Songs (title, artist, genre, bpm, root midi) ----------
    public static final String[][] SONGS = {
        {"Chip Shop Romance", "The Gravy Boats", "britpop", "118", "57"},
        {"Sweet Carolynn", "Neil Diamante", "singalong", "126", "55"},
        {"Dancing in the Kitchen", "Synth Sisters", "synthpop", "112", "60"},
        {"Rovers Till I Die", "The Terrace Choir", "chant", "104", "52"},
        {"Dirty Pint Blues", "Muddy Puddles", "blues", "88", "52"},
        {"Last Orders", "Marianne Faithless", "ballad", "72", "57"},
        {"Northern Lights Up", "DJ Pigeon", "dance", "132", "57"},
        {"My Nan's Gone Disco", "Kenny & The Slippers", "disco", "120", "55"},
        {"Fish & Chips & You", "The Cobbles", "folk", "100", "50"},
        {"Rainy Tuesday in Westbridge", "Mild Interest", "indie", "128", "59"},
        {"Whisky Soda Cha-Cha", "Frank Sinatrash", "swing", "108", "53"},
        {"Thunder Pigeon", "Leather Gnome", "metal", "150", "47"}};
    public static final String[] GENRES = {"britpop", "singalong", "synthpop", "chant", "blues", "ballad", "dance", "disco", "folk", "indie", "swing", "metal"};

    public static final String[] STRANGER_FIRST = {"Mick", "Paula", "Wendy", "Raj", "Tom", "Beth", "Nigel", "Chloe", "Ian", "Kayleigh", "Barry", "Zainab", "Ollie", "Mandy", "Craig", "Hannah", "Pete", "Sunita", "Rob", "Gemma", "Terry", "Lucy", "Darren", "Ffion", "Malcolm", "Tasha"};
    public static final String[] STRANGER_LAST = {"Ashworth", "Patel", "Dunn", "Hughes", "Mitchell", "Okoye", "Bell", "Fox", "Gill", "Marsh", "Doyle", "Khan", "Rudd", "Spence", "Lowe", "Barnes"};
    public static final String[] STRANGER_JOB = {"van driver", "dental receptionist", "warehouse operative", "student", "estate agent", "joiner", "teaching assistant", "chef", "call-centre worker", "bus driver", "bookkeeper", "dog walker", "delivery rider", "pharmacist"};

    public static final String[] DAYTIME_TV = {"Homes Under the Gavel", "Bargain Hunters", "Antiques Roadshed", "Cooking with Gordon Plimsoll", "Dusty Attic", "The Chase Is On", "Countdown to Lunch"};
}
