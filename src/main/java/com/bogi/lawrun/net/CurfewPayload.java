package com.bogi.lawrun.net;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent twice a second while a curfew run is active; the HUD counts down
 * between updates and hides itself when they stop.
 *
 * @param ticksLeft  until curfew starts, or until sunrise if it already has
 * @param enforced   false on the night a run was started
 * @param graceLeft  ticks until an exposed player fails, or -1 if not exposed
 * @param diamonds   toward the travel permit, or -1 once it's bought
 */
public record CurfewPayload(boolean curfew, int ticksLeft, boolean enforced, int graceLeft, int diamonds)
	implements CustomPacketPayload {
	public static final Type<CurfewPayload> TYPE = new Type<>(LawRun.id("curfew"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CurfewPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, CurfewPayload::curfew,
		ByteBufCodecs.VAR_INT, CurfewPayload::ticksLeft,
		ByteBufCodecs.BOOL, CurfewPayload::enforced,
		ByteBufCodecs.VAR_INT, CurfewPayload::graceLeft,
		ByteBufCodecs.VAR_INT, CurfewPayload::diamonds,
		CurfewPayload::new);

	@Override
	public Type<CurfewPayload> type() {
		return TYPE;
	}
}
