# Full-height mana geyser ecosystems

Geysers are persistent horizontal influence regions spanning the entire build height. There is no eruption depth, physical energy shaft, terrain carving or rendered mana stream. Crystals occupy suitable underground cave surfaces. Zone labels are available for future biome sets; this change does not register new biomes.

## New field sizes and zones

| Size | Footprint | Blocks across | White core across | Inner ring width | Middle ring width | Outer fringe width |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Small | 129 × 129 chunks | 2,064 | 272 | 416 | 416 | 64 |
| Medium | 193 × 193 chunks | 3,088 | 272 | 672 | 672 | 64 |
| Large | 257 × 257 chunks | 4,112 | 272 | 928 | 928 | 64 |

Ring widths measure a one-way crossing outward from one zone to the next. Zones are currently chunk-aligned square bands, using `GeyserRegion.zone(chunkX, chunkZ)`: `CORE`, `INNER`, `MIDDLE`, `OUTER`, and `OUTSIDE`. The compact core has an eight-chunk radius; the outer fringe is four chunks wide. The remaining radius is divided between the two biome-scale rings. These labels are full-height data and can later select separate biome sets without changing the mana ledger.

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

## Underground tunnel network

New Overworld geyser terrain replaces vanilla underground air and aquifers within the generation band (from Y -59 up to 24 blocks below the surface) with a smaller connected network. Replacement runs during raw generation before ores/decorations and only changes natural stone, air and fluids in the current chunk. Existing generated chunks and legacy fields remain unchanged.

Snaking routes cross every ring, roughly 8–11 blocks wide. Only about a quarter of the 128-block-spaced junctions open into chambers, approximately 26–40 blocks wide and 16–24 blocks high. Rare junctions have deeper drop shafts. Some chambers flood; others have short, stepped underground streams with upstream sources and scheduled water flow. Sparse floor/ceiling dripstone alternates with occasional moss, azalea, carpet and glow-berry sections. No mana-stream shaft is rendered.

Operators can use `/riftfall geyser teleport` repeatedly to choose a random undiscovered field away from the current field, making it convenient to test new world generation in one save. It finds a safe dry surface arrival near the target center and does not build a platform. `/riftfall geyser locate` still finds the nearest field without generating terrain.
