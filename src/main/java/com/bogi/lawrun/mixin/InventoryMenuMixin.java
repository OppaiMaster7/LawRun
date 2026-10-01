package com.bogi.lawrun.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bogi.lawrun.runs.germany.SafePocket;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Adds the safe pocket as slot 46, after the offhand. Vanilla's shift-click
 * code sends anything past slot 45 back into the inventory, which is right.
 */
@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin extends AbstractCraftingMenu {
	private InventoryMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
		super(menuType, containerId, width, height);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void lawrun$addSafePocket(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		addSlot(new SafePocket.PocketSlot(owner));
	}
}
