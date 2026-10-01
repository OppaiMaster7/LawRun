package com.bogi.lawrun.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.bogi.lawrun.LawRun;
import com.mojang.serialization.Codec;
import com.bogi.lawrun.net.NoticePayload;
import com.bogi.lawrun.net.RulePopupPayload;
import com.bogi.lawrun.net.RunIntroPayload;
import com.bogi.lawrun.net.RunOverPayload;
import com.bogi.lawrun.net.VictoryPayload;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/** Starts, ends and stores runs. Rules call {@link #fail} when a law is broken. */
public final class RunManager {
	/**
	 * Saved on the player and kept through death, so a failed run stays failed.
	 * Synced to the player's own client for the HUD and the safe pocket slot.
	 */
	public static final AttachmentType<RunState> RUN = AttachmentRegistry.create(LawRun.id("run"), builder -> builder
		.persistent(RunState.CODEC)
		.copyOnDeath()
		.syncWith(ByteBufCodecs.fromCodec(RunState.CODEC), AttachmentSyncPredicate.targetOnly()));

	/** Set once a won run's ending has played, so jumping in the portal again doesn't replay it. */
	public static final AttachmentType<Boolean> ENDING_SHOWN = AttachmentRegistry.create(LawRun.id("ending_shown"),
		builder -> builder.persistent(Codec.BOOL).copyOnDeath());

	/** Notices already shown in the current run; cleared when a run starts. */
	public static final AttachmentType<List<String>> SEEN_NOTICES = AttachmentRegistry.create(LawRun.id("seen_notices"),
		builder -> builder.persistent(Codec.STRING.listOf()).copyOnDeath());

	/** When each repeating notice last showed, by player. Not saved: a restart may repeat one early. */
	private static final Map<UUID, Map<String, Integer>> NOTICE_SHOWN_AT = new HashMap<>();

	/** How long the broken rule's card stays up before "RUN FAILED" replaces it. */
	public static final int POPUP_TICKS = 80;

	/** Players whose rule card is showing, and the server tick their failure lands on. */
	private static final Map<UUID, Integer> PENDING = new HashMap<>();

	/** The rule each pending failure was for, for the fail screen. */
	private static final Map<UUID, Integer> PENDING_LAW = new HashMap<>();

	/** Players who joined without a run, and the tick to show them how to start one. */
	private static final Map<UUID, Integer> WELCOME = new HashMap<>();

	/** Long enough for the loading screen to be gone, so the title isn't missed. */
	private static final int WELCOME_DELAY = 60;

	private RunManager() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				fail(player, source.getLocalizedDeathMessage(player).getString());
			} else if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level) {
				// Credit everyone in the End: bed and TNT kills have no attacker to credit.
				for (ServerPlayer player : level.players()) {
					win(player);
				}
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			RunState state = get(newPlayer);
			if (!alive && state != null && state.status() == RunStatus.FAILED) {
				newPlayer.setGameMode(GameType.SPECTATOR);
			}
		});

		ServerPlayerEvents.JOIN.register(player -> {
			if (get(player) == null) {
				WELCOME.put(player.getUUID(), player.level().getServer().getTickCount() + WELCOME_DELAY);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(RunManager::tick);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			RunState state = get(player);
			RunDefinition run = state == null || !state.active() ? null : RunRegistry.get(state.runId());
			if (run != null) {
				run.hooks().tick(player);
			}
		}

		runWhenDue(server, PENDING, player -> showFailure(player, PENDING_LAW.getOrDefault(player.getUUID(), 0)));
		runWhenDue(server, WELCOME, player -> {
			if (get(player) == null) {
				sendWelcome(player);
			}
		});
	}

	private static void runWhenDue(MinecraftServer server, Map<UUID, Integer> due, Consumer<ServerPlayer> action) {
		for (Iterator<Map.Entry<UUID, Integer>> it = due.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> entry = it.next();
			if (server.getTickCount() >= entry.getValue()) {
				it.remove();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player != null) {
					action.accept(player);
				}
			}
		}
	}

	/** Nothing in the game says the mod is there until a run starts, so say it, with buttons. */
	private static void sendWelcome(ServerPlayer player) {
		showTitle(player, Component.literal("LawRun").withStyle(ChatFormatting.GOLD),
			Component.literal("Press T and click START in chat to begin"));
		player.sendSystemMessage(Component.literal("LawRun is installed. No run has started in this world yet.")
			.withStyle(ChatFormatting.GOLD));
		for (RunDefinition run : RunRegistry.all()) {
			player.sendSystemMessage(Component.empty()
				.append(button("[START: " + run.title() + "]", ChatFormatting.GREEN, "/lawrun intro " + run.id(),
					"Meet the laws, then begin."))
				.append(Component.literal("  "))
				.append(button("[Read the rules]", ChatFormatting.AQUA, "/lawrun rules " + run.id(),
					"Show the laws in chat without starting.")));
		}
	}

	private static Component button(String label, ChatFormatting color, String command, String hover) {
		return Component.literal(label).withStyle(style -> style
			.withColor(color)
			.withBold(true)
			.withClickEvent(new ClickEvent.RunCommand(command))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	/** Returns null if the player has never started a run. */
	public static RunState get(Player player) {
		return player.getAttached(RUN);
	}

	public static boolean isActive(Player player) {
		RunState state = get(player);
		return state != null && state.active();
	}

	/** Works on both sides: the client sees its own player's run through the sync. */
	public static boolean isActive(Player player, String runId) {
		RunState state = get(player);
		return state != null && state.active() && state.runId().equals(runId);
	}

	public static void start(ServerPlayer player, RunDefinition run) {
		player.setAttached(RUN, RunState.begin(run.id(), now(player)));
		player.removeAttached(SEEN_NOTICES);
		NOTICE_SHOWN_AT.remove(player.getUUID());
		if (player.gameMode() == GameType.SPECTATOR) {
			player.setGameMode(GameType.SURVIVAL);
		}
		run.hooks().start(player);
		player.sendSystemMessage(Component.literal(run.title() + ": the run has started. ").withStyle(ChatFormatting.GOLD)
			.append(button("[Laws]", ChatFormatting.AQUA, "/lawrun rules " + run.id(), "Show the laws in chat.")));
		showTitle(player, Component.literal(run.title()).withStyle(ChatFormatting.GOLD),
			Component.literal("The run has started"));
	}

	/**
	 * Opens the laws screen: at the start (disclaimer, then Begin) or for
	 * reference mid-run. Falls back to the chat briefing.
	 */
	public static void sendIntro(ServerPlayer player, RunDefinition run, boolean reference) {
		if (ServerPlayNetworking.canSend(player, RunIntroPayload.TYPE)) {
			ServerPlayNetworking.send(player, new RunIntroPayload(run.id(), reference));
		} else {
			sendBriefing(player, run);
		}
	}

	/** Shows a notice from the active run, the first time only. */
	public static void notice(ServerPlayer player, String noticeId, String... args) {
		RunState state = get(player);
		if (state == null || !state.active()) {
			return;
		}
		List<String> seen = player.getAttachedOrElse(SEEN_NOTICES, List.of());
		if (seen.contains(noticeId)) {
			return;
		}
		List<String> updated = new ArrayList<>(seen);
		updated.add(noticeId);
		player.setAttached(SEEN_NOTICES, updated);
		sendNotice(player, state, noticeId, args);
	}

	/** Shows a notice from the active run again and again, but not more often than every {@code cooldownTicks}. */
	public static void noticeEvery(ServerPlayer player, String noticeId, int cooldownTicks, String... args) {
		RunState state = get(player);
		if (state == null || !state.active()) {
			return;
		}
		int now = player.level().getServer().getTickCount();
		Map<String, Integer> shownAt = NOTICE_SHOWN_AT.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
		Integer last = shownAt.get(noticeId);
		if (last != null && now - last < cooldownTicks) {
			return;
		}
		shownAt.put(noticeId, now);
		sendNotice(player, state, noticeId, args);
	}

	/** Starts a repeating notice's cooldown without showing it. */
	public static void primeCooldown(ServerPlayer player, String noticeId) {
		NOTICE_SHOWN_AT.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
			.put(noticeId, player.level().getServer().getTickCount());
	}

	public static boolean hasSeen(ServerPlayer player, String noticeId) {
		return player.getAttachedOrElse(SEEN_NOTICES, List.of()).contains(noticeId);
	}

	private static void sendNotice(ServerPlayer player, RunState state, String noticeId, String... args) {
		if (ServerPlayNetworking.canSend(player, NoticePayload.TYPE)) {
			ServerPlayNetworking.send(player, new NoticePayload(state.runId(), noticeId, List.of(args)));
		}
	}

	/** Adds to one of the active run's counters. */
	public static void addStat(ServerPlayer player, String key, long amount) {
		RunState state = get(player);
		if (state != null && state.active()) {
			player.setAttached(RUN, state.withStat(key, amount));
		}
	}

	/** Ends an active run for something that isn't a law, like dying. */
	public static void fail(ServerPlayer player, String reason) {
		fail(player, null, reason);
	}

	/**
	 * Ends an active run as a loss. With a law, the player first sees that
	 * law's card for {@link #POPUP_TICKS}, then the failure. Does nothing if no
	 * run is active, so a rule can call it every tick without care.
	 */
	public static void fail(ServerPlayer player, Law law, String reason) {
		RunState state = get(player);
		if (state == null || !state.active()) {
			return;
		}
		RunDefinition run = RunRegistry.get(state.runId());
		String summary = law == null ? reason : law.name() + ": " + reason;
		player.setAttached(RUN, state.end(RunStatus.FAILED, now(player), summary));
		if (run != null) {
			run.hooks().end(player);
		}

		if (law != null && run != null && ServerPlayNetworking.canSend(player, RulePopupPayload.TYPE)) {
			// The card is HUD, and the HUD is hidden behind open screens.
			player.closeContainer();
			ServerPlayNetworking.send(player,
				new RulePopupPayload(run.numberOf(law), law.name(), law.date(), law.inGame(), reason));
			PENDING.put(player.getUUID(), player.level().getServer().getTickCount() + POPUP_TICKS);
			PENDING_LAW.put(player.getUUID(), run.numberOf(law));
		} else {
			showFailure(player, 0);
		}
	}

	private static void showFailure(ServerPlayer player, int lawNumber) {
		PENDING_LAW.remove(player.getUUID());
		RunState state = get(player);
		if (state == null || state.status() != RunStatus.FAILED) {
			return;
		}
		if (ServerPlayNetworking.canSend(player, RunOverPayload.TYPE)) {
			ServerPlayNetworking.send(player, new RunOverPayload(state.runId(), lawNumber, state.reason(),
				state.lengthTicks(state.endedAt()), state.stats()));
		} else {
			showTitle(player, Component.literal("RUN FAILED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
				Component.literal(state.reason()).withStyle(ChatFormatting.RED));
		}
		player.sendSystemMessage(Component.literal("Run failed: ").withStyle(ChatFormatting.DARK_RED)
			.append(Component.literal(state.reason()).withStyle(ChatFormatting.RED)));
		player.sendSystemMessage(summary(state));

		if (player.isAlive()) {
			player.setGameMode(GameType.SPECTATOR);
		}
	}

	public static void win(ServerPlayer player) {
		RunState state = get(player);
		if (state == null || !state.active()) {
			return;
		}
		RunState won = state.end(RunStatus.WON, now(player), "Killed the Ender Dragon");
		player.setAttached(RUN, won);
		RunDefinition run = RunRegistry.get(state.runId());
		if (run != null) {
			run.hooks().end(player);
		}

		if (ServerPlayNetworking.canSend(player, VictoryPayload.TYPE)) {
			ServerPlayNetworking.send(player, new VictoryPayload(won.runId()));
		} else {
			showTitle(player, Component.literal("RUN COMPLETE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.literal("You beat the game without breaking a law"));
		}
		player.sendSystemMessage(summary(won));
	}

	/** True, once, when a won run's ending should play. Called as the player takes the exit portal. */
	public static boolean claimEnding(ServerPlayer player) {
		RunState state = get(player);
		if (state == null || state.status() != RunStatus.WON || player.getAttachedOrElse(ENDING_SHOWN, false)) {
			return false;
		}
		player.setAttached(ENDING_SHOWN, true);
		return true;
	}

	/** Forgets the player's run entirely. Debug only. */
	public static void reset(ServerPlayer player) {
		RunState state = get(player);
		RunDefinition run = state == null ? null : RunRegistry.get(state.runId());
		if (run != null) {
			if (state.active()) {
				run.hooks().end(player);
			}
			run.hooks().reset(player);
		}
		PENDING.remove(player.getUUID());
		PENDING_LAW.remove(player.getUUID());
		player.removeAttached(RUN);
		player.removeAttached(ENDING_SHOWN);
		player.removeAttached(SEEN_NOTICES);
		NOTICE_SHOWN_AT.remove(player.getUUID());
		WELCOME.put(player.getUUID(), player.level().getServer().getTickCount() + 20);
		if (player.gameMode() == GameType.SPECTATOR) {
			player.setGameMode(GameType.SURVIVAL);
		}
	}

	public static void sendBriefing(ServerPlayer player, RunDefinition run) {
		player.sendSystemMessage(Component.literal(run.title()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		player.sendSystemMessage(Component.literal(run.context()).withStyle(ChatFormatting.GRAY));

		for (Law law : run.laws()) {
			player.sendSystemMessage(Component.literal(run.numberOf(law) + ". ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(law.name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
				.append(Component.literal(" (" + law.date() + ")").withStyle(ChatFormatting.DARK_GRAY)));
			player.sendSystemMessage(Component.literal("   " + law.inGame()));
		}
		player.sendSystemMessage(Component.literal("Dying ends the run. Kill the Ender Dragon to win.")
			.withStyle(ChatFormatting.YELLOW));
		player.sendSystemMessage(credit(run.source()));
	}

	private static Component credit(RunDefinition.Source source) {
		MutableComponent link = Component.literal(source.videoTitle()).withStyle(style -> style
			.withColor(ChatFormatting.AQUA)
			.withUnderlined(true)
			.withClickEvent(new ClickEvent.OpenUrl(URI.create(source.url()))));
		return Component.literal("Rules adapted to Minecraft from ").withStyle(ChatFormatting.GRAY)
			.append(link)
			.append(Component.literal(" by " + source.creator() + ". A game adaptation, not a history lesson.")
				.withStyle(ChatFormatting.GRAY));
	}

	private static Component summary(RunState state) {
		RunDefinition run = RunRegistry.get(state.runId());
		String title = run == null ? state.runId() : run.title();
		return Component.literal(title + " · lasted " + formatTicks(state.lengthTicks(state.endedAt())))
			.withStyle(ChatFormatting.GRAY);
	}

	/** Play time as "1h 04m" or "12m 30s". */
	public static String formatTicks(long ticks) {
		long seconds = ticks / 20;
		long hours = seconds / 3600;
		long minutes = seconds / 60 % 60;
		return hours > 0
			? String.format("%dh %02dm", hours, minutes)
			: String.format("%dm %02ds", minutes, seconds % 60);
	}

	public static long now(ServerPlayer player) {
		return player.level().getLevelData().getGameTime();
	}

	public static void showTitle(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}
}
