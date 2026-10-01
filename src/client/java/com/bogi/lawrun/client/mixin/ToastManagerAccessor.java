package com.bogi.lawrun.client.mixin;

import java.util.BitSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.toasts.ToastManager;

/** Which of the 5 toast rows at the top right are taken, so the curfew timer can sit below them. */
@Mixin(ToastManager.class)
public interface ToastManagerAccessor {
	@Accessor("occupiedSlots")
	BitSet lawrun$getOccupiedSlots();
}
