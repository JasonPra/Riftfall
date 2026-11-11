# Mana crystals

Tiers, in order: **red → orange → yellow → green → cyan → blue → violet → white**. All emit light level 9. Models use translucent smoky mineral textures and intergrown blunt columns with uneven heights, tilted stems and smaller daughter growths. Two deterministic clump variations break up repetition. There are no pointed tips or full-square foundation slabs.

The structure is inspired by photographs of [Naica’s Cave of the Crystals](https://en.wikipedia.org/wiki/Cave_of_the_Crystals), adapted to the one-block growth limit. NeoForge model data composes growth against **every sturdy solid face** around the cluster. The primary placement face carries the taller stems; other solid contacts receive smaller intergrown stems and roots. These secondary surfaces do not create more crystal items or change refinement costs.

Connections follow each supported surface independently, regardless of the original placement face. A cluster against both a floor and wall joins neighbors along either surface. Convex corners wrap around the shared stone block; inside and three-surface corners work in all four horizontal directions, including wall-to-ceiling transitions. Roots cover shared junction corners without floating end caps. Crystal blocks themselves are not treated as solid supports.

Visual topology is computed from the client’s existing chunk render data, with no extra block entities, packets, or chunk loads. Server collision shapes follow the same surfaces. Placement requires natural stone or an ore block; removing the original support reanchors the cluster to another valid stone or ore face if one remains. Legacy connection properties are retained for saved-world compatibility, but the obsolete cap flag is always cleared. Connection/index repair waits until chunks are fully ready, avoiding reads during an unfinished load.

Neighboring tiers intermingle visually in a four-pixel band at their shared edges. Stable world-position grain makes this a patchy mineral transition; matching seam vertices agree on the color from either side. Centers retain their tier color. Only internal boundary quads fully covered by a neighbor’s continuous root are hidden, preserving exposed faces at unequal growth stages. This is client mesh generation, not a per-frame simulation. It does not alter block tiers, drops, energy costs or refinement.

## Placement and growth

Raw crystals can be placed on natural stone or ore floors, walls and ceilings. Their thickness stacks from 1 to 8 layers, like snow. Eight layers is mature, occupying at most one block. Crystals cannot support towers of other crystals. Support is controlled by `riftfall:mana_crystal_supports`, including the shared `c:ores` tag for vanilla and tagged modded ores.

A successful branching attempt consumes 600 mana, with a 10% chance per random tick (not per game tick). Failed rolls or blocked locations cost nothing. Existing height growth requires 600 available but does not spend it; after a layer grows, a branching attempt takes priority. Higher-tier parents seed lower tiers: 70% one tier down, 25% two down, 5% three down, clamped at red. White therefore seeds violet, blue or cyan, never new white.

Geyser chunks draw these costs from their shared pool. Ordinary chunks use their own reservoir. Growth checks use already-loaded terrain and never force neighboring chunks to load.

## Ancient deposits and recovery

Each eligible cave chunk receives one initial deposit of up to 24 connected clusters, about three quarters mature. Outer deposits favor red/orange/yellow, middle deposits green/cyan, and inner deposits blue/violet with exceptionally rare white. These are starting ancient deposits, not spontaneous high-tier formation.

A persistent survey flag prevents mining or reloading from granting a fresh free deposit. Completely mined-out eligible chunks need at least 2,000 mana available to nucleate a single young **red** seed, costing 600. Replacement attempts are spaced about a minute apart and require a suitable underground stone surface. No suitable cave means no terrain replacement or artificial cave creation.

## Refinement ledger

Only mature, uncut clusters refine. Default red-to-orange pace is **215 minutes**, costing **600 mana per tier upgrade**. Each higher step takes ×1.25 longer. Server config `riftfall-crystals-server.toml` exposes `refinementMinutes`, `refinementManaCost` and `refinementTimeMultiplierPerTier`, as well as deposit/spread settings.

Progress is tracked in chunk batches; crystals progress in parallel. When insufficient mana is available, progress pauses rather than being lost. Ready upgrades are funded from one shared geyser pool: **one upgrade per eligible chunk per round, ordered center-out**, then another round if funds remain. Partially completed rounds survive saves.

Discovered unloaded chunks keep crystal positions, physical tier, pending tier and progress in `riftfall_crystal_refinement.dat`. The geyser's separate authoritative ledger stores its balance and collective regeneration. The simulation jumps between refinement events rather than iterating every missed game tick. A funded upgrade immediately consumes mana and creates a pending tally. Loading that chunk validates the saved physical crystal, applies its pending tier and does not spend again. Newly discovered or newly matured crystals start fresh; they receive no retroactive upgrades. No unloaded blocks are accessed or edited.

Mining away seed stock already removes its ability to propagate higher tiers. Extra harvesting penalties, hazards and metal refinement are not implemented.

## Harvesting and cutting

Each layer drops one raw crystal of its tier (a mature cluster drops eight). Raw items can be planted again. Stonecutting a raw crystal produces one cut crystal, permanently converting it into a crafting material with no planting or refinement behavior. The `riftfall:cut_mana_crystals` item tag provides an integration point for future crafting. Memory crystals are outside this feature.

## Matching adult density

Fully grown clusters widen their existing prisms when touching adults of the same tier on a shared growth surface. A pair packs toward its common edge, complete 2×2 corners add a further bonus, and larger connected patches approach the block footprint. Only immediate neighbors and their corner diagonals are inspected. Young clusters, unlike tiers and isolated adults do not activate packing. No new crystal meshes, tiers, drops or mana costs are introduced. Each complete prism is fitted affinely within its own cell, preserving all original faces, UVs, growth depth and connections. This runs during chunk meshing rather than each frame.

## Seed-bed and prism colors

Only the thin seed-bed/root pieces use smooth edge color blending. Individual crystal prisms use a stable whole-piece choice between their own tier and a touching neighbor’s tier, including raised edges and corners on all six sides. Interior prisms retain their native color. Boundary prisms have a 50% chance to borrow a neighbor color; the same choice applies to every face, without extra geometry or tier changes.

## Water

Crystals can be placed and spread into source or flowing water on valid stone/ore supports. They waterlog rather than washing away, preserve water when mined, and retain water through growth and tier refinement. Waterlogged crystals have a 20% higher chance of height growth and spreading per random tick, with unchanged mana requirements and costs. This bonus does not accelerate tier refinement.
