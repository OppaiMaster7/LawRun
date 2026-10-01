package com.bogi.lawrun.gametest;

import java.util.List;
import java.util.Locale;

import com.bogi.lawrun.client.LawRunClient;
import com.bogi.lawrun.client.screen.EndingScreen;
import com.bogi.lawrun.client.screen.LawDetailScreen;
import com.bogi.lawrun.client.screen.LawsScreen;
import com.bogi.lawrun.client.screen.RunOverScreen;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunStatus;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Takes the mod page screenshots: a scripted run in a real, seeded world at
 * 1920x1080, through every feature worth showing. Only runs with
 * {@code ./gradlew runClientGameTest -Pmodpage}; the shots are copied to
 * docs/mod-page/ afterwards.
 */
public class ModPageShots implements FabricClientGameTest {
	public static final String PROPERTY = "lawrun.modpage";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Boolean.getBoolean(PROPERTY)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(minecraft -> {
			minecraft.options.guiScale().set(3);
			minecraft.resizeGui();
		});

		try (TestSingleplayerContext world = context.worldBuilder()
			.setUseConsistentSettings(false)
			.adjustSettings(state -> {
				state.setSeed("1940");
				state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
				state.setAllowCommands(true);
			})
			.create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();
			for (String command : List.of("gamerule advance_time false", "gamerule advance_weather false",
				"gamerule send_command_feedback false", "gamerule log_admin_commands false",
				"weather clear", "difficulty peaceful", "time set 11000")) {
				server.runCommand(command);
			}
			// Flat, open plains give clear views. Make them spawn, so the border centres there too.
			BlockPos plains = server.computeOnServer(s -> {
				var found = s.overworld().findClosestBiome3d(biome -> biome.is(Biomes.PLAINS), BlockPos.ZERO, 4000, 32, 64);
				return found == null ? BlockPos.ZERO : found.getFirst();
			});
			server.runCommand(String.format(Locale.ROOT, "spreadplayers %d %d 0 1 false @a", plains.getX(), plains.getZ()));
			world.getConnection().waitForChunksRender();
			server.runCommand("execute at @p run setworldspawn ~ ~ ~");
			clearGrass(server);
			context.waitTicks(20);
			// A village to the west, where the sun sets.
			server.runCommand("execute at @p run place structure minecraft:village_plains ~-36 ~ ~");
			server.runCommand("execute as @p at @p run tp @s ~ ~ ~ facing ~-40 ~-4 ~");
			world.getConnection().waitForChunksRender();
			context.waitTicks(60);

			// The welcome, over the village at golden hour. Its chat buttons stay in the shot.
			tidy(context);
			server.runCommand("execute as @p run lawrun reset");
			context.waitTicks(45);
			context.takeScreenshot("modpage-04-welcome");

			introAndLaws(context);
			BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
			buildHouse(server);
			context.waitTicks(20);

			curfew(context, server);
			earnSomeStats(server);
			sunrise(context, server);
			craftingAndPocket(context, server);
			borderAndPermit(context, server, home);
			chestAndTrade(context, server);
			victoryAndEnding(context, server, world);
			arrest(context, server, home);
		}
	}

	private static void introAndLaws(ClientGameTestContext context) {
		context.runOnClient(minecraft -> minecraft.player.connection.sendUnattendedCommand("lawrun intro germany_1940s", null));
		context.waitForScreen(LawsScreen.class);
		context.waitTicks(50);
		shot(context, "02-disclaimer");
		context.clickScreenButton("I understand");
		context.waitTicks(190);
		shot(context, "01-intro");
		clickCard(context, 2);
		context.waitForScreen(LawDetailScreen.class);
		context.waitTicks(25);
		shot(context, "03-law-page");
		context.clickScreenButton("Back");
		context.waitForScreen(LawsScreen.class);
		context.clickScreenButton("Begin");
		context.waitTicks(20);
	}

	/** A small spruce house around the player: a window on the village, a door behind, a lantern. */
	private static void buildHouse(TestServerContext server) {
		for (String command : List.of(
			"execute at @p run fill ~-2 ~-1 ~-2 ~2 ~3 ~2 minecraft:spruce_planks hollow",
			"execute at @p run fill ~-2 ~ ~-1 ~-2 ~1 ~1 minecraft:glass",
			"execute at @p run setblock ~2 ~ ~ minecraft:spruce_door[facing=west,half=lower,open=false]",
			"execute at @p run setblock ~2 ~1 ~ minecraft:spruce_door[facing=west,half=upper,open=false]",
			"execute at @p run setblock ~ ~2 ~ minecraft:lantern[hanging=true]",
			"execute at @p run setblock ~1 ~ ~1 minecraft:crafting_table",
			"execute as @p at @p run tp @s ~ ~ ~ facing ~-10 ~0.6 ~")) {
			server.runCommand(command);
		}
	}

	private static void curfew(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("time set 11400");
		context.waitTicks(10);
		tidy(context);
		server.runCommand("time add 600");
		context.waitTicks(24);
		shot(context, "05-sunset");

		server.runCommand("time set 15000");
		context.waitTicks(100);
		LawRunClient.clearNotices();
		tidy(context);
		shot(context, "07-sheltered");

		// Open the door: caught outside. Close it again before the three seconds are up.
		server.runCommand("execute at @p run setblock ~2 ~ ~ minecraft:spruce_door[facing=west,half=lower,open=true]");
		server.runCommand("execute at @p run setblock ~2 ~1 ~ minecraft:spruce_door[facing=west,half=upper,open=true]");
		context.waitTicks(25);
		shot(context, "06-get-inside");
		server.runCommand("execute at @p run setblock ~2 ~ ~ minecraft:spruce_door[facing=west,half=lower,open=false]");
		server.runCommand("execute at @p run setblock ~2 ~1 ~ minecraft:spruce_door[facing=west,half=upper,open=false]");
		context.waitTicks(30);
		check(status(server) == RunStatus.ACTIVE, "the close call must not end the run");
	}

	/** A few real blocks broken by hand and animals punched, so the stat screens have something to count. */
	private static void earnSomeStats(TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			for (int i = 0; i < 7; i++) {
				BlockPos pos = player.blockPosition().above(12).east(i);
				player.level().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
				player.gameMode.destroyBlock(pos);
			}
			for (int i = 0; i < 2; i++) {
				Cow cow = EntityTypes.COW.create(player.level(), EntitySpawnReason.COMMAND);
				cow.setPos(player.position().add(0, 12, i));
				player.level().addFreshEntity(cow);
				cow.hurtServer(player.level(), player.damageSources().playerAttack(player), 1000);
			}
		});
		server.runCommand("kill @e[type=item]");
	}

	private static void sunrise(ClientGameTestContext context, TestServerContext server) {
		for (String command : List.of("give @p bread 6", "give @p apple 4", "give @p oak_log 12", "give @p cobblestone 30",
			"give @p iron_ingot 3", "give @p coal 8", "give @p torch 16", "item replace entity @p armor.chest with iron_chestplate")) {
			server.runCommand(command);
		}
		context.waitTicks(10);
		tidy(context);
		LawRunClient.clearNotices();
		server.runCommand("time add 9000");
		context.waitTicks(14);
		shot(context, "08a-day-card");
		context.waitTicks(30);
		shot(context, "08b-confiscation");
		context.waitTicks(120);
	}

	private static void craftingAndPocket(ClientGameTestContext context, TestServerContext server) {
		tidy(context);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos table = player.blockPosition().offset(1, 0, 1);
			player.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(player.level(), table));
			player.containerMenu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS));
			player.containerMenu.getSlot(5).set(new ItemStack(Items.OAK_PLANKS));
			player.containerMenu.getSlot(8).set(new ItemStack(Items.STICK));
		});
		context.waitTicks(10);
		hoverPanel(context, 176, 166, 124 + 8, 35 + 8);
		context.waitTicks(5);
		shot(context, "09-rule-1");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		server.runCommand("clear @p");

		for (String command : List.of("give @p bread 2", "give @p apple 1", "give @p oak_planks 24", "give @p torch 8")) {
			server.runCommand(command);
		}
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(8);
		hoverPanel(context, 176, 166, 152 + 8, 62 + 8);
		context.waitTicks(5);
		shot(context, "10-safe-pocket");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
	}

	private static void borderAndPermit(ClientGameTestContext context, TestServerContext server, BlockPos home) {
		server.runCommand("time set 3000");
		server.runCommand("clear @p");
		tidy(context);
		LawRunClient.clearNotices();
		server.runCommand(String.format(Locale.ROOT, "spreadplayers %d %d 0 1 false @p", home.getX() + 122, home.getZ()));
		clearGrass(server);
		server.runCommand("execute as @p at @p run tp @s ~ ~ ~ facing ~10 ~1 ~");
		context.waitTicks(20);
		context.waitFor(minecraft -> !LawRunClient.noticesIdle(), 100);
		context.waitTicks(45);
		tidy(context);
		shot(context, "11-border");

		server.runCommand("give @p diamond 3");
		context.waitTicks(25);
		LawRunClient.clearNotices();
		server.runCommand("execute as @p run lawrun permit");
		context.waitTicks(2);
		LawRunClient.clearNotices();
		tidy(context);
		context.waitTicks(24);
		shot(context, "12-permit");
		context.waitTicks(60);
	}

	private static void chestAndTrade(ClientGameTestContext context, TestServerContext server) {
		BlockPos chest = server.computeOnServer(s -> player(s).blockPosition().east(2));
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
			((ChestBlockEntity) level.getBlockEntity(chest)).setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
		});
		use(server, chest);
		context.waitForScreen(ContainerScreen.class);
		server.runOnServer(s -> ((Container) s.overworld().getBlockEntity(chest)).setItem(13, new ItemStack(Items.IRON_SWORD)));
		context.waitTicks(8);
		tidy(context);
		hoverPanel(context, 176, 168, 80 + 8, 36 + 8);
		context.waitTicks(5);
		shot(context, "13-safe-chest");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);

		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			Villager villager = EntityTypes.VILLAGER.create(player.level(), EntitySpawnReason.COMMAND);
			villager.setPos(player.position().add(-2, 0, 0));
			player.level().addFreshEntity(villager);
			villager.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), new ItemStack(Items.BREAD, 6), 10, 1, 0.05F));
			villager.getOffers().add(new MerchantOffer(new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 10, 1, 0.05F));
			villager.setTradingPlayer(player);
			villager.openTradingScreen(player, villager.getDisplayName(), 1);
		});
		context.waitTicks(12);
		tidy(context);
		shot(context, "14-trade");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		server.runCommand("kill @e[type=villager,distance=..5]");
	}

	private static void victoryAndEnding(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext world) {
		server.runCommand("time set 1000");
		server.runCommand("execute in minecraft:the_end run spreadplayers 0 40 0 1 false @p");
		server.runCommand("execute as @p at @p run tp @s ~ ~ ~ facing 0 80 0");
		server.waitFor(s -> !s.getLevel(Level.END).getDragons().isEmpty(), 1200);
		world.getConnection().waitForChunksRender();
		context.waitTicks(80);
		LawRunClient.clearNotices();
		faceDragon(server);
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerLevel end = s.getLevel(Level.END);
			for (EnderDragon dragon : end.getDragons()) {
				dragon.setHealth(1);
				dragon.hurtServer(end, end.damageSources().explosion(null, null), 50);
			}
		});
		// It rises as it dies; keep it in view.
		context.waitTicks(30);
		faceDragon(server);
		context.waitTicks(50);
		shot(context, "17-victory");
		check(status(server) == RunStatus.WON, "the dragon's death should win the run");

		server.runOnServer(s -> player(s).showEndCredits());
		context.waitForScreen(EndingScreen.class);
		context.getInput().pressKey(InputConstants.KEY_SPACE);
		context.waitTicks(150);
		shot(context, "18-record");
		context.getInput().pressKey(InputConstants.KEY_SPACE);
		context.getInput().pressKey(InputConstants.KEY_SPACE);
		context.waitTicks(320);
		shot(context, "19-epilogue");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(40);
		context.clickScreenButton("Continue");
		server.waitFor(s -> player(s).isAlive() && !player(s).isRemoved(), 200);
		context.waitFor(minecraft -> minecraft.gui.screen() == null, 200);
	}

	/** A second, short run that ends the way most do: caught outside after dark. */
	private static void arrest(ClientGameTestContext context, TestServerContext server, BlockPos home) {
		server.runCommand("execute as @p run lawrun reset");
		server.runCommand(String.format(Locale.ROOT, "spreadplayers %d %d 0 1 false @p", home.getX() + 7, home.getZ() + 3));
		server.runCommand("execute as @p run lawrun start germany_1940s");
		context.waitTicks(10);
		for (String command : List.of("give @p bread 4", "give @p oak_log 20", "give @p cobblestone 40")) {
			server.runCommand(command);
		}
		earnSomeStats(server);
		server.runCommand("time add 23000");
		context.waitTicks(160);
		server.runCommand("execute as @p at @p run tp @s ~ ~ ~ facing " + (home.getX() - 40) + " " + home.getY() + " " + home.getZ());
		LawRunClient.clearNotices();
		tidy(context);
		server.runCommand("time add 13000");
		server.waitFor(s -> status(s) == RunStatus.FAILED, 200);
		context.waitTicks(46);
		shot(context, "15-arrest");
		context.waitForScreen(RunOverScreen.class);
		context.waitTicks(120);
		shot(context, "16-run-over");
	}

	private static void faceDragon(TestServerContext server) {
		Vec3 dragon = server.computeOnServer(s -> {
			List<? extends EnderDragon> dragons = s.getLevel(Level.END).getDragons();
			return dragons.isEmpty() ? Vec3.ZERO : dragons.getFirst().position();
		});
		server.runCommand(String.format(Locale.ROOT, "execute as @p at @p run tp @s ~ ~ ~ facing %.1f %.1f %.1f",
			dragon.x, dragon.y, dragon.z));
	}

	private static void use(TestServerContext server, BlockPos pos) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
			player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
		});
	}

	/** Puts the mouse over a point in a centred container panel, for its tooltip. */
	private static void hoverPanel(ClientGameTestContext context, int panelWidth, int panelHeight, int x, int y) {
		double[] cursor = context.computeOnClient(minecraft -> {
			double scale = minecraft.getWindow().getGuiScale();
			int left = (minecraft.getWindow().getGuiScaledWidth() - panelWidth) / 2;
			int top = (minecraft.getWindow().getGuiScaledHeight() - panelHeight) / 2;
			return new double[] {(left + x) * scale, (top + y) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
	}

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

	/** Clears chat, toasts and titles, so a shot shows the mod rather than the log. */
	private static void tidy(ClientGameTestContext context) {
		context.runOnClient(minecraft -> {
			minecraft.gui.hud.getChat().clearMessages(false);
			minecraft.gui.hud.clearTitles();
			minecraft.gui.toastManager().clear();
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		tidy(context);
		context.waitTicks(1);
		context.takeScreenshot("modpage-" + name);
	}

	/** Plains grass hides everything at eye level. */
	private static void clearGrass(TestServerContext server) {
		server.runCommand("execute at @p run fill ~-24 ~-2 ~-24 ~24 ~4 ~24 minecraft:air replace minecraft:short_grass");
		server.runCommand("execute at @p run fill ~-24 ~-2 ~-24 ~24 ~4 ~24 minecraft:air replace minecraft:tall_grass");
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static RunStatus status(TestServerContext server) {
		return server.computeOnServer(ModPageShots::status);
	}

	private static RunStatus status(MinecraftServer server) {
		var state = RunManager.get(player(server));
		return state == null ? null : state.status();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
