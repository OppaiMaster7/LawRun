package com.bogi.lawrun.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bogi.lawrun.runs.germany.Weapons;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Rule #1 card can be looked at, never taken. Runs on both sides, so the client doesn't mispredict. */
@Mixin(Slot.class)
abstract class SlotMixin {
	@Shadow
	public abstract ItemStack getItem();

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void lawrun$keepBannedWeapon(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (Weapons.isBannedDisplay(getItem())) {
			cir.setReturnValue(false);
		}
	}
}
