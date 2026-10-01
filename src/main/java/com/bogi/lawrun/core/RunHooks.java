package com.bogi.lawrun.core;

import net.minecraft.server.level.ServerPlayer;

/** What a run does on top of the core. Every method is optional. */
public interface RunHooks {
	RunHooks NONE = new RunHooks() {
	};

	default void start(ServerPlayer player) {
	}

	/** Called every server tick while the player's run is active. */
	default void tick(ServerPlayer player) {
	}

	/** The run just failed or was won. */
	default void end(ServerPlayer player) {
	}

	/** {@code /lawrun reset}: undo whatever {@link #start} changed. */
	default void reset(ServerPlayer player) {
	}
}
