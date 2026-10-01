package com.bogi.lawrun.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.net.ConfiscationPayload;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Sunrise, laws 3 and 4. Everything you carried lifts out of the hotbar and
 * flies off the top of the screen, a CONFISCATED stamp lands with the count,
 * and your 3 rations are handed back one by one.
 */
final class ConfiscationCinematic implements HudElement {
	/** The "DAY 3" title card that opens the sunrise. */
	private static final long DAY_MS = 1400;
	private static final long LENGTH = DAY_MS + 5400;
	private static final long FLIGHT_MS = 900;
	private static final long STAMP_AT = DAY_MS + 700;
	private static final long COUNT_AT = DAY_MS + 900;
	private static final long RATION_AT = DAY_MS + 2400;
	private static final long FADE_AT = DAY_MS + 4500;

	/** One confiscated stack on its way out. */
	private record Flyer(ItemStack stack, int slot, long delay, float drift, float spin) {
	}

	private static ConfiscationPayload current;
	private static final List<Flyer> flyers = new ArrayList<>();
	private static final Beats beats = new Beats();

	static void start(ConfiscationPayload payload) {
		current = payload;
		beats.restart();
		flyers.clear();
		Minecraft.getInstance().gui.hud.clearTitles();
		Random random = new Random();
		List<ItemStack> taken = payload.taken();
		long count = taken.stream().filter(stack -> !stack.isEmpty()).count();
		long step = count == 0 ? 0 : Math.min(70, 1400 / count);
		int order = 0;
		for (int slot = 0; slot < taken.size(); slot++) {
			if (!taken.get(slot).isEmpty()) {
				flyers.add(new Flyer(taken.get(slot), slot, DAY_MS + 150 + order++ * step,
					(random.nextFloat() - 0.5F) * 140, (random.nextBoolean() ? 1 : -1) * (180 + random.nextFloat() * 360)));
			}
		}
	}

	static void stop() {
		current = null;
	}

	static boolean isPlaying() {
		return current != null;
	}

	/** How sepia the world should be. */
	static float sepia() {
		if (current == null) {
			return 0;
		}
		long t = beats.ms();
		return t > LENGTH ? 0 : Ease.progress(t, 0, 250) * (1 - Ease.progress(t, FADE_AT - 400, 1300));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (current == null) {
			return;
		}
		long t = beats.ms();
		if (t > LENGTH) {
			current = null;
			return;
		}
		Font font = Minecraft.getInstance().font;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		float fadeOut = 1 - Ease.progress(t, FADE_AT, LENGTH - FADE_AT);

		beats.sound("bell", 0, SoundEvents.BELL_BLOCK, 1.2F, 0.8F);
		beats.sound("items", DAY_MS, SoundEvents.BELL_BLOCK, 0.9F, 0.5F);
		dayCard(graphics, font, t, width, height);
		beats.sound("stamp", STAMP_AT, SoundEvents.ANVIL_LAND, 0.55F, 0.35F);

		Newsreel.wash(graphics, 0xF2D9A0, 0.35F * (1 - Ease.progress(t, 0, 400)));
		Newsreel.grain(graphics, 0.25F * fadeOut);
		Newsreel.scratches(graphics, 0.4F * fadeOut);

		for (int i = 0; i < flyers.size(); i++) {
			Flyer flyer = flyers.get(i);
			if (i % 3 == 0) {
				beats.sound("paper" + i, flyer.delay(), SoundEvents.BOOK_PAGE_TURN, 0.9F + (i % 5) * 0.08F, 0.5F);
			}
			drawFlyer(graphics, flyer, t, width, height);
		}

		// The stamp and the tally.
		if (t >= STAMP_AT - 120) {
			float land = Ease.progress(t, STAMP_AT - 120, 200);
			Newsreel.stamp(graphics, font, "CONFISCATED", width / 2.0F, height * 0.22F, Newsreel.STAMP_RED, -7,
				Ease.lerp(4.0F, 2.0F, Ease.outBack(land)), Math.min(0.92F, land * 2) * fadeOut);
		}
		if (t >= COUNT_AT) {
			int shown = Math.round(current.total() * Ease.outCubic(Ease.progress(t, COUNT_AT, 1000)));
			String tally = shown + (current.total() == 1 ? " ITEM" : " ITEMS");
			Newsreel.text(graphics, font, tally, width / 2.0F, height * 0.22F + 22, 1.5F, Newsreel.CREAM,
				Ease.progress(t, COUNT_AT, 200) * fadeOut, true, true);
		}

		rations(graphics, font, t, width, height, fadeOut);
	}

	/** Black film leader with the day number, then it lifts to reveal the confiscation. */
	private static void dayCard(GuiGraphicsExtractor graphics, Font font, long t, int width, int height) {
		if (t > DAY_MS + 400) {
			return;
		}
		float alpha = 1 - Ease.progress(t, DAY_MS, 400);
		Newsreel.wash(graphics, 0x0B0907, 0.93F * alpha);
		Newsreel.grain(graphics, 0.4F * alpha);
		Newsreel.scratches(graphics, 0.7F * alpha);
		String day = Newsreel.typed("DAY " + current.day(), t, 150, 16);
		Newsreel.text(graphics, font, day, width / 2.0F, height * 0.38F, 3.5F, Newsreel.CREAM, alpha * Newsreel.flicker(), true, true);
		String caption = current.day() == 2 ? "You survived your first night." : "Sunrise. Everything you carry is taken.";
		Newsreel.text(graphics, font, caption, width / 2.0F, height * 0.38F + 44, 1, Newsreel.ASH,
			alpha * Ease.progress(t, 600, 300), true, true);
	}

	private static void drawFlyer(GuiGraphicsExtractor graphics, Flyer flyer, long t, int width, int height) {
		int slot = flyer.slot();
		float startX;
		float startY;
		int hotbarLeft = width / 2 - 91 + 3;
		if (slot < 9) {
			startX = hotbarLeft + slot * 20;
			startY = height - 19;
		} else if (slot == 40) {
			startX = hotbarLeft - 29;
			startY = height - 19;
		} else {
			// The rest of the inventory rises from just above the hotbar.
			startX = hotbarLeft + (slot % 9) * 20;
			startY = height - 42 - (slot / 9) * 5;
		}
		long local = t - flyer.delay();
		float p = Ease.progress(local, 0, FLIGHT_MS);
		if (p >= 1) {
			return;
		}
		float lift = Ease.inCubic(p);
		float x = startX + flyer.drift() * Ease.outCubic(p);
		float y = Ease.lerp(startY, -40, lift) - (local < 0 ? 0 : 6 * Ease.outCubic(Ease.progress(local, 0, 150)));
		float scale = 1 + 0.4F * (float) Math.sin(Math.min(1, p * 2) * Math.PI) - 0.3F * p;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 8, y + 8);
		graphics.pose().rotate((float) Math.toRadians(flyer.spin() * lift));
		graphics.pose().scale(scale, scale);
		graphics.item(flyer.stack(), -8, -8);
		graphics.pose().popMatrix();
	}

	/** The 3 pieces of food you're left with, handed back one at a time. */
	private static void rations(GuiGraphicsExtractor graphics, Font font, long t, int width, int height, float fadeOut) {
		if (t < RATION_AT) {
			return;
		}
		List<ItemStack> kept = current.kept();
		float y = height * 0.22F + 44;
		String label = kept.isEmpty() ? "NO FOOD LEFT TO KEEP" : "YOUR RATION";
		Newsreel.text(graphics, font, label, width / 2.0F, y, 1, Newsreel.GOLD, Ease.progress(t, RATION_AT, 250) * fadeOut, true, true);

		float glow = 0.5F + 0.5F * (float) Math.sin(t / 250.0);
		int spacing = 26;
		float rowLeft = width / 2.0F - (kept.size() - 1) * spacing / 2.0F;
		for (int i = 0; i < kept.size(); i++) {
			long at = RATION_AT + 250 + i * 250L;
			if (t < at) {
				break;
			}
			beats.sound("ration" + i, at, SoundEvents.ITEM_PICKUP, 1.2F + i * 0.15F, 0.7F);
			float pop = Ease.outBack(Ease.progress(t, at, 300)) * fadeOut;
			float cx = rowLeft + i * spacing;
			float cy = y + 24;
			graphics.fill(Math.round(cx - 11), Math.round(cy - 11), Math.round(cx + 11), Math.round(cy + 11),
				Newsreel.argb((0.25F + 0.2F * glow) * fadeOut, Newsreel.GOLD));
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx, cy);
			graphics.pose().scale(1.3F * pop, 1.3F * pop);
			graphics.item(kept.get(i), -8, -8);
			graphics.pose().popMatrix();
		}
	}
}
