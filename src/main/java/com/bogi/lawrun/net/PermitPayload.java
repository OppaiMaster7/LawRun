package com.bogi.lawrun.net;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A travel permit was just bought: stamp it on screen. */
public record PermitPayload() implements CustomPacketPayload {
	public static final PermitPayload INSTANCE = new PermitPayload();
	public static final Type<PermitPayload> TYPE = new Type<>(LawRun.id("permit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PermitPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<PermitPayload> type() {
		return TYPE;
	}
}
