# Full-height mana geyser regions

This implementation on `prototyping` starts a new geyser design. Each geyser is one persistent horizontal region spanning the world's entire build height. It has no eruption depth, origin Y, bottom layer or depth-based multiplier. Existing terrain is not carved or replaced.

## Starting sizes and placement

- Small: 9 × 9 chunks, 81 chunks, 144 × 144 blocks.
- Large: 17 × 17 chunks, 289 chunks, 272 × 272 blocks.

Odd widths provide a true center chunk. Each 128 × 128-chunk placement cell has a deterministic 50% chance to contain a geyser. Accepted fields have a 75% small / 25% large size split. Centers are inset from cell boundaries, so footprints cannot overlap. These are prototype balance defaults in `GeyserLayout`, not claims about novel canon.

The seed includes the world seed and dimension ID. Discovery order does not affect placement. One cell lookup identifies a chunk's field; no neighboring chunks are loaded. Entering any part of the footprint records the whole geyser descriptor. Discovered centers, sizes and output budgets are saved per dimension in `data/riftfall_geyser_regions.dat`.

## Shared mana pool

The reservoir covers **every chunk in the footprint**, not just visited chunks:

| Size | Chunks | Capacity | Mana/tick | Mana/minute |
| --- | ---: | ---: | ---: | ---: |
| Small | 81 | 162,000 | 40.50 | 24,300 |
| Large | 289 | 578,000 | 144.50 | 86,700 |

Capacity is `chunkCount × 2,000`; generation is `chunkCount × 0.50/tick`. Geyser chunks draw directly from this pool and do not regenerate independently. No output is duplicated per loaded chunk. Full pools discard excess generation.

`GeyserLedger` saves one balance, capacity, collective regeneration rate and last simulation tick per discovered geyser in `riftfall_geyser_mana.dat`. It advances using elapsed server game time, including time with no geyser terrain loaded. No chunk tickets or terrain access are needed. Crystal refinement spends from this authoritative balance even when physical upgrades are deferred. It never charges twice on reload.

The original normalized center-to-edge profile remains a geological concentration profile used for initial deposit quality. It no longer allocates independent chunk mana or controls regeneration. Crystal generation and refinement do not depend on depth.

## Cave crystals

Initial deposits occur only on natural stone in underground caves in the stable surrounding zone. The central stream is excluded. Terrain is not carved. See [mana-crystals.md](mana-crystals.md) for tiers, seeding, cutting and refinement.

## Testing in game

- `/riftfall geyser locate` finds a candidate without generating terrain.
- `/riftfall geyser inspect` shows footprint, shared capacity, collective mana/tick and crystal-zone membership.
- `/riftfall ambient inspect` shows the actual reservoir balance and mana/second.

Every chunk inside the same geyser reports the same pool. Changing altitude does not change membership. Crossing its footprint boundary returns to an ordinary independent chunk reservoir.
