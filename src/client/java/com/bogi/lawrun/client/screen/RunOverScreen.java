package com.bogi.lawrun.client.screen;

import java.util.List;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.Law;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunRegistry;
import com.bogi.lawrun.net.RunOverPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * "RUN OVER": which rule was broken, how long the run lasted and what it
 * counted, stamped and counted up newsreel-style over the greyed world.
 */
public class RunOverScreen extends Screen {
	private static final long STAMP_AT = 250;
	private static final long RULE_AT = 700;
	private static final long LASTED_AT = 1400;
	private static final long STATS_AT = 1800;
	private static final long STAT_EVERY = 450;
	private static final long COUNT_MS = 500;

	private final RunOverPayload result;
	private final RunDefinition run;
	private final Beats beats = new Beats();
	private Button spectate;
	private Button quit;

	public RunOverScreen(RunOverPayload result) {
		super(Component.literal("Run over"));
		this.result = result;
		this.run = RunRegistry.get(result.runId());
	}

	private List<RunDefinition.StatLine> stats() {
		return run == null ? List.of() : run.stats();
	}

	private long buttonsAt() {
		return STATS_AT + stats().size() * STAT_EVERY + 300;
	}

	@Override
	protected void init() {
		int y = height - Math.round(height * 0.11F) - 26;
		spectate = addRenderableWidget(Button.builder(Component.literal("Spectate"), button -> onClose())
			.bounds(width / 2 - 104, y, 100, 20).build());
		quit = addRenderableWidget(Button.builder(Component.literal("Quit to title"),
				button -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE))
			.bounds(width / 2 + 4, y, 100, 20).build());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (beats.ms() < buttonsAt()) {
			beats.skipTo(buttonsAt());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (beats.ms() < buttonsAt() && event.key() != InputConstants.KEY_ESCAPE) {
			beats.skipTo(buttonsAt());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		long t = beats.ms();
		// The arrest ends on black; lift it to a dark veil over the greyed world.
		Newsreel.wash(graphics, 0x000000, Ease.lerp(1.0F, 0.86F, Ease.progress(t, 0, 600)));
		Newsreel.vignette(graphics, 0x000000, 0.8F);
		Newsreel.grain(graphics, 0.3F);
		Newsreel.scratches(graphics, 0.5F);
		Newsreel.letterbox(graphics, 1);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		long t = beats.ms();
		boolean showButtons = t >= buttonsAt();
		spectate.visible = showButtons;
		quit.visible = showButtons;
		super.extractRenderState(graphics, mouseX, mouseY, a);

		beats.sound("stamp", STAMP_AT, SoundEvents.ANVIL_LAND, 0.55F, 0.4F);
		float flicker = Newsreel.flicker();
		int centre = width / 2;
		int y = Math.round(height * 0.11F) + 16;

		float land = Ease.progress(t, STAMP_AT - 120, 200);
		if (t >= STAMP_AT - 120) {
			Newsreel.stamp(graphics, font, "RUN OVER", centre, y + 4, Newsreel.STAMP_RED, -4,
				Ease.lerp(4.5F, 2.2F, Ease.outBack(land)), 0.95F);
		}
		y += 30;

		String heading;
		if (result.lawNumber() > 0 && run != null && result.lawNumber() <= run.laws().size()) {
			Law law = run.laws().get(result.lawNumber() - 1);
			heading = "RULE #" + result.lawNumber() + " · " + law.name().toUpperCase() + " · " + law.date();
		} else {
			heading = "YOU DIED";
		}
		Newsreel.text(graphics, font, Newsreel.typed(heading, t, RULE_AT, 60), centre, y, 1, Newsreel.CREAM,
			flicker, true, true);
		String reason = reasonLine();
		Newsreel.text(graphics, font, reason, centre, y + 12, 1, 0xE04A3A,
			Ease.progress(t, RULE_AT + Newsreel.typingMs(heading, 60), 250), true, true);
		y += 30;

		long days = result.stats().getOrDefault("days", 0L);
		String lasted = "Lasted " + RunManager.formatTicks(result.lengthTicks()) + " · " + days + (days == 1 ? " day" : " days");
		Newsreel.text(graphics, font, lasted, centre, y, 1, Newsreel.ASH, Ease.progress(t, LASTED_AT, 300), true, true);
		y += 18;

		List<RunDefinition.StatLine> lines = stats();
		int left = centre - 120;
		int right = centre + 120;
		for (int i = 0; i < lines.size(); i++) {
			RunDefinition.StatLine line = lines.get(i);
			long at = STATS_AT + i * STAT_EVERY;
			if (t < at) {
				break;
			}
			beats.sound("stat" + i, at, SoundEvents.BOOK_PAGE_TURN, 1.1F, 0.6F);
			float alpha = Ease.progress(t, at, 200);
			long value = result.stats().getOrDefault(line.key(), 0L);
			long shown = Math.round(value * Ease.outCubic(Ease.progress(t, at, COUNT_MS)));
			Newsreel.text(graphics, font, line.label(), left, y, 1, Newsreel.CREAM, alpha, false, true);
			String number = String.valueOf(shown);
			Newsreel.text(graphics, font, number, right - font.width(number), y, 1, Newsreel.GOLD, alpha, false, true);
			y += 11;
			if (!line.comparison().isEmpty()) {
				Newsreel.text(graphics, font, line.comparison(), left + 6, y, 0.75F, Newsreel.ASH, alpha, false, false);
				y += 9;
			}
		}
	}

	private String reasonLine() {
		String reason = result.reason();
		int colon = reason.indexOf(": ");
		// The stored reason is "Law name: what happened"; the heading already names the law.
		return result.lawNumber() > 0 && colon >= 0 ? reason.substring(colon + 2) : reason;
	}
}
