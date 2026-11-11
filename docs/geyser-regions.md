# Full-height mana geyser regions

This implementation on `prototyping` starts a new geyser design. Each geyser is one persistent horizontal region spanning the world's entire build height. It has no eruption depth, origin Y, bottom layer or depth-based multiplier. Existing terrain is not carved or replaced.

## Starting sizes and placement

- Small: 9 × 9 chunks, 81 chunks, 144 × 144 blocks.
- Large: 17 × 17 chunks, 289 chunks, 272 × 272 blocks.

Odd widths provide a true center chunk. Each 128 × 128-chunk placement cell has a deterministic 50% chance to contain a geyser. Accepted fields have a 75% small / 25% large size split. Centers are inset from cell boundaries, so footprints cannot overlap. These are prototype balance defaults in `GeyserLayout`, not claims about novel canon.

The seed includes the world seed and dimension ID. Discovery order does not affect placement. One cell lookup identifies a chunk's field; no neighboring chunks are loaded. Entering any part of the footprint records the whole geyser descriptor. Discovered centers, sizes and output budgets are saved per dimension in `data/riftfall_geyser_regions.dat`.

## Output and chunk storage

The starting **extra** output budgets are 405 mana/second for a small field and 2,890 for a large field. Across the footprint these average +5 and +10 mana/second per chunk respectively. Larger fields therefore provide both more total energy and a higher local concentration.

These budgets are distributed using normalized, separable tent weights, with the highest concentration at the center. Adding up all the chunk contributions equals one geyser's total output; the entire output is never copied into each chunk. The ordinary 0.50/second ambient rate remains additive. Individual chunk reservoirs still cap at 2,000 mana.

Only loaded chunks receive their own share. Unloaded shares are not rerouted into loaded chunks, do not accumulate offline, and do not force terrain to load. A full reservoir stops accepting mana as before.

## Cave-only crystal eligibility

`GeyserQueries.at(level, pos)` identifies the same full-height field at every Y. `GeyserQueries.crystalSurface(level, pos)` is stricter: the location must be a loaded, dry cave-air position, at least roughly eight blocks below the local surface, in the surrounding stable zone and beside sturdy natural stone. The central chunk area is reserved for the stream, independent of depth.

This first step does **not** add crystal blocks, crystal deposits, cave carving, tier refinement, metal conversion or ley lines. It supplies an eligibility API for a later crystal feature; it never places a crystal on the surface merely because the energy field extends there.

## Testing in game

Operator commands:

- `/riftfall geyser locate` finds the nearest candidate in a bounded regional search without generating terrain.
- `/riftfall geyser inspect` shows the current field, footprint and local contribution.
- `/riftfall ambient inspect` shows the actual chunk reservoir and total local regeneration rate.

Teleport to the reported X/Z at a safe Y in creative or spectator mode. Changing altitude within that chunk does not change field membership or output. Moving outward changes concentration, and crossing the footprint boundary returns to ordinary ambient regeneration.

Automated tests cover exact output-budget normalization, positive center-to-edge falloff, larger-field concentration, negative coordinates, complete footprint membership, non-overlap, descriptor persistence and capacity limits.

Native server checks confirmed full-height membership, center-to-fringe falloff, the exact footprint boundary, underground-only surface eligibility and restoration of the saved descriptor/output/mana after a complete restart. Temporary verification commands are excluded from the release.
