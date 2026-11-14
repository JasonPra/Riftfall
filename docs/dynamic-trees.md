# Dynamic Trees dependency

Riftfall requires **Dynamic Trees 1.7.2 for NeoForge**, CurseForge project **252818**, file **8227322**. The release declares Minecraft `[1.21.1, 1.22)` and NeoForge `[21.1.80,)`, compatible with this branch's Minecraft 1.21.1 / NeoForge 21.1.265.

Gradle pins `curse.maven:dynamic-trees-252818:8227322`; the mod metadata requires `dynamictrees` 1.7.2 on client and server. The library is a separate runtime dependency, not bundled into the Riftfall jar. Development runs resolve it automatically.

Riftfall registers its first custom family and species, `riftfall:vermillion_legacy`, through a bundled tree pack and NeoForge's Dynamic Trees registration hooks. It includes thick living branches, underground roots, leaves, a seed, a sapling, custom growth logic and three captured seed-grown references. See [Heartwood](legacy-tree.md) for preview commands and the remaining portions.

## Biome and placement integration

All mana geyser rings use a registered `riftfall:mana_geysers` biome source wrapping the world's original source. Chunk biome generation, uncached biome queries, biome searches and feature sorting use the same ring/transition resolver. The original source remains available for coast/climate sampling without recursively sampling the geyser overlay. The wrapper's codec preserves the original source when saving and reopening worlds. Outside geyser landscapes, original biome sampling is retained.

Trees use Dynamic Trees' normal biome-decoration feature. `trees/riftfall/world_gen/feature_cancellers.json` cancels vanilla tree features for every `riftfall:geyser_*` biome, and `default.json` explicitly selects the species for each ring. There are no direct jungle-tree calls in the custom surface-vegetation feature.

| Biome | Tree selection |
| --- | --- |
| Manaheart Grove | Heartwood; large crowns use spaced, jittered grove anchors |
| Ancient Wilds | Dynamic jungle, mega-jungle and cocoa trees |
| Elderwood Reach | Sparser dynamic jungle, undergrowth and cocoa trees |
| Verdant Frontier | Rare dynamic jungle trees |
| Retained river/lake biome IDs | No trees; these biomes remain registered for old saves only |

The other rings still use temporary jungle ecology; this integration does not redesign them or change their colors. Only Manaheart Grove has the new Heartwood species and lush-green grass/foliage palette. Terrain height and crystal rules are unchanged.

Heartwood trees generate through the same Dynamic Trees feature as the other species. Their custom species applies spacing to avoid ordinary small-tree density, and natural crowns leave room for leaf ageing inside Minecraft's 3×3-chunk feature-write area. `/legacytree <soil position>` remains available for immediate inspection; seeds use ordinary growth mechanics.

Existing incorrect trees are not removed or replaced. Test natural decoration in fresh chunks or a new world.

## Verification

Native NeoForge/Dynamic Trees checks compared 312 source, uncached and BIOMES-stage palette samples across all rings, seams, heights and the transition belt, plus a separate negative-coordinate field. The biome-source codec round-tripped successfully. Real FEATURES-stage generation produced Heartwood branches in Manaheart with no ordinary forest branches or vanilla logs in the sampled chunk. Inner/middle samples contained dynamic trees and no vanilla logs; the sampled sparse Frontier chunk contained no trees. The same checks passed after saving and reopening the world, with no far-chunk write errors.

Wild Heartwood layouts and branch radii are captured from planted trees, rather than authored as a separate scaffold. Both use the same living blocks and ongoing growth logic. `/legacytree inspect` reports measurable growth and fertility; Dynamic Trees has no definitive final-adult stage. See the [tree diagnostics](legacy-tree.md#inspecting-growth).
