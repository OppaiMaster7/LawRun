package com.bogi.lawrun.core;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum RunStatus implements StringRepresentable {
	ACTIVE("active"),
	FAILED("failed"),
	WON("won");

	public static final Codec<RunStatus> CODEC = StringRepresentable.fromEnum(RunStatus::values);

	private final String name;

	RunStatus(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
