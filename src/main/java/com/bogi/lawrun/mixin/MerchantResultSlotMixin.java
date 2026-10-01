package com.bogi.lawrun.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bogi.lawrun.runs.germany.GermanyRules;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;

/** Law 5: taking a trade's result is the trade. Villagers and wandering traders both use this slot. */
@Mixin(MerchantResultSlot.class)
abstract class MerchantResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void lawrun$banTrading(Player player, ItemStack carried, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer && GermanyRules.isOn(serverPlayer)) {
			GermanyRules.breakBusinessLaw(serverPlayer, "Made a trade");
		}
	}
}
