package com.bogi.lawrun.runs.germany;

import java.util.Set;

import com.bogi.lawrun.LawRun;
import com.bogi.lawrun.core.RunHooks;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.net.PermitPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

/** Enforces the 1940s Germany laws. The mixins in {@code com.bogi.lawrun.mixin} call in here too. */
public final class GermanyRules implements RunHooks {
	public static final int BORDER_SIZE = 256;
	public static final int PERMIT_PRICE = 3;
	private static final int WEAPON_CHECK_EVERY = 5;

	static final AttachmentType<GermanyState> STATE = AttachmentRegistry.create(LawRun.id("germany"),
		builder -> builder.persistent(GermanyState.CODEC).copyOnDeath());

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player instanceof ServerPlayer serverPlayer && isOn(serverPlayer)) {
				Plunder.checkContainer(serverPlayer, level, hit.getBlockPos(), true);
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer && isOn(serverPlayer)) {
				Plunder.checkContainer(serverPlayer, level, pos, false);
			}
			return true;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			// Real mining only: not grass and flowers that pop on touch, not leaves, not creative.
			if (player instanceof ServerPlayer serverPlayer && isOn(serverPlayer)
				&& !serverPlayer.isCreative()
				&& state.getDestroySpeed(level, pos) > 0
				&& !state.is(BlockTags.LEAVES)) {
				RunManager.addStat(serverPlayer, Germany1940s.Stats.BLOCKS_BY_HAND, 1);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			// A punch: the player is both the attacker and the thing that hit.
			if (!(entity instanceof Player)
				&& source.getEntity() instanceof ServerPlayer player
				&& source.getDirectEntity() == player
				&& isOn(player)) {
				RunManager.addStat(player, Germany1940s.Stats.MOBS_BAREHANDED, 1);
			}
		});
		Notices.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> registerCommand(dispatcher));
	}

	/** Diamonds toward the permit: the inventory and the safe pocket. */
	static int diamonds(ServerPlayer player) {
		int count = player.getInventory().clearOrCountMatchingItems(
			stack -> stack.is(Items.DIAMOND), true, 0, player.inventoryMenu.getCraftSlots());
		ItemStack pocket = SafePocket.get(player);
		return count + (pocket.is(Items.DIAMOND) ? pocket.getCount() : 0);
	}

	/** True on both sides while this player is on the 1940s Germany run. */
	public static boolean isOn(Player player) {
		return RunManager.isActive(player, Germany1940s.ID);
	}

	@Override
	public void start(ServerPlayer player) {
		player.setAttached(STATE, GermanyState.begin(Curfew.clock(player.level().getServer())));
		closeBorder(player);
	}

	@Override
	public void tick(ServerPlayer player) {
		if (!player.isAlive()) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		int serverTick = server.getTickCount();
		if (serverTick % WEAPON_CHECK_EVERY == 0) {
			Weapons.check(player);
			if (!isOn(player)) {
				return;
			}
		}
		GermanyState state = player.getAttachedOrGet(STATE, () -> GermanyState.begin(Curfew.clock(server)));
		state = Plunder.checkSunrise(player, state);
		Curfew.tick(player, state, serverTick);
		if (isOn(player)) {
			Notices.tick(player, state, serverTick);
		}
	}

	@Override
	public void end(ServerPlayer player) {
		Curfew.forget(player);
		SafePocket.empty(player);
	}

	@Override
	public void reset(ServerPlayer player) {
		player.removeAttached(STATE);
		openBorder(player.level().getServer());
	}

	/** Called by {@code MerchantResultSlotMixin} and {@code PiglinAiMixin}. */
	public static void breakBusinessLaw(ServerPlayer player, String reason) {
		RunManager.fail(player, Germany1940s.BUSINESS, reason);
	}

	private static void closeBorder(ServerPlayer player) {
		ServerLevel overworld = player.level().getServer().overworld();
		BlockPos spawn = overworld.getRespawnData().pos();
		WorldBorder border = overworld.getWorldBorder();
		border.setCenter(spawn.getX() + 0.5, spawn.getZ() + 0.5);
		border.setSize(BORDER_SIZE);

		if (player.level() == overworld && !border.isWithinBounds(player.blockPosition())) {
			int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
			player.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, Set.of(),
				player.getYRot(), player.getXRot(), true);
		}
	}

	private static void openBorder(MinecraftServer server) {
		server.overworld().getWorldBorder().setSize(WorldBorder.MAX_SIZE);
	}

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("lawrun")
			.then(Commands.literal("permit").executes(GermanyRules::buyPermit)));
	}

	private static int buyPermit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		GermanyState state = player.getAttached(STATE);
		if (!isOn(player) || state == null) {
			context.getSource().sendFailure(Component.literal("Travel permits are part of the 1940s Germany run."));
			return 0;
		}
		if (state.permit()) {
			context.getSource().sendFailure(Component.literal("You already have a travel permit."));
			return 0;
		}

		int diamonds = player.getInventory().clearOrCountMatchingItems(
			stack -> stack.is(Items.DIAMOND), true, 0, player.inventoryMenu.getCraftSlots());
		ItemStack pocket = SafePocket.get(player);
		if (pocket.is(Items.DIAMOND)) {
			diamonds += pocket.getCount();
		}
		if (diamonds < PERMIT_PRICE) {
			context.getSource().sendFailure(Component.literal("A travel permit costs " + PERMIT_PRICE
				+ " diamonds. You have " + diamonds + "."));
			return 0;
		}

		int owed = PERMIT_PRICE - player.getInventory().clearOrCountMatchingItems(
			stack -> stack.is(Items.DIAMOND), false, PERMIT_PRICE, player.inventoryMenu.getCraftSlots());
		if (owed > 0) {
			pocket.shrink(owed);
			if (pocket.isEmpty()) {
				player.removeAttached(SafePocket.POCKET);
			}
		}
		player.setAttached(STATE, state.withPermit());
		openBorder(player.level().getServer());

		if (ServerPlayNetworking.canSend(player, PermitPayload.TYPE)) {
			ServerPlayNetworking.send(player, PermitPayload.INSTANCE);
		} else {
			RunManager.showTitle(player, Component.literal("Travel permit").withStyle(ChatFormatting.GOLD),
				Component.literal("The border is open"));
		}
		player.sendSystemMessage(Component.literal("You paid " + PERMIT_PRICE
			+ " diamonds for a travel permit. The border is gone for the rest of the run.")
			.withStyle(ChatFormatting.GOLD));
		return 1;
	}
}
