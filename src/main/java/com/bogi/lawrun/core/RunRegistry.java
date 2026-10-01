package com.bogi.lawrun.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RunRegistry {
	private static final Map<String, RunDefinition> RUNS = new LinkedHashMap<>();

	private RunRegistry() {
	}

	public static void register(RunDefinition run) {
		if (RUNS.putIfAbsent(run.id(), run) != null) {
			throw new IllegalStateException("Duplicate run id: " + run.id());
		}
	}

	/** Returns null for an unknown id. */
	public static RunDefinition get(String id) {
		return RUNS.get(id);
	}

	public static Collection<RunDefinition> all() {
		return RUNS.values();
	}
}
