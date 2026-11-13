# Full-height mana geyser ecosystems

Geysers are persistent horizontal influence regions spanning the entire build height. There is no eruption depth, physical energy shaft or rendered mana stream. New terrain uses zone biomes, a gentle surface landscape and connected underground routes. Crystals occupy suitable underground cave surfaces.

## New field sizes and zones

| Size | Preserved mana budget | Approx. circular diameter | White core diameter | Inner ring width | Middle ring width | Outer fringe width |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Small | 129 × 129 chunk equivalents | 2,571 blocks | 336 blocks | 484 blocks | 502 blocks | 131 blocks |
| Medium | 193 × 193 chunk equivalents | 3,724 blocks | 336 blocks | 771 blocks | 791 blocks | 133 blocks |
| Large | 257 × 257 chunk equivalents | 4,879 blocks | 336 blocks | 1,059 blocks | 1,079 blocks | 134 blocks |

New fields are **circles with softly frayed edges**, using continuous world-space distance, not chunk-aligned square bands. Ring widths above measure an outward radial crossing and are nominal values before noise. Deterministic, smoothly interpolated noise at 144-, 48- and 16-block scales offsets every zone boundary: up to about 11 blocks at the core, 22 at the inner ring, and 32 at the middle/outer boundaries. Adjacent zone boundaries stay ordered. Biomes are assigned per Minecraft quart cell (4 × 4 × 4 blocks), so several ring biomes can occupy the same chunk. Core/inner/middle/outer palettes still span the full build height within their horizontal footprint.

The core was widened from the old 272-block square to a nominal 336-block-diameter circle. Each successive radius is calculated from the old zone's area plus enough room for both adjacent noisy boundaries. Even the conservative stable annulus between their maximum excursions retains at least the old zone's area. The full terrain footprint therefore expands, while shared mana capacity, regeneration and total output retain their old chunk-equivalent budgets; radial per-chunk output is normalized over the circular footprint.

**Verdant Frontier** is the outer grassland biome, retaining the saved ID `riftfall:geyser_red_outer`. Its plains-like grass/foliage greens have a slight blue tint and use Minecraft's ordinary biome-color blending. Some newly generated fields have [goblin camps](goblin-camps.md) in the Frontier; goblins spawn exclusively from camp spawners, not biome spawn pools. Camps are registered structures locatable with `/locate structure riftfall:goblin_camp`. Camp footprints must remain dry, including alongside elevated rivers and lakes.

Existing discovered descriptors retain their saved shape: pre-change modern fields and legacy fields remain square, and explored chunks are not rewritten. Newly discovered fields use the new circle. Descriptor format version 3 stores a shape marker alongside the existing center, size and output; version 1/2 saves load without changing their old footprint.

Each 512 × 512-chunk placement cell has a deterministic 50% chance of a field. Accepted candidates are 50% small, 37.5% medium and 12.5% large. Centers have a 192-chunk inset, leaving space between fields even at the largest size. The world seed and dimension determine layout; exploration order does not. Queries examine one placement cell and a bounded set of legacy index cells, without loading terrain.

## Existing worlds

Discovered legacy geysers keep their saved centers, 9/17-chunk footprints, output metadata, deposit rules, crystal-free centers, mana-pool identities and refinement records. Old 128-chunk spatial cells are indexed separately, so several legacy fields can safely coexist inside a new 512-chunk placement cell. New candidates that overlap any preserved field are excluded.

`riftfall_geyser_regions.dat` version 2 reads version-1 saves and records legacy markers when saving. It does not resize existing fields or reset deposit survey flags. New ecosystems can be discovered elsewhere in an existing world.

## Shared mana pool

Every chunk in the footprint counts toward the shared reservoir, including undiscovered chunks:

| Size | Chunks | Capacity | Mana per tick |
| --- | ---: | ---: | ---: |
| Small | 16,641 | 33,282,000 | 8,320.50 |
| Medium | 37,249 | 74,498,000 | 18,624.50 |
| Large | 66,049 | 132,098,000 | 33,024.50 |

Capacity remains `chunkCount × 2,000`; regeneration remains `chunkCount × 0.50/tick`. Included chunks draw from one pool and do not regenerate independently. The pool advances from elapsed game time without chunk tickets or terrain access. Refinement still spends once per pending upgrade and applies deferred block changes when chunks reload. Preserved fields retain their original capacities and rates.

## Starting crystal ecosystem

The core is eligible for underground deposits; an invisible stream does not exclude it. Core patches contain up to 48 connected clusters by default, roughly three quarters mature. Mature starting clusters are 80% white, 15% violet and 5% blue, with a white anchor. Their outer juvenile growth is violet/blue/cyan and never starts white. The `naturalWhiteCrystals` switch disables initial white generation when desired. Other zones retain the 24-cluster default, with zone-specific lower-tier distributions.

Core density is configurable with `coreInitialDepositSize`. Deposits are seeded once and never carve caves. Recovery after complete mining still nucleates red seedlings, and ordinary mother spreading still follows the lower-tier offspring rules. See [mana-crystals.md](mana-crystals.md).

## Diagnostics

- `/riftfall geyser locate` finds the nearest saved or new candidate without generating terrain; output identifies legacy and ecosystem fields.
- `/riftfall geyser teleport` selects a random undiscovered field away from the current field and brings the executing player above its center’s canopy or water surface. This development command does not require a safe or dry landing.
- `/riftfall geyser inspect` reports the footprint, zone, cave eligibility, shared capacity and regeneration.
- `/riftfall ambient inspect` reports the actual reservoir balance and mana per second.

Changing altitude does not change membership. Crossing the footprint boundary returns to ordinary independent chunk mana.

## Ring biomes and surface terrain

| Zone | Biome identifier | Temporary ecology | Test grass/foliage color |
| --- | --- | --- | --- |
| Manaheart Grove (core) | `riftfall:geyser_white_core` | Dense jungle foliage; some fields have a sheltered lake basin | White |
| Ancient Wilds (inner) | `riftfall:geyser_blue_inner` | Dense jungle canopy and occasional larger lakes | Blue |
| Elderwood Reach (middle) | `riftfall:geyser_gold_middle` | More open jungle woodland, lakes and connected streams | Yellow |
| Verdant Frontier | `riftfall:geyser_red_outer` | Plains-like fringe with a rare jungle tree | Subtly blue-tinged plains green |

These display names describe the planned ecosystems: a mana-rich core grove, an ancient inner forest, and more open woodland toward the Frontier. Saved biome identifiers remain unchanged. The names do not imply that the future trees, flora or terrain are already implemented.

The land biomes use vanilla jungle trees and existing flora/fauna as temporary content. Waterways now share one deterministic drainage plan. A high spring-fed pool starts the surface course, which follows lower passes toward a receiving lake, a rim outlet, or a swallow hole. The plan uses a 16-block priority-flood grid: depressions fill to the lowest pass leading downstream, and the channel continues through that spillway. Natural boundary drains remain available, so the geyser boundary cannot act as an invisible dam. Closed receiving lakes are selected from interior terrain minima. River stages never increase along the directed route. It can turn sideways and backtrack around terrain rather than remaining a single function of north/south position.

Lakes flood the connected low terrain of a depression instead of excavating a clipped ellipse. Their submerged ground remains the bed. Spring and receiving pools have broad shallow shelves that taper over 32 blocks into smoothed surrounding ground, with small irregularities retained. River channels have a 3–6 block center half-width plus a short shallow margin (normally about 9–15 wetted blocks across). The following 24-block bank apron is dry terrain grading, not additional submerged channel width. Larger lakes retain their separate basin footprints. Sediment uses interpolated two-dimensional noise instead of visible 7-by-7 material tiles.

Eligible permeable fields can divert their course into a receiving pool and swallow shaft. This ends the surface route and connects a falling-water column to actual upper and lower cave nodes. A bounded search connects to an existing flooded cave where available; otherwise the reach joins the lower connected cave passages. The water seal explicitly preserves this planned opening; unrelated holes remain sealed so a river cannot simply leak out through arbitrary terrain damage. This applies to newly generated terrain and does not retrofit openings in existing chunks.

This remains a generation-time drainage approximation over the current custom terrain, not a tick-by-tick erosion simulation or the native regional-generation overhaul tracked by #4. The grid can resolve regional passes and depressions, not every one-block obstacle. Player-built dams do not rebuild the world-generation plan.

Each zone has separate river and lake biome identities:

| Zone | River | Large lake |
| --- | --- | --- |
| Manaheart Grove | `riftfall:geyser_river_white_core` | `riftfall:geyser_lake_white_core` |
| Ancient Wilds | `riftfall:geyser_river_blue_inner` | `riftfall:geyser_lake_blue_inner` |
| Elderwood Reach | `riftfall:geyser_river_gold_middle` | `riftfall:geyser_lake_gold_middle` |
| Verdant Frontier | `riftfall:geyser_river_red_outer` | `riftfall:geyser_lake_red_outer` |

These vanilla-river-derived aquatic biomes use clean blue water, normal vegetation tints, seagrass and aquatic spawns. They do not inherit the land biomes' diagnostic grass/foliage colors, trees or terrestrial plant patches. Underground channel quart cells also receive the corresponding ring's river biome. Crystal tier rules follow the actual position's mana zone, including underneath rivers and lakes; crossing a water biome does not reset crystal tiers. They remain subject to the existing underground placement, support and mana rules. Biome assignment samples four-block columns, so an individual shoreline block may share the neighboring biome's tint.

The core, inner and middle land colors remain diagnostic placeholders. The existing 384-block outer transition belt remains until the native overhaul replaces it. Water sealing now stores each column's water level between SURFACE and CARVERS; old 256-entry in-progress plans retain their original sea-level meaning. Existing generated chunks are not rewritten. Test this revision in fresh terrain; borders against older generated terrain can still show generation-version seams.


## Vanilla transition belt and native river outlets

Each ecosystem field has a landscape-only buffer extending up to 384 blocks outside its mana footprint. It is kept inside the field's placement cell and excluded from preserved legacy fields. It grants no shared mana, crystal deposits or custom cave network to those outside chunks.

The BIOMES stage selects actual vanilla biome IDs at quart resolution. Inland transitions use plains followed by forest, sparse jungle, taiga, savanna or meadow according to the original surroundings. Ocean-facing transitions use land, beach (snowy where appropriate), shallow ocean and the matching ocean/deep-ocean family. Warm shallows can use `minecraft:warm_ocean`, including its normal reef decoration. Farther out, coherent patches give way to the original biome palette; below Y 32 the belt retains the original cave biomes. All transition biomes participate in feature sorting and ordinary decoration.

Surface profiles grade smoothly into the original ground and seabed. Beaches receive sand, and the outermost terrain retains its original top material. Ocean water uses the generator's native sea level (highest source-water block normally Y 62). Elevated river connections descend through a cached terrain-aware grade into that waterline; downstream ridges cannot raise their water level again. Structure exclusion and generation-only plant checks include the reshaped belt, while custom cave excavation remains inside the mana field.

For the chosen open rim outlet (in any compass direction), generation samples the original biome source and base-height functions beyond the buffer, searching nearby for an existing wet vanilla river or ocean. When one is available, a continuous connector shifts the channel toward that outlet and assigns river biomes along the inland section. It never loads surrounding chunks just to locate an outlet. If no wet native outlet is found within the bounded scan, the generator does not invent a river through arbitrary dry terrain beyond the buffer.

Minecraft 1.21.1 does not provide a standalone river-network API. The field interior still uses its custom lake/river profile; the buffer now uses native biome/terrain information to connect it to the surrounding generation. Existing already-generated borders are not rewritten.

## Underground tunnel network

New geyser terrain replaces vanilla underground air and aquifers below a protected surface layer. A jittered three-dimensional cave graph replaces the old repeated floor plans and fixed vertical intervals. Junction positions vary horizontally and in height; curved connections change width, ceiling height and grade along their length. Optional diagonal branches and dead-end fractures break up the former orthogonal layout.

Caves include narrow tall crevices, low passages, winding tunnels, occasional irregular chambers and steep drops. Most chambers are modest; rare wider ones approach 60 blocks across. A deep water table floods low passages, local pools occur above it, and selected shafts can be flooded all the way to their upper connection. Other shafts remain dry or partly flooded. Sparse dripstone and lush patches remain. Rare narrow entrances start on actual upper cave junctions and slope to dry surface ground.
Surface shaping now runs in the SURFACE generation stage; network excavation runs in CARVERS. Minecraft requires neighboring chunks to finish those stages before FEATURES, so later terrain edits cannot remove the ground underneath neighboring vegetation. Decoration no longer reshapes terrain. These operations apply to new chunks; existing generated chunks and legacy fields remain unchanged.

SURFACE records a compact authoritative floor/water plan on the unfinished chunk. After vanilla and custom carving, CARVERS restores each planned water column and its sediment seal, then removes the temporary attachment. This prevents carving from leaving an air pocket inside a river or lake and remains correct if an unfinished SURFACE chunk is saved before carving resumes. Cave roof limits also use that original plan, rather than a temporarily damaged heightmap.

Structure starts are explicitly suppressed inside ecosystem fields, and structure-piece placement cannot extend into the fields from an outside start. This is independent of the original terrain biome used for vanilla structure eligibility. Ordinary explicit operator structure placement remains available.

Underwater beds have several layers of coherent gravel, sand, clay, mud and coarse-dirt patches, with subtle depth relief. Seagrass grows sparsely where water depth and support permit it. Generation rejects unsupported plants and terrestrial plants attempting to replace water, including melons and pumpkins. Additional jungle-tree attempts require a dry, supported root position.

## River currents

Surface rivers exert gentle physical drift on entities, including players, boats and floating items, without converting the entire river into flowing-water blocks. Geyser rivers use the same directed route that generated the terrain; lake currents follow the drainage tree toward its spillway, and underground reaches follow their descending passage. Their direction no longer comes from choosing whichever broad water body happens to be detected nearby. The current checks ahead and stops at dry land or higher water, including in older generated chunks or at player-made obstructions. Swimming or rowing can overcome the drift. Ordinary vanilla river biomes retain the existing connected-water current solver; this change does not invent a global watershed for vanilla terrain.


The current follows connected water toward lower outlets first. On level stretches it moves away from the broadest connected body found by a bounded water-network search. Body size is estimated by sampled water width; this is not an unlimited global watershed simulation. Loaded terrain supplies actual water elevations, with biome/landscape metadata extending the search beyond loaded chunks. Results are cached, and current calculation never generates terrain. Underground generated streams retain their ordinary fluid flow.

Operators can use `/riftfall geyser teleport` repeatedly to choose a random undiscovered field away from the current field, testing the new generation without a new world. It loads the center chunk and arrives two blocks above its highest surface, including trees or water. Flooded, hazardous or obstructed ground does not reject a field; the player’s game mode is preserved. `/riftfall geyser locate` still finds the nearest field without generating terrain.

## Reproducible checks and limits

These commands require operator permission. Use `/riftfall geyser teleport` repeatedly for fresh fields in one save; `/riftfall geyser locate` deliberately continues to report the nearest field. The biome identifiers above are actual saved palettes visible in biome diagnostics, and their grass/foliage tints are intentionally stark placeholders. Main biome profiles currently reuse vanilla content, not custom species.

The generation order is `STRUCTURE_STARTS` (field exclusion), `BIOMES` (zone assignment), `SURFACE` (land/water/sediment), `CARVERS` (network), then `FEATURES` (vegetation and supported seagrass). Field biomes also participate in the generator's possible-biome set and feature sorting, so their configured decorations actually execute. Outer starts cannot place structure pieces into a field; explicit operator structure placement is not prohibited.

Native regression checks generated neighboring chunks in a mixed order and checked the same layout that previously produced unsupported plants. They confirmed that shaping/carving finish before decoration, with zero automatic structure starts and invalid plants in the sampled chunks, varied sediment and seagrass. Existing explored terrain is not repaired or regenerated by these changes. Previously buried structures or floating plants remain in those saved chunks.

## Directed drainage regression check

The opt-in [DrainageVerification](../tools/worldgen/DrainageVerification.java) probe runs on a disposable native development server with seed `923475`, normal Overworld settings. Follow the opt-in probe procedure in [worldgen-baselines.md](worldgen-baselines.md), substituting this class, and remove the main-source copy before a clean release build. It generates actual surface and underground chunks and stops the server; never run it against a valued save.

Automated checks exercise a ridge with a lower side pass, depression filling to a spillway, acyclic downhill routing, actual water-level monotonicity, graded lake shores, and the receiving pool's open descending shaft. Native runtime checks cover continuous surface water, matching downstream current vectors, an unplugged swallow shaft, wet connected cave reaches and full-chunk decoration. Visual review of newly generated shores remains useful; existing explored chunks are not rewritten.

The current seed-923475 native run sampled surface stages Y90→74→74→68, then a lateral/radial native-water connection Y68→66→63→62. The receiving shaft at `(3378952, 37656)` remained open through sealing, feeding wet cave reaches at Y26, Y−19 and Y−44; their ring-specific underground river biome assignments passed as well. A full sinkhole chunk completed decoration. These checks validate the generated connections; they do not simulate long-term erosion or replace visual review.
