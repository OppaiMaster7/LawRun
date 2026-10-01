package com.bogi.lawrun.client.anim;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;

/**
 * A cinematic's clock. Tracks time since it started and fires each cue once,
 * however often the frame is drawn.
 */
public final class Beats {
	private long startedAt = Util.getMillis();
	/** Cues before this point were skipped over and stay silent. */
	private long skippedTo = -1;
	private final Set<String> fired = new HashSet<>();

	public void restart() {
		startedAt = Util.getMillis();
		skippedTo = -1;
		fired.clear();
	}

	public long ms() {
		return Util.getMillis() - startedAt;
	}

	/** Pushes the whole timeline back, for time spent paused. */
	public void delay(long ms) {
		startedAt += ms;
	}

	/** Jumps ahead to {@code ms}. */
	public void skipTo(long ms) {
		if (ms() < ms) {
			startedAt = Util.getMillis() - ms;
			skippedTo = ms;
		}
	}

	/** True exactly once, the first frame at or after {@code atMs}. */
	public boolean once(String cue, long atMs) {
		return ms() >= atMs && fired.add(cue) && atMs >= skippedTo;
	}

	public void sound(String cue, long atMs, SoundEvent sound, float pitch, float volume) {
		if (once(cue, atMs)) {
			play(sound, pitch, volume);
		}
	}

	public static void play(SoundEvent sound, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}
}
