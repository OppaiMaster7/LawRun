package com.bogi.lawrun.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.bogi.lawrun.runs.germany.GermanyRules;
import com.bogi.lawrun.runs.germany.Weapons;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Rule #1: a weapon recipe still shows its result, but as the Rule #1 card.
 * Covers the crafting table and the 2x2 inventory grid, which share this method.
 * <p>
 * Swapped at the first read of {@code result}, so the result slot, the remote
 * copy and the packet to the client all get the card. Swapping just before the
 * {@code setItem} call is too late: its argument is already on the stack.
 */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
	@ModifyVariable(method = "slotChangedCraftingGrid", at = @At(value = "LOAD", ordinal = 0), ordinal = 0)
	private static ItemStack lawrun$showBannedWeapon(ItemStack result, @Local(argsOnly = true) Player player) {
		return GermanyRules.isOn(player) && Weapons.isWeapon(result) ? Weapons.bannedDisplay(result) : result;
	}
}
