# Minecraft 26.3 API notes

Server-side things this mod uses. Verified with `javap` against the 26.3 jars
and Fabric API 0.161.0+26.3, not from memory. The client-side notes in
`time_the_bogi/docs/api-notes-26.2.md` still apply.

```bash
CP="$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/26.3/minecraft-common-deobf-26.3.jar"
javap -cp "$CP" net.minecraft.server.level.ServerPlayer
```

Fabric API module versions are pinned in the `fabric-api-0.161.0+26.3.pom` in
`~/.gradle/caches/modules-2`. Look a module's version up there, then `javap` its jar.

## Per-player saved data: use attachments

```java
AttachmentType<RunState> RUN = AttachmentRegistry.create(id("run"),
    builder -> builder.persistent(RunState.CODEC).copyOnDeath());

player.getAttached(RUN);          // null if absent
player.setAttached(RUN, value);
player.removeAttached(RUN);
```

`copyOnDeath()` matters: without it the value is dropped when the player respawns.

## Commands

- `CommandRegistrationCallback.EVENT`: `(dispatcher, CommandBuildContext, Commands.CommandSelection)`
- Permission levels are no longer ints: `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`
- `source.sendSuccess(Supplier<Component>, boolean)`, `sendFailure(Component)`, `getPlayerOrException()`

## Time and day/night

- `Level.getGameTime()` is gone. Use `level.getLevelData().getGameTime()`.
- Day time moved to **world clocks** (`net.minecraft.world.clock`, data in `data/minecraft/world_clock/`).
  `Level.getOverworldClockTime()`, `isDarkOutside()` and `isBrightOutside()` are the easy entry points. The curfew and sunrise rules build on these.

## Traps hit while building the Germany run

- **Entity type constants moved:** `EntityType.ITEM` is now `EntityTypes.ITEM` (same for `VILLAGER`, `PIGLIN`...).
- **Key codes are SDL-style**, not GLFW: `InputConstants.KEY_ESCAPE` is 41 and `MOUSE_BUTTON_LEFT` is 1. GLFW isn't on the classpath at all.
- **`AbstractVillager.overrideOffers` is a no-op on the server** (it's for the client's copy). Add to `villager.getOffers()` instead.
- **World borders are per dimension** (`ServerLevel.getWorldBorder()` is saved data on that level). Setting the Overworld's leaves the Nether alone.
- **`ClickType` is now `ContainerInput`** (`menu.clicked(slot, button, ContainerInput.PICKUP, player)`).
- **`Prediction`** (`PREDICTED` / `SERVER_ONLY`) is a new parameter on `Inventory.placeItemBackInInventory` and `drop`.
- **Inventory equipment:** `Inventory.getContainerSize()` is 36 + 7 equipment slots (feet, legs, chest, head, offhand, body, saddle); `getItem(36..42)` reads equipment and `clearContent()` clears it too.
- **Mixin, `@ModifyVariable` at an `INVOKE`:** the call's arguments are already on the stack, so changing the local there doesn't change what gets passed. Target the variable's first `LOAD` instead (see `CraftingMenuMixin`).

- **No `ItemStack` in a static initialiser on the client**: items aren't bound when mod entrypoints run ("Components not bound yet"). Create them on first use.
- **Titles live on `Gui.hud`**: `minecraft.gui.hud.clearTitles()`.
- **The End credits**: the exit portal calls `ServerPlayer.showEndCredits()`, which sends `ClientboundGameEventPacket.WIN_GAME` only while `wonGame` is false; the client's `handleGameEvent` opens `WinScreen` and on finish sends `PERFORM_RESPAWN`. Inject after `PacketUtils.ensureRunningOnSameThread` to be on the main thread.
- **Post effects** are data: `assets/<ns>/post_effect/<name>.json`, applied via `GameRenderer.requestedPostEffects` (rebuilt each frame in `update`). Vanilla's `post/color_convolve` shader takes a colour matrix but boosts saturation by 1.8 afterwards, so matrices need compensating (see `tools/make_assets.py`).
- **Screens**: `extractBackground` / `extractRenderState`, input as `KeyEvent` / `MouseButtonEvent`, `getBackgroundMusic()` for music (`Musics.CREDITS`), links through `ConfirmLinkScreen.confirmLink`.

## Day and night, in detail

The overworld timeline (`world/timeline/Timelines.java`) has a 24000-tick period
with markers `day` 1000, `noon` 6000, `night` 13000, `midnight` 18000 and
`wake_up_from_sleep` 0. `getOverworldClockTime()` is the running total, so
`floorDiv(total, 24000)` is the day number and sleeping moves it to the next day.
`canSeeSky(pos)` is just "sky light is 15 here": glass passes it, leaves don't.

## Client game tests

`fabricApi { configureTests { ... enableClientGameTests = true } }` in
`build.gradle` gives a `gametest` source set and `runClientGameTest`. Useful calls:
`context.worldBuilder().create()` (flat world, time frozen, no mobs),
`world.getServer().runCommand/computeOnServer/waitFor`, `context.setScreen`,
`context.getInput().setCursorPos` (window pixels, so multiply GUI coordinates by
`getGuiScale()`), `context.takeScreenshot(name)`, `context.clickScreenButton("Label")`.
To click a block reliably, call `player.gameMode.useItemOn(...)` on the server
rather than aiming the camera. Check server-side results with `server.waitFor`,
not a client-side wait: the client can be a tick ahead.

## Misc

- Titles: `ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut)`, then `ClientboundSetSubtitleTextPacket` and `ClientboundSetTitleTextPacket`, sent via `player.connection.send(...)`.
- Clickable links: `new ClickEvent.OpenUrl(URI)` (a record, not the old enum + string).
- `player.setGameMode(GameType)` / `player.gameMode()` on `ServerPlayer`.
- `DamageSource.getLocalizedDeathMessage(LivingEntity)` gives the vanilla death message.
- `net.minecraft.world.entity.boss.enderdragon.EnderDragon`. `ServerLivingEntityEvents.AFTER_DEATH` fires for it in principle, but still has to be confirmed with a bed kill in game.
