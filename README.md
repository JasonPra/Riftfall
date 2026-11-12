# Riftfall

Minecraft **1.21.1 / NeoForge**, Java **21**. This branch implements goblin combat and squad behavior, shared mob resources and equipment, mana-crystal cultivation, and biome-scale mana geyser ecosystems. The current mana systems are separate from the removed rift/soul-essence prototype.

## Goblins and combat

Goblins have three roles, authored Epic Fight attacks, approximately 1.5-block height, animated faces, fear trembling, and equipment rendering. Spawn them through eggs or `/summon`; natural goblin spawning is not implemented.

| Role | Current abilities |
| --- | --- |
| Warrior | Goblin Shortsword combos, buckler guarding, high-ground plunges, flanking/screening, wounded retreat, and opportunistic rear ambushes. Unshielded warriors grapple; shielded warriors learn Backstab. |
| Archer | Bow attacks, friendly-fire avoidance, rear/elevated positions, retreat, and covering fire for withdrawing warriors. |
| Shaman | Bone Staff sweep/smash, Iron's Spells offensive casting, allied Healing Circle, and ranged positioning. Shamans do not burrow-escape. |

The **Goblin Shortsword** uses stone-sword stats and an item-bound Epic Fight moveset usable by players. Its three-attack unshielded chain ends in a four-cut flurry; a shield adds an overhead fourth attack. Actual weapon colliders determine hits. Raised shields use physical shield contact, with native guard reactions and stamina costs. See [goblin combat, sword and action priorities](docs/goblin-combat.md) and [shield collision](docs/shield-colliders.md).

All roles use one weighted behavior engine. Readiness, confidence, target awareness, commitment, exhaustion, healing and panic determine the action; scores are not a fixed list of unconditional orders. Goblins remember the last seen target position rather than tracking hidden targets through walls. They regroup, sprint tactically, seek safe allied healing circles, and exploit an unwatched player's rear even when normally too frightened to advance. Detailed scores and eligibility are in the [combat reference](docs/goblin-combat.md); engine APIs and diagnostics are in [behavior-engine.md](docs/behavior-engine.md).

Goblins scavenge dropped items, carry 36 storage slots, upgrade permitted one-handed gear and armor, and return collected loot on death. A thief that successfully burrows away is preserved in saved data for a future encounter system; it is not automatically respawned. See [loot and stash](docs/goblin-loot-and-stash.md).

## Resources, skills and equipment

- Non-undead mobs share a persistent stamina resource for physical actions. Exhaustion causes exposed panting until fully recovered; players retain Epic Fight stamina. See [mob stamina](docs/mob-stamina.md).
- Mob spellcasting uses Iron's mana, including equipment-derived capacity/regeneration, spell costs, and full-refill recovery after exhaustion. Casting does not spend stamina. The mob mana pool is separate from world ambient mana. See [mana, Curios and equipment](docs/mob-mana-and-equipment.md).
- Five reusable active-skill slots support NPC adapters. Backstab has player and mob implementations, including a dedicated greatsword sequence. Equipping a skill book as loot does not teach a goblin the skill. See [mob skills](docs/mob-skills.md).
- The Bone Staff, Goblin Buckler, Goblin Shortsword and Backstab skill book are in the Combat tab. Spawn eggs are in Spawn Eggs. Crystals use Natural Blocks (raw) and Ingredients (cut).

## Mana crystals

Eight tiers progress **red → orange → yellow → green → cyan → blue → violet → white**. Raw crystals can be planted on natural stone and tagged ores, including underwater, on floors, walls and ceilings. Eight layers form a mature mother. Only mothers spread and refine; offspring are lower-tier, with red clamped to red. Water modestly accelerates growth/spreading, while higher-tier height growth is slower. Spreading spends ambient mana.

Connected clusters follow supporting surfaces and corners. Same-tier adults pack more closely together; mixed-tier edges blend only their seed beds and choose whole prism colors from neighbors. Smoky translucent prisms, opaque seed beds, coplanar seam cleanup, and fullbright star sparkles provide the current appearance.

Initial geyser deposits, recovery after mining, slow tier refinement, assistance from the highest adjacent higher-tier mother, unloaded progression, drops, stonecutting and all balance settings are covered in [mana-crystals.md](docs/mana-crystals.md).

## Mana geysers and world generation

New fields span approximately **2,064, 3,088 or 4,112 blocks**. Each has a compact white core, two broad rings and a narrow outer fringe. Their full footprint contributes to one shared mana pool; ordinary chunks use their own reservoirs. The mana stream has no rendered shaft. See [ambient mana](docs/ambient-mana.md).

| Zone | Current biome profile | Test grass/foliage color |
| --- | --- | --- |
| White core | Dense jungle, occasional sheltered lake basin; dense white mother deposits below | White |
| Inner ring | Dense jungle and occasional larger lakes | Blue |
| Middle ring | More open jungle woodland, lakes and connected streams | Yellow |
| Outer fringe | Plains-like grassland with rare jungle trees | Red |

A 384-block landscape-only belt cushions the fields with vanilla plains/woodland, climate-appropriate beaches, shallow oceans and deeper water where the surrounding generator calls for them. It preserves native cave biomes below the belt and does not enlarge the mana footprint. Terrain uses gentle hills, valleys, irregular lakes and a meandering river with changing width. The junction lake has two channel connections instead of a four-way crossing. Underground routes follow a jittered 3D graph with tight fractures, changing ceiling heights, winding passages, irregular caverns and steep drops. Low tunnels and some shafts flood; dripstone, lush sections and rare sloping surface entrances provide variety. Riverbeds have depth variation and layered gravel, sand, clay, mud and coarse dirt; suitable water receives seagrass. Jungle trees and vanilla flora/fauna are placeholders for future custom ecosystems.

Generation follows Minecraft's stages: structure starts are excluded from fields, biomes are assigned at `BIOMES`, terrain is shaped at `SURFACE`, and caves are excavated at `CARVERS`, before vegetation at `FEATURES`. Structures cannot extend pieces into fields from outside starts. Plant placement checks reject unsupported/submerged land plants and maintain support when other decoration replaces blocks. An unfinished-chunk surface plan restores river/lake water and sediment after carving, preventing underwater air pockets. Surface water matches vanilla sea level; outlet connectors seek an actual nearby vanilla river or ocean rather than cutting off at the field boundary.

River currents also affect existing vanilla rivers. They apply physical drift without requiring flowing-water block states: downhill outlets take precedence; otherwise direction points away from the broadest sampled connected water body. The bounded, cached search estimates water-body size and does not load terrain.

New generation does not repair already-generated floating vegetation or buried structures. Discovered legacy geysers keep their old footprints and deposits. Use the random teleport command to test fresh fields in the same save. Sizes, biome identifiers, generation stages, persistence and limits are documented in [geyser-regions.md](docs/geyser-regions.md).

## In-game testing

Most `/riftfall` diagnostics and world commands require operator permission. Client collider/model overlays do not.

```mcfunction
/summon riftfall:goblin
/summon riftfall:goblin_archer
/summon riftfall:goblin_shaman
/summon riftfall:behavior_tester
/give @s riftfall:goblin_shortsword
/give @s riftfall:bone_staff
/give @s riftfall:goblin_buckler
/give @s riftfall:backstab_skill_book
/give @s riftfall:raw_red_mana_crystal
/riftfall behavior debug on
/riftfall behavior inspect @e[type=riftfall:goblin,sort=nearest,limit=1]
/riftfall geyser locate
/riftfall geyser inspect
/riftfall geyser teleport
/riftfall ambient inspect
```

`geyser locate` finds the nearest candidate without generating terrain. `geyser teleport` sends the executing player to a **random undiscovered field**, arriving above its center’s canopy or water surface without landing-safety checks; repeat it to visit another field. Teleporting generates destination terrain. Spawned goblins ignore creative/spectator players as ordinary combat targets; use survival for behavior tests.

**F8** / `/riftfall debug helditems` shows model bounds (cyan shields, gold other items). **F3+B** / `/riftfall debug colliders` shows entity hurtboxes, native Epic Fight attack colliders and raised physical shield boxes. `/riftfall debug off` disables both overlays.

## Documentation index

| Topic | Reference |
| --- | --- |
| Sword, role movesets and exact goblin action scores | [Goblin combat](docs/goblin-combat.md) |
| Shared engine, awareness, healing and debug overlays | [Behavior engine](docs/behavior-engine.md) |
| Confidence weights, equipment threats and losses | [Combat confidence](docs/combat-confidence.md) |
| Panic, burrowing, audio completion and future rift flag | [Panic escape](docs/panic-escape.md) |
| Grapples, struggle and shared NPC ambush cooldown | [Grapples and ambushes](docs/grapples-and-ambush-cooldowns.md) |
| Backstab and NPC active-skill adapters | [Mob skills](docs/mob-skills.md) |
| Scavenging, equipment restrictions and saved thieves | [Loot and stash](docs/goblin-loot-and-stash.md) |
| Physical resources and exhaustion | [Mob stamina](docs/mob-stamina.md) |
| Spell resources, attributes and Curios | [Mob mana and equipment](docs/mob-mana-and-equipment.md) |
| Independent chunk and shared field reservoirs | [Ambient mana](docs/ambient-mana.md) |
| Crystal cultivation, visuals and refinement | [Mana crystals](docs/mana-crystals.md) |
| Biomes, terrain, caves, rivers and compatibility | [Geyser ecosystems](docs/geyser-regions.md) |
| Shield contact and custom shield profiles | [Shield colliders](docs/shield-colliders.md) |
| Animation/moveset integration | [Epic Fight registration](docs/epic-fight-registration.md) |
| Reusable eyes, expressions and skin layout | [Facial animation](docs/facial-animation.md) |
| Third-person controls and collision | [Third-person camera](docs/third-person-camera.md) |
| Scheduling and behavior costs | [Behavior performance](docs/behavior-performance.md) |
| GeckoLib outline rendering compatibility | [Outline compatibility](docs/geckolib-outline-compatibility.md) |

## Other current features and scope

Goblins have independent blinking and eye tracking, context-driven expressions, fear-scaled limb tremor, and mouth/voice presentation. Panic escape retains digging particles/audio and waits for current voice playback to finish; actual health loss and fully blocked hits use different vocal reactions. The configurable rear third-person camera adds shoulder/height offsets, terrain collision and smooth extension. Backstab keeps the attacker anchored while allowing camera orbit, with victim-perspective restoration through recovery or respawn.

Block-carrying Endermen use the vanilla carrying pose outside combat. Combat hides the rendered block, retaining the actual held block; hiding emits one purple puff, suppressed immediately after teleporting. Epic Fight combat animations retain priority.

Previously spell-trained living Pillagers retain opted-in casting. Shamans drop bones instead of the removed soul-essence item. There is no current rift portal/dungeon generation, arena dimension, rift economy, natural goblin spawning or escaped-thief encounter/respawn system. The panic `RiftfallInRift` flag is an integration hook, not a working rift system. Custom ecosystem trees/flora/fauna, crystal metal refinement and harvesting hazards are future work.

## Development

Java **21** is required. Pinned integrations include Epic Fight **21.17.3.1**, Iron's Spells **1.21.1-3.16.3**, GeckoLib **4.9.3**, Curios, Iron's Lib and Player Animator. Exact versions/file IDs are in [gradle.properties](gradle.properties); Gradle resolves supporting dependencies.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

World-specific server configuration lives in `<world>/serverconfig`: `riftfall-server.toml` (confidence/behavior), `riftfall-stamina-server.toml`, `riftfall-mana-server.toml`, and `riftfall-crystals-server.toml`. Existing values can override documented defaults. Cosmetic camera settings are client configuration; see the camera reference.

The automated suite covers combat geometry, action selection, resources, crystal growth/refinement, saved mana fields, terrain functions and river direction. Native generation checks additionally verify generation-stage order, neighboring chunks, supported plants, sediment/seagrass and physical drift. Animation previews remain in [docs/previews](docs/previews).
