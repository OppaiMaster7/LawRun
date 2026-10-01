package com.bogi.lawrun.net;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent as a container opens during a run: whether its contents survive
 * sunrise. The client stamps the container screen with it.
 */
public record ContainerStatusPayload(boolean safe) implements CustomPacketPayload {
	public static final Type<ContainerStatusPayload> TYPE = new Type<>(LawRun.id("container_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ContainerStatusPayload> CODEC =
		ByteBufCodecs.BOOL.map(ContainerStatusPayload::new, ContainerStatusPayload::safe).cast();

	@Override
	public Type<ContainerStatusPayload> type() {
		return TYPE;
	}
}
