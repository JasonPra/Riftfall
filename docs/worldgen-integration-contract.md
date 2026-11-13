# Native regional world generation: first prototype contract

Current status: the custom river/lake generator and routed underground rivers have been removed at the user's request. Elevation changes, including the proposed inner-ring mountains, are on hold. The architecture below records the earlier native-overhaul plan, not authorization to restore the removed hydrology.

Tracking: [roadmap #4](https://github.com/JasonPra/Riftfall/issues/4), [architecture #5](https://github.com/JasonPra/Riftfall/issues/5).

## Scope

Build one temperate mana-geyser region first: a white core, inner forest/jungle, woodland middle ring and Verdant Frontier. Start with restrained hills, valleys and Minecraft-style rivers and lakes. Giant trees, special terrain, other climate families and custom flora/fauna follow later. Maintain all four zones when scaling: increasing the core must not consume surrounding rings. Issue #6 will choose exact dimensions and spacing; doubling today's approximate diameters is a proposal, not an implemented setting.

Reserve actual space for the region and move the surrounding native landscape outward coherently. Do not manufacture another plains/beach/ocean padding belt. Use continuous seeded noise on circular internal and external biome boundaries. This document defines the integration requirements; it does not claim coordinate displacement is already implemented or proven.

## Baseline implementation and failure mechanism

Baseline commit: `872a8f1` on `prototyping`, Minecraft 1.21.1, NeoForge 21.1.265, Java 21.

`GeyserBiomeGenerationMixin` replaces quart biomes after BIOMES, suppresses ordinary structure starts, reshapes columns after SURFACE, then adds custom caves and repairs water after CARVERS. `GeyserTerrain` makes its own meandering channel, lake bowls and height profile. `GeyserCoast` adds a 384-block transition belt. These run during chunk generation, but the terrain rewrite is still downstream of native noise and surface decisions. Native height queries therefore do not describe the rewritten terrain. Adding river biome names alone cannot fix that mismatch.

Heightmaps summarize generated block columns. Native Overworld terrain uses three-dimensional density functions, including caves, and is not generated from a single heightmap. River selection and terrain shaping share climate/noise inputs; there is no standalone river API that creates a coherent watercourse just by placing a river biome.

## Coordinate contract

Introduce one immutable, seed/dimension/version-specific regional sampler. Input is the real output-world position; it returns the native source coordinates and regional influence. Outside the finite influence radius the transform must be identity, with a smooth handoff. Preserve Y initially. Sampling must work without loading chunks and must not depend on discovery order. The reserved interior needs an explicit native-like terrain/climate policy; squeezing the entire interior onto one source point is unacceptable.

The outward transform must be continuous, nonfolding and deterministic at negative coordinates. Its inverse/forward convention must be documented before implementation. Single-region Jacobian/stretch and seam checks precede multi-region composition. Large nominal sizes do not prove acceptable stretching; issue #7 is an experimental gate.

| Native path inspected in 1.21.1 sources | Required agreement |
| --- | --- |
| `RandomState` router and `Climate.Sampler`; `MultiNoiseBiomeSource.getNoiseBiome` | Temperature, vegetation, continents, erosion, depth and ridges must sample the same regional landscape as terrain. Convert quart to block coordinates before mapping; do not round source coordinates prematurely. Apply ring biome selection in this path, not as a later mask. |
| `NoiseBasedChunkGenerator.fillFromNoise`, `createNoiseChunk`; `NoiseChunk` interpolation | Map the underlying native density/noise evaluation consistently, including cave inputs. Interpolation, bulk evaluation and cached wrappers must preserve semantics; wrapping only scalar `compute` is insufficient. Avoid applying the transform twice to nested density functions. |
| `getBaseHeight`, `getBaseColumn`, `NoiseChunk.preliminarySurfaceLevel` | Structure forecasts and aquifer surface estimates must see the same terrain as generated chunks. Cached preliminary heights need the same regional policy. |
| `Aquifer.NoiseBasedAquifer.computeSubstance` | Density, floodedness/spread/barrier/lava noises and surface estimates must agree. Keep aquifer neighborhood topology/cache allocation in output space unless deliberately rebuilding it; mapping the context into an existing chunk-local cache can index outside its allocation. Test water continuity and cave flooding explicitly. |
| `SurfaceSystem.buildSurface`, surface rules | Evaluate against actual output terrain and selected biome; coherent material noises must follow the regional policy. Run once after density filling; remove column stamping for the new version. |
| `NoiseBasedChunkGenerator.applyCarvers` | Carver biome selection and aquifer use must agree with regional terrain. Preserve output-space carver neighborhood coverage and seeds; do not redirect neighbor chunk requests to source coordinates. Retire cave refilling/custom graph for the prototype, then assess native caves before enhancements. |
| Structure starts, placement checks, beardifier, locate, pieces | Start positions/indexes/pieces stay in output space. Eligibility and predicted height must use regional biomes/terrain. Preserve locatable goblin camps; remove their dependence on `GeyserTerrain.height` for the new version. Reassess vanilla exclusions explicitly rather than deleting structure blocks afterward. |
| Features, vegetation, crystal placement | Decorate final terrain after surface/carvers. Evaluate zone at actual placement position, including river variants; do not use chunk-center zone for all placements. Use ordinary survival/support/fluid checks. |

Block writes, chunk/status identities, lighting, saved positions, structure references, feature scheduling and cache ownership remain in output-world coordinates. The transform changes sampling, not which physical chunks get written. Cache keys must include world/dimension and descriptor version; bound caches and avoid mutable global state in generation workers. Ordinary worlds and dimensions without regional policy must retain native behavior.

A router wrapper or custom density function may cover much of the terrain, but aquifers, surface noises and height queries need separate verification. Do not assume a single injection fixes every path. Prefer the narrowest native extension that meets this contract; a custom chunk generator/codec is justified only if necessary to expose consistent settings and sampling. Do not globally replace `RandomState` coordinates indiscriminately.

## Saved-world decision

Current SavedData format is version 3. Missing/older shape markers retain legacy squares; shape 1 denotes current circles. Width squared is the historical mana budget, not the actual circle area. Shape is not a terrain-generation version.

Before the new layout ships, persist independent layout/terrain-policy versions plus the transform parameters needed to reproduce each descriptor. Preserve old descriptors and explored chunks exactly; never reinterpret shape 1 as the new terrain. New spacing and dimensions apply only to new reservations. Define a protected margin around existing generated chunks/reservations before selecting new regions. Missing version fields resolve to the old policy, not the newest. Unknown future versions must fail visibly rather than silently regenerate terrain. Migration, overlap and partially generated boundary behavior require dedicated tests in #19; this contract is not a claim those cases are implemented.

## Preserved prototype inventory

Inventory captured before starting implementation; identify stashes by message because numeric indexes change.

| Work | Status / reuse |
| --- | --- |
| `Native ring-river terrain integration pending generator redesign` | Unshipped. Candidate river resources/rules can be reviewed; do not adopt its terrain-version or routing assumptions without the new contract. |
| `Deferred custom river profile; replace with native river biome variants per ring` | Superseded custom channel profile. Keep as reference only; do not restore trench shaping or bank patches. |
| `Ancient grove prototype in progress; add locatable goblin camp structures first` | Incomplete giant-tree/grove work, deferred to #17 after terrain integration. |
| Working-tree four `geyser_river_*` JSONs, language names and biome tags | Unshipped resource-only work. Vanilla river template with clean blue water, no host land tint. Not connected to the shipped generator. Review under #13; inherit only positional crystal rules from the corresponding ring. |
| Committed camp structures (`872a8f1`) | Preserve native locate/indexing and weighted camp spawners; goblins are not added to natural spawn lists. |

No stash was applied for this investigation. Old local native fixtures/logs are historical evidence, not a reproducible clean baseline for the new generator.

## Implementation gates

1. #6 chooses descriptor sizes/spacing and versions.
2. #7 proves one mapping with fixed seed samples, seam/negative-coordinate checks and stretch bounds.
3. #8 integrates density, climate, forecasts and water consistently; #9/#25/#10 validate river, lake and coastline behavior before expanding to multiple geysers.
4. Compare the fixed baseline cases in `worldgen-baselines.md`, then inspect fresh generated chunks in game. Repeat both generation orders at chunk boundaries. Save/reload must preserve results.

No milestone is complete just because it compiles or a river biome ID appears in F3. Require restrained terrain, continuous water, sensible banks, no artificial ocean walls, and agreement between forecasts and actual terrain. Keep other regional families, global Large Biomes and the optional atlas preset outside this first milestone.
