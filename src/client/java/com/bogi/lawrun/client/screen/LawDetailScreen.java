package com.bogi.lawrun.client.screen;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bogi.lawrun.client.anim.Beats;
import com.bogi.lawrun.client.anim.Ease;
import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.core.Law;
import com.bogi.lawrun.core.RunDefinition;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * One law, in full, on a sheet of paper: what the real law was, how the run
 * enforces it, how to survive it, and how GuyBordo handled it, with a link
 * to that moment in his video.
 */
public class LawDetailScreen extends Screen {
	private final Screen parent;
	private final RunDefinition run;
	private final Law law;
	private final int number;
	private final Beats beats = new Beats();

	public LawDetailScreen(Screen parent, RunDefinition run, Law law, int number) {
		super(Component.literal(law.name()));
		this.parent = parent;
		this.run = run;
		this.law = law;
		this.number = number;
	}

	@Override
	protected void init() {
		int y = height - 26;
		String when = String.format("%d:%02d", law.videoAt() / 60, law.videoAt() % 60);
		addRenderableWidget(Button.builder(Component.literal("Watch this moment (" + when + ")"),
				ConfirmLinkScreen.confirmLink(this, URI.create(run.source().urlAt(law.videoAt()))))
			.bounds(width / 2 - 154, y, 200, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
			.bounds(width / 2 + 50, y, 104, 20).build());
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return parent == null || parent.isPauseScreen();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Newsreel.wash(graphics, 0x0B0907, 0.96F);
		Newsreel.vignette(graphics, 0x000000, 0.9F);
		Newsreel.grain(graphics, 0.3F);
		Newsreel.scratches(graphics, 0.4F);
	}

	private record Section(String label, String body) {
	}

	private List<Section> sections() {
		StringBuilder tips = new StringBuilder();
		for (String tip : law.tips()) {
			if (!tips.isEmpty()) {
				tips.append('\n');
			}
			tips.append("- ").append(tip);
		}
		List<Section> sections = new ArrayList<>();
		sections.add(new Section("THE LAW", law.history()));
		sections.add(new Section("IN THIS RUN", law.inGame()));
		sections.add(new Section("HOW TO SURVIVE IT", tips.toString()));
		sections.add(new Section("IN THE VIDEO", law.inVideo()));
		return sections;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		long t = beats.ms();
		beats.sound("page", 0, SoundEvents.BOOK_PAGE_TURN, 0.9F, 1.0F);

		int pageWidth = Math.min(440, width - 30);
		int maxPageHeight = height - 44;
		int textWidth = pageWidth - 24;
		List<Section> sections = sections();

		// Shrink the body text until everything fits, then size the page to the text.
		float scale = 0.9F;
		while (scale > 0.55F && contentHeight(sections, scale, textWidth) > maxPageHeight - 52) {
			scale -= 0.05F;
		}
		int pageHeight = Math.min(maxPageHeight, 52 + contentHeight(sections, scale, textWidth) + 8);
		int top = Math.max(8, (height - 32 - pageHeight) / 2);

		float slide = Ease.outBack(Ease.progress(t, 0, 380));
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0F, Ease.lerp(height + pageHeight / 2.0F, top + pageHeight / 2.0F, slide));
		graphics.pose().rotate((float) Math.toRadians(Ease.lerp(-4, -0.6F, Ease.outCubic(Ease.progress(t, 0, 380)))));
		graphics.pose().translate(-pageWidth / 2.0F, -pageHeight / 2.0F);

		Newsreel.paper(graphics, 0, 0, pageWidth, pageHeight, 1);
		Newsreel.text(graphics, font, "RULE #" + number, 12, 10, 1, Newsreel.STAMP_RED, 1, false, false);
		Newsreel.text(graphics, font, law.date(), pageWidth - 12 - font.width(law.date()), 10, 1, Newsreel.FADED_INK, 1, false, false);
		String name = law.name().toUpperCase(Locale.ROOT);
		float nameScale = Math.min(1.8F, textWidth / (float) font.width(name));
		Newsreel.text(graphics, font, name, 12, 22, nameScale, Newsreel.INK, 1, false, false);
		int y = 22 + Math.round(10 * nameScale) + 4;
		graphics.fill(12, y, pageWidth - 12, y + 1, Newsreel.argb(0.6F, Newsreel.FADED_INK));
		y += 6;

		for (Section section : sections) {
			Newsreel.text(graphics, font, section.label(), 12, y, 0.7F, Newsreel.STAMP_RED, 1, false, false);
			y += 9;
			for (String line : section.body().split("\n")) {
				y += Newsreel.paragraph(graphics, font, line, 12, y, scale, textWidth, Newsreel.INK, 1, false);
			}
			y += 6;
		}
		graphics.pose().popMatrix();
	}

	private int contentHeight(List<Section> sections, float scale, int textWidth) {
		int total = 0;
		for (Section section : sections) {
			total += 9 + 6;
			for (String line : section.body().split("\n")) {
				total += Newsreel.paragraphHeight(font, line, scale, textWidth);
			}
		}
		return total;
	}
}
