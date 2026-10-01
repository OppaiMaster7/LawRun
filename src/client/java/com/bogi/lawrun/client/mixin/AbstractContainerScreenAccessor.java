package com.bogi.lawrun.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Where a container screen's panel sits, so stamps can be placed on it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int lawrun$leftPos();

	@Accessor("topPos")
	int lawrun$topPos();

	@Accessor("imageWidth")
	int lawrun$imageWidth();
}
