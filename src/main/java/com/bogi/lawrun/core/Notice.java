package com.bogi.lawrun.core;

/**
 * A slip of paper that slides in to teach a rule as it becomes relevant, warn
 * about a trap, or point out a moment from the video.
 *
 * @param lawNumber the rule it's about, or 0
 * @param body      may contain {@code %s} placeholders, filled from the notice's arguments
 */
public record Notice(String id, Kind kind, int lawNumber, String title, String body) {
	public enum Kind {
		/** Teaches one of the run's laws. */
		LAW,
		/** General advice. */
		TIP,
		/** A callback to the source video. */
		VIDEO,
		/** Something about to end the run. */
		WARNING
	}
}
