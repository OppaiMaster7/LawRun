package com.bogi.lawrun.client.anim;

import java.util.List;
import java.util.Random;

import com.bogi.lawrun.LawRun;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/**
 * The 1940s newsreel look, as drawing helpers: film grain, scratches, flicker,
 * letterbox bars, vignettes, paper cards and rubber stamps. Textures come from
 * {@code tools/make_assets.py}. Colours are RGB; alpha is passed separately.
 */
public final class Newsreel {
	public static final Identifier VIGNETTE = LawRun.id("textures/gui/vignette.png");
	public static final Identifier GRAIN = LawRun.id("textures/gui/grain.png");
	public static final Identifier PAPER = LawRun.id("textures/gui/paper.png");

	public static final int INK = 0x2B2118;
	public static final int FADED_INK = 0x6B5A45;
	public static final int STAMP_RED = 0xB3261E;
	public static final int STAMP_BLUE = 0x23407A;
	public static final int CREAM = 0xF2E6C9;
	public static final int GOLD = 0xE0B04A;
	public static final int ASH = 0x9C958A;

	/** Old film runs at 24 frames a second; the grain jumps at that rate too. */
	private static final long FILM_FRAME_MS = 42;

	private Newsreel() {
	}

	public static int argb(float alpha, int rgb) {
		int a = Math.round(Ease.clamp(alpha) * 255);
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private static Random filmFrame(int salt) {
		return new Random(Util.getMillis() / FILM_FRAME_MS * 31 + salt);
	}

	/** A brightness wobble between about 0.9 and 1, changing every film frame. */
	public static float flicker() {
		return 1 - filmFrame(7).nextFloat() * 0.1F;
	}

	public static void grain(GuiGraphicsExtractor graphics, float alpha) {
		if (alpha <= 0.01F) {
			return;
		}
		Random random = filmFrame(1);
		int u = random.nextInt(128);
		int v = random.nextInt(128);
		int color = argb(alpha, 0xFFFFFF);
		for (int x = 0; x < graphics.guiWidth(); x += 256) {
			for (int y = 0; y < graphics.guiHeight(); y += 256) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, GRAIN, x, y, u, v, 256, 256, 128, 128, 256, 256, color);
			}
		}
	}

	/** Vertical scratches and dust, a few per frame. */
	public static void scratches(GuiGraphicsExtractor graphics, float alpha) {
		if (alpha <= 0.01F) {
			return;
		}
		Random random = filmFrame(2);
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int lines = random.nextInt(3);
		for (int i = 0; i < lines; i++) {
			int x = random.nextInt(width);
			int shade = random.nextBoolean() ? 0xFFFFFF : 0x000000;
			graphics.fill(x, 0, x + 1, height, argb(alpha * (0.15F + random.nextFloat() * 0.2F), shade));
		}
		int specks = random.nextInt(5);
		for (int i = 0; i < specks; i++) {
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			int size = 1 + random.nextInt(2);
			graphics.fill(x, y, x + size, y + size, argb(alpha * 0.5F, 0x000000));
		}
	}

	/** Cinema bars; {@code t} slides them in from 0 to 1. Returns the bar height. */
	public static int letterbox(GuiGraphicsExtractor graphics, float t) {
		int bar = Math.round(graphics.guiHeight() * 0.11F * Ease.outCubic(t));
		if (bar > 0) {
			graphics.fill(0, 0, graphics.guiWidth(), bar, 0xFF000000);
			graphics.fill(0, graphics.guiHeight() - bar, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
		}
		return bar;
	}

	public static void vignette(GuiGraphicsExtractor graphics, int rgb, float alpha) {
		if (alpha > 0.01F) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0, 0,
				graphics.guiWidth(), graphics.guiHeight(), 256, 256, 256, 256, argb(alpha, rgb));
		}
	}

	public static void wash(GuiGraphicsExtractor graphics, int rgb, float alpha) {
		if (alpha > 0.01F) {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), argb(alpha, rgb));
		}
	}

	/** An aged paper card with a drop shadow and a dark edge. */
	public static void paper(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float alpha) {
		graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, argb(alpha * 0.45F, 0x000000));
		int color = argb(alpha, 0xFFFFFF);
		for (int px = 0; px < width; px += 128) {
			for (int py = 0; py < height; py += 128) {
				int w = Math.min(128, width - px);
				int h = Math.min(128, height - py);
				graphics.blit(RenderPipelines.GUI_TEXTURED, PAPER, x + px, y + py, 0, 0, w, h, 128, 128, color);
			}
		}
		graphics.outline(x, y, width, height, argb(alpha * 0.8F, 0x5A4632));
	}

	/**
	 * A rubber stamp: text in a double-ruled box, rotated, drawn at
	 * {@code scale}. Animate the scale from about 3 down to 1 with
	 * {@link Ease#outBack} for the slam.
	 */
	public static void stamp(GuiGraphicsExtractor graphics, Font font, String text, float centerX, float centerY,
		int rgb, float degrees, float scale, float alpha) {
		if (alpha <= 0.01F || scale <= 0) {
			return;
		}
		int color = argb(alpha, rgb);
		int textWidth = font.width(text);
		int halfWidth = textWidth / 2 + 5;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, centerY);
		graphics.pose().rotate((float) Math.toRadians(degrees));
		graphics.pose().scale(scale, scale);
		graphics.outline(-halfWidth, -9, halfWidth * 2, 18, color);
		graphics.outline(-halfWidth + 2, -7, halfWidth * 2 - 4, 14, color);
		graphics.text(font, text, -textWidth / 2, -4, color, false);
		graphics.pose().popMatrix();
	}

	/** One line of text at a scale: centred on {@code x}, or starting there. */
	public static void text(GuiGraphicsExtractor graphics, Font font, String text, float x, float y, float scale,
		int rgb, float alpha, boolean centred, boolean shadow) {
		if (alpha <= 0.01F || text.isEmpty()) {
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		int offset = centred ? -font.width(text) / 2 : 0;
		graphics.text(font, text, offset, 0, argb(alpha, rgb), shadow);
		graphics.pose().popMatrix();
	}

	/** Wrapped text at a scale. Returns the height used, in GUI pixels. */
	public static int paragraph(GuiGraphicsExtractor graphics, Font font, String text, float x, float y, float scale,
		int maxWidth, int rgb, float alpha, boolean centred) {
		List<FormattedCharSequence> lines = font.split(FormattedText.of(text), Math.round(maxWidth / scale));
		if (alpha > 0.01F) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().scale(scale, scale);
			for (int i = 0; i < lines.size(); i++) {
				int offset = centred ? -font.width(lines.get(i)) / 2 : 0;
				graphics.text(font, lines.get(i), offset, i * 10, argb(alpha, rgb), false);
			}
			graphics.pose().popMatrix();
		}
		return Math.round(lines.size() * 10 * scale);
	}

	public static int paragraphHeight(Font font, String text, float scale, int maxWidth) {
		return Math.round(font.split(FormattedText.of(text), Math.round(maxWidth / scale)).size() * 10 * scale);
	}

	/** How much of {@code text} a typewriter that started at {@code startMs} has typed by now. */
	public static String typed(String text, long elapsedMs, long startMs, float charsPerSecond) {
		if (elapsedMs < startMs) {
			return "";
		}
		int chars = (int) ((elapsedMs - startMs) * charsPerSecond / 1000);
		return text.substring(0, Math.min(text.length(), chars));
	}

	public static long typingMs(String text, float charsPerSecond) {
		return (long) (text.length() * 1000 / charsPerSecond);
	}

	/** A random offset for screen shake, at most {@code strength} pixels. */
	public static float shake(float strength, int salt) {
		if (strength <= 0) {
			return 0;
		}
		return (new Random(Util.getMillis() / 16 * 17 + salt).nextFloat() - 0.5F) * 2 * strength;
	}
}
