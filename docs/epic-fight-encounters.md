# Tower and rift combat compatibility

Audited Player System 0.0.11, Quintessence 0.144.0 and Epic Fight 21.17.3.1. The [complete roster](epic-fight-encounter-audit.json) records all 65 Player System species and the 19 entity types named in Quintessence's shipped elemental-rift tags. Datapacks can extend these rosters.

## Humanoid combat

| Player System body | Epic Fight patch |
| --- | --- |
| `orc_warrior` | Vindicator |
| `rotting_zombie` | Zombie |
| `gargoyle_striker` | Zombie |
| `decayed_skeleton` | Skeleton |
| `kobold_scout` | Skeleton |
| `death_knight` | Wither skeleton |

These are explicitly registered entity types, not a blanket replacement of every mob's AI. Epic Fight owns their melee animation, weapon collision and hit reactions. Player System's species stats, affixes and encounter bookkeeping remain in place. Their installed GeckoLib cube geometry is converted to a skinned humanoid mesh at resource load, using the installed textures and resource-pack geometry. No third-party model or texture is bundled. Appendages follow their parent humanoid joint; independent GeckoLib wing, jaw, cape and tail animations are not reproduced. This is a baseline humanoid binding, not a bespoke animation set for each species.

The pinned Epic Fight jar's GeckoLib integration contains an armor mesh transformer and armor-renderer mixin. It does not automatically register combat patches and entity skeletons for these custom mob IDs. This adapter supplies those missing bindings.

## Goblin substitution and confinement

Player System's `goblin_scavenger` and `hobgoblin_guard` species keep their original species IDs, ranks, statistics and skills, but use `riftfall:goblin` bodies. Their foreign fleeing/shield behaviors are removed so that Riftfall owns goblin movement and blocking. A factory redirect also handles those original body IDs in Player System encounter spawners and Quintessence rifts. Substitution occurs before species scaling, encounter tracking and rewards are applied; it does not cancel and respawn an already tracked entity.

On server world entry, all Riftfall goblin roles are marked `RiftfallInRift=true` when any of these holds:

- The dimension is `player_system:dungeon_tower`.
- Player System assigned the `player_system.dungeon_mob` scoreboard tag or `player_system_instance` persistent key.
- Quintessence assigned the `quintessence_rift` persistent key.

Tower goblins are also marked during construction, before world-entry listeners can intercept their spawn. The flag is set before the first AI tick and survives save/load. Ordinary overworld goblins without an encounter marker keep the default false value. The flag controls the existing panic-escape restriction; tactical movement and short combat retreats remain available. Existing saved foreign goblin entities are not rewritten by this change. The original goblin spawn eggs also create Riftfall goblins, but do not set the confinement flag outside encounters. Explicit `/summon player_system:...` commands and existing saved foreign entities are not migrated.

## Remaining custom integrations

The audit deliberately does not mark Cataclysm bosses, Iron's spellcasters, nonhumanoid Player System bodies, or Quintessence's custom guardians as fully supported. Their spells, phases, multipart bodies and area attacks need explicit integration. Examples include Player System's Ravager-based minotaur, Warden-based sovereign, flying wyvern, dungeon sorcerer and crypt necromancer. Assigning humanoid melee AI to them would remove important native behavior. Native AI remains active for these mobs.

Quintessence's shipped rift tags mostly select vanilla mobs. Those use Epic Fight's existing vanilla support where it exists; a vanilla entry such as a blaze or breeze does not imply that Epic Fight supplies it with a combat patch. Quintessence can also choose dynamically registered species, so the tag list alone is not an exhaustive runtime spawn list.

## Validation

The native server check creates all six patched humanoids, verifies their attached patch classes, and confirms that an orc deals damage through an Epic Fight attack animation. It exercises Player System's species factory for both substituted goblins and checks flag persistence, marked-rift spawns, and ordinary unmarked spawns. Mesh regression checks load the installed geometry for every supported humanoid, validate finite vertices/normals and valid skin/triangle indices, and verify outward cube-face winding after coordinate conversion. Temporary verification entities and launch fixtures are excluded from the release build.
