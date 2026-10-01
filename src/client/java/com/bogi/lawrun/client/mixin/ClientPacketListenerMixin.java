package com.bogi.lawrun.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bogi.lawrun.client.screen.EndingScreen;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunRegistry;
import com.bogi.lawrun.core.RunState;
import com.bogi.lawrun.core.RunStatus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/**
 * The exit portal's "you won" packet normally opens the End Poem and credits.
 * For a won run it opens the run's own ending instead, and finishes the same
 * way: ask the server to respawn, then close.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
	@Inject(
		method = "handleGameEvent",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
			shift = At.Shift.AFTER),
		cancellable = true)
	private void lawrun$playRunEnding(ClientboundGameEventPacket packet, CallbackInfo ci) {
		if (packet.getEvent() != ClientboundGameEventPacket.WIN_GAME) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		RunState state = player == null ? null : RunManager.get(player);
		RunDefinition run = state == null ? null : RunRegistry.get(state.runId());
		if (run == null || state.status() != RunStatus.WON) {
			return;
		}
		minecraft.gui.setScreen(new EndingScreen(run, state, () -> {
			player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
			minecraft.gui.setScreen(null);
		}));
		ci.cancel();
	}
}
