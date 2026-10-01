package com.bogi.lawrun.runs.germany;

import java.util.ArrayList;
import java.util.List;

import com.bogi.lawrun.LawRun;
import com.bogi.lawrun.core.Law;
import com.bogi.lawrun.core.RunManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Law 1. His test in the video is "does it have attack damage?", which catches
 * every tool. Bows and crossbows have none, so they're listed by tag instead:
 * {@code data/lawrun/tags/item/banned_weapons.json}.
 */
public final class Weapons {
	public static final TagKey<Item> BANNED = TagKey.create(Registries.ITEM, LawRun.id("banned_weapons"));

	/** Marks the look-but-don't-touch copy shown in a crafting output slot. */
	private static final String DISPLAY_MARKER = "lawrun_banned_weapon";
	private static final int LORE_WIDTH = 38;

	private Weapons() {
	}

	public static boolean isWeapon(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.is(BANNED) || stack.has(DataComponents.WEAPON)) {
			return true;
		}
		ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The weapon's own icon, renamed to Rule #1 with the law as its
	 * description. {@code SlotMixin} refuses to let it be picked up.
	 */
	public static ItemStack bannedDisplay(ItemStack weapon) {
		Law law = Germany1940s.WEAPONS;
		List<Component> lore = new ArrayList<>();
		lore.add(gray(law.name() + " · " + law.date()));
		for (String line : wrap(law.history())) {
			lore.add(gray(line));
		}
		lore.add(Component.empty());
		lore.add(Component.literal("You can't take this.").withStyle(style -> style
			.withColor(ChatFormatting.DARK_RED)
			.withItalic(false)));

		ItemStack display = weapon.copy();
		display.set(DataComponents.CUSTOM_NAME, Component.literal("Rule #1 · Banned weapon").withStyle(style -> style
			.withColor(ChatFormatting.RED)
			.withItalic(false)));
		display.set(DataComponents.LORE, new ItemLore(lore));
		display.set(DataComponents.TOOLTIP_DISPLAY,
			TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true));
		CustomData.update(DataComponents.CUSTOM_DATA, display, tag -> tag.putBoolean(DISPLAY_MARKER, true));
		return display;
	}

	public static boolean isBannedDisplay(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(DISPLAY_MARKER, false);
	}

	/** Holding a weapon in any slot, the safe pocket or on the cursor ends the run. */
	static void check(ServerPlayer player) {
		ItemStack found = findWeapon(player);
		if (!found.isEmpty()) {
			RunManager.fail(player, Germany1940s.WEAPONS, "Picked up " + found.getHoverName().getString());
		}
	}

	private static ItemStack findWeapon(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (isWeapon(inventory.getItem(slot))) {
				return inventory.getItem(slot);
			}
		}
		for (ItemStack stack : List.of(player.containerMenu.getCarried(), SafePocket.get(player))) {
			if (isWeapon(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static Component gray(String text) {
		return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	/** Tooltip lines don't wrap by themselves. */
	private static List<String> wrap(String text) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (!line.isEmpty() && line.length() + 1 + word.length() > LORE_WIDTH) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if (!line.isEmpty()) {
				line.append(' ');
			}
			line.append(word);
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}
}
