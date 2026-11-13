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
| Manaheart Grove (core) | `riftfall:geyser_white_core` | Naturally generated Vermillion Legacy groves | White grass, lush green foliage |
| Ancient Wilds (inner) | `riftfall:geyser_blue_inner` | Dense dynamic jungle canopy | Blue |
| Elderwood Reach (middle) | `riftfall:geyser_gold_middle` | More open dynamic jungle woodland | Yellow |
| Verdant Frontier | `riftfall:geyser_red_outer` | Plains-like fringe with rare dynamic jungle trees | Subtly blue-tinged plains green |

These display names describe the planned ecosystems: a mana-rich core grove, an ancient inner forest, and more open woodland toward the Frontier. Saved biome identifiers remain unchanged. The names do not imply that the future trees, flora or terrain are already implemented.

Manaheart Grove's old jungle trees and surface plant patches are removed; its grass blocks, underground features, fauna and crystal rules remain. Bamboo, vines, melons, pumpkins, grass tufts, flowers, cane and surface mushrooms are removed from its biome decoration. Its foliage palette is lush green (`#63B36B`), matching the [Vermillion Legacy tree](legacy-tree.md), which now generates through Dynamic Trees' normal biome-decoration feature. The other rings retain their existing colors and temporary jungle ecology, with Dynamic Trees replacing the vanilla tree features. There are no direct jungle-tree placement calls. Custom river and lake generation has been removed: there is no drainage planner, channel carving, spring/receiving pool generation, river outlet connector, or routed underground river. Newly generated fields assign the four land biomes only.

The registered `riftfall:mana_geysers` biome source wraps the world's original source. All ring and transition selection uses this source during ordinary chunk biome generation, uncached queries and biome searches; the former post-BIOMES palette rewrite is removed. Explicit [Dynamic Trees rules](dynamic-trees.md) cover all four land biomes and the retained aquatic IDs. Existing generated trees and palettes are not retroactively rewritten.

The existing land elevation formula and outer transition grading are unchanged. Mountain generation is on hold; no inner-ring uplift, peaks or elevation redesign is included in this removal. Removing the waterway cuts means new terrain uses the existing underlying land height in their former footprints. Already generated terrain is not rewritten.

The eight `geyser_river_*` and `geyser_lake_*` biome definitions and their display names remain registered solely for saved-world compatibility. They are not selected for new terrain. Saved region membership and crystal tier rules remain unchanged.

## Vanilla transition belt

Each ecosystem field has a landscape-only buffer extending up to 384 blocks outside its mana footprint. It is kept inside the field's placement cell and excluded from preserved legacy fields. It grants no shared mana, crystal deposits or custom cave network to those outside chunks.

The BIOMES stage selects actual vanilla biome IDs at quart resolution. Inland transitions use plains followed by forest, sparse jungle, taiga, savanna or meadow according to the original surroundings. Ocean-facing transitions use land, beach (snowy where appropriate), shallow ocean and the matching ocean/deep-ocean family. Warm shallows can use `minecraft:warm_ocean`, including its normal reef decoration. Farther out, coherent patches give way to the original biome palette; below Y 32 the belt retains the original cave biomes. All transition biomes participate in feature sorting and ordinary decoration.

Surface profiles grade smoothly into the original ground and seabed. Beaches receive sand, and the outermost terrain retains its original top material. Ocean water uses the generator's native sea level (highest source-water block normally Y 62). Structure exclusion and generation-only plant checks include the reshaped belt, while custom cave excavation remains inside the mana field.

The coast sampler now only reads the original biome source for the existing outer biome transition. It does not search for river outlets, carve connector channels or assign connector river biomes. Vanilla generation outside the geyser transition remains unchanged.

## Underground tunnel network

New geyser terrain replaces vanilla underground air and aquifers below a protected surface layer. A jittered three-dimensional cave graph replaces the old repeated floor plans and fixed vertical intervals. Junction positions vary horizontally and in height; curved connections change width, ceiling height and grade along their length. Optional diagonal branches and dead-end fractures break up the former orthogonal layout.

Caves include narrow tall crevices, low passages, winding tunnels, occasional irregular chambers and steep drops. Most chambers are modest; rare wider ones approach 60 blocks across. A deep water table floods low passages, local pools occur above it, and selected shafts can be flooded all the way to their upper connection. Other shafts remain dry or partly flooded. Sparse dripstone and lush patches remain. Rare narrow entrances start on actual upper cave junctions and slope to dry surface ground.
Surface shaping now runs in the SURFACE generation stage; network excavation runs in CARVERS. Minecraft requires neighboring chunks to finish those stages before FEATURES, so later terrain edits cannot remove the ground underneath neighboring vegetation. Decoration no longer reshapes terrain. These operations apply to new chunks; existing generated chunks and legacy fields remain unchanged.

SURFACE records a compact floor plan for cave roof limits and flags ocean columns in the outer transition. After carving, CARVERS finishes those ocean columns and releases the plan. Historical 512-entry in-progress plans remain readable to allow saved unfinished chunks to finish; new plans use 256 entries. There is no inland lake, river or sinkhole water-sealing pass.

Structure starts are explicitly suppressed inside ecosystem fields, and structure-piece placement cannot extend into the fields from an outside start. This is independent of the original terrain biome used for vanilla structure eligibility. Ordinary explicit operator structure placement remains available.

Generation still rejects unsupported plants and terrestrial plants attempting to replace water. Additional jungle-tree attempts require a dry, supported root position.

## River currents

The pre-existing connected-water drift for river-tagged biomes remains. Custom geyser drainage vectors and underground-river current handling have been removed. The generic solver uses loaded water elevations and native biome metadata without generating chunks. Ordinary cave fluids retain Minecraft's fluid behavior.

Operators can use `/riftfall geyser teleport` repeatedly to choose a random undiscovered field away from the current field, testing the new generation without a new world. It loads the center chunk and arrives two blocks above its highest surface, including trees or water. Flooded, hazardous or obstructed ground does not reject a field; the player’s game mode is preserved. `/riftfall geyser locate` still finds the nearest field without generating terrain.

## Reproducible checks and limits

These commands require operator permission. Use `/riftfall geyser teleport` repeatedly for fresh fields in one save; `/riftfall geyser locate` deliberately continues to report the nearest field. The biome identifiers above are actual saved palettes visible in biome diagnostics, and their grass/foliage tints are intentionally stark placeholders. Main biome profiles currently reuse vanilla content, not custom species.

The generation order is `STRUCTURE_STARTS` (field exclusion), `BIOMES` (zone assignment), `SURFACE` (existing land profile and outer coast), `CARVERS` (network), then `FEATURES` (vegetation). Field biomes also participate in the generator's possible-biome set and feature sorting, so their configured decorations actually execute. Outer starts cannot place structure pieces into a field; explicit operator structure placement is not prohibited.

Earlier waterway revisions and their screenshots describe historical generated chunks. Existing explored terrain is not repaired or regenerated by removing that generation code. Previously generated lakes, rivers and aquatic biome palettes remain in those saved chunks.

## Removal verification

Regression checks preserve the previous underlying land-height formula at positive and negative coordinates, retain all four land biome keys, and keep historical aquatic registry keys available. A disposable native-world check verifies land biome assignment, dry surface columns and full-chunk decoration without the removed generator classes. Use fresh terrain to inspect the removal.
