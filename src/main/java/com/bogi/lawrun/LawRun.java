package com.bogi.lawrun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bogi.lawrun.core.LawRunCommand;
import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.core.RunRegistry;
import com.bogi.lawrun.net.ConfiscationPayload;
import com.bogi.lawrun.net.ContainerStatusPayload;
import com.bogi.lawrun.net.CurfewPayload;
import com.bogi.lawrun.net.NoticePayload;
import com.bogi.lawrun.net.PermitPayload;
import com.bogi.lawrun.net.RulePopupPayload;
import com.bogi.lawrun.net.RunIntroPayload;
import com.bogi.lawrun.net.RunOverPayload;
import com.bogi.lawrun.net.VictoryPayload;
import com.bogi.lawrun.runs.germany.Germany1940s;
import com.bogi.lawrun.runs.germany.GermanyRules;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

public class LawRun implements ModInitializer {
	public static final String MOD_ID = "lawrun";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(RulePopupPayload.TYPE, RulePopupPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CurfewPayload.TYPE, CurfewPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RunIntroPayload.TYPE, RunIntroPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RunOverPayload.TYPE, RunOverPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VictoryPayload.TYPE, VictoryPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConfiscationPayload.TYPE, ConfiscationPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PermitPayload.TYPE, PermitPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NoticePayload.TYPE, NoticePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ContainerStatusPayload.TYPE, ContainerStatusPayload.CODEC);

		RunRegistry.register(Germany1940s.RUN);

		RunManager.init();
		GermanyRules.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
			LawRunCommand.register(dispatcher));
	}
}
