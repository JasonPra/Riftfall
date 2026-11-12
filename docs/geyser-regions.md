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

**Verdant Frontier** is the outer grassland biome, retaining the saved ID `riftfall:geyser_red_outer`. Its plains-like grass/foliage greens have a slight blue tint and use Minecraft's ordinary biome-color blending. Some newly generated fields have [goblin camps](goblin-camps.md) in the Frontier; goblins spawn exclusively from camp spawners, not biome spawn pools.

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
| Core | `riftfall:geyser_white_core` | Dense jungle foliage; some fields have a sheltered lake basin | White |
| Inner | `riftfall:geyser_blue_inner` | Dense jungle canopy and occasional larger lakes | Blue |
| Middle | `riftfall:geyser_gold_middle` | More open jungle woodland, lakes and connected streams | Yellow |
| Verdant Frontier | `riftfall:geyser_red_outer` | Plains-like fringe with a rare jungle tree | Subtly blue-tinged plains green |

The biomes use vanilla jungle trees and existing flora/fauna as temporary content. Surface elevations are generally around Y 49–92, with gentle hills, valleys, irregular lake shores and a winding river route crossing every ring. River bends and widths vary with deterministic smooth noise. The old four-way crossing is replaced by a broad irregular junction lake; its continuous river has one inlet and one outlet. Other basins can be isolated or meet that same route, rather than adding crossing channels. There is no raised impact-crater rim. The field's outer fringe now joins a separate 384-block vanilla-biome transition belt; the terrain grade extends outside the mana footprint rather than turning back into the original ocean inside the red fringe. The core, inner and middle ring colors remain diagnostic placeholders; Verdant Frontier now uses plains-like greens. All are subject to Minecraft's biome-color blending.

## Vanilla transition belt and native river outlets

Each ecosystem field has a landscape-only buffer extending up to 384 blocks outside its mana footprint. It is kept inside the field's placement cell and excluded from preserved legacy fields. It grants no shared mana, crystal deposits or custom cave network to those outside chunks.

The BIOMES stage selects actual vanilla biome IDs at quart resolution. Inland transitions use plains followed by forest, sparse jungle, taiga, savanna or meadow according to the original surroundings. Ocean-facing transitions use land, beach (snowy where appropriate), shallow ocean and the matching ocean/deep-ocean family. Warm shallows can use `minecraft:warm_ocean`, including its normal reef decoration. Farther out, coherent patches give way to the original biome palette; below Y 32 the belt retains the original cave biomes. All transition biomes participate in feature sorting and ordinary decoration.

Surface profiles grade smoothly into the original ground and seabed. Beaches receive sand, and the outermost terrain retains its original top material. Water uses the generator's native sea level (highest source-water block normally Y 62), matching the surrounding ocean instead of creating a raised water lip. Structure exclusion and generation-only plant checks include the reshaped belt, while custom cave excavation remains inside the mana field.

For each north/south river exit, generation samples the original biome source and base-height functions beyond the buffer, searching nearby for an existing wet vanilla river or ocean. When one is available, a continuous connector shifts the channel toward that outlet and assigns river biomes along the inland section. It never loads surrounding chunks just to locate an outlet. If no wet native outlet is found within the bounded scan, the generator does not invent a river through arbitrary dry terrain beyond the buffer.

Minecraft 1.21.1 does not provide a standalone river-network API. The field interior still uses its custom lake/river profile; the buffer now uses native biome/terrain information to connect it to the surrounding generation. Existing already-generated borders are not rewritten.

## Underground tunnel network

New geyser terrain replaces vanilla underground air and aquifers below a protected surface layer. A jittered three-dimensional cave graph replaces the old repeated floor plans and fixed vertical intervals. Junction positions vary horizontally and in height; curved connections change width, ceiling height and grade along their length. Optional diagonal branches and dead-end fractures break up the former orthogonal layout.

Caves include narrow tall crevices, low passages, winding tunnels, occasional irregular chambers and steep drops. Most chambers are modest; rare wider ones approach 60 blocks across. A deep water table floods low passages, local pools occur above it, and selected shafts can be flooded all the way to their upper connection. Other shafts remain dry or partly flooded. Sparse dripstone and lush patches remain. Rare narrow entrances start on actual upper cave junctions and slope to dry surface ground.
Surface shaping now runs in the SURFACE generation stage; network excavation runs in CARVERS. Minecraft requires neighboring chunks to finish those stages before FEATURES, so later terrain edits cannot remove the ground underneath neighboring vegetation. Decoration no longer reshapes terrain. These operations apply to new chunks; existing generated chunks and legacy fields remain unchanged.

SURFACE records a compact authoritative floor/water plan on the unfinished chunk. After vanilla and custom carving, CARVERS restores each planned water column and its sediment seal, then removes the temporary attachment. This prevents carving from leaving an air pocket inside a river or lake and remains correct if an unfinished SURFACE chunk is saved before carving resumes. Cave roof limits also use that original plan, rather than a temporarily damaged heightmap.

Structure starts are explicitly suppressed inside ecosystem fields, and structure-piece placement cannot extend into the fields from an outside start. This is independent of the original terrain biome used for vanilla structure eligibility. Ordinary explicit operator structure placement remains available.

Underwater beds have several layers of coherent gravel, sand, clay, mud and coarse-dirt patches, with subtle depth relief. Seagrass grows sparsely where water depth and support permit it. Generation rejects unsupported plants and terrestrial plants attempting to replace water, including melons and pumpkins. Additional jungle-tree attempts require a dry, supported root position.

## River currents

Surface rivers exert gentle physical drift on entities, including players, boats and floating items, without converting the whole river into flowing-water blocks. This applies to vanilla river biomes in existing worlds as well as new geyser river channels. Swimming or rowing can overcome the drift.

The current follows connected water toward lower outlets first. On level stretches it moves away from the broadest connected body found by a bounded water-network search. Body size is estimated by sampled water width; this is not an unlimited global watershed simulation. Loaded terrain supplies actual water elevations, with biome/landscape metadata extending the search beyond loaded chunks. Results are cached, and current calculation never generates terrain. Underground generated streams retain their ordinary fluid flow.

Operators can use `/riftfall geyser teleport` repeatedly to choose a random undiscovered field away from the current field, testing the new generation without a new world. It loads the center chunk and arrives two blocks above its highest surface, including trees or water. Flooded, hazardous or obstructed ground does not reject a field; the player’s game mode is preserved. `/riftfall geyser locate` still finds the nearest field without generating terrain.

## Reproducible checks and limits

These commands require operator permission. Use `/riftfall geyser teleport` repeatedly for fresh fields in one save; `/riftfall geyser locate` deliberately continues to report the nearest field. The biome identifiers above are actual saved palettes visible in biome diagnostics, and their grass/foliage tints are intentionally stark placeholders. Main biome profiles currently reuse vanilla content, not custom species.

The generation order is `STRUCTURE_STARTS` (field exclusion), `BIOMES` (zone assignment), `SURFACE` (land/water/sediment), `CARVERS` (network), then `FEATURES` (vegetation and supported seagrass). Field biomes also participate in the generator's possible-biome set and feature sorting, so their configured decorations actually execute. Outer starts cannot place structure pieces into a field; explicit operator structure placement is not prohibited.

Native regression checks generated neighboring chunks in a mixed order and checked the same layout that previously produced unsupported plants. They confirmed that shaping/carving finish before decoration, with zero automatic structure starts and invalid plants in the sampled chunks, varied sediment and seagrass. Existing explored terrain is not repaired or regenerated by these changes. Previously buried structures or floating plants remain in those saved chunks.
