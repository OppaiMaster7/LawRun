package com.bogi.lawrun.client;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunState;
import com.bogi.lawrun.core.RunStatus;
import com.bogi.lawrun.net.RulePopupPayload;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;

/**
 * The moment a rule is broken. The world drains to grey, the rule card
 * slams in and types itself out, a "RULE #N BROKEN" stamp lands on it, then
 * everything fades to black just as the server opens the fail screen.
 */
final class ArrestCinematic implements HudElement {
	/** Matches the server's delay before it sends the fail screen. */
	private static final long LENGTH = RunManager.POPUP_TICKS * 50L;
	private static final long CARD_AT = 150;
	private static final long TYPE_AT = 550;
	private static final float TYPE_SPEED = 140;
	private static final long STAMP_AT = 2100;
	/** Hold the black a little past the end, in case the fail screen is late. */
	private static final long HOLD_BLACK = 2500;

	private static RulePopupPayload current;
	private static final Beats beats = new Beats();

	static void start(RulePopupPayload payload) {
		current = payload;
		beats.restart();
		Minecraft.getInstance().gui.setScreen(null);
		// A leftover title (like the run's start) would show through behind the card.
		Minecraft.getInstance().gui.hud.clearTitles();
	}

	/** Only while the run is still the failed one: a reset or a new run clears it at once. */
	static boolean isShowing() {
		if (current == null || beats.ms() >= LENGTH + HOLD_BLACK) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		RunState state = minecraft.player == null ? null : RunManager.get(minecraft.player);
		return state != null && state.status() == RunStatus.FAILED;
	}

	/** How grey the world should be, 0 to 1. */
	static float grey() {
		return isShowing() ? Ease.progress(beats.ms(), 0, 700) : 0;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (!isShowing()) {
			current = null;
			return;
		}
		long t = beats.ms();
		beats.sound("bell", 0, SoundEvents.BELL_BLOCK, 0.7F, 1.0F);
		for (int beat = 0; beat < 5; beat++) {
			beats.sound("heart" + beat, beat * 750L, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
		}
		beats.sound("stamp", STAMP_AT, SoundEvents.ANVIL_LAND, 0.6F, 0.35F);

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		Newsreel.wash(graphics, 0xB00000, 0.55F * (1 - Ease.progress(t, 0, 250)));
		float heartbeat = (float) Math.pow(Math.max(0, Math.sin(t / 750.0 * Math.PI * 2)), 6);
		Newsreel.vignette(graphics, 0x8A0000, 0.5F + 0.35F * heartbeat);
		Newsreel.letterbox(graphics, Ease.progress(t, 0, 450));
		Newsreel.grain(graphics, 0.35F);
		Newsreel.scratches(graphics, 0.6F);

		if (t >= CARD_AT) {
			drawCard(graphics, Minecraft.getInstance().font, t, width, height);
		}
		Newsreel.wash(graphics, 0x000000, Ease.progress(t, LENGTH - 700, 700));
	}

	private static void drawCard(GuiGraphicsExtractor graphics, Font font, long t, int width, int height) {
		int cardWidth = Math.min(270, width - 40);
		int textWidth = cardWidth - 16;
		String body = current.text();
		int bodyHeight = Newsreel.paragraphHeight(font, body, 0.9F, textWidth);
		int cardHeight = 38 + bodyHeight + 30;

		float slam = Ease.progress(t, CARD_AT, 320);
		float scale = Ease.lerp(1.7F, 1.0F, Ease.outBack(slam));
		float rotation = Ease.lerp(-7.0F, -1.5F, Ease.outCubic(slam));
		float shakeStrength = 3 * (1 - Ease.progress(t, CARD_AT + 150, 400))
			+ 5 * (t >= STAMP_AT ? 1 - Ease.progress(t, STAMP_AT, 350) : 0);
		float alpha = Ease.progress(t, CARD_AT, 120);

		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0F + Newsreel.shake(shakeStrength, 1),
			height / 2.0F - 10 + Newsreel.shake(shakeStrength, 2));
		graphics.pose().rotate((float) Math.toRadians(rotation));
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-cardWidth / 2.0F, -cardHeight / 2.0F);

		Newsreel.paper(graphics, 0, 0, cardWidth, cardHeight, alpha);
		Newsreel.text(graphics, font, "RULE #" + current.number(), 8, 8, 1, Newsreel.FADED_INK, alpha, false, false);
		String date = current.date();
		Newsreel.text(graphics, font, date, cardWidth - 8 - font.width(date), 8, 1, Newsreel.FADED_INK, alpha, false, false);
		Newsreel.text(graphics, font, current.name().toUpperCase(), 8, 20, 1.5F, Newsreel.INK, alpha, false, false);
		Newsreel.paragraph(graphics, font, Newsreel.typed(body, t, TYPE_AT, TYPE_SPEED), 8, 38, 0.9F, textWidth,
			Newsreel.INK, alpha, false);

		long reasonAt = TYPE_AT + Newsreel.typingMs(body, TYPE_SPEED) + 150;
		float reasonAlpha = Ease.progress(t, reasonAt, 200);
		int reasonY = 38 + bodyHeight + 8;
		Newsreel.text(graphics, font, current.reason(), 8, reasonY, 1, Newsreel.STAMP_RED, reasonAlpha, false, false);
		graphics.fill(8, reasonY + 10, 8 + font.width(current.reason()), reasonY + 11, Newsreel.argb(reasonAlpha, Newsreel.STAMP_RED));

		// The slam starts a moment before the thud, so the sound lands with it.
		if (t >= STAMP_AT - 120) {
			float land = Ease.progress(t, STAMP_AT - 120, 200);
			Newsreel.stamp(graphics, font, "RULE #" + current.number() + " BROKEN", cardWidth - 72, cardHeight - 14,
				Newsreel.STAMP_RED, -11, Ease.lerp(3.2F, 1.25F, Ease.outBack(land)), Math.min(0.9F, land * 2));
		}
		graphics.pose().popMatrix();
	}
}
