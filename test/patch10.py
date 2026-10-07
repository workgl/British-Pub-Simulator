def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)

MORE_STORIES = r'''        "I once accidentally ordered forty kilos of sprouts. In July. Don't ask.",
        "My uncle tried to smuggle a ferret through customs in a thermos. Worked, too. Ferret never forgave him.",
        "I got a speeding ticket on a mobility scooter. Twelve miles an hour in a four. I'm proud.",
        "My sister married a man she met in a car park. They've been together thirty years. The car's long gone.",
        "I once won a hamper at the church raffle. It was just tins of mushy peas. Eleven of them.",
        "A fella on the bus once tried to sell me a parrot. I said no. He said the parrot would be offended. It was.",
        "Got stuck in a lift at the Savemore with the area manager. We're friends now. Mostly.",
        "I once judged a village fete's vegetable competition and got threatened by a marrow enthusiast.",
        "Our cat figured out the doorbell. Now it rings it at 3am and gives me a look.",
        "I borrowed a hedge-trimmer from a neighbour in 2011. I've been avoiding him ever since. Hedge is gone."};'''

def da(s):
    s = s.replace('''        "A seagull took my whole pasty in Skegness. Looked me dead in the eye. Never forgiven."};''', '''        "A seagull took my whole pasty in Skegness. Looked me dead in the eye. Never forgiven.",\n''' + MORE_STORIES)
    s = s.replace('''        {"WESTBRIDGE BUS 43 'ARRIVES ON TIME FOR FIRST TIME SINCE 1998'", "Retired driver Kev Bagshaw said, 'Suspicious.'"}};''', '''        {"WESTBRIDGE BUS 43 'ARRIVES ON TIME FOR FIRST TIME SINCE 1998'", "Retired driver Kev Bagshaw said, 'Suspicious.'"},
        {"LOCAL MAN BUILDS FULL-SIZE SHED IN LIVING ROOM", "Wife 'has questions'. Shed 'is very nice'."},
        {"SUPERMARKET INTRODUCES 'SILENT HOUR'; STAFF 'LOVING IT'", "Customers reported feeling 'unnervingly calm'."},
        {"WESTBRIDGE ALLOTMENT SOCIETY IN MARROW 'SCANDAL'", "Judges are 'looking into' the unusually shiny vegetable."},
        {"TOWN CLOCK NOW SHOWS 'ROUGHLY' CORRECT TIME", "A council spokesperson called this 'progress, spiritually'."},
        {"SEAGULL STEALS BACON ROLL FROM MAYOR", "'It was a calculated attack,' said the mayor, who has not eaten a bacon roll since."},
        {"LOCAL DOG OWNER INSISTS PET 'ISN'T LIKE THAT'", "Dog, which is exactly like that, declined to comment."}};''')
    return s
rw('src/pub/Data.java', da)
