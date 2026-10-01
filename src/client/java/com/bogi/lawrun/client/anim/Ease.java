package com.bogi.lawrun.client.anim;

/** Easing curves over 0..1. */
public final class Ease {
	private Ease() {
	}

	public static float clamp(float t) {
		return t < 0 ? 0 : t > 1 ? 1 : t;
	}

	/** Progress of a step that runs from {@code startMs} for {@code durationMs}. */
	public static float progress(long elapsedMs, long startMs, long durationMs) {
		return clamp((elapsedMs - startMs) / (float) durationMs);
	}

	public static float outCubic(float t) {
		float u = 1 - clamp(t);
		return 1 - u * u * u;
	}

	public static float inCubic(float t) {
		t = clamp(t);
		return t * t * t;
	}

	public static float inOut(float t) {
		t = clamp(t);
		return t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	/** Overshoots past 1 and settles back: the rubber-stamp landing. */
	public static float outBack(float t) {
		t = clamp(t);
		float c1 = 1.9F;
		float c3 = c1 + 1;
		float u = t - 1;
		return 1 + c3 * u * u * u + c1 * u * u;
	}

	public static float lerp(float from, float to, float t) {
		return from + (to - from) * t;
	}
}
