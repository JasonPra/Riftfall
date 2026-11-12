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

The biomes use vanilla jungle trees and existing flora/fauna as temporary content. Surface elevations are generally around Y 57–92, with gentle hills, valleys, irregular lake shores and winding river channels crossing every ring. There is no raised impact-crater rim. The terrain blends back into the surrounding landscape over the field's outer 192 blocks. These stark colors are diagnostic placeholders, subject to Minecraft's biome-color blending.

## Underground tunnel network

New geyser terrain replaces vanilla underground air and aquifers below a protected surface layer. Separate winding networks repeat through the available depth at 28-block intervals, starting near bedrock and rising into higher ground; selected junctions link the levels with shafts. Routes are roughly 8–11 blocks wide, with occasional 26–40-block chambers. Sparse dripstone, lush patches, flooded chambers and stepped streams remain. A few narrow sloping entrances break the surface, spaced on a 256-block grid with a low chance and excluded from river channels.

Terrain replacement runs during raw generation before ores/decorations. It changes natural terrain in new chunks, leaving existing generated chunks and legacy fields unchanged.

## River currents

Surface rivers exert gentle physical drift on entities, including players, boats and floating items, without converting the whole river into flowing-water blocks. This applies to vanilla river biomes in existing worlds as well as new geyser river channels. Swimming or rowing can overcome the drift.

The current follows connected water toward lower outlets first. On level stretches it moves away from the broadest connected body found by a bounded water-network search. Body size is estimated by sampled water width; this is not an unlimited global watershed simulation. Loaded terrain supplies actual water elevations, with biome/landscape metadata extending the search beyond loaded chunks. Results are cached, and current calculation never generates terrain. Underground generated streams retain their ordinary fluid flow.

Operators can use `/riftfall geyser teleport` repeatedly to choose a random undiscovered field away from the current field, testing the new generation without a new world. It finds a safe dry surface arrival near the target center. `/riftfall geyser locate` still finds the nearest field without generating terrain.
