package com.bogi.lawrun.core;

import java.util.List;

/**
 * Everything the player is told about a run, plus the code that enforces it.
 * The client reads this too, for the laws, notices, fail and ending screens.
 *
 * @param context    one or two factual sentences on where the rules come from
 * @param disclaimer shown before every run starts
 * @param stats      what the fail and ending screens count, in order
 */
public record RunDefinition(String id, String title, String context, String disclaimer, Source source, List<Law> laws,
	List<StatLine> stats, List<Notice> notices, Ending ending, RunHooks hooks) {

	/** The video a run is adapted from. Shown on every briefing. */
	public record Source(String creator, String videoTitle, String url) {
		/** A link to one moment of the video. */
		public String urlAt(int seconds) {
			return url + (url.contains("?") ? "&" : "?") + "t=" + seconds + "s";
		}
	}

	/**
	 * One counted stat.
	 *
	 * @param comparison what the video's creator managed, or empty
	 */
	public record StatLine(String key, String label, String comparison) {
	}

	/**
	 * What the ending screen says after the stats.
	 *
	 * @param notes    extra lines under the stats, such as how the video's run differed
	 * @param epilogue shown last, alone, on black
	 */
	public record Ending(String victoryLine, List<String> notes, List<String> epilogue, String learnMoreLabel,
		String learnMoreUrl) {
	}

	/** Rule numbers start at 1, in the order the laws are listed. */
	public int numberOf(Law law) {
		return laws.indexOf(law) + 1;
	}

	/** Returns null for an unknown id. */
	public Notice notice(String noticeId) {
		for (Notice notice : notices) {
			if (notice.id().equals(noticeId)) {
				return notice;
			}
		}
		return null;
	}
}
