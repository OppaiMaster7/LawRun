package com.bogi.lawrun.runs.germany;

import java.util.List;

import com.bogi.lawrun.core.RunManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Decides when the 1940s Germany notices appear: rule tutorials as each law
 * becomes relevant, a warning about weapons lying around, and callbacks to
 * moments in GuyBordo's video. The text lives in {@link Germany1940s}.
 */
final class Notices {
	private static final int CHECK_EVERY = 20;
	private static final int BORDER_WARNING = 16;
	private static final int BORDER_REPEAT_TICKS = 5 * 60 * 20;
	private static final int WEAPON_REPEAT_TICKS = 30 * 20;
	private static final long DUSK_NOTICE_TICKS = 60 * 20;

	private Notices() {
	}

	static void init() {
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			if (!GermanyRules.isOn(player)) {
				return;
			}
			String left = sunsetIn(player);
			if (destination.dimension() == Level.NETHER) {
				RunManager.notice(player, Germany1940s.Notices.NETHER, left);
			} else if (destination.dimension() == Level.END) {
				RunManager.notice(player, Germany1940s.Notices.END, left);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Zombie && source.is(DamageTypes.FALL) && entity.level() instanceof ServerLevel level) {
				for (ServerPlayer player : level.players()) {
					if (player.distanceTo(entity) < 16 && GermanyRules.isOn(player)) {
						RunManager.notice(player, Germany1940s.Notices.FALL_KILL);
					}
				}
			}
		});

		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player instanceof ServerPlayer serverPlayer && GermanyRules.isOn(serverPlayer)) {
				ItemStack held = player.getItemInHand(hand);
				if (held.is(Items.CHEST) || held.is(Items.TRAPPED_CHEST) || held.is(Items.BARREL)) {
					RunManager.notice(serverPlayer, Germany1940s.Notices.CHEST_PLACED);
				}
			}
			return InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (player instanceof ServerPlayer serverPlayer && entity instanceof Sheep sheep
				&& player.getItemInHand(hand).is(Items.SHEARS) && sheep.readyForShearing() && GermanyRules.isOn(serverPlayer)
				&& level.getEntitiesOfClass(Sheep.class, sheep.getBoundingBox().inflate(32), other -> other != sheep).isEmpty()) {
				RunManager.notice(serverPlayer, Germany1940s.Notices.SHEEP);
			}
			return InteractionResult.PASS;
		});
	}

	/** Called by {@link Plunder} when a world-generated chest is opened for the first time. */
	static void worldChestOpened(ServerPlayer player, String lootTablePath) {
		if (lootTablePath.contains("shipwreck") || lootTablePath.contains("simple_dungeon")) {
			RunManager.notice(player, Germany1940s.Notices.WORLD_STASH);
		}
	}

	static void tick(ServerPlayer player, GermanyState state, int serverTick) {
		if (serverTick % CHECK_EVERY != 0) {
			return;
		}
		carrying(player, state);
		surroundings(player, state, serverTick);

		long clock = Curfew.clock(player.level().getServer());
		long timeOfDay = Math.floorMod(clock, Curfew.DAY_LENGTH);
		if (timeOfDay < Curfew.SUNSET && Curfew.SUNSET - timeOfDay <= DUSK_NOTICE_TICKS) {
			RunManager.notice(player, Germany1940s.Notices.DUSK);
		}
	}

	/** Diamonds toward the permit, and firsts for the items the video made famous. */
	private static void carrying(ServerPlayer player, GermanyState state) {
		Inventory inventory = player.getInventory();
		boolean food = false;
		boolean snowball = false;
		boolean tnt = false;
		boolean rottenFlesh = false;
		boolean pearl = false;
		boolean blazeRod = false;
		boolean bed = false;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			food |= stack.has(DataComponents.FOOD);
			snowball |= stack.is(Items.SNOWBALL);
			tnt |= stack.is(Items.TNT);
			rottenFlesh |= stack.is(Items.ROTTEN_FLESH);
			pearl |= stack.is(Items.ENDER_PEARL);
			blazeRod |= stack.is(Items.BLAZE_ROD);
			bed |= stack.is(ItemTags.BEDS);
		}
		if (food) {
			RunManager.notice(player, Germany1940s.Notices.FOOD);
		}
		if (snowball) {
			RunManager.notice(player, Germany1940s.Notices.SNOWBALL);
		}
		if (tnt) {
			RunManager.notice(player, Germany1940s.Notices.TNT);
		}
		if (rottenFlesh) {
			RunManager.notice(player, Germany1940s.Notices.ROTTEN_FLESH);
		}
		if (pearl) {
			RunManager.notice(player, Germany1940s.Notices.ENDER_PEARL);
		}
		if (blazeRod) {
			RunManager.notice(player, Germany1940s.Notices.BLAZE_ROD);
		}
		if (bed && player.level().dimension() == Level.END) {
			RunManager.notice(player, Germany1940s.Notices.BED_END);
		}

		if (!state.permit()) {
			int diamonds = GermanyRules.diamonds(player);
			if (diamonds >= GermanyRules.PERMIT_PRICE && !RunManager.hasSeen(player, Germany1940s.Notices.DIAMONDS_3)) {
				RunManager.notice(player, Germany1940s.Notices.DIAMONDS_3);
				player.sendSystemMessage(Component.literal("You can afford a travel permit. ").withStyle(ChatFormatting.GOLD)
					.append(Component.literal("[Buy travel permit]").withStyle(style -> style
						.withColor(ChatFormatting.GREEN)
						.withBold(true)
						.withClickEvent(new ClickEvent.RunCommand("/lawrun permit"))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("Pay 3 diamonds and open the border."))))));
			} else if (diamonds == 2) {
				RunManager.notice(player, Germany1940s.Notices.DIAMONDS_2);
			} else if (diamonds == 1) {
				RunManager.notice(player, Germany1940s.Notices.DIAMONDS_1);
			}
		}
	}

	private static void surroundings(ServerPlayer player, GermanyState state, int serverTick) {
		ServerLevel level = player.level();
		if (!state.permit() && level.dimension() == Level.OVERWORLD
			&& level.getWorldBorder().getDistanceToBorder(player) < BORDER_WARNING) {
			String diamonds = GermanyRules.diamonds(player) + "/" + GermanyRules.PERMIT_PRICE;
			if (RunManager.hasSeen(player, Germany1940s.Notices.BORDER)) {
				RunManager.noticeEvery(player, Germany1940s.Notices.BORDER_AGAIN, BORDER_REPEAT_TICKS, diamonds);
			} else {
				RunManager.notice(player, Germany1940s.Notices.BORDER, diamonds);
				// The reminder waits a full cooldown after the first lesson.
				RunManager.primeCooldown(player, Germany1940s.Notices.BORDER_AGAIN);
			}
		}

		if (!RunManager.hasSeen(player, Germany1940s.Notices.VILLAGER)
			&& !level.getEntitiesOfClass(AbstractVillager.class, player.getBoundingBox().inflate(6)).isEmpty()) {
			RunManager.notice(player, Germany1940s.Notices.VILLAGER);
		}
		if (!RunManager.hasSeen(player, Germany1940s.Notices.PIGLIN)
			&& !level.getEntitiesOfClass(AbstractPiglin.class, player.getBoundingBox().inflate(8)).isEmpty()) {
			RunManager.notice(player, Germany1940s.Notices.PIGLIN);
		}

		List<ItemEntity> weapons = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4),
			item -> Weapons.isWeapon(item.getItem()));
		if (!weapons.isEmpty()) {
			RunManager.noticeEvery(player, Germany1940s.Notices.WEAPON_GROUND, WEAPON_REPEAT_TICKS);
		}

		// Scanning blocks is the costly check: every other second, and only until found.
		if (serverTick % (CHECK_EVERY * 2) == 0 && !RunManager.hasSeen(player, Germany1940s.Notices.SPAWNER)) {
			BlockPos centre = player.blockPosition();
			for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-6, -6, -6), centre.offset(6, 6, 6))) {
				if (level.getBlockState(pos).is(Blocks.SPAWNER)) {
					RunManager.notice(player, Germany1940s.Notices.SPAWNER);
					break;
				}
			}
		}
	}

	/** "4:32" until sunset, for the Nether and End notices. */
	private static String sunsetIn(ServerPlayer player) {
		long timeOfDay = Math.floorMod(Curfew.clock(player.level().getServer()), Curfew.DAY_LENGTH);
		long ticks = timeOfDay < Curfew.SUNSET ? Curfew.SUNSET - timeOfDay : 0;
		long seconds = ticks / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
