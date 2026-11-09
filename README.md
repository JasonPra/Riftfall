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

## Dungeon building blocks

Dungeon Bricks (`riftfall:dungeon_bricks`), Dungeon Brick Slab (`riftfall:dungeon_brick_slab`), and Dungeon Brick Stairs (`riftfall:dungeon_brick_stairs`) are available in the Building Blocks creative tab. They use vanilla deepslate brick textures and sounds, cannot be mined outside Creative mode, resist explosions and boss destruction, and cannot be moved by pistons. Slabs and stairs retain normal placement, shape connections, and waterlogging. These are building materials for future dungeon construction; existing arenas are unchanged. There are no survival recipes or block drops.

## Soul essence and crystals

Loaded overworld chunks cultivate soul essence. The base rate is **0.05 essence/second**, plus **0.05/second for each fully mature crystal cluster** in that chunk. Twenty mature clusters restore a total rate of 1.05/second. Unloaded chunks and offline servers do not accumulate missed time. Chunk essence and crystal growth stages persist across saves and restarts.

**Soul Essence Crystal Clusters** have one to five visible crystals. Mining one drops exactly that many **Soul Essence Crystal** items. Harvested crystals can be planted on any sturdy face as a one-crystal seed. Colonies grow across floors, up walls, around corners, and along ceilings. Their six-sided, pointed crystals branch from a shared stem, with three stable shape variations per growth stage. Dense pale-blue mineral surrounds a full-bright soul core; sparse soul particles drift from the growth. Collision shapes follow the actual prism geometry and attachment direction. Neighboring clusters on the same cave surface merge into low, irregular mineral beds with seamlessly tiled textures and matching shared edges. Connections update on placement, spreading, and removal, including wall and ceiling attachments. The old cyan rails and square base plates are removed. This uses a built-in NeoForge geometry loader and needs no connected-texture mod. They emit light level 12.

Every 20 seconds, a cluster may spend 10 local essence to add one crystal. A mature five-crystal cluster instead attempts to plant neighboring seeds, spending 20 essence per successful seed. Actions require a chunk to have **more than 100 essence, or to have reached its configured critical mass**, and enough essence to pay the action's cost. Blocked locations and insufficient funds cost nothing. Each new seed starts at one crystal; player planting and spreading never generate free essence.

There are two natural sources:

- **World generation:** a rare underground feature rolls approximately once per 32 chunks, then searches for a dry cave surface below Y=32 and at least eight blocks below the surface. Successful generation places a colony of **8–15 blocks**, including **two or three mature five-crystal clusters** and surrounding one- or two-crystal young growth. Patches follow nearby exposed cave surfaces and skip cramped sites without enough room. Each colony adds **50 starting essence** to its chunk, regardless of its size. This bonus is credited during generation, not every time the chunk loads.
- **Existing loaded chunks:** high-essence chunks have a 0.1% chance per minute to attempt a new cave seed. Successful placement costs 20 local essence. This allows older worlds to develop colonies without regenerating terrain.

The block retains `riftfall:crystal_cluster` for existing-world compatibility. Find it under Natural Blocks. The new item is `riftfall:soul_essence_crystal`, under Ingredients.

## Rift formation and root pools

The world spawn chunk starts at critical mass once per world. The default critical mass is 600 essence. A critical chunk rolls a 10% chance every 10 seconds to form a rift on safe terrain. A successful formation consumes its chunk's charge into the rift's construction bank.

**New rifts must be more than 1,000 horizontal blocks from every existing rift.** Active and dormant rifts both reserve this radius. Natural formation and `/rift spawn` use the same spacing rule. Previously saved rifts keep their locations.

A rift's chunk is its **root chunk**. Edge-connected, fully loaded overworld chunks within 1,000 blocks of the root (measured between chunk centers) form its essence catchment. If two catchments meet, each chunk belongs to the nearest reachable root, with deterministic ties; essence is never counted twice. Unloaded chunks retain their stored points, but are not force-loaded or remotely drained.

Active and dormant roots refill whenever they are below critical mass. Each connected donor can contribute up to 10 essence per second, with a combined cap of 100 per root per second (`riftPoolDrawPerSecond` is now per donor; `riftPoolTotalDrawPerSecond` controls the combined cap). With ten stocked donors, a depleted 600-point root can refill in about six seconds. Crystal growth and cave seeding in a dormant root reserve its reopening charge; other chunks and active roots retain normal crystal spending. Transfers conserve essence and never overdraw a donor. Each chunk's cultivation, including mature-crystal bonuses, contributes to this shared supply.

An opening rift erupts with an entity-damaging explosion, smoke, flame, and a local Wither spawn cue. Terrain stays intact. The Wither cue uses a positional sound packet, audible at approximately 32 blocks, rather than the boss's global announcement. Portal entities remain stationary and use animated translucent interiors. Exterior orientation is randomized once and saved; dungeon exits sit on the south side facing north.

## Delving, rewards, and dormancy

Enter a rift to reach its pocket dungeon. An initial encounter has six combat mobs. An uncleared rift breaks after ten minutes and spends its energy on repeated overworld waves while its root is loaded. The tracked escaped-mob cap defaults to 20 per rift.

Defeat every tracked dungeon enemy to clear the encounter. **Clearing never teleports visitors out.** A reward chest appears at the dungeon's north end, the breach stops, and visitors can loot and explore before walking back through the south exit. Entering visitors start away from that exit so standing still does not automatically eject them.

After the last visitor leaves, the entrance and exit disappear and the rift becomes **dormant**. It keeps its identity, power, bank, root, and reserved area. It can reopen after 60 seconds if its root reaches critical mass and the active-rift cap has room. Reopening transfers that charge to the bank and constructs a new encounter on the next entry. Existing escaped mobs remain in the world.

Logged-out visitors are remembered: restarting or disconnecting inside a cleared dungeon does not discard their visit or forcibly close the gate. `/rift leave` is an escape option and counts as departing. Dormant rifts do not count against the active-rift cap.

## Rift cultivation and encounter construction

A loaded rift absorbs **0.1 essence per second** from its root into its bank and permanent power. Killing a player grants **50 essence immediately** to both the spendable bank and the historical power counter. Rift-owned mobs, and non-PvP deaths caused by hazards inside a dungeon, while looting, credit the owning rift. Player kills therefore advance it much faster than ambient cultivation.

Every 50 accumulated power unlocks another level, up to level 10. An encounter uses the highest unlocked level the rift can afford. If all visitors leave or die in an uncleared encounter, a newly stronger rift can refit it for the next attempt, provided it can pay for that upgrade. Occupied encounters are not rebuilt around visitors. Essence pays for its arena shell, monsters, and reserved rewards; power records its accumulated ability and is not erased by spending the bank.

- Enemy counts start at six and increase by two per level. Larger encounters get more open space. Procedural room layouts and all traps are disabled while dungeon design is reworked. Existing active arenas are simplified once, preserving reward chests.
- Increasing levels unlock more dangerous species, improve health and combat attributes, add appropriate weapons and armor, and raise Epic Fight Impact and Stun Armor.
- The pool includes only monster-category mobs. Animals (including wolves, llamas, bees, pandas and polar bears), golems, silverfish, and endermites cannot be selected. Wither, Ender Dragon, Guardians, Elder Guardians, and the Warden are excluded. Datapacks can extend `riftfall:excluded_rift_mobs`; common boss tags and boss-like identifiers are also excluded.
- Monsters are directed against delvers and share a team to discourage infighting. Hostile summoned offspring join the encounter and cost energy too. Encounters and reinforcements have a 128-mob ownership limit.
- Reward chests scale with the constructed encounter's danger: more materials, then diamonds and higher-tier weapons. Rewards are created once per cleared encounter and persist when the server restarts.

Each rift saves its **entrance biome and weighted monster roster**. Reopened encounters and dungeon-break waves use that same roster:

| Entrance | Current pool |
| --- | --- |
| Plains / sunflower plains | Goblin (weight 7), Goblin Archer (3); Goblin Shaman (2) unlocks at level three. No undead |
| Cold biomes | The entrance biome's vanilla monster weights; no Drowned or Husks. A funded level-five-plus encounter can add an Ice Spider boss |
| Other biomes | That biome's default vanilla monster roster and weights, subject to the general animal, guardian, boss, silverfish and endermite exclusions |

Plains encounters contain only goblins. Saved rosters are migrated, and tracked undead left in old plains encounters are removed. Zombies (including their variants) and skeletons now hunt goblins; goblins also recognize those undead as enemies. **Every Iron's Spells wizard/mage, including the Archevoker, is overworld-exclusive.** No Iron's Spells mob enters a normal rift roster. The **Ice Spider** is the sole enabled exception for a boss encounter, restricted to cold rifts and a separated, fixed northern boss enclosure with a health bar. The boss must die before clearing; boss rewards include extra soul crystals and diamonds. Older saved rosters are migrated to these rules; incompatible old inhabitants are removed.

**Goblins** are primitive, living humanoids, about 1.5 blocks tall. Their pointed-ear meshes, textured faces, hide clothing and hand attachments work in Epic Fight and the vanilla renderer. Existing goblins using either previous default scale are corrected on load; deliberately customized scales are preserved.

- **Melee goblins** use primitive weapons and form the frontline.
- **Goblin Archers** use bows. Archers and shamans flee nearby attackers and seek safer positions behind allied melee goblins before resuming ranged attacks.
- **Goblin Shamans** wear an animal-bone helmet, tooth necklace and hide wraps, and carry a **Bone Staff**. They cast Iron's Spells Magic Arrow. A close attacker interrupts spell preparation; the staff becomes a club with a telegraphed strike and knockback to create space. A wounded ally can trigger Iron's Spells **Healing Circle**, aimed at that ally to heal nearby allies with a 20-second cooldown. Enemies are not healed. Wild goblins recognize each other as allies; rift teams keep different rifts separate.

Warrior goblins cycle through an Epic Fight swipe, backswing, and three-cut flurry, with damage timed to each cut. Their uneven wind-ups, snapping edge-first cuts, startled head movements, and ragged recoveries emphasize their frightened, feral temperament. Small local head jitters and arm/shin trembles appear in combat; the body root and torso are not shaken. From nearby high ground (roughly one block above a target, 1.5–4.5 blocks away), they crouch, push off, raise the sword with both hands overhead, and cut downward as they fall. This plunge has a four-second cooldown, commits to its launch direction, and checks normal melee reach and line of sight during descent, so dodging and cover can make it miss.

A warrior close behind a grounded player can climb onto their back and restrain them for up to four seconds, making four stabs for **1 base damage (half a heart) each**, before normal armor/resistance effects. The HUD prompts the player to mash their bound attack key; **six distinct presses** shake the goblin off. The server validates and rate-limits escape presses. Damage/stun to the goblin, death, disconnection, changing dimension, switching to Creative/Spectator, or teleporting out also ends the hold. Only one goblin can hold a player at a time, and that goblin has a ten-second grapple cooldown.

Goblin base movement speed is **0.23**. Four or more available warriors fighting the same enemy spread into distinct flank slots, approach around that enemy, and then close in to strike. This applies to any enemy they attack, including mobs. A portion of the frontline screens nearby archers; archers position behind warriors, spread into firing lanes, and seek reachable ground one or two blocks higher when it offers a clear shot. Blocked routes fall back to ordinary pursuit rather than teleporting or forcing terrain changes.

Unsupported archers flee in short, slower, imperfect directions with pauses, rather than continuously selecting the best escape vector. Four nearby available warriors enable more organized retreats behind their screen. Nearby numbers increase confidence, wounds reduce it, and targets at or below **30% health**, or affected by **poison or wither**, make goblins bolder. Confidence lowers the distance at which ranged goblins panic and the health threshold at which warriors retreat. Archers avoid shooting through allies. Nearby allies can assist an existing fight, and the target and tactical positions update as enemies move or reinforcements arrive or die.

Archers and shamans still prioritize covering a withdrawing warrior's pursuer, and shamans prioritize healing wounded retreating warriors. Tactics respect faction/scoreboard alliances and never cooperate across different rift teams. Positions use loaded, supported terrain with reachable navigation paths.


Animated in-game previews: [high-ground plunge](docs/previews/goblin-high-ground-plunge.gif), [swipe / backswing / flurry](docs/previews/goblin-sword-combo.gif), [rear grapple](docs/previews/goblin-back-grapple.gif), and [attack-button escape](docs/previews/goblin-grapple-escape.gif).

The Bone Staff has a slim, jointed bone shaft, hide-wrapped grip, and a small weathered animal-skull head. The model follows the normal held-tool axis, with its wrapped grip positioned at the item origin. It uses the goblin’s normal arm pose and Epic Fight’s standard item attachment; there is no staff-specific elbow or wrist override.

Casting uses original Epic Fight charge, release and recovery clips, plus a staff-club animation synchronized to its hit. The hand-bone initialization and scale correction apply to body meshes and held equipment. Spawn eggs exist for all three goblins, and the Bone Staff is in the Combat creative tab. Goblins currently spawn through the plains rift roster rather than natural overworld generation. Existing spell-trained Pillagers retain their previously granted Firebolt ability, but Pillagers are not added to the new plains pool.

Construction costs are 20 essence for the simple arena shell, 10–40 per normal monster according to its species, and 20 per level reserved for rewards. An Ice Spider boss and enclosure require an additional 200 surplus essence. There are no procedural-room or trap costs. Cleared encounters rebuild on the next visit after reopening; unclaimed container contents reset for the new encounter. Vanilla dropped items retain their normal lifetime.

## Commands

- `/rift status` — active/dormant gates, timers, level, cultivated power, construction bank, and surviving enemies.
- `/rift roster <id>` — inspect a rift’s saved entrance biome and weighted monster pool.
- `/rift essence` — the current loaded chunk's charge, cultivation rate, mature-cluster count, and connected pool owner.
- `/rift essence set <amount>` — set the current chunk's charge (operator).
- `/rift spawn` — create a test rift at the command source, with the same spacing rule and opening eruption (operator; grants a starter bank if needed).
- `/rift leave` — return from the dungeon without clearing it.
- `/give @s riftfall:soul_essence_crystal` — obtain a plantable crystal item.
- `/give @s riftfall:crystal_cluster` — obtain the cluster block item.
- `/summon riftfall:goblin`, `/summon riftfall:goblin_archer`, or `/summon riftfall:goblin_shaman` — test the new mobs. Their spawn eggs are also in the Spawn Eggs creative tab.

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
