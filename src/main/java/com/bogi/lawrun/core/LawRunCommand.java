package com.bogi.lawrun.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /lawrun}. Anyone can list, start and check runs; {@code fail} and
 * {@code reset} are for testing and need cheats.
 */
public final class LawRunCommand {
	private static final SuggestionProvider<CommandSourceStack> RUN_IDS = (context, builder) ->
		SharedSuggestionProvider.suggest(RunRegistry.all().stream().map(RunDefinition::id), builder);

	private LawRunCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("lawrun")
			.then(Commands.literal("list").executes(LawRunCommand::list))
			.then(Commands.literal("rules")
				.executes(LawRunCommand::currentRules)
				.then(Commands.argument("run", StringArgumentType.word()).suggests(RUN_IDS)
					.executes(context -> rules(context, StringArgumentType.getString(context, "run")))))
			.then(Commands.literal("intro")
				.then(Commands.argument("run", StringArgumentType.word()).suggests(RUN_IDS)
					.executes(LawRunCommand::intro)))
			.then(Commands.literal("start")
				.then(Commands.argument("run", StringArgumentType.word()).suggests(RUN_IDS)
					.executes(LawRunCommand::start)))
			.then(Commands.literal("status").executes(LawRunCommand::status))
			.then(Commands.literal("fail")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("reason", StringArgumentType.greedyString())
					.executes(LawRunCommand::fail)))
			.then(Commands.literal("reset")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(LawRunCommand::reset)));
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		for (RunDefinition run : RunRegistry.all()) {
			source.sendSuccess(() -> Component.literal(run.id()).withStyle(ChatFormatting.GOLD)
				.append(Component.literal(" " + run.title() + ", " + run.laws().size() + " laws")
					.withStyle(ChatFormatting.GRAY)), false);
		}
		return RunRegistry.all().size();
	}

	private static int currentRules(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunState state = RunManager.get(context.getSource().getPlayerOrException());
		if (state == null) {
			context.getSource().sendFailure(Component.literal("You're not on a run. Try /lawrun rules <run>."));
			return 0;
		}
		return rules(context, state.runId());
	}

	private static int rules(CommandContext<CommandSourceStack> context, String runId) throws CommandSyntaxException {
		RunDefinition run = RunRegistry.get(runId);
		if (run == null) {
			context.getSource().sendFailure(Component.literal("No run called \"" + runId + "\". See /lawrun list."));
			return 0;
		}
		// The chat's [Laws] button lands here: open the laws screen for reference.
		RunManager.sendIntro(context.getSource().getPlayerOrException(), run, true);
		return 1;
	}

	private static int intro(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String runId = StringArgumentType.getString(context, "run");
		RunDefinition run = RunRegistry.get(runId);
		if (run == null) {
			context.getSource().sendFailure(Component.literal("No run called \"" + runId + "\". See /lawrun list."));
			return 0;
		}
		RunManager.sendIntro(context.getSource().getPlayerOrException(), run, false);
		return 1;
	}

	private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String runId = StringArgumentType.getString(context, "run");
		RunDefinition run = RunRegistry.get(runId);
		if (run == null) {
			context.getSource().sendFailure(Component.literal("No run called \"" + runId + "\". See /lawrun list."));
			return 0;
		}
		if (RunManager.isActive(player)) {
			context.getSource().sendFailure(Component.literal("You're already on a run. See /lawrun status."));
			return 0;
		}
		RunManager.start(player, run);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		RunState state = RunManager.get(player);
		if (state == null) {
			context.getSource().sendSuccess(() -> Component.literal("No run yet. Start one with /lawrun start <run>."), false);
			return 0;
		}
		RunDefinition run = RunRegistry.get(state.runId());
		String title = run == null ? state.runId() : run.title();
		String length = RunManager.formatTicks(state.lengthTicks(RunManager.now(player)));
		String line = switch (state.status()) {
			case ACTIVE -> title + ": in progress, " + length + " so far";
			case FAILED -> title + ": failed after " + length + ". " + state.reason();
			case WON -> title + ": won in " + length;
		};
		context.getSource().sendSuccess(() -> Component.literal(line), false);
		return 1;
	}

	private static int fail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (!RunManager.isActive(player)) {
			context.getSource().sendFailure(Component.literal("No active run to fail."));
			return 0;
		}
		RunManager.fail(player, StringArgumentType.getString(context, "reason"));
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager.reset(context.getSource().getPlayerOrException());
		context.getSource().sendSuccess(() -> Component.literal("Run cleared."), false);
		return 1;
	}
}
