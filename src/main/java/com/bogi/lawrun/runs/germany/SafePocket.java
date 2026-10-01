package com.bogi.lawrun.runs.germany;

import com.bogi.lawrun.LawRun;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ticks.ContainerSingleItem;

/**
 * One extra inventory slot whose stack survives the sunrise confiscation.
 * {@code InventoryMenuMixin} adds it to every player's inventory; it only
 * shows and accepts items while a 1940s Germany run is active.
 */
public final class SafePocket {
	/** Not synced: the inventory menu already keeps the client's copy up to date. */
	public static final AttachmentType<ItemStack> POCKET = AttachmentRegistry.create(LawRun.id("safe_pocket"),
		builder -> builder.persistent(ItemStack.OPTIONAL_CODEC));

	/** Bottom right of the inventory's top half, under the crafting output. */
	public static final int X = 152;
	public static final int Y = 62;

	private SafePocket() {
	}

	public static ItemStack get(Player player) {
		return player.getAttachedOrElse(POCKET, ItemStack.EMPTY);
	}

	/** Containers are out, or one shulker box would carry a whole inventory past sunrise. */
	public static boolean accepts(ItemStack stack) {
		return !stack.is(ItemTags.SHULKER_BOXES)
			&& !stack.is(ItemTags.BUNDLES)
			&& !stack.has(DataComponents.CONTAINER)
			&& !stack.has(DataComponents.BUNDLE_CONTENTS);
	}

	/** When a run ends the slot disappears, so hand back whatever was in it. */
	static void empty(ServerPlayer player) {
		ItemStack stack = get(player);
		player.removeAttached(POCKET);
		if (stack.isEmpty()) {
			return;
		}
		if (player.isAlive()) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		} else {
			player.spawnAtLocation(player.level(), stack);
		}
	}

	private record PocketContainer(Player owner) implements ContainerSingleItem {
		@Override
		public ItemStack getTheItem() {
			return get(owner);
		}

		@Override
		public void setTheItem(ItemStack stack) {
			if (stack.isEmpty()) {
				owner.removeAttached(POCKET);
			} else {
				owner.setAttached(POCKET, stack);
			}
		}

		@Override
		public void setChanged() {
		}

		@Override
		public boolean stillValid(Player player) {
			return player == owner;
		}
	}

	public static final class PocketSlot extends Slot {
		private final Player owner;

		public PocketSlot(Player owner) {
			super(new PocketContainer(owner), 0, X, Y);
			this.owner = owner;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return accepts(stack);
		}

		@Override
		public boolean isActive() {
			return GermanyRules.isOn(owner);
		}
	}
}
