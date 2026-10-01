package com.bogi.lawrun.net;

import java.util.HashMap;
import java.util.Map;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Opens the fail screen.
 *
 * @param lawNumber the broken rule's number, or 0 when the run ended another way (death)
 */
public record RunOverPayload(String runId, int lawNumber, String reason, long lengthTicks, Map<String, Long> stats)
	implements CustomPacketPayload {
	public static final Type<RunOverPayload> TYPE = new Type<>(LawRun.id("run_over"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RunOverPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, RunOverPayload::runId,
		ByteBufCodecs.VAR_INT, RunOverPayload::lawNumber,
		ByteBufCodecs.STRING_UTF8, RunOverPayload::reason,
		ByteBufCodecs.VAR_LONG, RunOverPayload::lengthTicks,
		ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), RunOverPayload::stats,
		RunOverPayload::new);

	@Override
	public Type<RunOverPayload> type() {
		return TYPE;
	}
}
