package com.bogi.lawrun.net;

import com.bogi.lawrun.LawRun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the client to show the card for the rule the player just broke. */
public record RulePopupPayload(int number, String name, String date, String text, String reason)
	implements CustomPacketPayload {
	public static final Type<RulePopupPayload> TYPE = new Type<>(LawRun.id("rule_popup"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RulePopupPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RulePopupPayload::number,
		ByteBufCodecs.STRING_UTF8, RulePopupPayload::name,
		ByteBufCodecs.STRING_UTF8, RulePopupPayload::date,
		ByteBufCodecs.STRING_UTF8, RulePopupPayload::text,
		ByteBufCodecs.STRING_UTF8, RulePopupPayload::reason,
		RulePopupPayload::new);

	@Override
	public Type<RulePopupPayload> type() {
		return TYPE;
	}
}
