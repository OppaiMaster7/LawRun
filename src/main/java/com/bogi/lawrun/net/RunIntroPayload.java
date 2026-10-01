package com.bogi.lawrun.net;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Opens the laws screen. The client knows the run's laws itself.
 *
 * @param reference true to look the laws up mid-run; false for the start, with the disclaimer and Begin
 */
public record RunIntroPayload(String runId, boolean reference) implements CustomPacketPayload {
	public static final Type<RunIntroPayload> TYPE = new Type<>(LawRun.id("run_intro"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RunIntroPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, RunIntroPayload::runId,
		ByteBufCodecs.BOOL, RunIntroPayload::reference,
		RunIntroPayload::new);

	@Override
	public Type<RunIntroPayload> type() {
		return TYPE;
	}
}
