package com.bogi.lawrun.core;

import java.util.List;

/**
 * One rule of a run, as the source video states it.
 *
 * @param id      stable key that enforcement code and config refer to
 * @param history what the real rule was, in the video's terms
 * @param inGame  how the run translates it into Minecraft
 * @param tips    practical advice for the law's detail page
 * @param inVideo how the video's creator dealt with it
 * @param videoAt where in the video that happens, in seconds
 */
public record Law(String id, String name, String date, String history, String inGame, List<String> tips,
	String inVideo, int videoAt) {
}
