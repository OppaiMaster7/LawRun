package com.bogi.lawrun.net;

import java.util.List;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * What sunrise just took, for the animation of it leaving.
 *
 * @param taken one stack per inventory slot that was emptied, capped by the server
 * @param total every item taken, including ones not in {@code taken}
 * @param day   the day that's starting, for the "DAY 3" card
 */
public record ConfiscationPayload(List<ItemStack> taken, List<ItemStack> kept, int total, int day)
	implements CustomPacketPayload {
	public static final Type<ConfiscationPayload> TYPE = new Type<>(LawRun.id("confiscation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConfiscationPayload> CODEC = StreamCodec.composite(
		ItemStack.OPTIONAL_LIST_STREAM_CODEC, ConfiscationPayload::taken,
		ItemStack.OPTIONAL_LIST_STREAM_CODEC, ConfiscationPayload::kept,
		ByteBufCodecs.VAR_INT, ConfiscationPayload::total,
		ByteBufCodecs.VAR_INT, ConfiscationPayload::day,
		ConfiscationPayload::new);

	@Override
	public Type<ConfiscationPayload> type() {
		return TYPE;
	}
}
