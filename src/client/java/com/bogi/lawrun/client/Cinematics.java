package com.bogi.lawrun.client;

import com.bogi.lawrun.LawRun;
import com.bogi.lawrun.client.screen.RunOverScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Picks the world's colour grade from whatever moment is playing. The grades
 * are post effects in {@code assets/lawrun/post_effect/}, in steps of 25%,
 * so a fade is a few quick swaps. {@code GameRendererMixin} applies the pick.
 */
public final class Cinematics {
	private Cinematics() {
	}

	/** Null when the world should look normal. */
	public static Identifier grade() {
		float grey = Math.max(ArrestCinematic.grey(), CurfewHud.grey());
		if (Minecraft.getInstance().gui.screen() instanceof RunOverScreen) {
			grey = 1;
		}
		float sepia = Math.max(ConfiscationCinematic.sepia(), VictoryCinematic.sepia());
		if (grey >= sepia && grey > 0.1F) {
			return step("grey", grey);
		}
		return sepia > 0.1F ? step("sepia", sepia) : null;
	}

	private static Identifier step(String grade, float amount) {
		int percent = amount > 0.875F ? 100 : amount > 0.625F ? 75 : amount > 0.375F ? 50 : 25;
		return LawRun.id(grade + "_" + percent);
	}
}
