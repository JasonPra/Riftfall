# Ambient mana and ley-line data foundation

This is server-side bookkeeping on `prototyping`. It adds no rendered markers, entities, particles, spells, crystal blocks or mana transfer along lines.

## Chunk reservoirs

Every loaded full chunk gets a serialized `riftfall:ambient_mana` NeoForge attachment. New reservoirs start empty, have a capacity of **2,000**, and regenerate **0.50 mana per second**. Each recorded mature crystal cluster contributes another **0.50 mana per second**. Quantities are finite doubles; additions saturate at capacity and extraction cannot overdraw.

Only loaded time counts. Updates are batched once per 20 server ticks, with fractional elapsed time settled on access, crystal-count changes and unloading. No unloaded chunks are forced to load, and there is no offline catch-up or spawn-chunk exception. Mature-crystal counts currently default to zero: the crystal blocks have not been reintroduced.

Use these server-thread APIs rather than mutating the attachment directly:

- `WorldMana.inspect(LevelChunk)` returns an immutable snapshot.
- `WorldMana.add(LevelChunk, amount)` returns the amount accepted.
- `WorldMana.extract(LevelChunk, amount)` returns the amount actually withdrawn.
- `WorldMana.setMatureCrystalCount(LevelChunk, count)` updates the future crystal bonus.

Future crystal lifecycle code must keep that count accurate when clusters mature, break, or move across chunk boundaries. This foundation does not change Iron's Spells creature mana pools.

## Invisible ley-line nodes

Placement uses the world seed and dimension ID. Each 63-by-63-chunk region has a 25% chance of producing one randomly positioned candidate. Candidates within a **circular 63-chunk radius** compete using a stable random priority; the losing candidate is suppressed. This makes individual chunks' chances very small and guarantees that accepted nodes are more than 63 chunks apart, including across region boundaries and negative coordinates. The radius itself limits possible density.

Checking an ordinary chunk is constant-time. Only an actual candidate checks its eight neighboring regions. Unexplored neighbors are considered mathematically, without generating them, so exploration order cannot violate spacing.

A node is materialized when its full chunk first loads. Its X/Z position is the exact geometric chunk center, `chunkCoordinate * 16 + 8`. Its Y position is the first air block above the center's world surface. Existing full chunks can also acquire deterministic nodes when first loaded with this system installed.

## Connection tree

Each newly discovered node chooses a parent randomly among the three nearest existing nodes in that dimension. It creates exactly one connection. Thus all discovered nodes form one connected tree, with branching possible and no cycles. Placement is generation-order independent; the tree's precise topology can depend on discovery order.

Connections are endpoint references, not blocks or rendered lines. Crossing segments do not implicitly create extra nodes. Newly explored areas extend the existing tree; they do not require generating the world's undiscovered nodes first.

Nodes and parent links persist in each dimension's `data/riftfall_ley_lines.dat`, retaining unloaded nodes. `LeyLineNetwork.get(level)` exposes immutable node records for future systems. There is no automatic mana collection or redistribution yet.

## Read-only diagnostics

Operators can use:

- `/riftfall ambient inspect` — reservoir and node presence in the current loaded chunk.
- `/riftfall leylines list` — node count and the first ten stored node records in the current dimension.

No diagnostic command renders the network or directly grants or drains mana. Normal operators' coordinate and teleport commands can later be used alongside these records.

## Verification

The automated suite covers saturation/withdrawals, fractional loaded-time regeneration, attachment codec persistence, invalid quantities, spacing across region boundaries and negative coordinates, deterministic placement, connected acyclic graphs and exact saved-data round trips.

A native server fixture checked the real chunk attachment, exact 2-mana regeneration over 20 ticks with three recorded mature clusters, node discovery through chunk loading, and a complete server restart. All three node positions and parent links were identical after restart, and chunk mana/count data survived. The temporary fixture command is excluded from the release.
