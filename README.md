# Riftfall

A bare-bones Solo Leveling-inspired Minecraft **1.21.1 / NeoForge 21.1** mod. Requires Java 21 and Epic Fight 21.17.3.1 on both client and server.

## Play loop

- Every loaded overworld chunk gains soul essence passively. By default, it gains 1 point per second and reaches critical mass at 600 points. The world spawn chunk starts critical once per world.
- Critical chunks have a 10% chance every 10 seconds to open a rift on a safe surface inside that chunk. A successful spawn consumes the charge. At most five rifts are active, with one natural rift per chunk and at least 16 blocks between rifts. Coordinates are announced in chat.
- A rift erupts with a small explosion, smoke, and flame before its portal appears. The blast damages and knocks back nearby entities while preserving terrain. The Wither spawn cue is positional and local, audible within approximately 32 blocks rather than globally.
- Walk into the animated rift portal to enter a separate, enclosed arena. Six persistent zombies are the placeholder encounter. All visitors share the same enemies and progress.
- Kill every arena enemy to clear the rift. Players inside return outside and the rift closes. Future bosses can join this same tracked encounter.
- After ten minutes of server time, an uncleared rift breaks, even if someone is inside. Its energy rim turns red and spawns two overworld zombies every fifteen seconds while its chunk is loaded.
- A rift tracks up to twenty living escaped mobs to limit server load. Kills free slots for new waves. Escaped mobs remain after closure; clearing stops future waves.
- The arena exit portal and `/rift leave` let players retreat without clearing the encounter. Portal travel has a five-second cooldown.
- Rift coordinates, timers, arena progress, and enemy UUIDs are saved with the world. Timers pause while the server is off. Arena chunks stay loaded until cleared.

Use Easy, Normal, or Hard difficulty. Peaceful removes the placeholder hostile mobs, so it cannot demonstrate combat or dungeon breaks. Arenas are simple bedrock rooms with lit ceilings; no custom models, loot, ranks, bosses, or progression yet. Avoid changing dimension data on an existing production world; test in a fresh world first.

## Soul essence

Essence is a persistent `riftfall:soul_essence` chunk attachment. It accumulates only while the overworld chunk is loaded, including force-loaded chunks without nearby players. Arena chunks do not accumulate essence. Unloading a chunk or stopping the server pauses accumulation; loading it later resumes its saved value without adding missed time. Passive gain caps at critical mass.

Failed probability rolls, unavailable terrain, existing rifts in the same chunk, and the active-rift cap preserve the chunk's charge for another attempt. Opening a rift consumes all of that chunk's essence, including when using the operator spawn command. The spawn-chunk bonus is applied once per world, including existing worlds on their first launch with this update; clearing a rift or restarting never grants it again.

- `/rift essence` reports the current chunk's points and critical threshold.
- `/rift essence set <amount>` sets the current chunk's essence (operator permission required).
- These commands only inspect already loaded overworld chunks.

## Rift portals

Each entrance and arena exit is a stationary `riftfall:rift` entity with a two-sided animated portal surface and a rippling energy rim. Exterior portals receive a random orientation when created, saved across restarts. The arena exit sits at the south end of the dungeon and always faces north; entering players also face north into the room. Each portal's orientation stays fixed in the world; it never turns toward the camera or moves with physics. The interior uses Minecraft's animated Nether portal texture, and nearby players hear the vanilla Nether portal ambient sound through the Blocks sound category. A broken entrance has a red-tinted rim.

Portal visuals are reconstructed from saved rift data when chunks load or a server restarts, so existing worlds automatically gain the new portals. Clearing a rift removes both endpoint entities. Clients and servers both need the updated mod for the custom entity renderer.

## Crystal cluster

`riftfall:crystal_cluster` is a decorative, ground-mounted cluster of five translucent, pale blue, voxel-shaped crystal shards with stepped tips and a native 16×16 pixel texture. It emits light level 12 and uses a block entity with no ticking or special interaction yet. Place it on a sturdy upper surface; it drops itself when broken or when its support is removed.

Find **Crystal Cluster** in the Natural Blocks creative tab, or use `/give @s riftfall:crystal_cluster`. It has no crafting recipe or natural generation at this stage.

## Required dependency

Install [Epic Fight 21.17.3.1 for Minecraft 1.21.1 / NeoForge (CurseForge file 8175609)](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod/files/8175609) alongside Riftfall on both the client and server. Riftfall declares this exact version as required; the Epic Fight jar is installed separately, not bundled inside Riftfall.

Gradle resolves the pinned file from CurseMaven for compilation and development runs. `epic_fight_file_id` and `epic_fight_version` in `gradle.properties` identify the required release.

## Build and run

```sh
# Linux / macOS
bash gradlew build
bash gradlew runClient
# Dedicated server (accept Minecraft's EULA in run/eula.txt first)
bash gradlew runServer
```

On Windows, use `gradlew.bat`. Built jars are in `build/libs/`.

Server settings are generated in `<world>/serverconfig/riftfall-server.toml`:

- `essencePerSecond` (1.0), `criticalMass` (600.0), `essenceRollSeconds` (10), and `essenceSpawnChance` (0.1) control soul essence and natural spawning. Set chance to 0 to disable natural spawning while retaining essence accumulation.
- `breakSeconds`, `waveSeconds`, `maxRifts`, `arenaEnemies`, and `maxOutsideMobs` control encounters and dungeon breaks.

The former `spawnSeconds` timer is no longer used.

## Manual acceptance check

1. Start a fresh creative world with cheats enabled; choose Normal difficulty.
2. Stand on solid, open overworld ground. Use `/rift spawn` (operator permission required), then `/rift status`. A rift appears at your feet and should teleport you into its arena.
3. Verify six enemies appear. Return through the exit portal after five seconds, or use `/rift leave`. Re-enter and confirm progress remains.
4. Kill every enemy. Confirm automatic return, a closure announcement, and removal from `/rift status`.
5. For a fast break test, set `breakSeconds = 10` and `waveSeconds = 2` in the server config, restart, then spawn another rift and step away. Verify repeated outside waves, the cap, and resumed spawning after kills.
6. Enter and clear the broken rift. Confirm spawning stops. Already escaped mobs should remain.
7. Restart during an active encounter and check remaining enemies and timer. Repeat with two players and with one player logged out inside during closure; that player should return to world spawn when reconnecting.
8. Use `/rift essence` to inspect a chunk, wait a few seconds, and verify passive gain. In a fresh world, verify that the world spawn chunk starts critical.
9. For a deterministic natural-spawn test, set `essenceSpawnChance = 1.0` and `essenceRollSeconds = 1`, then use `/rift essence set 600` in a loaded, open chunk. Verify an eruption and rift appear, the chunk's essence resets, and there is no terrain damage. Restore normal settings afterward.
10. Test with one player near the spawn and one farther than 64 blocks away. Only the nearby player should hear the opening Wither cue. Nearby survival-mode entities should take blast damage.
11. Unload and reload a partially charged chunk, then restart the server. Verify its saved essence resumes and the spawn chunk is not refilled after its initial bonus has been consumed.

## Prototype validation

`gradlew build` passes, including persistence tests for countdowns, broken state, tracked enemy IDs, and non-reuse of cleared arena IDs. A local dedicated-server check with an automated Minecraft client verified dimension loading, portal entry, six arena enemies, clearing and automatic return, dungeon breaks, the four-mob test cap, and reopening a saved broken rift after restart. Arena kills were issued through server commands. Clearing the broken rift left escaped mobs alive and stopped further waves. Human combat feel and long-term spawn balancing still need manual playtesting.

The portal entity update was also checked with a real NeoForge client and dedicated server: animated frames, visibility from both faces, fixed orientation when the player changes sides, resistance to movement, entrance/exit entity creation, immediate removal of both entities on clearing, repeated portal ambient sound packets, and reconstruction of exactly one portal after a server restart.

The soul essence update passes six unit tests and dedicated-server checks for spawn-chunk initialization, passive gain in force-loaded chunks without players, unload/reload pause behavior, probability-zero retries, natural spawning and charge consumption, and fractional attachment persistence across restarts. A measured before/after health check confirmed blast damage; a 1,024-block comparison confirmed terrain was unchanged. Packet checks with nearby and distant clients confirmed smoke/flame and explosion delivery plus a local Wither opening cue. A spent spawn chunk remained spent after restarting.

The crystal cluster was checked in a real client with Epic Fight loaded, including its voxel model, transparent texture, inventory item, and nighttime glow. Server checks confirmed its block entity survives a restart, removing its support drops the cluster item, and saved world lighting records level 12 at the crystal and level 11 in the neighboring air block.
