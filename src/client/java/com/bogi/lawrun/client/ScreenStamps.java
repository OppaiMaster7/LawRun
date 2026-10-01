package com.bogi.lawrun.client;

import com.bogi.lawrun.client.anim.Newsreel;
import com.bogi.lawrun.client.mixin.AbstractContainerScreenAccessor;
import com.bogi.lawrun.net.ContainerStatusPayload;
import com.bogi.lawrun.runs.germany.GermanyRules;
import com.bogi.lawrun.runs.germany.Weapons;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.Slot;

/**
 * Stamps on container screens during a run: SAFE or NOT SAFE on storage,
 * TRADING BANNED on the trade screen, and a red mark on any weapon you
 * might be about to pick up.
 */
final class ScreenStamps {
	/** The server's verdict on the container being opened, if it arrived just now. */
	private static Boolean pendingSafe;
	private static long pendingAt;

	private ScreenStamps() {
	}

	static void containerStatus(ContainerStatusPayload payload) {
		pendingSafe = payload.safe();
		pendingAt = Util.getMillis();
	}

	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container) || client.player == null
				|| !GermanyRules.isOn(client.player)) {
				return;
			}
			// The verdict is sent just before the screen opens; anything older belongs to another container.
			Boolean safe = pendingSafe != null && Util.getMillis() - pendingAt < 1500 ? pendingSafe : null;
			pendingSafe = null;
			ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, delta) ->
				draw(container, graphics, safe));
		});

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null && GermanyRules.isOn(minecraft.player)
				&& Weapons.isWeapon(stack) && !Weapons.isBannedDisplay(stack)) {
				lines.add(Math.min(1, lines.size()),
					Component.literal("Rule #1: picking this up ends the run").withStyle(ChatFormatting.RED));
			}
		});
	}

	private static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, Boolean safe) {
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
		int left = panel.lawrun$leftPos();
		int top = panel.lawrun$topPos();
		int panelWidth = panel.lawrun$imageWidth();
		Font font = Minecraft.getInstance().font;

		for (Slot slot : screen.getMenu().slots) {
			if (slot.isActive() && Weapons.isWeapon(slot.getItem()) && !Weapons.isBannedDisplay(slot.getItem())) {
				int x = left + slot.x;
				int y = top + slot.y;
				graphics.fill(x, y, x + 16, y + 16, 0x70E0201A);
				graphics.outline(x - 1, y - 1, 18, 18, 0xFFE0201A);
			}
		}

		if (screen instanceof MerchantScreen) {
			float y = top >= 26 ? top - 12 : top + 14;
			Newsreel.stamp(graphics, font, "TRADING BANNED", left + panelWidth / 2.0F, y, Newsreel.STAMP_RED, -3, 1.3F, 0.95F);
			Newsreel.text(graphics, font, "Taking a trade ends the run", left + panelWidth / 2.0F, y + 13, 0.7F,
				0xFF6B5E, 1, true, true);
		} else if (safe != null && !(screen instanceof InventoryScreen)) {
			String stamp = safe ? "SAFE" : "NOT SAFE";
			String caption = safe ? "world chest: kept at sunrise" : "you placed this: emptied at sunrise";
			int color = safe ? 0x3F6FD8 : Newsreel.STAMP_RED;
			// Above the panel where there's room; toasts cover the top right corner.
			float x = left + panelWidth / 2.0F + 20;
			float y = top >= 30 ? top - 16 : top + 10;
			Newsreel.stamp(graphics, font, stamp, x, y, color, -6, 1.3F, 0.95F);
			Newsreel.text(graphics, font, caption, x, y + 13, 0.65F, safe ? 0x8FB4FF : 0xFF6B5E, 1, true, true);
		}
	}
}
