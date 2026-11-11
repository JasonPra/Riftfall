# Mana crystals

Tiers, in order: **red → orange → yellow → green → cyan → blue → violet → white**. All emit light level 9. Models use translucent smoky mineral textures and intergrown blunt columns with uneven heights, tilted stems and smaller daughter growths. Two deterministic clump variations break up repetition. There are no pointed tips or full-square foundation slabs.

The structure is inspired by photographs of [Naica’s Cave of the Crystals](https://en.wikipedia.org/wiki/Cave_of_the_Crystals), adapted to the one-block growth limit. NeoForge model data composes growth against **every sturdy solid face** around the cluster. The primary placement face carries the taller stems; other solid contacts receive smaller intergrown stems and roots. These secondary surfaces do not create more crystal items or change refinement costs.

Connections follow each supported surface independently, regardless of the original placement face. A cluster against both a floor and wall joins neighbors along either surface. Convex corners wrap around the shared stone block; inside and three-surface corners work in all four horizontal directions, including wall-to-ceiling transitions. Roots cover shared junction corners without floating end caps. Crystal blocks themselves are not treated as solid supports.

Visual topology is computed from the client’s existing chunk render data, with no extra block entities, packets, or chunk loads. Server collision shapes follow the same surfaces. Placement still requires natural stone; removing the original support reanchors the cluster to another valid natural stone face if one remains. Legacy connection properties are retained for saved-world compatibility, but the obsolete cap flag is always cleared. Connection/index repair waits until chunks are fully ready, avoiding reads during an unfinished load.

Each crystal prism has two layers: a softly mottled, translucent inner core and a slightly larger translucent shell. The smoky, milky shell uses dense cloudy opacity with greater coverage near the base, normal cave lighting and directional shading; flat root matrices have no emissive core, while narrower cores start above the bottom third of each column. The inset core alone has `shade: false`, no ambient occlusion and fullbright vertex light `0xF000F0`. For NeoForge 1.21.1 the model uses `neoforge_data` block/sky light 15, rather than the unsupported vanilla `light_emission` field. This does not raise the block's world light above 9. All eight tiers have separate glowing core textures with softened edges and reduced contrast, so the inner shape does not read as a solid bright insert.

Main bodies and root matrices always keep their own tier color. Only tagged, small outer daughter prisms wholly within the outer four pixels may inherit an actual adjacent tier's material, at a stable 25% chance. At most two pieces per block can borrow a color. Their core and shell switch together; camera movement does not reroll the choices. Bridges do not recolor the main bodies across their gap. Block tiers, drops, energy costs and refinement do not change.

Shared border planes are clipped against the neighbor’s root volume, including partial coverage; only exposed portions survive, avoiding a translucent block grid. Coplanar root and tilted stem faces are unioned with higher-tier material precedence and stable authored order for ties. Core lighting and texture coordinates survive clipping. Non-overlapping or separate-depth faces retain their geometry and ordinary transparency sorting; no global depth bias is imposed. This runs during client chunk meshing, not each frame.

A complete mature 2×2×2 group (four clusters below, four opposing clusters above) receives staggered intergrown branches at its shared central junction. This breaks up the artificial rectangular opening left by eight separate clump models. The rule rotates to all six attachment directions and four local corners, and requires all eight mature surface contacts. Removing or making any member immature disables that junction. No flat cap, new block, extra drop or gameplay cost is added; branches use the same fragment material inheritance and overlap cleanup as the rest of the cluster.

Touching crystal blocks connect even when suspended above the ground. Empty blocks stay empty: no crystal branches bridge gaps. Complete mature 2×2×2 junctions add vertical columns near the shared center rather than horizontal connecting bars.

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
