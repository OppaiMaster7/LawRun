package com.bogi.lawrun.net;

import java.util.List;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Shows one of the run's notices. The client looks up the text by id.
 *
 * @param args fill the notice body's {@code %s} placeholders
 */
public record NoticePayload(String runId, String noticeId, List<String> args) implements CustomPacketPayload {
	public static final Type<NoticePayload> TYPE = new Type<>(LawRun.id("notice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NoticePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, NoticePayload::runId,
		ByteBufCodecs.STRING_UTF8, NoticePayload::noticeId,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), NoticePayload::args,
		NoticePayload::new);

	@Override
	public Type<NoticePayload> type() {
		return TYPE;
	}
}
