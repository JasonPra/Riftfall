# Riftfall

A bare-bones Solo Leveling-inspired Minecraft **1.21.1 / NeoForge 21.1** mod. Requires Java 21. No Create dependencies.

## Play loop

- Every five minutes, attempt to open a rift on safe, loaded overworld terrain within 64 blocks of an online player. At most five rifts are active. Coordinates are announced in chat.
- Walk into the purple particles to enter a separate, enclosed arena. Six persistent zombies are the placeholder encounter. All visitors share the same enemies and progress.
- Kill every arena enemy to clear the rift. Players inside return outside and the rift closes. Future bosses can join this same tracked encounter.
- After ten minutes of server time, an uncleared rift breaks, even if someone is inside. It emits flame particles and spawns two overworld zombies every fifteen seconds while its chunk is loaded.
- A rift tracks up to twenty living escaped mobs to limit server load. Kills free slots for new waves. Escaped mobs remain after closure; clearing stops future waves.
- The purple arena exit and `/rift leave` let players retreat without clearing the encounter. Portal travel has a five-second cooldown.
- Rift coordinates, timers, arena progress, and enemy UUIDs are saved with the world. Timers pause while the server is off. Arena chunks stay loaded until cleared.

Use Easy, Normal, or Hard difficulty. Peaceful removes the placeholder hostile mobs, so it cannot demonstrate combat or dungeon breaks. Arenas are simple bedrock rooms with lit ceilings; no custom models, loot, ranks, bosses, or progression yet. Avoid changing dimension data on an existing production world; test in a fresh world first.

## Build and run

```sh
# Linux / macOS
bash gradlew build
bash gradlew runClient
# Dedicated server (accept Minecraft's EULA in run/eula.txt first)
bash gradlew runServer
```

On Windows, use `gradlew.bat`. Built jars are in `build/libs/`.

Server settings are generated in `<world>/serverconfig/riftfall-server.toml`: `spawnSeconds`, `breakSeconds`, `waveSeconds`, `maxRifts`, `arenaEnemies`, and `maxOutsideMobs`.

## Manual acceptance check

1. Start a fresh creative world with cheats enabled; choose Normal difficulty.
2. Stand on solid, open overworld ground. Use `/rift spawn` (operator permission required), then `/rift status`. A rift appears at your feet and should teleport you into its arena.
3. Verify six enemies appear. Return through the purple marker after five seconds, or use `/rift leave`. Re-enter and confirm progress remains.
4. Kill every enemy. Confirm automatic return, a closure announcement, and removal from `/rift status`.
5. For a fast break test, set `breakSeconds = 10` and `waveSeconds = 2` in the server config, restart, then spawn another rift and step away. Verify repeated outside waves, the cap, and resumed spawning after kills.
6. Enter and clear the broken rift. Confirm spawning stops. Already escaped mobs should remain.
7. Restart during an active encounter and check remaining enemies and timer. Repeat with two players and with one player logged out inside during closure; that player should return to world spawn when reconnecting.
8. Leave random spawning enabled and verify a naturally spawned rift appears near an overworld player after the configured interval.

## Prototype validation

`gradlew build` passes, including two persistence tests for countdowns, broken state, tracked enemy IDs, and non-reuse of cleared arena IDs. A local dedicated-server check with an automated Minecraft client verified dimension loading, portal entry, six arena enemies, clearing and automatic return, dungeon breaks, the four-mob test cap, and reopening a saved broken rift after restart. Arena kills were issued through server commands. Clearing the broken rift left escaped mobs alive and stopped further waves. Natural spawn timing and human combat feel still need manual playtesting.
