package com.bogi.lawrun.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bogi.lawrun.client.Cinematics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

/** Adds the newsreel colour grade to the post effects vanilla collects each frame (creeper vision and so on). */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Shadow
	@Final
	private List<Identifier> requestedPostEffects;

	@Inject(method = "update", at = @At("TAIL"))
	private void lawrun$addColourGrade(DeltaTracker deltaTracker, CallbackInfo ci) {
		Identifier grade = Cinematics.grade();
		if (grade != null) {
			requestedPostEffects.add(grade);
		}
	}
}
