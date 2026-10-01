package com.bogi.lawrun.client;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;

/** Law 6: a travel permit slides up, gets stamped APPROVED, and slides away. */
final class PermitCinematic implements HudElement {
	private static final long LENGTH = 3800;
	private static final long STAMP_AT = 1000;

	private static boolean playing;
	private static final Beats beats = new Beats();

	static boolean isPlaying() {
		return playing;
	}

	static void start() {
		playing = true;
		beats.restart();
		Minecraft.getInstance().gui.hud.clearTitles();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (!playing) {
			return;
		}
		long t = beats.ms();
		if (t > LENGTH) {
			playing = false;
			return;
		}
		beats.sound("paper", 0, SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.0F);
		beats.sound("stamp", STAMP_AT, SoundEvents.ANVIL_LAND, 0.7F, 0.35F);
		beats.sound("bell", STAMP_AT + 150, SoundEvents.BELL_BLOCK, 1.5F, 0.5F);

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		int width = 190;
		int height = 96;
		float in = Ease.outBack(Ease.progress(t, 0, 550));
		float out = Ease.inCubic(Ease.progress(t, LENGTH - 500, 500));
		float restY = graphics.guiHeight() * 0.18F;
		float y = Ease.lerp(graphics.guiHeight() + 10, restY, in) + out * (graphics.guiHeight() - restY + 20);
		float x = (graphics.guiWidth() - width) / 2.0F;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + width / 2.0F, y + height / 2.0F);
		graphics.pose().rotate((float) Math.toRadians(Ease.lerp(6, -1.5F, in)));
		graphics.pose().translate(-width / 2.0F, -height / 2.0F);
		Newsreel.paper(graphics, 0, 0, width, height, 1);
		Newsreel.text(graphics, font, "TRAVEL PERMIT", width / 2.0F, 8, 1.4F, Newsreel.INK, 1, true, false);
		graphics.fill(10, 24, width - 10, 25, Newsreel.argb(0.6F, Newsreel.FADED_INK));
		String holder = minecraft.player == null ? "" : minecraft.player.getName().getString();
		Newsreel.text(graphics, font, "Holder: " + holder, 10, 32, 0.8F, Newsreel.INK, 1, false, false);
		Newsreel.text(graphics, font, "Fee: 3 diamonds, paid in full", 10, 44, 0.8F, Newsreel.INK, 1, false, false);
		Newsreel.text(graphics, font, "Valid: for the rest of the run", 10, 56, 0.8F, Newsreel.INK, 1, false, false);
		if (t >= STAMP_AT - 120) {
			float land = Ease.progress(t, STAMP_AT - 120, 200);
			Newsreel.stamp(graphics, font, "APPROVED", width - 58, height - 20, Newsreel.STAMP_BLUE, -12,
				Ease.lerp(3.4F, 1.5F, Ease.outBack(land)), Math.min(0.9F, land * 2));
		}
		graphics.pose().popMatrix();
	}
}
