package com.bogi.lawrun.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bogi.lawrun.core.RunManager;

import net.minecraft.server.level.ServerPlayer;

/**
 * Vanilla only sends the ending the first time a player ever beats the game.
 * A run's ending plays once per won run, even for a player who has beaten
 * the game before in this world.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
	@Inject(method = "showEndCredits", at = @At("HEAD"))
	private void lawrun$alwaysSendRunEnding(CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (RunManager.claimEnding(self)) {
			self.wonGame = false;
		}
	}
}
