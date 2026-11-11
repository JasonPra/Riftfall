# Mana crystals

Tiers, in order: **red → orange → yellow → green → cyan → blue → violet → white**. All emit light level 9. Models use translucent smoky mineral textures, blunt stepped columns and a continuous base that joins neighboring clusters. They have no pointed tips.

## Placement and growth

Raw crystals can be placed on natural stone floors, walls and ceilings. Their thickness stacks from 1 to 8 layers, like snow. Eight layers is mature, occupying at most one block. Crystals cannot support towers of other crystals. Natural stone support is controlled by `riftfall:mana_crystal_supports`.

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
