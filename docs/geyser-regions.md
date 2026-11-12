# Full-height mana geyser ecosystems

Geysers are persistent horizontal influence regions spanning the entire build height. There is no eruption depth, physical energy shaft or rendered mana stream. New terrain uses zone biomes, a gentle surface landscape and connected underground routes. Crystals occupy suitable underground cave surfaces.

## New field sizes and zones

| Size | Footprint | Blocks across | White core across | Inner ring width | Middle ring width | Outer fringe width |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Small | 129 × 129 chunks | 2,064 | 272 | 416 | 416 | 64 |
| Medium | 193 × 193 chunks | 3,088 | 272 | 672 | 672 | 64 |
| Large | 257 × 257 chunks | 4,112 | 272 | 928 | 928 | 64 |

Ring widths measure a one-way crossing outward from one zone to the next. Zones are currently chunk-aligned square bands, using `GeyserRegion.zone(chunkX, chunkZ)`: `CORE`, `INNER`, `MIDDLE`, `OUTER`, and `OUTSIDE`. The compact core has an eight-chunk radius; the outer fringe is four chunks wide. The remaining radius is divided between the two biome-scale rings. These labels select real full-height biome palettes during the BIOMES generation step, before terrain surfacing and decoration. Existing chunks and preserved legacy fields retain their biomes.

Each 512 × 512-chunk placement cell has a deterministic 50% chance of a field. Accepted candidates are 50% small, 37.5% medium and 12.5% large. Centers have a 160-chunk inset, leaving space between fields even at the largest size. The world seed and dimension determine layout; exploration order does not. Queries examine one placement cell and a bounded set of legacy index cells, without loading terrain.

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
- `/riftfall geyser teleport` selects a random undiscovered field away from the current field and generates a safe dry surface arrival for the executing player.
- `/riftfall geyser inspect` reports the footprint, zone, cave eligibility, shared capacity and regeneration.
- `/riftfall ambient inspect` reports the actual reservoir balance and mana per second.

Changing altitude does not change membership. Crossing the footprint boundary returns to ordinary independent chunk mana.

## Ring biomes and surface terrain

| Zone | Biome identifier | Temporary ecology | Test grass/foliage color |
| --- | --- | --- | --- |
| Core | `riftfall:geyser_white_core` | Dense jungle foliage; some fields have a sheltered lake basin | White |
| Inner | `riftfall:geyser_blue_inner` | Dense jungle canopy and occasional larger lakes | Blue |
| Middle | `riftfall:geyser_gold_middle` | More open jungle woodland, lakes and connected streams | Yellow |
| Outer | `riftfall:geyser_red_outer` | Plains-like fringe with a rare jungle tree | Red |

The biomes use vanilla jungle trees and existing flora/fauna as temporary content. Surface elevations are generally around Y 49–92, with gentle hills, valleys, irregular lake shores and a winding river route crossing every ring. River bends and widths vary with deterministic smooth noise. The old four-way crossing is replaced by a broad irregular junction lake; its continuous river has one inlet and one outlet. Other basins can be isolated or meet that same route, rather than adding crossing channels. There is no raised impact-crater rim. The terrain blends back into the surrounding landscape over the field's outer 192 blocks. These stark colors are diagnostic placeholders, subject to Minecraft's biome-color blending.

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

Operators can use `/riftfall geyser teleport` repeatedly to choose a random undiscovered field away from the current field, testing the new generation without a new world. It finds a safe dry surface arrival near the target center. `/riftfall geyser locate` still finds the nearest field without generating terrain.

## Reproducible checks and limits

These commands require operator permission. Use `/riftfall geyser teleport` repeatedly for fresh fields in one save; `/riftfall geyser locate` deliberately continues to report the nearest field. The biome identifiers above are actual saved palettes visible in biome diagnostics, and their grass/foliage tints are intentionally stark placeholders. Main biome profiles currently reuse vanilla content, not custom species.

The generation order is `STRUCTURE_STARTS` (field exclusion), `BIOMES` (zone assignment), `SURFACE` (land/water/sediment), `CARVERS` (network), then `FEATURES` (vegetation and supported seagrass). Field biomes also participate in the generator's possible-biome set and feature sorting, so their configured decorations actually execute. Outer starts cannot place structure pieces into a field; explicit operator structure placement is not prohibited.

Native regression checks generated neighboring chunks in a mixed order and checked the same layout that previously produced unsupported plants. They confirmed that shaping/carving finish before decoration, with zero automatic structure starts and invalid plants in the sampled chunks, varied sediment and seagrass. Existing explored terrain is not repaired or regenerated by these changes. Previously buried structures or floating plants remain in those saved chunks.
