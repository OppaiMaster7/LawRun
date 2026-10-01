package com.bogi.lawrun.core;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One player's attempt at a run. Times are world game time in ticks, so the
 * length of a run is play time, not wall-clock time.
 *
 * @param stats counters a run keeps for its fail and ending screens; the keys
 *              are listed in its {@link RunDefinition#stats()}
 */
public record RunState(String runId, RunStatus status, long startedAt, long endedAt, String reason, Map<String, Long> stats) {
	public static final Codec<RunState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("run").forGetter(RunState::runId),
		RunStatus.CODEC.fieldOf("status").forGetter(RunState::status),
		Codec.LONG.fieldOf("started_at").forGetter(RunState::startedAt),
		Codec.LONG.optionalFieldOf("ended_at", 0L).forGetter(RunState::endedAt),
		Codec.STRING.optionalFieldOf("reason", "").forGetter(RunState::reason),
		Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("stats", Map.of()).forGetter(RunState::stats)
	).apply(instance, RunState::new));

	public RunState {
		stats = Map.copyOf(stats);
	}

	public static RunState begin(String runId, long now) {
		return new RunState(runId, RunStatus.ACTIVE, now, 0L, "", Map.of());
	}

	public RunState end(RunStatus result, long now, String why) {
		return new RunState(runId, result, startedAt, now, why, stats);
	}

	public RunState withStat(String key, long delta) {
		Map<String, Long> updated = new HashMap<>(stats);
		updated.merge(key, delta, Long::sum);
		return new RunState(runId, status, startedAt, endedAt, reason, updated);
	}

	public long stat(String key) {
		return stats.getOrDefault(key, 0L);
	}

	public boolean active() {
		return status == RunStatus.ACTIVE;
	}

	public long lengthTicks(long now) {
		return (active() ? now : endedAt) - startedAt;
	}
}
