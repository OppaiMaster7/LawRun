# Run: 1940s Germany

**Source:** GuyBordo, [I Beat Minecraft Without Breaking the Laws of 1940's Germany](https://www.youtube.com/watch?v=oEDzhaMs0Iw), uploaded 2026-09-25, 18:25 long.
**Extracted:** 2026-09-26 from the full auto-generated English transcript (456 caption lines, read end to end).

The title says "1940s Germany", but at 00:28 he clarifies the real premise: the
player follows **the laws Jewish people were forced to follow** in Nazi Germany.
The player is the persecuted, never the persecutor. Every design choice below
keeps it that way.

## The rules as he plays them

Six laws, one assumed rule (death), and one win condition. Timestamps are where
he states or clarifies the rule.

| # | Law (his name) | Year he cites | Rule in Minecraft | He breaks it how | Timestamps |
|---|---|---|---|---|---|
| 1 | Decree against Jewish ownership of weapons | 1938 | No item that deals attack damage. That bans every tool: swords, axes, pickaxes, shovels, hoes. He can't even craft them. He mines stone and kills ~700 zombies by hand. TNT is ruled "not a weapon" because it has no attack damage stat | n/a | 00:47–01:50, 09:00–09:22 |
| 2 | Travel restrictions | 1941 | World border around spawn. All resources must come from inside it | n/a | 02:16–02:53 |
| 2b | Travel permit | pre-1941 | Before the ban, permits existed but were unaffordable. He pays **3 diamonds** to remove the border, once, right before the End | n/a | 15:25–16:00 |
| 3 | Curfew | 1940 | Can't be on the Overworld surface at night; must be underground or inside a house. Gives him ~10 minutes of daylight per day. **Breaking it ends the run** | Ends the run | 03:33–04:10, 08:06–08:23 |
| 3b | Curfew in the Nether and End | n/a | Implied: he plans Nether trips as "10 minutes before the curfew law kicks in" and says he'll have "10 minutes to kill [the dragon] because of the curfew law" | Ends the run | 09:50–10:10, 14:23–14:29 |
| 4 | Wealth plundering | n/a | At the start of every Minecraft day (every 20 min), **every item in the inventory is taken**. Items are safe only in **naturally generated chests**, not chests he placed | Items deleted | 04:27–05:07, 06:18–06:35, 09:42–09:46 |
| 4b | Rationing | n/a | At the start of each day he may keep **at most 3 pieces of food** | Extra food deleted | 05:09–05:16 |
| 5 | Restricted business | n/a | No trading with villagers or piglins | n/a | 12:36–12:50 |
| — | Death | n/a | Never stated, but he treats near-deaths as run-ending | Ends the run | 06:48, 11:03, 11:41 |
| Win | Kill the Ender Dragon | n/a | With no weapons he one-cycles it with beds, before nightfall | n/a | 14:21–14:58, 16:59–18:04 |

### Where he bent his own rules

These are **not** part of the mod. The mod enforces the rules as stated.

- **Scouting in spectator mode** (07:40–07:54): he used Flashback replay footage to find a lava pool.
- **Duping** (13:45–14:06): he duplicated ender pearls and blaze rods. He said grinding them legitimately would have taken 30–50 hours.

If the mod proves too hard in playtesting, add assists as clearly labelled
options, never as defaults.

### Stats from his run (for the win/fail screen comparison)

- ~6 hours and ~700 hand-killed zombies (spawner + 22-block drop) for 4 iron (07:25–07:33)
- 1 blaze rod earned legitimately before duping (13:39)
- ~35 wool from the only sheep inside the border (15:11)
- 2+ hours practising the one-cycle offline (14:50)
- 3 diamonds spent on the travel permit (15:54)

## Historical notes for the rule text

The mod's rule text has to be factual. Checked against his claims:

- **Weapons:** the Regulation Against Jews' Possession of Weapons, 11 November 1938. Matches.
- **Emigration ban:** October 1941. Matches.
- **Curfew:** he says 1940, and the mod says 1940 to match the video (Bogi's call). The curfew for Jewish people in Germany actually started in **September 1939** (8 p.m., 9 p.m. in summer) and was still in force in 1940.
- **Exclusion from business:** the decree excluding Jews from economic life, 12 November 1938. Matches.
- **"30% of the German war effort was funded by money stolen from Jewish people"** (04:41): not verified, so the mod does not repeat it.

## How the mod enforces each rule

All six laws are enforced as of 2026-09-26. Every row marked **tested** is
covered by `./gradlew runClientGameTest` (see the README).

| # | Law | What the player gets | How it works | Tested |
|---|---|---|---|---|
| 1 | Weapons ban | Crafting a weapon shows the weapon's own icon in the output slot, renamed **"Rule #1 · Banned weapon"** with the law as its description, and it can't be taken. Picking a weapon up anywhere (chest, ground, mob drop) shows the Rule #1 card, then the run fails | Weapon = the `lawrun:banned_weapons` tag (bows, crossbows), the `weapon` component, or an attack damage modifier (every tool, trident, mace, spear). `CraftingMenuMixin` swaps the crafting result; `SlotMixin` refuses to let it be picked up; inventory, cursor and pocket are scanned every 5 ticks | tested |
| 2 | Curfew | Timer in the top right: "Curfew in 7:32" by day, "Curfew · sunrise in 5:10" plus **Sheltered** or a red **OUTSIDE! Get in: 2.4s** at night. Outside for 3 seconds shows the Rule #2 card, then the run fails | Curfew is Overworld clock 12000–24000 (sunset to sunrise): 10 minutes of day, 10 of night, as in the video. Checked every 10 ticks. Nether and End always count as outside | tested |
| 3 | Confiscation | At sunrise everything carried is taken (armor, offhand, cursor, 2×2 grid, ender chest, items the player dropped). Chests you placed are emptied the next time they're opened or broken; chests the world generated keep their contents. The **safe pocket** keeps one stack of anything except shulker boxes and bundles | Sunrise = the Overworld day number goes up (sleeping counts). A container is "natural" if it still had its loot table when first opened | tested |
| 4 | Rationing | 3 **random** pieces of food survive sunrise (each piece is one ticket) | Drawn without replacement from every food item carried | tested |
| 5 | Exclusion from business | Taking a trade's result or giving a piglin gold shows the Rule #5 card, then the run fails | `MerchantResultSlotMixin` (villagers and wandering traders), `PiglinAiMixin` (thrown or handed gold) | tested |
| 6 | Emigration ban | 256 × 256 world border around spawn. `/lawrun permit` with 3 diamonds (inventory first, then the pocket) removes it for the rest of the run | Overworld border only: in 26.x each dimension has its own border, and portals can't drop you outside the Overworld's | tested |
| — | Death | Dying fails the run with the vanilla death message | `AFTER_DEATH` | not yet |
| Win | Ender Dragon | Dragon death wins for every active runner in the End | Bed kills have no attacker, so it doesn't rely on who killed it | **needs a real bed one-cycle** |

### What counts as shelter

Something must block the sky above your head, and then either:

- **Underground:** 5 or more solid blocks above your head (caves, mines, a tunnel off a shaft), or
- **Sealed in:** a flood fill through the air around you (up to 24 blocks away) never reaches a block under open sky. Walls, roof and **closed** doors, trapdoors and gates seal a room; an open door or a hole does not. A space too big to search counts as sealed.

A bed in a sealed room is fine. Sleeping skips to sunrise, which also means confiscation.

### Decisions made where the video is vague

| Question | Decision | Who |
|---|---|---|
| Border size | 256 × 256 | Bogi |
| Bows and crossbows | Banned | Bogi |
| Crafting a weapon | Output shows the weapon as the Rule #1 card; can't be taken; no fail | Bogi |
| Weapons found elsewhere | Rule #1 card, then fail | Bogi |
| Shelter | Sealed in or 5+ blocks underground | Bogi |
| Food at sunrise | 3 random pieces | Bogi |
| Safe pocket | Added (not in the video); no shulker boxes or bundles | Bogi |
| Nether and End at night | Count as outside | default |
| Grace when caught outside | 3 seconds | default |
| Run started at night | Curfew not enforced that night | default |
| Curfew date on screen | 1940, as in the video | Bogi |
| Trading | Rule #5 card, then fail (not just blocked) | default |

### Known gaps

- **Walking over a dropped weapon still fails the run**, because auto-pickup counts as picking it up. A warning notice now appears whenever one is within 4 blocks, and weapons in chests are marked red.
- Chest boats and chest minecarts you place aren't confiscated yet.
- Items you dropped are only swept at sunrise if their chunk is loaded.
- A thick enough roof (5 solid blocks) with open sides counts as underground.
- In creative mode the safe pocket slot overlaps a hotbar slot. Runs are meant for survival.
- Death has no automated test yet. The dragon win and the ending are tested by calling the win directly; a real bed one-cycle hasn't been played yet.

## Guardrails specific to this run

- The player is always the one persecuted. No enforcer mobs, no Nazi symbols or uniforms, no "officer" NPCs.
- The rule card and fail title state the law and the reason ("Curfew: Outside after dark") in plain words. No jokes on them.
- The cinematics get their drama from the game (the clock, the stamps, the stakes), never from the history.
- The ending closes with four plain lines on black and a link to the US Holocaust Memorial Museum's article on anti-Jewish legislation in prewar Germany. The wording lives in `Germany1940s.java` (`Ending`).
- The start screen opens with one factual sentence of context (see `Germany1940s.java`).
- His duping gag ("so it's pretty historically accurate", 14:00) stays out of the mod and its descriptions.
- Mod pages (Modrinth, CurseForge) must describe it the same way. Expect extra moderation scrutiny on this topic.
