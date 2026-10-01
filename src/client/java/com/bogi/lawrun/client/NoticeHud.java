package com.bogi.lawrun.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.Notice;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunRegistry;
import com.bogi.lawrun.net.NoticePayload;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

/**
 * Slips of paper that slide in from the left: rule tutorials, warnings and
 * moments from the video. One at a time, never over a cinematic.
 */
final class NoticeHud implements HudElement {
	private static final int WIDTH = 200;
	private static final long SLIDE_MS = 350;
	private static final float TYPE_SPEED = 70;

	private record Slip(Notice notice, String body, long length) {
	}

	private static final Deque<Slip> queue = new ArrayDeque<>();
	private static Slip current;
	private static final Beats beats = new Beats();
	/** When a screen covered the current slip; it resumes where it was once the screen closes. */
	private static long pausedAt;

	static void show(NoticePayload payload) {
		RunDefinition run = RunRegistry.get(payload.runId());
		Notice notice = run == null ? null : run.notice(payload.noticeId());
		if (notice == null) {
			return;
		}
		String body = payload.args().isEmpty() ? notice.body() : String.format(notice.body(), payload.args().toArray());
		long length = Math.max(6000, Math.min(9500, 3500 + body.length() * 45L));
		queue.add(new Slip(notice, body, length));
	}

	static boolean idle() {
		return current == null && queue.isEmpty();
	}

	static void clear() {
		queue.clear();
		current = null;
	}

	/** Other moments own the screen; notices wait their turn. */
	private static boolean busy() {
		return ArrestCinematic.isShowing() || ConfiscationCinematic.isPlaying() || VictoryCinematic.isPlaying()
			|| PermitCinematic.isPlaying();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		// Behind an open screen the slip would be half hidden and unread: hold it until the screen closes.
		if (Minecraft.getInstance().gui.screen() != null) {
			if (pausedAt == 0) {
				pausedAt = Util.getMillis();
			}
			return;
		}
		if (pausedAt != 0) {
			beats.delay(Util.getMillis() - pausedAt);
			pausedAt = 0;
		}
		if (current == null) {
			if (queue.isEmpty() || busy()) {
				return;
			}
			current = queue.poll();
			beats.restart();
		}
		long t = beats.ms();
		if (t > current.length()) {
			current = null;
			return;
		}
		Notice notice = current.notice();
		beats.sound("paper", 0, SoundEvents.BOOK_PAGE_TURN, 1.1F, 0.8F);
		if (notice.kind() == Notice.Kind.WARNING) {
			beats.sound("bell", 0, SoundEvents.BELL_BLOCK, 1.6F, 0.6F);
		}

		Font font = Minecraft.getInstance().font;
		int textWidth = WIDTH - 16;
		int bodyHeight = Newsreel.paragraphHeight(font, current.body(), 0.8F, textWidth);
		int height = 14 + 6 + 10 + 3 + bodyHeight + 7;
		float in = Ease.outBack(Ease.progress(t, 0, SLIDE_MS));
		float out = Ease.inCubic(Ease.progress(t, current.length() - SLIDE_MS, SLIDE_MS));
		float x = Ease.lerp(-WIDTH - 10, 6, in) - out * (WIDTH + 16);
		float y = graphics.guiHeight() * 0.2F;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().rotate((float) Math.toRadians(-1.2));
		Newsreel.paper(graphics, 0, 0, WIDTH, height, 1);
		graphics.fill(0, 0, WIDTH, 14, Newsreel.argb(1, headerColor(notice.kind())));
		graphics.text(font, header(notice), 6, 3, 0xFF000000 | Newsreel.CREAM, false);
		Newsreel.text(graphics, font, notice.title().toUpperCase(Locale.ROOT), 8, 20, 1, Newsreel.INK, 1, false, false);
		Newsreel.paragraph(graphics, font, Newsreel.typed(current.body(), t, SLIDE_MS, TYPE_SPEED), 8, 33, 0.8F,
			textWidth, Newsreel.INK, 1, false);
		graphics.pose().popMatrix();
	}

	private static String header(Notice notice) {
		return switch (notice.kind()) {
			case LAW -> "RULE #" + notice.lawNumber();
			case TIP -> "NOTICE";
			case VIDEO -> "FROM THE VIDEO";
			case WARNING -> "WARNING";
		};
	}

	private static int headerColor(Notice.Kind kind) {
		return switch (kind) {
			case LAW -> Newsreel.STAMP_RED;
			case TIP -> Newsreel.STAMP_BLUE;
			case VIDEO -> 0x8A6414;
			case WARNING -> 0x8E0E0E;
		};
	}
}
