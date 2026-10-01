package com.bogi.lawrun.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bogi.lawrun.runs.germany.GermanyRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;

/** Law 5: bartering is trading. Throwing gold to a piglin or handing it over both count. */
@Mixin(PiglinAi.class)
abstract class PiglinAiMixin {
	@Inject(method = "pickUpItem", at = @At("HEAD"))
	private static void lawrun$banThrownGold(ServerLevel level, Piglin body, ItemEntity itemEntity, CallbackInfo ci) {
		if (itemEntity.getItem().is(PiglinAi.BARTERING_ITEM)
			&& itemEntity.getOwner() instanceof ServerPlayer player
			&& GermanyRules.isOn(player)) {
			GermanyRules.breakBusinessLaw(player, "Bartered with a piglin");
		}
	}

	@Inject(method = "mobInteract", at = @At("HEAD"))
	private static void lawrun$banHandedGold(ServerLevel level, Piglin body, Player player, InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> cir) {
		if (body.isAdult()
			&& player.getItemInHand(hand).is(PiglinAi.BARTERING_ITEM)
			&& player instanceof ServerPlayer serverPlayer
			&& GermanyRules.isOn(serverPlayer)) {
			GermanyRules.breakBusinessLaw(serverPlayer, "Bartered with a piglin");
		}
	}
}
