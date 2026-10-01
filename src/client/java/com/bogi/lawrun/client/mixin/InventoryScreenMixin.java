package com.bogi.lawrun.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bogi.lawrun.runs.germany.SafePocket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/** The inventory texture has no frame where the safe pocket sits, so draw one, plus a tooltip when it's empty. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
	@Unique
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

	@Unique
	private static final List<FormattedCharSequence> EMPTY_TOOLTIP = List.of(
		Component.literal("Safe pocket").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
		Component.literal("Whatever you keep here survives sunrise.").withStyle(ChatFormatting.GRAY).getVisualOrderText(),
		Component.literal("No shulker boxes or bundles.").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());

	private InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBook, Inventory inventory, Component title) {
		super(menu, recipeBook, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void lawrun$drawSafePocket(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		for (Slot slot : menu.slots) {
			if (slot instanceof SafePocket.PocketSlot && slot.isActive()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
			}
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void lawrun$explainSafePocket(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (hoveredSlot instanceof SafePocket.PocketSlot && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
			graphics.setTooltipForNextFrame(EMPTY_TOOLTIP, mouseX, mouseY);
		}
	}
}
