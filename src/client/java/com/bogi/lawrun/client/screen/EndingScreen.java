package com.bogi.lawrun.client.screen;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunState;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;

/**
 * Replaces the End Poem and credits for a won run. Five scenes play in turn:
 * the title, the record, the credit to the video, the epilogue on plain
 * black, and the closing credits with Continue. Space moves on, Esc skips to
 * the end, and Continue respawns the player the same way the vanilla screen does.
 */
public class EndingScreen extends Screen {
	private enum Scene { TITLE, RECORD, SOURCE, EPILOGUE, CREDITS }

	private static final long EPILOGUE_LINE_EVERY = 3000;

	private final RunDefinition run;
	private final RunState state;
	private final Runnable onFinished;
	private final Beats beats = new Beats();
	private Scene scene = Scene.TITLE;
	private boolean finished;

	public EndingScreen(RunDefinition run, RunState state, Runnable onFinished) {
		super(Component.literal(run.title() + ": run complete"));
		this.run = run;
		this.state = state;
		this.onFinished = onFinished;
	}

	private long length(Scene which) {
		return switch (which) {
			case TITLE -> 7500;
			case RECORD -> 1500 + (run.stats().size() + 1) * 600L + run.ending().notes().size() * 900L + 4500;
			case SOURCE -> 7000;
			case EPILOGUE -> 1500 + run.ending().epilogue().size() * EPILOGUE_LINE_EVERY + 6500;
			case CREDITS -> Long.MAX_VALUE;
		};
	}

	@Override
	protected void init() {
		buildWidgets();
	}

	private void buildWidgets() {
		clearWidgets();
		int y = height - 28;
		switch (scene) {
			case SOURCE -> addRenderableWidget(Button.builder(Component.literal("Watch the video"),
					ConfirmLinkScreen.confirmLink(this, URI.create(run.source().url())))
				.bounds(width / 2 - 70, y, 140, 20).build());
			case EPILOGUE -> addRenderableWidget(Button.builder(Component.literal("Learn more"),
					ConfirmLinkScreen.confirmLink(this, URI.create(run.ending().learnMoreUrl())))
				.bounds(width / 2 - 70, y, 140, 20).build());
			case CREDITS -> addRenderableWidget(Button.builder(Component.literal("Continue"), button -> finish())
				.bounds(width / 2 - 70, y, 140, 20).build());
			default -> {
			}
		}
	}

	private void goTo(Scene next) {
		scene = next;
		beats.restart();
		buildWidgets();
	}

	private void advance() {
		if (scene != Scene.CREDITS) {
			goTo(Scene.values()[scene.ordinal() + 1]);
		}
	}

	private void finish() {
		if (!finished) {
			finished = true;
			onFinished.run();
		}
	}

	@Override
	public void tick() {
		if (beats.ms() > length(scene)) {
			advance();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		advance();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE) {
			if (scene == Scene.CREDITS) {
				finish();
			} else {
				goTo(Scene.CREDITS);
			}
			return true;
		}
		if (event.key() == InputConstants.KEY_SPACE) {
			advance();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		finish();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public Music getBackgroundMusic() {
		return Musics.CREDITS;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(0, 0, width, height, 0xFF000000);
		// The epilogue is plain black: no film effects on it.
		if (scene != Scene.EPILOGUE) {
			Newsreel.wash(graphics, 0x1A140C, 0.6F);
			Newsreel.wash(graphics, 0xFFF4DD, (1 - Newsreel.flicker()) * 0.3F);
			Newsreel.vignette(graphics, 0x000000, 0.9F);
			Newsreel.grain(graphics, 0.35F);
			Newsreel.scratches(graphics, 0.6F);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		long t = beats.ms();
		// Each scene fades up from black.
		float fadeIn = Ease.progress(t, 0, 600);
		switch (scene) {
			case TITLE -> title(graphics, t);
			case RECORD -> record(graphics, t);
			case SOURCE -> source(graphics, t);
			case EPILOGUE -> epilogue(graphics, t);
			case CREDITS -> credits(graphics, t);
		}
		if (scene != Scene.CREDITS) {
			Newsreel.text(graphics, font, "Space: next   Esc: skip", width - 6 - Math.round(font.width("Space: next   Esc: skip") * 0.6F),
				height - 9, 0.6F, Newsreel.ASH, 0.5F, false, false);
		}
		Newsreel.wash(graphics, 0x000000, 1 - fadeIn);
	}

	private void title(GuiGraphicsExtractor graphics, long t) {
		String title = run.title().toUpperCase(Locale.ROOT).replaceAll("(\\d)S\\b", "$1s");
		float scale = Math.min(3.0F, (width - 40) / (float) font.width(title));
		for (int i = 1; i <= title.length(); i += 2) {
			if (Newsreel.typed(title, t, 400, 12).length() >= i) {
				beats.sound("key" + i, 0, SoundEvents.NOTE_BLOCK_HAT.value(), 1.8F, 0.35F);
			}
		}
		Newsreel.text(graphics, font, Newsreel.typed(title, t, 400, 12), width / 2.0F, height * 0.28F, scale,
			Newsreel.CREAM, Newsreel.flicker(), true, true);
		beats.sound("bell", 1900, SoundEvents.BELL_BLOCK, 1.0F, 1.0F);
		beats.sound("stamp", 1900, SoundEvents.ANVIL_LAND, 0.8F, 0.3F);
		if (t >= 1780) {
			float land = Ease.progress(t, 1780, 220);
			Newsreel.stamp(graphics, font, "RUN COMPLETE", width / 2.0F, height * 0.28F + 10 * scale + 24, Newsreel.GOLD, -4,
				Ease.lerp(4.5F, 2.2F, Ease.outBack(land)), Math.min(0.95F, land * 2));
		}
		String line = run.ending().victoryLine();
		Newsreel.text(graphics, font, Newsreel.typed(line, t, 2800, 35), width / 2.0F, height * 0.28F + 10 * scale + 56, 1.1F,
			Newsreel.CREAM, 1, true, true);
	}

	private void record(GuiGraphicsExtractor graphics, long t) {
		int centre = width / 2;
		int y = Math.round(height * 0.12F);
		Newsreel.text(graphics, font, "THE RECORD", centre, y, 1.6F, Newsreel.GOLD, Ease.progress(t, 200, 300), true, true);
		y += 26;
		int left = centre - 130;
		int right = centre + 130;

		long timeAt = 800;
		if (t >= timeAt) {
			beats.sound("time", timeAt, SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.6F);
			long played = state.lengthTicks(state.endedAt());
			String value = RunManager.formatTicks(Math.round(played * Ease.outCubic(Ease.progress(t, timeAt, 700))));
			row(graphics, "Time played", value, left, right, y, Ease.progress(t, timeAt, 200));
		}
		y += 12;

		List<RunDefinition.StatLine> stats = run.stats();
		for (int i = 0; i < stats.size(); i++) {
			long at = timeAt + (i + 1) * 600L;
			RunDefinition.StatLine line = stats.get(i);
			if (t >= at) {
				beats.sound("stat" + i, at, SoundEvents.BOOK_PAGE_TURN, 1.0F + i * 0.05F, 0.6F);
				long value = Math.round(state.stat(line.key()) * Ease.outCubic(Ease.progress(t, at, 600)));
				float alpha = Ease.progress(t, at, 200);
				row(graphics, line.label(), String.valueOf(value), left, right, y, alpha);
				if (!line.comparison().isEmpty()) {
					Newsreel.text(graphics, font, line.comparison(), left + 6, y + 11, 0.75F, Newsreel.ASH, alpha, false, false);
				}
			}
			y += line.comparison().isEmpty() ? 12 : 21;
		}

		y += 8;
		long notesAt = timeAt + (stats.size() + 1) * 600L + 400;
		List<String> notes = run.ending().notes();
		for (int i = 0; i < notes.size(); i++) {
			float alpha = Ease.progress(t, notesAt + i * 900L, 700);
			y += Newsreel.paragraph(graphics, font, notes.get(i), centre, y, 0.9F, 300, Newsreel.CREAM, alpha, true) + 4;
		}
	}

	private void row(GuiGraphicsExtractor graphics, String label, String value, int left, int right, int y, float alpha) {
		Newsreel.text(graphics, font, label, left, y, 1, Newsreel.CREAM, alpha, false, true);
		Newsreel.text(graphics, font, value, right - font.width(value), y, 1, Newsreel.GOLD, alpha, false, true);
	}

	private void source(GuiGraphicsExtractor graphics, long t) {
		float y = height * 0.25F;
		Newsreel.text(graphics, font, "BASED ON", width / 2.0F, y, 0.8F, Newsreel.ASH, Ease.progress(t, 300, 400), true, false);
		String video = run.source().videoTitle();
		float scale = Math.min(1.4F, (width - 40) / (float) font.width(video));
		Newsreel.text(graphics, font, Newsreel.typed(video, t, 900, 40), width / 2.0F, y + 14, scale, Newsreel.CREAM, 1, true, true);
		long byAt = 900 + Newsreel.typingMs(video, 40) + 300;
		Newsreel.text(graphics, font, "by " + run.source().creator(), width / 2.0F, y + 14 + 10 * scale + 8, 1.5F,
			Newsreel.GOLD, Ease.progress(t, byAt, 500), true, true);
		Newsreel.paragraph(graphics, font,
			"Every rule in this run is his. Go and watch how he did it, dupes and all.",
			width / 2.0F, y + 14 + 10 * scale + 34, 0.8F, 300, Newsreel.ASH, Ease.progress(t, byAt + 900, 600), true);
	}

	/** Plain white on black, one line at a time, slowly. */
	private void epilogue(GuiGraphicsExtractor graphics, long t) {
		List<String> lines = run.ending().epilogue();
		int textWidth = Math.min(340, width - 40);
		int total = 0;
		for (String line : lines) {
			total += Newsreel.paragraphHeight(font, line, 1, textWidth) + 10;
		}
		float y = (height - total) / 2.0F - 12;
		for (int i = 0; i < lines.size(); i++) {
			float alpha = Ease.progress(t, 1500 + i * EPILOGUE_LINE_EVERY, 1400);
			y += Newsreel.paragraph(graphics, font, lines.get(i), width / 2.0F, y, 1, textWidth, 0xEDEDED, alpha, true) + 10;
		}
		Newsreel.paragraph(graphics, font, run.ending().learnMoreLabel(), width / 2.0F, height - 44, 0.7F, textWidth,
			Newsreel.ASH, Ease.progress(t, 1500 + lines.size() * EPILOGUE_LINE_EVERY, 1000), true);
	}

	private void credits(GuiGraphicsExtractor graphics, long t) {
		float y = height * 0.2F;
		String[][] lines = {
			{"LAWRUN", "2.0"},
			{"Made by Bogi", "1"},
			{"Rules from " + run.source().creator() + "'s video", "1"},
			{"Built with Fabric for Minecraft 26.3", "0.8"},
			{"", "1"},
			{"Thanks for playing.", "1"},
		};
		for (int i = 0; i < lines.length; i++) {
			float scale = Float.parseFloat(lines[i][1]);
			float alpha = Ease.progress(t, 300 + i * 500L, 600);
			int color = i == 0 ? Newsreel.GOLD : Newsreel.CREAM;
			Newsreel.text(graphics, font, lines[i][0], width / 2.0F, y, scale, color, alpha, true, true);
			y += 10 * scale + 8;
		}
	}
}
