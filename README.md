# Riftfall

A Solo Leveling-inspired Minecraft **1.21.1 / NeoForge 21.1** mod. Requires Java 21 and the mods listed below on both client and server.

## Required mods

Install these alongside Riftfall on both client and server. Gradle resolves the pinned versions for development; they are not bundled inside the Riftfall jar.

| Mod | Pinned release |
| --- | --- |
| Epic Fight | [21.17.3.1 / file 8175609](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod/files/8175609) |
| Iron's Spells 'n Spellbooks | [1.21.1-3.16.3 / file 8680204](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks/files/8680204) |
| GeckoLib | [4.9.3, NeoForge / file 8893490](https://www.curseforge.com/minecraft/mc-mods/geckolib/files/8893490) |
| Player Animator | [2.0.4+1.21.1, NeoForge](https://modrinth.com/mod/playeranimator/version/HJZB6bmA) |
| Curios | [9.5.1+1.21.1](https://modrinth.com/mod/curios/version/yohfFbgD) |
| Iron's Lib | [1.21.1-2.3.0](https://modrinth.com/mod/irons-lib/version/dgH7P81J) |

The `*_version`, `*_version_id`, and `*_file_id` properties in `gradle.properties` identify these releases. Modrinth version IDs pin the exact NeoForge artifacts, avoiding ambiguity between loader builds that share a version number.

## Soul essence and crystals

Loaded overworld chunks cultivate soul essence. The base rate is **0.05 essence/second**, plus **0.05/second for each fully mature crystal cluster** in that chunk. Twenty mature clusters restore a total rate of 1.05/second. Unloaded chunks and offline servers do not accumulate missed time. Chunk essence and crystal growth stages persist across saves and restarts.

**Soul Essence Crystal Clusters** have one to five visible crystals. Mining one drops exactly that many **Soul Essence Crystal** items. Harvested crystals can be planted on any sturdy face as a one-crystal seed. Colonies grow across floors, up walls, around corners, and along ceilings. Their translucent voxel models grow from a shared stem into five angled, stepped crystal points. Collision shapes follow their growth stage and attachment direction. Neighboring clusters on the same cave surface join with seamless crystal veins; these connections update on placement, spreading, and removal, including wall and ceiling attachments. This uses native multipart models and does not require a connected-texture mod. They emit light level 12.

Every 20 seconds, a cluster may spend 10 local essence to add one crystal. A mature five-crystal cluster instead attempts to plant neighboring seeds, spending 20 essence per successful seed. Actions require a chunk to have **more than 100 essence, or to have reached its configured critical mass**, and enough essence to pay the action's cost. Blocked locations and insufficient funds cost nothing. Each new seed starts at one crystal; player planting and spreading never generate free essence.

There are two natural sources:

- **World generation:** a rare underground feature rolls approximately once per 32 chunks, then searches for a dry cave surface below Y=32 and at least eight blocks below the surface. Successful generation places a colony of **8–15 blocks**, including **two or three mature five-crystal clusters** and surrounding one- or two-crystal young growth. Patches follow nearby exposed cave surfaces and skip cramped sites without enough room. Each colony adds **50 starting essence** to its chunk, regardless of its size. This bonus is credited during generation, not every time the chunk loads.
- **Existing loaded chunks:** high-essence chunks have a 0.1% chance per minute to attempt a new cave seed. Successful placement costs 20 local essence. This allows older worlds to develop colonies without regenerating terrain.

The block retains `riftfall:crystal_cluster` for existing-world compatibility. Find it under Natural Blocks. The new item is `riftfall:soul_essence_crystal`, under Ingredients.

## Rift formation and root pools

The world spawn chunk starts at critical mass once per world. The default critical mass is 600 essence. A critical chunk rolls a 10% chance every 10 seconds to form a rift on safe terrain. A successful formation consumes its chunk's charge into the rift's construction bank.

**New rifts must be more than 1,000 horizontal blocks from every existing rift.** Active and dormant rifts both reserve this radius. Natural formation and `/rift spawn` use the same spacing rule. Previously saved rifts keep their locations.

A rift's chunk is its **root chunk**. Edge-connected, fully loaded overworld chunks form its essence catchment. If two catchments meet, each chunk belongs to the nearest reachable root, with deterministic ties; essence is never counted twice. Unloaded chunks retain their stored points, but are not force-loaded or remotely drained.

When a root falls below 25% of critical mass, it begins refilling from its connected donors, transferring up to 10 essence per second until it reaches critical mass again. Transfers conserve essence and never overdraw a donor. Each chunk's cultivation, including mature-crystal bonuses, contributes to this shared supply.

An opening rift erupts with an entity-damaging explosion, smoke, flame, and a local Wither spawn cue. Terrain stays intact. The Wither cue uses a positional sound packet, audible at approximately 32 blocks, rather than the boss's global announcement. Portal entities remain stationary and use animated translucent interiors. Exterior orientation is randomized once and saved; dungeon exits sit on the south side facing north.

## Delving, rewards, and dormancy

Enter a rift to reach its pocket dungeon. An initial encounter has six combat mobs. An uncleared rift breaks after ten minutes and spends its energy on repeated overworld waves while its root is loaded. The tracked escaped-mob cap defaults to 20 per rift.

Defeat every tracked dungeon enemy to clear the encounter. **Clearing never teleports visitors out.** A reward chest appears at the dungeon's north end, the breach stops, and visitors can loot and explore before walking back through the south exit. Entering visitors start away from that exit so standing still does not automatically eject them.

After the last visitor leaves, the entrance and exit disappear and the rift becomes **dormant**. It keeps its identity, power, bank, root, and reserved area. It can reopen after 60 seconds if its root reaches critical mass and the active-rift cap has room. Reopening transfers that charge to the bank and constructs a new encounter on the next entry. Existing escaped mobs remain in the world.

Logged-out visitors are remembered: restarting or disconnecting inside a cleared dungeon does not discard their visit or forcibly close the gate. `/rift leave` is an escape option and counts as departing. Dormant rifts do not count against the active-rift cap.

## Rift cultivation and encounter construction

A loaded rift absorbs **0.1 essence per second** from its root into its bank and permanent power. Killing a player grants **50 essence immediately** to both the spendable bank and the historical power counter. Rift-owned mobs, and non-PvP deaths caused by hazards inside a dungeon, including traps while looting, credit the owning rift. Player kills therefore advance it much faster than ambient cultivation.

Every 50 accumulated power unlocks another level, up to level 10. An encounter uses the highest unlocked level the rift can afford. If all visitors leave or die in an uncleared encounter, a newly stronger rift can refit it for the next attempt, provided it can pay for that upgrade. Occupied encounters are not rebuilt around visitors. Essence pays for its room shell, monsters, traps, and reserved rewards; power records its accumulated ability and is not erased by spending the bank.

- Enemy counts start at six and increase by two per level. Any encounter exceeding six enemies gets a larger arena with partitioned chambers, connecting doorways, and corridors.
- Increasing levels unlock more dangerous species, improve health and combat attributes, add appropriate weapons and armor, and raise Epic Fight Impact and Stun Armor.
- The pool includes only monster-category mobs. Animals (including wolves, llamas, bees, pandas and polar bears), golems, silverfish, and endermites cannot be selected. Wither, Ender Dragon, Guardians, Elder Guardians, and the Warden are excluded. Datapacks can extend `riftfall:excluded_rift_mobs`; common boss tags and boss-like identifiers are also excluded.
- Monsters are directed against delvers and share a team to discourage infighting. Hostile summoned offspring join the encounter and cost energy too. Encounters and reinforcements have a 128-mob ownership limit.
- Stronger encounters buy doorway pressure-plate/dispenser arrow traps. These are functional traps with finite ammunition.
- Reward chests scale with the constructed encounter's danger: more materials, then diamonds and higher-tier weapons. Rewards are created once per cleared encounter and persist when the server restarts.

Each rift saves the **entrance biome and its native monster roster** when it forms. New encounters and dungeon-break waves use that ecology, including after reopening or restarting. Snowy biomes add Strays and Cryomancers; deserts add Husks and Pyromancers; swamps allow Witches, Slimes, and Apothecarists; woodland rifts can include illagers and Cultists. Species still need the appropriate rift level and budget. Drowned are excluded from the current dry arenas. Summoned/debug entities and undead Iron's Spells casters cannot be selected as ordinary inhabitants.

**Archevokers (Grand Evokers) and Ice Spiders are boss-only.** A level-five-or-higher encounter with 200 surplus essence can add one boss and a separated northern boss room: Ice Spiders in frozen rifts, Archevokers in temperate, woodland, or swamp rifts. Their existing boss statistics are preserved. Ordinary inhabitants stay outside that chamber; boss summons may join it. A purple health bar is shown to players in the room, follows the boss's health, and disappears on death or leaving. The boss must die before the rift clears; its reward chest includes additional diamonds and soul crystals. Vanilla bosses and guardians remain unavailable.

**Goblins** are short, living humanoid melee enemies. **Goblin Shamans** unlock at level three and use Iron's Spells Magic Arrow. At level three and above, a quarter of newly created Pillagers learn Firebolt, with spell levels capped at three. Animals, ordinary goblins, and undead cannot learn these spells. Casts require a living hostile target, range, line of sight, a one-second wind-up, and a cooldown; damage or an Epic Fight stun can interrupt them. Shamans keep their distance from players. Their separate skins, spawn eggs, and loot tables are included; they currently join appropriate land-biome rift rosters rather than overworld natural spawning.

Riftfall supplies three original **Epic Fight biped casting animations**: drawing energy into the hands, releasing the spell, and recovering. The server synchronizes each phase to tracking clients for Goblin Shamans and spell-trained Pillagers. Goblins have their own Epic Fight humanoid patch and renderer while retaining their melee/spell AI. Iron's Spells mobs retain their existing native animations.

Basic costs are 20 essence for a simple room shell, 120 for a multiroom shell, 10–40 per monster according to its species, 15 per trap, and 20 per level reserved for rewards. Cleared encounters rebuild on the next visit after reopening; unclaimed container contents are reset for that new encounter. Vanilla dropped items retain their normal lifetime.

## Commands

- `/rift status` — active/dormant gates, timers, level, cultivated power, construction bank, and surviving enemies.
- `/rift essence` — the current loaded chunk's charge, cultivation rate, mature-cluster count, and connected pool owner.
- `/rift essence set <amount>` — set the current chunk's charge (operator).
- `/rift spawn` — create a test rift at the command source, with the same spacing rule and opening eruption (operator; grants a starter bank if needed).
- `/rift leave` — return from the dungeon without clearing it.
- `/give @s riftfall:soul_essence_crystal` — obtain a plantable crystal item.
- `/give @s riftfall:crystal_cluster` — obtain the cluster block item.
- `/summon riftfall:goblin` or `/summon riftfall:goblin_shaman` — test the new mobs. Their spawn eggs are also in the Spawn Eggs creative tab.

## Configuration

Server settings are in `<world>/serverconfig/riftfall-server.toml`:

| Setting | Default | Purpose |
| --- | --- | --- |
| `baseEssencePerSecond` | 0.05 | Base cultivation in loaded overworld chunks |
| `matureCrystalEssenceBonus` | 0.05 | Additional cultivation per mature cluster |
| `criticalMass` | 600 | Rift formation/reopening threshold |
| `essenceRollSeconds` / `essenceSpawnChance` | 10 / 0.1 | Natural rift rolls |
| `crystalGrowthSeconds` | 20 | Growth/spread attempt interval |
| `crystalGrowthCost` / `crystalSeedCost` | 10 / 20 | Paid growth and spreading |
| `crystalCaveSeedSeconds` / `crystalCaveSeedChance` | 60 / 0.001 | Runtime cave seed rolls |
| `riftPoolDrawPerSecond` | 10 | Low-root donor transfer limit |
| `riftAmbientIntake` | 0.1 | Rift bank/power cultivation from its root |
| `riftDeathEssence` / `riftUpgradeEssence` | 50 / 50 | Player-death gain and power per level |
| `riftMaxLevel` | 10 | Danger cap |
| `riftDormantSeconds` | 60 | Minimum closed interval after departure |
| `breakSeconds` / `waveSeconds` | 600 / 15 | Breach deadline and outside wave interval |
| `maxRifts` / `maxOutsideMobs` | 5 / 20 | Active-gate and escaped-mob limits |
| `arenaEnemies` | 6 | Base encounter count |

The old `essencePerSecond` key is replaced by `baseEssencePerSecond` so existing worlds receive the slower baseline. The original `spawnSeconds` timer is also retired. Worldgen frequency and the mob exclusion list are datapack resources.

## Build and run

```sh
bash gradlew build
bash gradlew runClient
bash gradlew runServer
```

Use `gradlew.bat` on Windows. Accept Minecraft's EULA in the development run directory before running a dedicated server. Built jars are in `build/libs/`. Use Easy, Normal, or Hard difficulty; Peaceful removes hostile encounters and cannot validate dungeon combat.

## Validation

The build includes tests for persistence, fractional essence, growth funding, cultivation bonuses, danger scaling, arena size limits, spacing boundaries, dormant reservations, connected pool ownership, and conserved donor transfers.

Dedicated-server checks cover all five crystal drop counts, floor/wall/ceiling support, paid growth, spreading around surfaces, maturity bonuses, rare cave seeding, and native world generation with its initial essence credit. Real-client checks cover portal travel, staying inside after clearing, manual exit and conditional reopening, player-death progression, expanded encounters, Epic Fight attributes, functional arrow traps, and higher-tier rewards. Long-term balance and dungeon variety still need playtesting.
