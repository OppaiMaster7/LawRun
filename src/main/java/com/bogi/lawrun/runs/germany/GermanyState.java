package com.bogi.lawrun.runs.germany;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player progress through the 1940s Germany run.
 *
 * @param startClock Overworld clock tick the run began on, so a run started at
 *                   night isn't failed for curfew before the player can hide
 * @param lastDay    the last Overworld day the player was plundered on
 */
public record GermanyState(long startClock, long lastDay, boolean permit) {
	public static final Codec<GermanyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.LONG.fieldOf("start_clock").forGetter(GermanyState::startClock),
		Codec.LONG.fieldOf("last_day").forGetter(GermanyState::lastDay),
		Codec.BOOL.optionalFieldOf("permit", false).forGetter(GermanyState::permit)
	).apply(instance, GermanyState::new));

	public static GermanyState begin(long clock) {
		return new GermanyState(clock, Curfew.day(clock), false);
	}

	public GermanyState plunderedOn(long day) {
		return new GermanyState(startClock, day, permit);
	}

	public GermanyState withPermit() {
		return new GermanyState(startClock, lastDay, true);
	}
}
