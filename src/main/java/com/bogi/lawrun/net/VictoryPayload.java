package com.bogi.lawrun.net;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The dragon just died: play the in-world victory moment. The full ending waits for the exit portal. */
public record VictoryPayload(String runId) implements CustomPacketPayload {
	public static final Type<VictoryPayload> TYPE = new Type<>(LawRun.id("victory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VictoryPayload> CODEC =
		ByteBufCodecs.STRING_UTF8.map(VictoryPayload::new, VictoryPayload::runId).cast();

	@Override
	public Type<VictoryPayload> type() {
		return TYPE;
	}
}
