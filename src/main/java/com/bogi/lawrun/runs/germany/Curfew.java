package com.bogi.lawrun.runs.germany;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bogi.lawrun.core.RunManager;
import com.bogi.lawrun.net.CurfewPayload;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Law 2. Curfew runs from sunset (clock tick 12000) to sunrise (24000), which
 * gives the 10 minutes of day and 10 of night the video describes.
 */
final class Curfew {
	static final long DAY_LENGTH = 24000;
	static final long SUNSET = 12000;

	/** How long a player can be outside after dark before the run fails. */
	static final int GRACE_TICKS = 60;
	private static final int CHECK_EVERY = 10;

	/** Solid blocks overhead that count as underground, walls or not. */
	private static final int UNDERGROUND_DEPTH = 5;
	private static final int SEARCH_RADIUS = 24;
	private static final int SEARCH_LIMIT = 4096;

	private static final Map<UUID, Integer> exposedTicks = new HashMap<>();

	private Curfew() {
	}

	static long clock(MinecraftServer server) {
		return server.overworld().getOverworldClockTime();
	}

	static long day(long clock) {
		return Math.floorDiv(clock, DAY_LENGTH);
	}

	static boolean isNight(long clock) {
		return Math.floorMod(clock, DAY_LENGTH) >= SUNSET;
	}

	static void tick(ServerPlayer player, GermanyState state, int serverTick) {
		if (serverTick % CHECK_EVERY != 0) {
			return;
		}
		long clock = clock(player.level().getServer());
		boolean night = isNight(clock);
		long timeOfDay = Math.floorMod(clock, DAY_LENGTH);
		int ticksLeft = (int) (night ? DAY_LENGTH - timeOfDay : SUNSET - timeOfDay);
		// A run started after dark gets that night off: there was no warning.
		boolean enforced = night && day(clock) * DAY_LENGTH + SUNSET > state.startClock();

		int graceLeft = -1;
		if (enforced && !isSheltered(player)) {
			int exposed = exposedTicks.merge(player.getUUID(), CHECK_EVERY, Integer::sum);
			if (exposed >= GRACE_TICKS) {
				exposedTicks.remove(player.getUUID());
				RunManager.fail(player, Germany1940s.CURFEW, whereExposed(player));
				return;
			}
			graceLeft = GRACE_TICKS - exposed;
		} else if (exposedTicks.remove(player.getUUID()) != null) {
			// Caught outside and made it back before the grace ran out.
			RunManager.addStat(player, Germany1940s.Stats.CLOSE_CALLS, 1);
		}

		if (ServerPlayNetworking.canSend(player, CurfewPayload.TYPE)) {
			int diamonds = state.permit() ? -1 : GermanyRules.diamonds(player);
			ServerPlayNetworking.send(player, new CurfewPayload(night, ticksLeft, enforced, graceLeft, diamonds));
		}
	}

	static void forget(ServerPlayer player) {
		exposedTicks.remove(player.getUUID());
	}

	private static String whereExposed(ServerPlayer player) {
		if (player.level().dimension() == Level.NETHER) {
			return "In the Nether after dark";
		}
		if (player.level().dimension() == Level.END) {
			return "In the End after dark";
		}
		return "Outside after dark";
	}

	/**
	 * Sheltered means the sky is blocked above your head, and either there's
	 * plenty of rock above you (a cave or mine) or you're sealed in: walls,
	 * roof and closed doors, with no gap that leads out to open sky.
	 */
	static boolean isSheltered(ServerPlayer player) {
		ServerLevel level = player.level();
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		BlockPos head = BlockPos.containing(player.getEyePosition());
		if (level.canSeeSky(head)) {
			return false;
		}
		return solidBlocksAbove(level, head) >= UNDERGROUND_DEPTH || isSealedIn(level, player.blockPosition(), head);
	}

	private static int solidBlocksAbove(ServerLevel level, BlockPos head) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, head.getX(), head.getZ());
		BlockPos.MutableBlockPos pos = head.mutable();
		int count = 0;
		for (int y = head.getY() + 1; y < top && count < UNDERGROUND_DEPTH; y++) {
			if (level.getBlockState(pos.setY(y)).isSolidRender()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Flood-fills the air around the player. Reaching a block under open sky
	 * means there's a way out. A space too big to search (a cave system, a
	 * castle) counts as sealed.
	 */
	private static boolean isSealedIn(ServerLevel level, BlockPos feet, BlockPos head) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		LongSet seen = new LongOpenHashSet();
		for (BlockPos start : new BlockPos[] {feet, head}) {
			if (seen.add(start.asLong())) {
				queue.add(start);
			}
		}

		while (!queue.isEmpty()) {
			if (seen.size() > SEARCH_LIMIT) {
				return true;
			}
			BlockPos pos = queue.poll();
			if (level.canSeeSky(pos)) {
				return false;
			}
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (!isNear(next, head) || level.isOutsideBuildHeight(next) || !isOpen(level, next)) {
					continue;
				}
				if (seen.add(next.asLong())) {
					queue.add(next);
				}
			}
		}
		return true;
	}

	private static boolean isNear(BlockPos pos, BlockPos center) {
		return Math.abs(pos.getX() - center.getX()) <= SEARCH_RADIUS
			&& Math.abs(pos.getY() - center.getY()) <= SEARCH_RADIUS
			&& Math.abs(pos.getZ() - center.getZ()) <= SEARCH_RADIUS;
	}

	/** Air, water and plants let the outside in; so do open doors, trapdoors and gates. */
	private static boolean isOpen(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock) {
			return state.getValue(BlockStateProperties.OPEN);
		}
		return state.getCollisionShape(level, pos).isEmpty();
	}
}
