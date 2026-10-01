package com.bogi.lawrun.client;

import com.bogi.lawrun.LawRun;
import com.bogi.lawrun.client.screen.LawsScreen;
import com.bogi.lawrun.client.screen.RunOverScreen;
import com.bogi.lawrun.core.RunDefinition;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunRegistry;
import com.bogi.lawrun.core.RunState;
import com.bogi.lawrun.net.ConfiscationPayload;
import com.bogi.lawrun.net.ContainerStatusPayload;
import com.bogi.lawrun.net.CurfewPayload;
import com.bogi.lawrun.net.NoticePayload;
import com.bogi.lawrun.net.PermitPayload;
import com.bogi.lawrun.net.RulePopupPayload;
import com.bogi.lawrun.net.RunIntroPayload;
import com.bogi.lawrun.net.RunOverPayload;
import com.bogi.lawrun.net.VictoryPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

public class LawRunClient implements ClientModInitializer {
	private static KeyMapping lawsKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(RunIntroPayload.TYPE, (payload, context) -> {
			RunDefinition run = RunRegistry.get(payload.runId());
			if (run != null) {
				context.client().gui.setScreen(new LawsScreen(run,
					payload.reference() ? LawsScreen.Mode.REFERENCE : LawsScreen.Mode.START));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(CurfewPayload.TYPE, (payload, context) -> CurfewHud.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(ConfiscationPayload.TYPE,
			(payload, context) -> ConfiscationCinematic.start(payload));
		ClientPlayNetworking.registerGlobalReceiver(PermitPayload.TYPE, (payload, context) -> PermitCinematic.start());
		ClientPlayNetworking.registerGlobalReceiver(RulePopupPayload.TYPE, (payload, context) -> ArrestCinematic.start(payload));
		ClientPlayNetworking.registerGlobalReceiver(RunOverPayload.TYPE,
			(payload, context) -> context.client().gui.setScreen(new RunOverScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(VictoryPayload.TYPE, (payload, context) -> VictoryCinematic.start(payload));
		ClientPlayNetworking.registerGlobalReceiver(NoticePayload.TYPE, (payload, context) -> NoticeHud.show(payload));
		ClientPlayNetworking.registerGlobalReceiver(ContainerStatusPayload.TYPE,
			(payload, context) -> ScreenStamps.containerStatus(payload));

		HudElementRegistry.addLast(LawRun.id("curfew"), new CurfewHud());
		HudElementRegistry.addLast(LawRun.id("notices"), new NoticeHud());
		HudElementRegistry.addLast(LawRun.id("confiscation"), new ConfiscationCinematic());
		HudElementRegistry.addLast(LawRun.id("permit"), new PermitCinematic());
		HudElementRegistry.addLast(LawRun.id("victory"), new VictoryCinematic());
		// Last, so it covers everything else on the HUD.
		HudElementRegistry.addLast(LawRun.id("arrest"), new ArrestCinematic());

		ScreenStamps.register();

		lawsKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.lawrun.laws", InputConstants.KEY_J, KeyMapping.Category.MISC));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (lawsKey.consumeClick()) {
				openLaws(client);
			}
		});

		// A way back to the laws from the pause menu, mid-run.
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof PauseScreen && client.player != null && RunManager.get(client.player) != null) {
				Screens.getWidgets(screen).add(Button.builder(Component.literal("The Laws"), button -> openLaws(client))
					.bounds(6, 6, 90, 20).build());
			}
		});
	}

	/** For the mod page screenshot harness: true once every notice slip has come and gone. */
	public static boolean noticesIdle() {
		return NoticeHud.idle();
	}

	/** For the mod page screenshot harness: drop any queued notice slips. */
	public static void clearNotices() {
		NoticeHud.clear();
	}

	/** Mid-run: the laws for reference. No run yet: the start of one, disclaimer first. */
	static void openLaws(Minecraft client) {
		if (client.player == null) {
			return;
		}
		RunState state = RunManager.get(client.player);
		RunDefinition run = state != null ? RunRegistry.get(state.runId()) : RunRegistry.all().stream().findFirst().orElse(null);
		if (run != null) {
			client.gui.setScreen(new LawsScreen(run, state != null ? LawsScreen.Mode.REFERENCE : LawsScreen.Mode.START));
		}
	}
}
