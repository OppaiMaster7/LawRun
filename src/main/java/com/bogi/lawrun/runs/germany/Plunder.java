package com.bogi.lawrun.runs.germany;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.bogi.lawrun.LawRun;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.net.ConfiscationPayload;
import com.bogi.lawrun.net.ContainerStatusPayload;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Laws 3 and 4. At sunrise everything the player carries is taken except the
 * safe pocket and 3 random pieces of food. Only containers the world generated
 * are safe storage.
 */
final class Plunder {
	static final int FOOD_KEPT = 3;

	/** Set on a container the first time it's opened with its world-generated loot still inside. */
	private static final AttachmentType<Boolean> NATURAL = AttachmentRegistry.create(LawRun.id("natural"),
		builder -> builder.persistent(Codec.BOOL));

	/** The day a player-made container was last opened. Anything in it from an earlier day is taken. */
	private static final AttachmentType<Long> STASH_DAY = AttachmentRegistry.create(LawRun.id("stash_day"),
		builder -> builder.persistent(Codec.LONG));

	private Plunder() {
	}

	/** Returns the state to keep using this tick. */
	static GermanyState checkSunrise(ServerPlayer player, GermanyState state) {
		long day = Curfew.day(Curfew.clock(player.level().getServer()));
		if (day == state.lastDay()) {
			return state;
		}
		// Moving time backwards (/time set) just resyncs; only a new day plunders.
		if (day > state.lastDay()) {
			RunManager.addStat(player, Germany1940s.Stats.DAYS, day - state.lastDay());
			RunManager.addStat(player, Germany1940s.Stats.ITEMS_TAKEN, confiscate(player));
		}
		GermanyState updated = state.plunderedOn(day);
		player.setAttached(GermanyRules.STATE, updated);
		return updated;
	}

	private static int confiscate(ServerPlayer player) {
		if (player.containerMenu != player.inventoryMenu) {
			player.closeContainer();
		}
		// The cursor and the 2x2 crafting grid are carried too.
		player.containerMenu.setCarried(ItemStack.EMPTY);
		player.inventoryMenu.getCraftSlots().clearContent();

		Inventory inventory = player.getInventory();
		List<ItemStack> food = new ArrayList<>();
		// Slot order matters to the client: slots 0-8 fly out of the hotbar.
		List<ItemStack> takenStacks = new ArrayList<>();
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.has(DataComponents.FOOD)) {
				for (int i = 0; i < stack.getCount(); i++) {
					food.add(stack.copyWithCount(1));
				}
			}
			taken += stack.getCount();
			takenStacks.add(stack.copy());
		}
		inventory.clearContent();

		List<ItemStack> kept = pickRandom(food, FOOD_KEPT, player.getRandom());
		for (ItemStack stack : kept) {
			inventory.add(stack.copy());
		}
		taken -= kept.size();

		taken += emptyContainer(player.getEnderChestInventory());
		for (ItemEntity item : player.level().getEntities(EntityTypes.ITEM, item -> item.getOwner() == player)) {
			taken += item.getItem().getCount();
			item.discard();
		}

		if (ServerPlayNetworking.canSend(player, ConfiscationPayload.TYPE)) {
			int day = (int) RunManager.get(player).stat(Germany1940s.Stats.DAYS) + 1;
			ServerPlayNetworking.send(player, new ConfiscationPayload(takenStacks, kept, taken, day));
		} else {
			RunManager.showTitle(player, Component.literal("Sunrise").withStyle(ChatFormatting.GOLD),
				Component.literal("Your belongings were confiscated"));
		}
		player.sendSystemMessage(Component.literal("Sunrise: " + taken + " items confiscated. ")
			.withStyle(ChatFormatting.GOLD)
			.append(Component.literal(describeRations(kept)).withStyle(ChatFormatting.GRAY)));
		return taken;
	}

	/** Every piece of food is one ticket; draw without replacement. */
	private static List<ItemStack> pickRandom(List<ItemStack> pieces, int count, RandomSource random) {
		List<ItemStack> picked = new ArrayList<>();
		for (int i = 0; i < count && i < pieces.size(); i++) {
			int choice = i + random.nextInt(pieces.size() - i);
			ItemStack chosen = pieces.get(choice);
			pieces.set(choice, pieces.get(i));
			pieces.set(i, chosen);
			picked.add(chosen);
		}
		return picked;
	}

	private static String describeRations(List<ItemStack> kept) {
		if (kept.isEmpty()) {
			return "You had no food to keep.";
		}
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (ItemStack stack : kept) {
			counts.merge(stack.getHoverName().getString(), 1, Integer::sum);
		}
		StringJoiner joiner = new StringJoiner(", ", "Your ration: ", ".");
		counts.forEach((name, count) -> joiner.add(count + " " + name));
		return joiner.toString();
	}

	/**
	 * Called before a player opens or breaks a block. Natural containers are
	 * left alone. Anything else that still holds items from before the last
	 * sunrise is emptied, the same as if it had been carried.
	 */
	static void checkContainer(ServerPlayer player, Level level, BlockPos pos, boolean opening) {
		inspect(player, level, pos, opening);
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			inspect(player, level, pos.relative(ChestBlock.getConnectedDirection(state)), opening);
		}
	}

	private static void inspect(ServerPlayer player, Level level, BlockPos pos, boolean opening) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof Container container)) {
			return;
		}
		if (blockEntity instanceof RandomizableContainer lootable && lootable.getLootTable() != null) {
			blockEntity.setAttached(NATURAL, true);
			blockEntity.setChanged();
			if (opening) {
				tellClient(player, true);
				Notices.worldChestOpened(player, lootable.getLootTable().identifier().getPath());
			}
			return;
		}
		if (blockEntity.getAttachedOrElse(NATURAL, false)) {
			if (opening) {
				tellClient(player, true);
			}
			return;
		}
		if (opening) {
			tellClient(player, false);
		}

		long today = Curfew.day(Curfew.clock(player.level().getServer()));
		Long stashed = blockEntity.getAttached(STASH_DAY);
		if (stashed != null && stashed < today && !container.isEmpty()) {
			int taken = emptyContainer(container);
			player.sendSystemMessage(Component.literal(taken + " items stored here were confiscated at sunrise."
				+ " Only chests the world generated are safe.").withStyle(ChatFormatting.GOLD));
			RunManager.addStat(player, Germany1940s.Stats.ITEMS_TAKEN, taken);
		}
		if (opening) {
			blockEntity.setAttached(STASH_DAY, today);
		}
		blockEntity.setChanged();
	}

	/** For the SAFE / NOT SAFE stamp on the container screen that's about to open. */
	private static void tellClient(ServerPlayer player, boolean safe) {
		if (ServerPlayNetworking.canSend(player, ContainerStatusPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ContainerStatusPayload(safe));
		}
	}

	private static int emptyContainer(Container container) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			count += container.getItem(slot).getCount();
		}
		container.clearContent();
		return count;
	}
}
