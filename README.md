# 🍺 British Pub Simulator

A chaotic, cosy, surprisingly deep life-and-management sim set in **The Speckled Pigeon**, a slightly worn local pub in the fictional town of Westbridge.
You're the landlord. The pub lives whether you're watching or not: sixteen regulars with their own jobs, moods, memories, grudges, crushes, secrets and opinions
argue about football, gossip, fall in love, fall out, buy rounds, play darts and pool, and occasionally lock themselves in the toilet.

Written in plain **Java (Swing / Java2D)** — no dependencies, no build tool, all sound synthesised in code.

![The pub](docs/screenshot-pub.png)

## Play

Needs a JDK/JRE 17 or newer.

```
build.bat      (Windows)   or   ./build.sh
play.bat                   or   java -jar PubSimulator.jar
```

Or, no jar: `javac -encoding UTF-8 -d out src/pub/*.java && java -cp out pub.Main`

## How to play

* **Move**: click the floor, or WASD / arrows. **Click a person** to select them (double-click to talk). Customers with an amber **!** are waiting at the bar —
  click them to serve instantly, or stand behind the bar and you'll pull pints automatically. Staff serve too.
* **Side card actions**: Talk (real choices with consequences), Serve, Buy a drink, Gossip, Join chat, Calm a row, Darts, Pool, Apologise, Throw out.
* **Click things**: the TV (live fictional football + league table), jukebox, dartboard, pool table, notice board (events), kitchen, fruit machine, spills (mop them!), the pigeon.
* **Hotkeys**: `Space` pause · `1/2/3` speed · `M` manage · `N` newspaper · `T` town · `B` regulars book · `J` jukebox · `F5` save · `Esc` menu.

## What's in it

* **A living pub** — customers decide for themselves when to arrive, what to drink, where to sit, who to talk to, when to play darts or pool, when to
  watch the match, when to go to the loo, and when to leave. Out of the pub they have lives in **Westbridge** (work, shops, the Frog & Trumpet, the stadium…) — see the Town map.
* **Memory & relationships** — arguments, laughs, favours, snubs and gossip are remembered and talked about later. Friendships, rivalries, crushes, couples, break-ups,
  running jokes, secrets that leak (Linda!), regulars who defect to the rival pub or become lifelong locals.
* **Conversations** — topic-driven small talk that agrees, disagrees, misunderstands, interrupts, changes subject, flirts, brags (Gaz, every ~17 minutes) and escalates into rows.
* **Football** — a fictional league with live matches on the pub telly, goal reactions, spilled pints, chants, gloating, people storming out; match days are huge.
* **Playable activities** — real darts (301 with a swaying reticle), real pool physics, a pub quiz with rival teams, karaoke (rhythm game), a jukebox with synthesised songs customers react to.
* **Management** — stock & ordering, price setting (customers notice), hiring/firing staff with personalities, opening hours, upgrades, decor, advertising, bookings (karaoke / live band / beer festival / charity), reputation, popularity and satisfaction, five pub levels, 40+ achievements.
* **30+ random events** — locked in the loo, power cuts, TV dying mid-match, a pigeon heist, a celebrity, proposals, stag dos, a rival pub opening, police misunderstandings, ridiculous bets, a mystery Friday regular with a secret…
* **The Westbridge Chronicle** — a newspaper reporting what actually happened in your pub, plus the odd mystery pigeon story.
* **Saving** — autosaves hourly and nightly (`~/.speckled-pigeon/`).

## Layout

`src/pub/` — `Sim` (clock/state), `Ai` (customer behaviour), `Social` (conversation/memory/gossip), `Football`, `Events`, `Mgmt` (economy), `Nav` (pathfinding),
`Gfx` (renderer), `Audio` (synth), `Games` (darts/pool/quiz/karaoke), `Screens`/`GameWindow`/`UI` (interface).
