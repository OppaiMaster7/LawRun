package com.bogi.lawrun.runs.germany;

import java.util.List;

import com.bogi.lawrun.core.Law;
import com.bogi.lawrun.core.Notice;
import com.bogi.lawrun.core.RunDefinition;

/**
 * GuyBordo's 1940s Germany run (uploaded 2026-09-25). Rule text follows the
 * video; see docs/runs/germany-1940s.md for timestamps and the decisions made
 * where the video is vague.
 */
public final class Germany1940s {
	public static final String ID = "germany_1940s";

	public static final Law WEAPONS = new Law("weapons", "Weapons ban", "November 1938",
		"Jewish people were banned from owning weapons of any kind, blades included.",
		"No swords, tools, bows or crossbows: nothing that deals attack damage. Crafting one is blocked."
			+ " Picking one up ends the run. TNT is allowed.",
		List.of(
			"Mine and fight with your hands. It's slow, but it's legal.",
			"Snowballs, TNT and beds are not weapons: none of them deal attack damage.",
			"A crafting grid will show you a banned recipe, but won't hand it over.",
			"Weapons in chests are marked in red. Leave them there."),
		"He mined stone by hand and punched about 700 zombies at a spawner, dropping them 22 blocks first.",
		47);

	public static final Law CURFEW = new Law("curfew", "Curfew", "1940",
		"Jewish people had to stay indoors after dark.",
		"From sunset to sunrise, stay in a closed building or underground in the Overworld."
			+ " The Nether and the End count as outside. Out for more than 3 seconds and the run ends.",
		List.of(
			"The plate in the top right counts down to sunset.",
			"Shelter means a room with the doors shut, or at least 5 solid blocks over your head.",
			"The Nether and the End count as outside after dark.",
			"Sleeping in a sealed room skips the night, and brings sunrise sooner."),
		"He hid in a dirt hut on his first night and timed every trip to the 10 minutes of daylight.",
		209);

	public static final Law PLUNDER = new Law("plunder", "Confiscation of property", "1938 onward",
		"Jewish property and savings were seized through levies, forced sales and confiscation.",
		"At every sunrise, everything you carry is taken, armor included. Only your safe pocket and chests"
			+ " the world generated are safe; chests you place are emptied too.",
		List.of(
			"Chests the world made are safe: shipwrecks, dungeons, villages, mineshafts.",
			"Chests you place are emptied at sunrise, so don't build a vault.",
			"Your safe pocket keeps one stack, whatever happens."),
		"He threw everything out at dawn and kept his loot in a shipwreck chest and a dungeon chest.",
		267);

	public static final Law RATIONS = new Law("rations", "Rationing", "1939 onward",
		"Food was rationed, and Jewish people were given smaller rations.",
		"At sunrise you are left 3 random pieces of the food you carry.",
		List.of(
			"Only 3 random pieces of food survive each sunrise.",
			"Keep the food you care about in your safe pocket or a world chest."),
		"Three pieces a day. On his big Nether trip, all he had was rotten flesh.",
		309);

	public static final Law BUSINESS = new Law("business", "Exclusion from business", "November 1938",
		"Jewish people were barred from running businesses and trades.",
		"Trading with villagers or wandering traders, or bartering with piglins, ends the run.",
		List.of(
			"Opening a villager's trades is fine. Taking the result is not.",
			"Don't throw gold to piglins or hand it to them. Wearing gold armor is fine."),
		"With no trading, the 12 ender pearls he needed had to come from somewhere else.",
		756);

	public static final Law TRAVEL = new Law("travel", "Emigration ban", "October 1941",
		"Jewish people were forbidden to leave Germany. Before the ban, leaving meant exit taxes and"
			+ " fees most could not pay.",
		"A " + GermanyRules.BORDER_SIZE + " by " + GermanyRules.BORDER_SIZE + " border surrounds spawn."
			+ " Buy a travel permit for " + GermanyRules.PERMIT_PRICE + " diamonds (/lawrun permit) to lift it.",
		List.of(
			"Everything you need has to come from inside the " + GermanyRules.BORDER_SIZE + " by "
				+ GermanyRules.BORDER_SIZE + " area, until you buy a permit.",
			"Carry " + GermanyRules.PERMIT_PRICE + " diamonds and run /lawrun permit to lift the border for good.",
			"The Nether has no border."),
		"He spent the three diamonds he had on a travel permit to reach the stronghold.",
		925);

	/** Ids for {@link #RUN}'s notices, as fired from {@code Notices}. */
	public static final class Notices {
		public static final String BORDER = "border";
		public static final String BORDER_AGAIN = "border_again";
		public static final String DIAMONDS_1 = "diamonds_1";
		public static final String DIAMONDS_2 = "diamonds_2";
		public static final String DIAMONDS_3 = "diamonds_3";
		public static final String DUSK = "dusk";
		public static final String FOOD = "food";
		public static final String VILLAGER = "villager";
		public static final String PIGLIN = "piglin";
		public static final String NETHER = "nether";
		public static final String END = "end";
		public static final String CHEST_PLACED = "chest_placed";
		public static final String WEAPON_GROUND = "weapon_ground";
		public static final String SNOWBALL = "snowball";
		public static final String TNT = "tnt";
		public static final String SPAWNER = "spawner";
		public static final String FALL_KILL = "fall_kill";
		public static final String ROTTEN_FLESH = "rotten_flesh";
		public static final String ENDER_PEARL = "ender_pearl";
		public static final String BLAZE_ROD = "blaze_rod";
		public static final String WORLD_STASH = "world_stash";
		public static final String BED_END = "bed_end";
		public static final String SHEEP = "sheep";

		private Notices() {
		}
	}

	private static Notice law(String id, Law law, String title, String body) {
		return new Notice(id, Notice.Kind.LAW, lawNumber(law), title, body);
	}

	private static Notice video(String id, String title, String body) {
		return new Notice(id, Notice.Kind.VIDEO, 0, title, body);
	}

	/** The same order as the laws in {@link #RUN}, which isn't built yet when the notices are. */
	private static int lawNumber(Law law) {
		return List.of(WEAPONS, CURFEW, PLUNDER, RATIONS, BUSINESS, TRAVEL).indexOf(law) + 1;
	}

	public static final RunDefinition RUN = new RunDefinition(
		ID,
		"1940s Germany",
		"You play under laws the Nazi government imposed on Jewish people in Germany between 1938 and 1941,"
			+ " steps in a persecution that ended in the Holocaust.",
		"LawRun is a fan-made challenge based on GuyBordo's video \"I Beat Minecraft Without Breaking the Laws"
			+ " of 1940's Germany\". It is made for entertainment. The laws in this run were real, and they were used"
			+ " to persecute Jewish people; here they are simplified so they work as game rules. Nothing in this mod"
			+ " is meant to make light of the people who lived under them. It is not a history lesson. To learn what"
			+ " really happened, visit the US Holocaust Memorial Museum (ushmm.org).",
		new RunDefinition.Source(
			"GuyBordo",
			"I Beat Minecraft Without Breaking the Laws of 1940's Germany",
			"https://www.youtube.com/watch?v=oEDzhaMs0Iw"),
		List.of(WEAPONS, CURFEW, PLUNDER, RATIONS, BUSINESS, TRAVEL),
		List.of(
			new RunDefinition.StatLine(Stats.DAYS, "Days survived", ""),
			new RunDefinition.StatLine(Stats.ITEMS_TAKEN, "Items confiscated", ""),
			new RunDefinition.StatLine(Stats.CLOSE_CALLS, "Close calls after curfew", ""),
			new RunDefinition.StatLine(Stats.BLOCKS_BY_HAND, "Blocks mined by hand", ""),
			new RunDefinition.StatLine(Stats.MOBS_BAREHANDED, "Mobs killed barehanded",
				"GuyBordo punched about 700 zombies for 4 iron, over about 6 hours")),
		List.of(
			law(Notices.BORDER, TRAVEL, "The border is closed",
				"Leaving takes a travel permit, and it costs 3 diamonds. You have %s. Once you do, run /lawrun permit."),
			law(Notices.BORDER_AGAIN, TRAVEL, "Still no permit",
				"The border stays shut until you pay. Diamonds: %s."),
			law(Notices.DIAMONDS_1, TRAVEL, "Diamonds 1/3",
				"A travel permit costs 3. Keep them safe: in your pocket or a world chest."),
			law(Notices.DIAMONDS_2, TRAVEL, "Diamonds 2/3", "One more and the border can open."),
			law(Notices.DIAMONDS_3, TRAVEL, "You can afford a travel permit",
				"Run /lawrun permit, or click [Buy travel permit] in chat. Mind the curfew on the other side."),
			law(Notices.DUSK, CURFEW, "Curfew in one minute",
				"Get into a room with the doors shut, or at least 5 blocks underground. Caught outside for 3 seconds"
					+ " and the run ends."),
			law(Notices.FOOD, RATIONS, "Rationing",
				"At sunrise you keep only 3 random pieces of food. Put the rest in your safe pocket (the new inventory"
					+ " slot) or a world chest."),
			law(Notices.VILLAGER, BUSINESS, "Villagers",
				"Looking at their trades is fine. Taking a trade ends the run."),
			law(Notices.PIGLIN, BUSINESS, "Piglins",
				"Don't throw or hand them gold: bartering is trading. Wearing gold armor is fine."),
			law(Notices.NETHER, CURFEW, "The Nether counts as outside",
				"After dark, being here ends the run. Be back in the Overworld within %s."),
			law(Notices.END, CURFEW, "The End counts as outside",
				"The dragon has to fall before sunset: %s left."),
			law(Notices.CHEST_PLACED, PLUNDER, "Chests you place aren't safe",
				"Anything left in a chest you placed is confiscated at sunrise. Only chests the world generated keep"
					+ " things safe."),
			new Notice(Notices.WEAPON_GROUND, Notice.Kind.WARNING, lawNumber(WEAPONS), "Weapon on the ground",
				"Don't walk over it. Picking it up ends the run."),
			video(Notices.SNOWBALL, "Snowballs",
				"Not weapons: no attack damage. GuyBordo took down blazes with these."),
			video(Notices.TNT, "TNT",
				"No attack damage, so not a weapon. GuyBordo blew up snow with it to get snowballs."),
			video(Notices.SPAWNER, "A spawner",
				"GuyBordo farmed a zombie spawner with a 22-block drop. Six hours for four iron."),
			video(Notices.FALL_KILL, "22 blocks",
				"A fall that long is a one-shot. That was GuyBordo's whole zombie farm."),
			video(Notices.ROTTEN_FLESH, "Rotten flesh", "\"The worst food. The worst.\" GuyBordo, in the Nether."),
			video(Notices.ENDER_PEARL, "An ender pearl", "\"Sweet mother of pearl.\" GuyBordo needed twelve."),
			video(Notices.BLAZE_ROD, "A blaze rod",
				"GuyBordo got one the honest way. He needed six... and duped the rest."),
			video(Notices.WORLD_STASH, "A world chest",
				"GuyBordo kept his stash in chests like this one: a shipwreck and a dungeon."),
			video(Notices.BED_END, "A bed in the End",
				"Beds explode here. GuyBordo one-cycled the dragon with them, after two hours of practice."),
			video(Notices.SHEEP, "The only sheep",
				"GuyBordo had one sheep inside his border. 35 wool took forever.")),
		new RunDefinition.Ending(
			"You beat Minecraft without breaking a single law.",
			List.of("GuyBordo duped his ender pearls and blaze rods to finish. You didn't have to."),
			List.of(
				"The laws in this run were real.",
				"Between 1938 and 1941 they took the property, the work and the freedom of Jewish people in Germany.",
				"The people they targeted had no dragon to defeat, and no way to win.",
				"Six million Jews were murdered in the Holocaust."),
			"Learn more: Anti-Jewish Legislation in Prewar Germany (US Holocaust Memorial Museum)",
			"https://encyclopedia.ushmm.org/content/en/article/anti-jewish-legislation-in-prewar-germany"),
		new GermanyRules());

	/** Keys for {@code RunState.stats}. */
	public static final class Stats {
		public static final String DAYS = "days";
		public static final String ITEMS_TAKEN = "items_taken";
		public static final String CLOSE_CALLS = "close_calls";
		public static final String BLOCKS_BY_HAND = "blocks_by_hand";
		public static final String MOBS_BAREHANDED = "mobs_barehanded";

		private Stats() {
		}
	}

	private Germany1940s() {
	}
}
