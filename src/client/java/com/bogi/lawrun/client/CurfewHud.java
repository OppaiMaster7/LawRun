package com.bogi.lawrun.client;

import java.util.BitSet;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.client.mixin.ToastManagerAccessor;
import com.bogi.lawrun.net.CurfewPayload;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Law 2 on screen. A plate in the top right counts down to curfew or to
 * sunrise. The edges darken as sunset nears, bells toll when it lands, and
 * being caught outside brings a red pulse, a heartbeat and a shaking warning.
 */
final class CurfewHud implements HudElement {
	/** The server sends every 10 ticks; after this long without one, the run is over. */
	private static final long STALE_MS = 2000;
	/** Matches the server's grace period, for scaling the warning. */
	private static final int GRACE_TICKS = 60;
	private static final int WARN_TICKS = 60 * 20;

	/** Made on first draw: item stacks can't exist before the game has loaded its items. */
	private static ItemStack clock;
	private static ItemStack diamond;

	private static CurfewPayload last;
	private static long receivedAt;

	private static final Beats sunset = new Beats();
	private static final Beats sunrise = new Beats();
	private static final Beats closeCall = new Beats();
	private static boolean sunsetPlaying;
	private static boolean sunrisePlaying;
	private static boolean closeCallPlaying;
	private static int lastTickSecond = -1;
	/** Where the plate was drawn this frame, so banners can keep clear of it. */
	private static int plateLeft;
	private static int plateBottom;
	private static long lastHeartbeat;

	/** How grey the world goes while you're caught outside. */
	static float grey() {
		CurfewPayload payload = fresh();
		if (payload == null || payload.graceLeft() < 0) {
			return 0;
		}
		return 0.3F + 0.6F * (1 - graceLeft(payload) / (float) GRACE_TICKS);
	}

	static void update(CurfewPayload payload) {
		if (last != null) {
			if (!last.curfew() && payload.curfew() && payload.enforced()) {
				sunset.restart();
				sunsetPlaying = true;
			}
			if (last.curfew() && !payload.curfew()) {
				sunrise.restart();
				sunrisePlaying = true;
			}
			if (last.graceLeft() >= 0 && payload.graceLeft() < 0 && payload.curfew()) {
				closeCall.restart();
				closeCallPlaying = true;
			}
		}
		last = payload;
		receivedAt = Util.getMillis();
	}

	private static CurfewPayload fresh() {
		return last != null && Util.getMillis() - receivedAt <= STALE_MS ? last : null;
	}

	private static int elapsedTicks() {
		return (int) ((Util.getMillis() - receivedAt) / 50);
	}

	private static int ticksLeft(CurfewPayload payload) {
		return Math.max(0, payload.ticksLeft() - elapsedTicks());
	}

	private static int graceLeft(CurfewPayload payload) {
		return Math.max(0, payload.graceLeft() - elapsedTicks());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		CurfewPayload payload = fresh();
		// The run just ended; the rule card has the screen.
		if (payload == null || minecraft.player == null || ArrestCinematic.isShowing()) {
			return;
		}
		Font font = minecraft.font;
		int left = ticksLeft(payload);
		boolean exposed = payload.graceLeft() >= 0;

		edges(graphics, payload, left, exposed);
		countdownTicks(payload, left);
		if (exposed) {
			warning(graphics, font, payload);
		}
		int plateBottom = plate(graphics, font, minecraft, payload, left, exposed);
		if (closeCallPlaying) {
			closeCall(graphics, font, plateBottom);
		}
		if (sunsetPlaying) {
			sunsetBanner(graphics, font);
		}
		if (sunrisePlaying) {
			sunriseBanner(graphics, font);
		}
	}

	/** Darkness creeping in before sunset, a steady gloom at night, a red pulse when caught outside. */
	private static void edges(GuiGraphicsExtractor graphics, CurfewPayload payload, int left, boolean exposed) {
		if (exposed) {
			float heartbeat = (float) Math.pow(Math.max(0, Math.sin(Util.getMillis() / 1000.0 * Math.PI * 2 * 1.4)), 4);
			Newsreel.vignette(graphics, 0x9A0000, 0.55F + 0.35F * heartbeat);
		} else if (payload.curfew()) {
			Newsreel.vignette(graphics, 0x000000, payload.enforced() ? 0.45F : 0.25F);
		} else if (left < WARN_TICKS) {
			Newsreel.vignette(graphics, 0x000000, 0.6F * (1 - left / (float) WARN_TICKS));
		}
	}

	/** A clock tick for each of the last ten seconds before curfew, and a heartbeat that speeds up outside. */
	private static void countdownTicks(CurfewPayload payload, int left) {
		int second = (left + 19) / 20;
		if (!payload.curfew() && second <= 10 && second >= 1 && second != lastTickSecond) {
			Beats.play(SoundEvents.NOTE_BLOCK_HAT.value(), second <= 3 ? 1.4F : 0.8F, 0.9F);
		}
		lastTickSecond = second;

		if (payload.graceLeft() >= 0) {
			float danger = 1 - graceLeft(payload) / (float) GRACE_TICKS;
			long interval = (long) Ease.lerp(700, 280, danger);
			if (Util.getMillis() - lastHeartbeat >= interval) {
				lastHeartbeat = Util.getMillis();
				Beats.play(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
			}
		}
	}

	private static void warning(GuiGraphicsExtractor graphics, Font font, CurfewPayload payload) {
		float danger = 1 - graceLeft(payload) / (float) GRACE_TICKS;
		float shake = 1 + 3 * danger;
		float x = graphics.guiWidth() / 2.0F + Newsreel.shake(shake, 11);
		float y = graphics.guiHeight() * 0.26F + Newsreel.shake(shake, 12);
		Newsreel.text(graphics, font, "GET INSIDE", x, y, 2.5F, 0xFF4A3A, 1, true, true);
		String seconds = String.format("%.1f", graceLeft(payload) / 20.0F);
		Newsreel.text(graphics, font, seconds, x, y + 26, 1.8F, Newsreel.CREAM, 1, true, true);
	}

	/** The countdown plate. Returns its bottom edge. */
	private static int plate(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft, CurfewPayload payload,
		int left, boolean exposed) {
		boolean away = minecraft.level != null && minecraft.level.dimension() != Level.OVERWORLD;
		String top = payload.curfew() ? "CURFEW · SUNRISE IN " + clock(left)
			: away ? "RETURN BY SUNSET " + clock(left)
			: "CURFEW IN " + clock(left);
		String bottom = null;
		int bottomColor = Newsreel.ASH;
		if (payload.curfew()) {
			if (!payload.enforced()) {
				bottom = "Not enforced tonight";
			} else if (exposed) {
				bottom = "OUTSIDE";
				bottomColor = (Util.getMillis() / 250) % 2 == 0 ? 0xFF4A3A : Newsreel.CREAM;
			} else {
				bottom = "Sheltered";
				bottomColor = 0x8FD18B;
			}
		}
		int topColor = payload.curfew() ? 0xF0A24A
			: left <= 10 * 20 || away && left <= 2 * 60 * 20 ? 0xFF4A3A
			: left <= WARN_TICKS ? 0xFFD866
			: Newsreel.CREAM;

		int textWidth = Math.max(font.width(top), bottom == null ? 0 : font.width(bottom));
		int width = textWidth + 30;
		int height = bottom == null ? 22 : 30;
		int right = graphics.guiWidth() - 4;
		int y = minecraft.player.getActiveEffects().isEmpty() ? 4 : 54;
		BitSet toastRows = ((ToastManagerAccessor) minecraft.gui.toastManager()).lawrun$getOccupiedSlots();
		if (!toastRows.isEmpty()) {
			y = Math.max(y, toastRows.length() * Toast.SLOT_HEIGHT + 6);
		}

		// In the last minute the plate throbs once a second.
		float pulse = !payload.curfew() && left <= WARN_TICKS
			? 1 + 0.05F * (float) Math.pow(Math.max(0, Math.sin(Util.getMillis() / 1000.0 * Math.PI)), 8)
			: 1;
		graphics.pose().pushMatrix();
		graphics.pose().translate(right - width / 2.0F, y + height / 2.0F);
		graphics.pose().scale(pulse, pulse);
		graphics.pose().translate(-width / 2.0F, -height / 2.0F);
		graphics.fill(0, 0, width, height, 0xD0120E0A);
		graphics.outline(0, 0, width, height, left <= 10 * 20 && !payload.curfew() ? 0xFFB3261E : 0xFF6E5A3E);
		if (clock == null) {
			clock = new ItemStack(Items.CLOCK);
		}
		graphics.item(clock, 4, (height - 16) / 2);
		graphics.text(font, top, 24, bottom == null ? 7 : 5, 0xFF000000 | topColor, true);
		if (bottom != null) {
			graphics.text(font, bottom, 24, 16, 0xFF000000 | bottomColor, true);
		}
		graphics.pose().popMatrix();
		plateLeft = right - width;
		plateBottom = y + height;
		permitTracker(graphics, font, payload, right);
		return plateBottom;
	}

	/** "PERMIT ◆ 2/3" under the plate until the permit is bought. */
	private static void permitTracker(GuiGraphicsExtractor graphics, Font font, CurfewPayload payload, int right) {
		if (payload.diamonds() < 0) {
			return;
		}
		int have = Math.min(payload.diamonds(), 3);
		String text = "PERMIT " + have + "/3";
		int textWidth = Math.round(font.width(text) * 0.8F);
		int y = plateBottom + 3;
		if (diamond == null) {
			diamond = new ItemStack(Items.DIAMOND);
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(right - textWidth - 14, y);
		graphics.pose().scale(0.6F, 0.6F);
		graphics.item(diamond, 0, 0);
		graphics.pose().popMatrix();
		Newsreel.text(graphics, font, text, right - textWidth - 2, y + 2, 0.8F, have >= 3 ? Newsreel.GOLD : Newsreel.ASH,
			1, false, true);
		plateBottom += 13;
	}

	private static void closeCall(GuiGraphicsExtractor graphics, Font font, int plateBottom) {
		long t = closeCall.ms();
		if (t > 2000) {
			closeCallPlaying = false;
			return;
		}
		float alpha = Math.min(Ease.progress(t, 0, 200), 1 - Ease.progress(t, 1500, 500));
		String text = "Made it.";
		Newsreel.text(graphics, font, text, graphics.guiWidth() - 8 - font.width(text), plateBottom + 5, 1,
			Newsreel.GOLD, alpha, false, true);
	}

	/** A paper strip drops from the top with a CURFEW stamp while the bells toll. */
	private static void sunsetBanner(GuiGraphicsExtractor graphics, Font font) {
		long t = sunset.ms();
		if (t > 4500) {
			sunsetPlaying = false;
			return;
		}
		for (int bell = 0; bell < 3; bell++) {
			sunset.sound("bell" + bell, bell * 1100L, SoundEvents.BELL_BLOCK, 0.9F - bell * 0.06F, 1.0F);
		}
		sunset.sound("stamp", 600, SoundEvents.ANVIL_LAND, 0.6F, 0.3F);
		banner(graphics, font, t, 4500, "CURFEW", "STAY INDOORS UNTIL SUNRISE", Newsreel.STAMP_RED);
	}

	private static void sunriseBanner(GuiGraphicsExtractor graphics, Font font) {
		long t = sunrise.ms();
		if (t > 3000) {
			sunrisePlaying = false;
			return;
		}
		sunrise.sound("bell", 0, SoundEvents.BELL_BLOCK, 1.3F, 0.6F);
		banner(graphics, font, t, 3000, "CURFEW LIFTED", "UNTIL SUNSET", Newsreel.STAMP_BLUE);
	}

	private static void banner(GuiGraphicsExtractor graphics, Font font, long t, long length, String stamp,
		String caption, int stampColor) {
		int width = 200;
		int height = 44;
		float drop = Ease.outBack(Ease.progress(t, 0, 450)) - Ease.inCubic(Ease.progress(t, length - 400, 400));
		int x = (graphics.guiWidth() - width) / 2;
		// On narrow screens the banner would run into the plate; drop it below instead.
		int rest = x + width + 4 > plateLeft ? plateBottom + 6 : 8;
		float y = Ease.lerp(-height - 6, rest, drop);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		Newsreel.paper(graphics, 0, 0, width, height, 1);
		Newsreel.text(graphics, font, caption, width / 2.0F, 30, 0.8F, Newsreel.INK, 1, true, false);
		if (t >= 480) {
			float land = Ease.progress(t, 480, 200);
			Newsreel.stamp(graphics, font, stamp, width / 2.0F, 15, stampColor, -3,
				Ease.lerp(3.0F, 1.4F, Ease.outBack(land)), Math.min(0.9F, land * 2));
		}
		graphics.pose().popMatrix();
	}

	private static String clock(int ticks) {
		int seconds = (ticks + 19) / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
