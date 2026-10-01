package com.bogi.lawrun.client.screen;

import java.util.List;
import java.util.Locale;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.Law;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunState;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * The run's laws, dealt onto the table as paper cards. Click one to read
 * more about it.
 * <p>
 * {@link Mode#START} opens with the disclaimer and ends with Begin.
 * {@link Mode#REFERENCE} (the J key, the chat's [Laws] button, the pause menu)
 * deals the cards in a second and just has Close.
 */
public class LawsScreen extends Screen {
	public enum Mode { START, REFERENCE }

	private final RunDefinition run;
	private final Mode mode;
	private final String title;
	private final Beats beats = new Beats();
	private boolean disclaimerAccepted;

	private final float titleSpeed;
	private final float contextSpeed;
	private final long cardEvery;
	private final long cardFlight;
	private final long titleAt;
	private long contextAt;
	private long cardsAt;
	private long buttonsAt;

	/** Where each card was drawn last frame (x, y, width, height), for hover and clicks. */
	private final int[][] cardRects;
	private int hovered = -1;

	public LawsScreen(RunDefinition run, Mode mode) {
		super(Component.literal(run.title()));
		this.run = run;
		this.mode = mode;
		// Upper case, but "1940s" keeps its small s.
		this.title = run.title().toUpperCase(Locale.ROOT).replaceAll("(\\d)S\\b", "$1s");
		this.disclaimerAccepted = mode == Mode.REFERENCE;
		boolean quick = mode == Mode.REFERENCE;
		this.titleSpeed = quick ? 400 : 14;
		this.contextSpeed = quick ? 2000 : 90;
		this.cardEvery = quick ? 110 : 420;
		this.cardFlight = quick ? 320 : 480;
		this.titleAt = quick ? 0 : 300;
		this.cardRects = new int[run.laws().size()][4];
		timeReel();
	}

	private void timeReel() {
		contextAt = titleAt + Newsreel.typingMs(title, titleSpeed) + (mode == Mode.REFERENCE ? 0 : 400);
		cardsAt = contextAt + Newsreel.typingMs(run.context(), contextSpeed) + (mode == Mode.REFERENCE ? 50 : 300);
		buttonsAt = cardsAt + run.laws().size() * cardEvery + cardFlight + (mode == Mode.REFERENCE ? 0 : 300);
	}

	@Override
	protected void init() {
		minecraft.gui.hud.clearTitles();
		int y = height - 26;
		if (!disclaimerAccepted) {
			addRenderableWidget(Button.builder(Component.literal("I understand"), button -> acceptDisclaimer())
				.bounds(width / 2 - 70, y, 140, 20).build());
		} else if (mode == Mode.START) {
			addRenderableWidget(Button.builder(Component.literal("Begin"), button -> begin())
				.bounds(width / 2 - 104, y, 100, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Not yet"), button -> onClose())
				.bounds(width / 2 + 4, y, 100, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
				.bounds(width / 2 - 50, y, 100, 20).build());
		}
	}

	private void acceptDisclaimer() {
		disclaimerAccepted = true;
		beats.restart();
		rebuildWidgets();
	}

	private void begin() {
		minecraft.player.connection.sendUnattendedCommand("lawrun start " + run.id(), null);
	}

	private boolean buttonsShown() {
		return disclaimerAccepted ? beats.ms() >= buttonsAt : beats.ms() >= 1500;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (disclaimerAccepted && beats.ms() < buttonsAt) {
			beats.skipTo(buttonsAt);
			return true;
		}
		if (disclaimerAccepted && hovered >= 0) {
			Beats.play(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.9F);
			minecraft.gui.setScreen(new LawDetailScreen(this, run, run.laws().get(hovered), hovered + 1));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (disclaimerAccepted && beats.ms() < buttonsAt && event.key() != InputConstants.KEY_ESCAPE) {
			beats.skipTo(buttonsAt);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Newsreel.wash(graphics, 0x0B0907, 0.96F);
		Newsreel.wash(graphics, 0xFFF4DD, (1 - Newsreel.flicker()) * 0.3F);
		Newsreel.vignette(graphics, 0x000000, 0.9F);
		Newsreel.grain(graphics, 0.35F);
		Newsreel.scratches(graphics, 0.6F);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		for (var child : children()) {
			if (child instanceof Button button) {
				button.visible = buttonsShown();
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (disclaimerAccepted) {
			reel(graphics, mouseX, mouseY);
		} else {
			disclaimer(graphics);
		}
	}

	/** DISCLAIMER in red; the text itself in the normal colour. */
	private void disclaimer(GuiGraphicsExtractor graphics) {
		long t = beats.ms();
		beats.sound("bell", 0, SoundEvents.BELL_BLOCK, 0.6F, 0.8F);
		float y = height * 0.14F;
		float scale = Math.min(3.2F, (width - 40) / (float) font.width("DISCLAIMER"));
		Newsreel.text(graphics, font, "DISCLAIMER", width / 2.0F, y, scale, 0xE0312B, Ease.progress(t, 0, 300), true, true);
		int ruleWidth = Math.round(font.width("DISCLAIMER") * scale);
		int ruleY = Math.round(y + 10 * scale + 4);
		graphics.fill(width / 2 - ruleWidth / 2, ruleY, width / 2 + ruleWidth / 2, ruleY + 2,
			Newsreel.argb(Ease.progress(t, 200, 400), 0xE0312B));
		Newsreel.paragraph(graphics, font, run.disclaimer(), width / 2.0F, ruleY + 14, 1, Math.min(380, width - 40),
			Newsreel.CREAM, Ease.progress(t, 400, 800), true);
	}

	private void reel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		long t = beats.ms();
		if (mode == Mode.START) {
			typingClicks(title, t, titleAt, titleSpeed, "title");
		}
		float titleScale = Math.min(2.6F, (width - 40) / (float) Math.max(1, font.width(title)));
		int y = 10;
		Newsreel.text(graphics, font, Newsreel.typed(title, t, titleAt, titleSpeed), width / 2.0F, y, titleScale,
			Newsreel.CREAM, Newsreel.flicker(), true, true);
		y += Math.round(10 * titleScale) + 4;

		int contextWidth = Math.min(380, width - 40);
		String subtitle = mode == Mode.REFERENCE ? statusLine() : run.context();
		Newsreel.paragraph(graphics, font, Newsreel.typed(subtitle, t, contextAt, contextSpeed),
			width / 2.0F, y, 0.8F, contextWidth, Newsreel.ASH, 1, true);
		y += Newsreel.paragraphHeight(font, subtitle, 0.8F, contextWidth) + 8;

		int bottom = height - 26 - 16;
		drawCards(graphics, t, y, bottom, mouseX, mouseY);

		String footer = t >= buttonsAt
			? "Click a law to read more."
			: "";
		Newsreel.text(graphics, font, footer, width / 2.0F, bottom + 4, 0.7F, Newsreel.GOLD,
			Ease.progress(t, buttonsAt, 400), true, false);
	}

	/** "In progress · Day 3 · 1h 04m", or how the run ended. */
	private String statusLine() {
		RunState state = minecraft.player == null ? null : RunManager.get(minecraft.player);
		if (state == null || !state.runId().equals(run.id())) {
			return "No run in progress. Start one with the START button in chat.";
		}
		long now = minecraft.level == null ? state.startedAt() : minecraft.level.getLevelData().getGameTime();
		String played = RunManager.formatTicks(state.lengthTicks(now));
		long day = state.stat("days") + 1;
		return switch (state.status()) {
			case ACTIVE -> "In progress · Day " + day + " · " + played;
			case FAILED -> "Run over: " + state.reason();
			case WON -> "Run complete in " + played;
		};
	}

	private void drawCards(GuiGraphicsExtractor graphics, long t, int top, int bottom, int mouseX, int mouseY) {
		List<Law> laws = run.laws();
		int columns = 3;
		int rows = (laws.size() + columns - 1) / columns;
		int gap = 8;
		int cardWidth = Math.min(160, (width - 32 - gap * (columns - 1)) / columns);
		int cardHeight = Math.min(100, (bottom - top - gap * (rows - 1)) / rows);
		int gridWidth = columns * cardWidth + (columns - 1) * gap;
		int left = (width - gridWidth) / 2;
		boolean dealt = t >= buttonsAt;

		int nowHovered = -1;
		for (int i = 0; i < laws.size(); i++) {
			int column = i % columns;
			int row = i / columns;
			int x = left + column * (cardWidth + gap);
			int y = top + row * (cardHeight + gap);
			cardRects[i] = new int[] {x, y, cardWidth, cardHeight};
			if (dealt && mouseX >= x && mouseX < x + cardWidth && mouseY >= y && mouseY < y + cardHeight) {
				nowHovered = i;
			}
		}
		if (nowHovered != hovered && nowHovered >= 0) {
			Beats.play(SoundEvents.BOOK_PAGE_TURN, 1.5F, 0.25F);
		}
		hovered = nowHovered;

		for (int i = 0; i < laws.size(); i++) {
			long at = cardsAt + i * cardEvery;
			if (t < at) {
				break;
			}
			if (mode == Mode.START) {
				beats.sound("card" + i, at, SoundEvents.BOOK_PAGE_TURN, 0.9F + i * 0.05F, 0.8F);
			}
			float p = Ease.progress(t, at, cardFlight);
			float settle = Ease.outBack(p);
			int[] rect = cardRects[i];
			float targetX = rect[0] + cardWidth / 2.0F;
			float targetY = rect[1] + cardHeight / 2.0F - (i == hovered ? 3 : 0);
			// Each card lands at its own slight angle, as if tossed onto a desk; hovering straightens it.
			float restAngle = i == hovered ? 0 : ((i * 37) % 7 - 3) * 0.8F;
			float startAngle = (i % 2 == 0 ? -1 : 1) * 16;

			graphics.pose().pushMatrix();
			graphics.pose().translate(Ease.lerp(width / 2.0F, targetX, Ease.outCubic(p)),
				Ease.lerp(height + cardHeight, targetY, settle));
			graphics.pose().rotate((float) Math.toRadians(Ease.lerp(startAngle, restAngle, Ease.outCubic(p))));
			graphics.pose().translate(-cardWidth / 2.0F, -cardHeight / 2.0F);
			drawCard(graphics, laws.get(i), i + 1, cardWidth, cardHeight);
			if (i == hovered) {
				graphics.outline(-2, -2, cardWidth + 4, cardHeight + 4, Newsreel.argb(1, Newsreel.GOLD));
			}
			graphics.pose().popMatrix();
		}
	}

	private void drawCard(GuiGraphicsExtractor graphics, Law law, int number, int cardWidth, int cardHeight) {
		Newsreel.paper(graphics, 0, 0, cardWidth, cardHeight, 1);
		Newsreel.text(graphics, font, "#" + number, 5, 5, 1, Newsreel.STAMP_RED, 1, false, false);
		Newsreel.text(graphics, font, law.date(), cardWidth - 5 - Math.round(font.width(law.date()) * 0.65F), 6, 0.65F,
			Newsreel.FADED_INK, 1, false, false);
		int nameHeight = Newsreel.paragraph(graphics, font, law.name().toUpperCase(Locale.ROOT), 5, 17, 0.9F,
			cardWidth - 10, Newsreel.INK, 1, false);
		int bodyTop = 17 + nameHeight + 3;
		int room = cardHeight - bodyTop - 4;
		float scale = 0.65F;
		while (scale > 0.45F && Newsreel.paragraphHeight(font, law.inGame(), scale, cardWidth - 10) > room) {
			scale -= 0.05F;
		}
		Newsreel.paragraph(graphics, font, law.inGame(), 5, bodyTop, scale, cardWidth - 10, Newsreel.INK, 1, false);
	}

	/** A soft key click every other character while a line types itself out. */
	private void typingClicks(String text, long t, long startMs, float speed, String cue) {
		int typed = Newsreel.typed(text, t, startMs, speed).length();
		for (int i = 1; i <= typed; i += 2) {
			beats.sound(cue + i, 0, SoundEvents.NOTE_BLOCK_HAT.value(), 1.8F, 0.35F);
		}
	}

	/** Where a law's card was last drawn (x, y, width, height), in GUI pixels. Used by the game test to click one. */
	public int[] cardRect(int index) {
		return cardRects[index].clone();
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
