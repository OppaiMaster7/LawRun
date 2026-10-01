package com.bogi.lawrun.gametest;

import com.mojang.blaze3d.platform.InputConstants;

import com.bogi.lawrun.client.screen.EndingScreen;
import com.bogi.lawrun.client.screen.LawDetailScreen;
import com.bogi.lawrun.client.screen.LawsScreen;
import com.bogi.lawrun.client.screen.RunOverScreen;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunState;
import com.bogi.lawrun.core.RunStatus;
import com.bogi.lawrun.runs.germany.Germany1940s;
import com.bogi.lawrun.runs.germany.SafePocket;
import com.bogi.lawrun.runs.germany.Weapons;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plays through the 1940s Germany rules in a flat world. Time is frozen in
 * test worlds, so every day and night here is a /time add. Screenshots land
 * in the run directory's screenshots folder.
 */
public class GermanyRunClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (Boolean.getBoolean(ModPageShots.PROPERTY)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("time set 1000");

			// A new world greets the player with a START button; send what clicking it sends.
			context.waitTicks(70);
			context.takeScreenshot("germany-00-welcome");
			context.runOnClient(minecraft -> minecraft.player.connection.sendUnattendedCommand("lawrun intro germany_1940s", null));
			context.waitForScreen(LawsScreen.class);
			context.waitTicks(40);
			context.takeScreenshot("germany-00a-disclaimer");
			context.clickScreenButton("I understand");
			context.waitTicks(40);
			context.takeScreenshot("germany-00b-intro-typing");
			context.waitTicks(130);
			context.takeScreenshot("germany-00c-intro-dealt");
			// Click the Curfew card for its page, then come back.
			clickCard(context, 1);
			context.waitForScreen(LawDetailScreen.class);
			context.waitTicks(20);
			context.takeScreenshot("germany-00d-law-page");
			context.clickScreenButton("Back");
			context.waitForScreen(LawsScreen.class);
			context.clickScreenButton("Begin");
			context.waitTicks(10);
			check(status(server) == RunStatus.ACTIVE, "Begin on the intro should start the run");
			check(server.computeOnServer(s -> s.overworld().getWorldBorder().getSize()) == 256,
				"the border should be 256 wide");
			context.waitTicks(20);
			context.takeScreenshot("germany-01-curfew-timer-day");

			// Mid-run, J and the chat's [Laws] button both bring the laws back for reference.
			context.getInput().pressKey(InputConstants.KEY_J);
			context.waitForScreen(LawsScreen.class);
			context.waitTicks(30);
			context.takeScreenshot("germany-01b-laws-reference");
			context.clickScreenButton("Close");
			context.waitTicks(5);
			server.runCommand("execute as @p run lawrun rules germany_1940s");
			context.waitForScreen(LawsScreen.class);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);

			safePocket(context, server);
			ruleOneCrafting(context, server);
			travelPermit(context, server);
			statsCounting(context, server);
			sunriseAndChests(context, server);
			curfew(context, server);
			foundWeapon(context, server);
			tradeBan(context, server);
			barterBan(context, server);
			victoryAndEnding(context, server);
		}
	}

	private static void startRun(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("execute as @p run lawrun start germany_1940s");
		context.waitTicks(5);
		check(status(server) == RunStatus.ACTIVE, "the run should have started");
	}

	private static void safePocket(ClientGameTestContext context, TestServerContext server) {
		check(!SafePocket.accepts(new ItemStack(Items.SHULKER_BOX)), "shulker boxes don't fit in the pocket");
		check(!SafePocket.accepts(new ItemStack(Items.BUNDLE)), "bundles don't fit in the pocket");
		server.runOnServer(s -> player(s).setAttached(SafePocket.POCKET, new ItemStack(Items.DIAMOND, 2)));

		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(10);
		context.takeScreenshot("germany-02-safe-pocket");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
	}

	/** A wooden sword recipe shows the sword as Rule #1, and clicking it takes nothing. */
	private static void ruleOneCrafting(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos table = player.blockPosition().north(2);
			player.level().setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
			player.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(player.level(), table));
			player.containerMenu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS));
			player.containerMenu.getSlot(5).set(new ItemStack(Items.OAK_PLANKS));
			player.containerMenu.getSlot(8).set(new ItemStack(Items.STICK));
		});
		context.waitTicks(10);
		check(server.computeOnServer(s -> Weapons.isBannedDisplay(player(s).containerMenu.getSlot(0).getItem())),
			"the crafting output should be the Rule #1 card, got: " + server.computeOnServer(s -> {
				var menu = player(s).containerMenu;
				StringBuilder slots = new StringBuilder(menu.getClass().getSimpleName());
				for (int i = 0; i < 10; i++) {
					slots.append(" | ").append(i).append('=').append(menu.getSlot(i).getItem()).append(menu.getSlot(i).getItem().getComponentsPatch());
				}
				return slots.toString();
			}));

		// The output slot sits at (124, 35) in the 176x166 crafting screen.
		double[] cursor = context.computeOnClient(minecraft -> {
			double scale = minecraft.getWindow().getGuiScale();
			int left = (minecraft.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (minecraft.getWindow().getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + 124 + 8) * scale, (top + 35 + 8) * scale};
		});
		TestInput input = context.getInput();
		input.setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(5);
		context.takeScreenshot("germany-03-rule-1-crafting");

		input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
		check(server.computeOnServer(s -> player(s).containerMenu.getCarried().isEmpty()),
			"clicking the Rule #1 card must not pick it up");
		check(status(server) == RunStatus.ACTIVE, "looking at a banned recipe isn't a crime");
		input.pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		check(server.computeOnServer(s -> countItems(player(s).getInventory()) == 3),
			"closing the table returns the 2 planks and the stick, and no sword");
		server.runCommand("clear @p");
	}

	private static void travelPermit(ClientGameTestContext context, TestServerContext server) {
		// Walk up to the border: the permit lesson.
		server.runCommand("execute as @p at @p run tp @p ~118 ~ ~");
		context.waitTicks(30);
		check(server.computeOnServer(s -> RunManager.hasSeen(player(s), Germany1940s.Notices.BORDER)),
			"the border teaches the permit");
		context.waitTicks(20);
		context.takeScreenshot("germany-03a-border-notice");
		server.runCommand("execute as @p at @p run tp @p ~-118 ~ ~");

		server.runCommand("give @p diamond 3");
		context.waitTicks(25);
		check(server.computeOnServer(s -> RunManager.hasSeen(player(s), Germany1940s.Notices.DIAMONDS_3)),
			"three diamonds means a permit is affordable");
		server.runCommand("execute as @p run lawrun permit");
		context.waitTicks(25);
		context.takeScreenshot("germany-03b-permit-stamp");
		check(server.computeOnServer(s -> s.overworld().getWorldBorder().getSize()) > 1_000_000,
			"the permit should lift the border");
		check(server.computeOnServer(s -> countItems(player(s).getInventory()) == 0),
			"the permit costs the 3 diamonds in the inventory");
		check(server.computeOnServer(s -> SafePocket.get(player(s)).getCount() == 2),
			"the pocket diamonds weren't needed");
	}

	/** Breaking a block by hand and punching a zombie to death both count. */
	private static void statsCounting(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos pos = player.blockPosition().south(2);
			player.level().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
			player.gameMode.destroyBlock(pos);
			// Grass pops on touch: that isn't mining.
			BlockPos grass = player.blockPosition().south(3);
			player.level().setBlockAndUpdate(grass, Blocks.SHORT_GRASS.defaultBlockState());
			player.gameMode.destroyBlock(grass);

			Zombie zombie = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.COMMAND);
			zombie.setPos(player.position().add(0, 0, -2));
			player.level().addFreshEntity(zombie);
			zombie.hurtServer(player.level(), player.damageSources().playerAttack(player), 1000);
		});
		context.waitTicks(5);
		check(stat(server, Germany1940s.Stats.BLOCKS_BY_HAND) == 1, "a block broken by hand counts");
		check(stat(server, Germany1940s.Stats.MOBS_BAREHANDED) == 1, "a zombie punched to death counts");
		server.runCommand("kill @e[type=item]");

		// An easter egg from the video, and the red warning on a weapon's tooltip.
		server.runCommand("give @p snowball");
		context.waitTicks(25);
		check(server.computeOnServer(s -> RunManager.hasSeen(player(s), Germany1940s.Notices.SNOWBALL)),
			"the first snowball recalls the video");
		check(context.computeOnClient(minecraft -> new ItemStack(Items.IRON_SWORD)
				.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL).stream()
				.anyMatch(line -> line.getString().contains("Rule #1"))),
			"weapons carry a Rule #1 warning in their tooltip");
		server.runCommand("clear @p");
	}

	/**
	 * Day 0: open a placed chest and a natural one and stash something in each.
	 * Day 1: the inventory is gone bar 3 food and the pocket, the placed chest
	 * is emptied when opened, and the natural one keeps its contents.
	 */
	private static void sunriseAndChests(ClientGameTestContext context, TestServerContext server) {
		BlockPos placed = server.computeOnServer(s -> player(s).blockPosition().east(2));
		BlockPos natural = server.computeOnServer(s -> player(s).blockPosition().west(2));
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			level.setBlockAndUpdate(placed, Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(natural, Blocks.CHEST.defaultBlockState());
			((ChestBlockEntity) level.getBlockEntity(natural)).setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
		});
		openAndClose(context, server, placed, "germany-04d-chest-not-safe");
		openAndClose(context, server, natural, null);
		server.runOnServer(s -> {
			((Container) s.overworld().getBlockEntity(placed)).setItem(26, new ItemStack(Items.COBBLESTONE, 16));
			((Container) s.overworld().getBlockEntity(natural)).setItem(26, new ItemStack(Items.COBBLESTONE, 16));
			((Container) s.overworld().getBlockEntity(natural)).setItem(13, new ItemStack(Items.IRON_SWORD));
		});

		server.runCommand("give @p bread 10");
		server.runCommand("give @p apple 5");
		server.runCommand("give @p cobblestone 20");
		server.runCommand("give @p iron_chestplate");
		server.runCommand("item replace entity @p armor.chest with iron_chestplate");
		server.runCommand("time add 23000");
		context.waitTicks(12);
		context.takeScreenshot("germany-04-day-card");
		context.waitTicks(28);
		context.takeScreenshot("germany-04a-sunrise-flight");
		context.waitTicks(30);
		context.takeScreenshot("germany-04b-sunrise-stamp");
		context.waitTicks(20);
		context.takeScreenshot("germany-04c-sunrise-ration");
		context.waitTicks(80);

		check(server.computeOnServer(s -> {
			Inventory inventory = player(s).getInventory();
			int food = 0;
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				if (inventory.getItem(slot).is(Items.BREAD) || inventory.getItem(slot).is(Items.APPLE)) {
					food += inventory.getItem(slot).getCount();
				}
			}
			return food == 3 && countItems(inventory) == 3;
		}), "sunrise should leave exactly 3 pieces of food and nothing else, armor included");
		check(server.computeOnServer(s -> SafePocket.get(player(s)).getCount() == 2),
			"the safe pocket survives sunrise");
		check(stat(server, Germany1940s.Stats.DAYS) == 1, "one sunrise survived");

		openAndClose(context, server, placed, null);
		openAndClose(context, server, natural, "germany-04e-chest-safe-weapon");
		check(server.computeOnServer(s -> ((Container) s.overworld().getBlockEntity(placed)).isEmpty()),
			"a chest the world didn't generate is emptied at sunrise");
		check(server.computeOnServer(s -> ((Container) s.overworld().getBlockEntity(natural)).getItem(26).getCount() == 16),
			"a world-generated chest keeps what's in it");
	}

	private static void curfew(ClientGameTestContext context, TestServerContext server) {
		// A sealed 3x3x3 stone room around the player.
		server.runCommand("execute at @p run fill ~-2 ~-1 ~-2 ~2 ~3 ~2 stone hollow");
		// 30 seconds before sunset: the edges darken and the plate throbs.
		server.runCommand("time add 11400");
		context.waitTicks(20);
		context.takeScreenshot("germany-05a-curfew-warning");
		// Sunset: bells and the banner.
		server.runCommand("time add 600");
		context.waitTicks(22);
		context.takeScreenshot("germany-05b-sunset-banner");
		context.waitTicks(100);
		check(status(server) == RunStatus.ACTIVE, "a sealed room is shelter");
		context.takeScreenshot("germany-05-curfew-sheltered");

		// Knock a doorway into the wall, step out of line for a second, then seal it again: a close call.
		server.runCommand("execute at @p run fill ~2 ~ ~ ~2 ~1 ~ air");
		context.waitTicks(20);
		context.takeScreenshot("germany-06-curfew-outside");
		server.runCommand("execute at @p run fill ~2 ~ ~ ~2 ~1 ~ stone");
		context.waitTicks(20);
		check(status(server) == RunStatus.ACTIVE, "getting back inside in time keeps the run alive");
		check(stat(server, Germany1940s.Stats.CLOSE_CALLS) == 1, "that was a close call");
		context.takeScreenshot("germany-06b-close-call");

		// And this time stay out.
		server.runCommand("execute at @p run fill ~2 ~ ~ ~2 ~1 ~ air");
		server.waitFor(s -> status(s) == RunStatus.FAILED, 100);
		check(reason(server).startsWith("Curfew"), "should fail for curfew, got: " + reason(server));
		context.waitTicks(20);
		context.takeScreenshot("germany-07-arrest-card");
		context.waitTicks(30);
		context.takeScreenshot("germany-07b-arrest-stamp");
		context.waitForScreen(RunOverScreen.class);
		context.waitTicks(110);
		context.takeScreenshot("germany-08-run-over-screen");
		check(server.computeOnServer(s -> player(s).gameMode() == GameType.SPECTATOR),
			"a failed run ends in spectator mode");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
	}

	private static void foundWeapon(ClientGameTestContext context, TestServerContext server) {
		restartRun(context, server);
		server.runCommand("give @p bow");
		server.waitFor(s -> status(s) == RunStatus.FAILED, 40);
		check(reason(server).startsWith("Weapons ban"), "should fail for the weapons ban, got: " + reason(server));
		context.waitTicks(50);
		context.takeScreenshot("germany-09-rule-1-arrest");
	}

	/** A right click on the block, through the same server code a real click reaches. */
	private static void tradeBan(ClientGameTestContext context, TestServerContext server) {
		restartRun(context, server);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			Villager villager = EntityTypes.VILLAGER.create(player.level(), EntitySpawnReason.COMMAND);
			villager.setPos(player.position().add(1, 0, 0));
			player.level().addFreshEntity(villager);
			villager.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.BREAD), 10, 1, 0.05F));
			villager.setTradingPlayer(player);
			villager.openTradingScreen(player, villager.getDisplayName(), 1);
			player.containerMenu.getSlot(0).set(new ItemStack(Items.EMERALD));
		});
		context.waitTicks(15);
		context.takeScreenshot("germany-10a-trade-stamp");
		server.runOnServer(s -> player(s).containerMenu.clicked(2, 0, ContainerInput.PICKUP, player(s)));
		server.waitFor(s -> status(s) == RunStatus.FAILED, 40);
		check(reason(server).equals("Exclusion from business: Made a trade"), "should fail for trading, got: " + reason(server));
		context.waitTicks(15);
		context.takeScreenshot("germany-10-rule-5-trade");
		server.runCommand("kill @e[type=villager]");
	}

	private static void barterBan(ClientGameTestContext context, TestServerContext server) {
		restartRun(context, server);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			Piglin piglin = EntityTypes.PIGLIN.create(player.level(), EntitySpawnReason.COMMAND);
			piglin.setPos(player.position().add(1, 0, 0));
			player.level().addFreshEntity(piglin);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
			player.interactOn(piglin, InteractionHand.MAIN_HAND, piglin.position());
		});
		server.waitFor(s -> status(s) == RunStatus.FAILED, 40);
		check(reason(server).equals("Exclusion from business: Bartered with a piglin"),
			"should fail for bartering, got: " + reason(server));
		server.runCommand("kill @e[type=piglin]");
	}

	/**
	 * The dragon dies: the victory moment plays in the world. Then the exit
	 * portal's ending: ours must open instead of the End Poem, and Continue
	 * must respawn the player.
	 */
	private static void victoryAndEnding(ClientGameTestContext context, TestServerContext server) {
		restartRun(context, server);
		server.runCommand("time set 6000");
		server.runOnServer(s -> RunManager.win(player(s)));
		context.waitTicks(50);
		context.takeScreenshot("germany-11-victory");
		check(status(server) == RunStatus.WON, "killing the dragon wins the run");

		server.runOnServer(s -> player(s).showEndCredits());
		context.waitForScreen(EndingScreen.class);
		context.waitTicks(80);
		context.takeScreenshot("germany-12-ending-title");
		context.getInput().pressKey(InputConstants.KEY_SPACE);
		context.waitTicks(110);
		context.takeScreenshot("germany-13-ending-record");
		context.getInput().pressKey(InputConstants.KEY_SPACE);
		context.waitTicks(90);
		context.takeScreenshot("germany-14-ending-source");
		context.getInput().pressKey(InputConstants.KEY_SPACE);
		context.waitTicks(300);
		context.takeScreenshot("germany-15-ending-epilogue");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(80);
		context.takeScreenshot("germany-16-ending-credits");
		context.clickScreenButton("Continue");
		context.waitFor(minecraft -> minecraft.gui.screen() == null, 100);
		server.waitFor(s -> player(s).isAlive() && !player(s).isRemoved(), 100);
		check(status(server) == RunStatus.WON, "the won run survives the respawn");
	}

	/** A fresh attempt with empty pockets, so leftovers from the last test can't break a law. */
	private static void restartRun(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("execute as @p run lawrun reset");
		server.runCommand("clear @p");
		startRun(context, server);
	}

	/** Clicks a law's card on the open laws screen. */
	private static void clickCard(ClientGameTestContext context, int index) {
		double[] centre = context.computeOnClient(minecraft -> {
			int[] rect = ((LawsScreen) minecraft.gui.screen()).cardRect(index);
			double scale = minecraft.getWindow().getGuiScale();
			return new double[] {(rect[0] + rect[2] / 2.0) * scale, (rect[1] + rect[3] / 2.0) * scale};
		});
		context.getInput().setCursorPos(centre[0], centre[1]);
		context.waitTicks(3);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}

	/** A right click on the block, through the same server code a real click reaches. */
	private static void openAndClose(ClientGameTestContext context, TestServerContext server, BlockPos pos, String screenshot) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
			player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
		});
		context.waitForScreen(ContainerScreen.class);
		if (screenshot != null) {
			context.waitTicks(5);
			context.takeScreenshot(screenshot);
		}
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static RunStatus status(TestServerContext server) {
		return server.computeOnServer(GermanyRunClientTest::status);
	}

	private static RunStatus status(MinecraftServer server) {
		RunState state = RunManager.get(player(server));
		return state == null ? null : state.status();
	}

	private static long stat(TestServerContext server, String key) {
		return server.computeOnServer(s -> RunManager.get(player(s)).stat(key));
	}

	private static String reason(TestServerContext server) {
		return server.computeOnServer(s -> RunManager.get(player(s)).reason());
	}

	private static int countItems(Inventory inventory) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			count += inventory.getItem(slot).getCount();
		}
		return count;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
