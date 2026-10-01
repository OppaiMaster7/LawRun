package com.bogi.lawrun.client;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunRegistry;
import com.bogi.lawrun.net.VictoryPayload;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;

/**
 * The dragon is dead. While it breaks apart, the world goes sepia under
 * letterbox bars, bells ring, and a gold RUN COMPLETE stamp lands. The full
 * ending plays when the player takes the exit portal.
 */
final class VictoryCinematic implements HudElement {
	private static final long LENGTH = 12000;
	private static final long STAMP_AT = 1200;
	private static final long LINE_AT = 2100;
	private static final long PORTAL_AT = 5500;
	private static final long FADE_AT = 10500;

	private static RunDefinition run;
	private static final Beats beats = new Beats();

	static void start(VictoryPayload payload) {
		run = RunRegistry.get(payload.runId());
		beats.restart();
		ConfiscationCinematic.stop();
		Minecraft.getInstance().gui.hud.clearTitles();
	}

	static boolean isPlaying() {
		return run != null;
	}

	static float sepia() {
		if (run == null) {
			return 0;
		}
		long t = beats.ms();
		return t > LENGTH ? 0 : Ease.progress(t, 0, 1500) * (1 - Ease.progress(t, FADE_AT, LENGTH - FADE_AT));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (run == null) {
			return;
		}
		long t = beats.ms();
		if (t > LENGTH) {
			run = null;
			return;
		}
		for (int bell = 0; bell < 3; bell++) {
			beats.sound("bell" + bell, 300 + bell * 1200L, SoundEvents.BELL_BLOCK, bell == 1 ? 1.2F : 1.0F, 1.0F);
		}
		beats.sound("resonate", 3900, SoundEvents.BELL_RESONATE, 1.0F, 0.8F);
		beats.sound("stamp", STAMP_AT, SoundEvents.ANVIL_LAND, 0.8F, 0.3F);

		Font font = Minecraft.getInstance().font;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		float fade = 1 - Ease.progress(t, FADE_AT, LENGTH - FADE_AT);
		Newsreel.letterbox(graphics, Ease.progress(t, 0, 700) * fade);
		Newsreel.vignette(graphics, 0x000000, 0.5F * fade);
		Newsreel.grain(graphics, 0.3F * fade);
		Newsreel.scratches(graphics, 0.5F * fade);

		if (t >= STAMP_AT - 120) {
			float land = Ease.progress(t, STAMP_AT - 120, 220);
			Newsreel.stamp(graphics, font, "RUN COMPLETE", width / 2.0F, height * 0.3F, Newsreel.GOLD, -5,
				Ease.lerp(5.0F, 2.4F, Ease.outBack(land)), Math.min(0.95F, land * 2) * fade);
		}
		String line = run.ending().victoryLine();
		Newsreel.text(graphics, font, Newsreel.typed(line, t, LINE_AT, 40), width / 2.0F, height * 0.3F + 30, 1.1F,
			Newsreel.CREAM, fade, true, true);
		float portal = Ease.progress(t, PORTAL_AT, 600) * (0.6F + 0.4F * (float) Math.sin(t / 300.0)) * fade;
		Newsreel.text(graphics, font, "The exit portal is open.", width / 2.0F, height * 0.3F + 48, 0.9F,
			Newsreel.ASH, portal, true, true);
	}
}
