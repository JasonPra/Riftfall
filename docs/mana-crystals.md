# Mana crystals

Tiers, in order: **red → orange → yellow → green → cyan → blue → violet → white**. All emit light level 9. Models use translucent smoky mineral textures and intergrown blunt columns with uneven heights, tilted stems and smaller daughter growths. Six distinct clump presets per stage break up repetition. The selected preset is saved in the block state, randomly chosen on placement and growth, and stays fixed through reloads and neighbor updates. There are no pointed tips or full-square foundation slabs.

The structure is inspired by photographs of [Naica’s Cave of the Crystals](https://en.wikipedia.org/wiki/Cave_of_the_Crystals), adapted to the one-block growth limit. NeoForge model data composes growth against **every sturdy solid face** around the cluster. The primary placement face carries the taller stems; other solid contacts receive smaller intergrown stems and buds. These secondary surfaces do not create more crystal items or change refinement costs.

Connections follow each supported surface independently, regardless of the original placement face. A cluster against both a floor and wall joins neighbors along either surface. Convex corners wrap around the shared stone block; inside and three-surface corners work in all four horizontal directions, including wall-to-ceiling transitions. Individual edge buds reach the shared boundary and overlap neighboring young or adult buds without floating end caps. Crystal blocks themselves are not treated as solid supports.

Visual topology is computed from the client’s existing chunk render data, with no extra block entities, packets, or chunk loads. Server collision shapes follow the same surfaces. Placement still requires natural stone; removing the original support reanchors the cluster to another valid natural stone face if one remains. Legacy edge connection properties are retained; the unused floating-cap property has been removed to avoid multiplying saved preset states. Connection/index repair waits until chunks are fully ready, avoiding reads during an unfinished load.

Each crystal prism has two layers: a softly mottled, translucent inner core and a slightly larger translucent shell. The smoky, milky shell uses dense cloudy opacity with greater coverage near the base, normal cave lighting and directional shading; there are no flat backing or root plates, and narrower cores start above the bottom third of each column. Young columns start narrow and grow in width as well as height. The inset core alone has `shade: false`, no ambient occlusion and fullbright vertex light `0xF000F0`. For NeoForge 1.21.1 the model uses `neoforge_data` block/sky light 15, rather than the unsupported vanilla `light_emission` field. This does not raise the block's world light above 9. All eight tiers have separate glowing core textures with softened edges and reduced contrast, so the inner shape does not read as a solid bright insert.

Main bodies always keep their own tier color. Only tagged, small outer daughter prisms wholly within the outer four pixels may inherit an actual adjacent tier's material. Each differently colored shared edge receives three borrowed-color daughter crystals, with a stable 85% chance per additional piece up to five. Small daughter prisms are distributed along all four shared surface edges, so mixing is visible in every direction. Their core and shell switch together; camera movement does not reroll the choices. Block tiers, drops, energy costs and refinement do not change.


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

## Sparkles

Nearby crystals occasionally emit small, stationary, fullbright glints in their tier highlight color. Custom four-point star glints have a near-white center, a rapid flare and a short fade over 8–14 ticks. Their increased size and frequency make them visible as sparkles rather than faint dots. Floor, wall and ceiling growths all sparkle; this uses vanilla client display ticks and particle settings without server tasks or network packets.

Growth models consist entirely of individual prisms, including connected edges. Exposed stone remains visible between young buds; growth enlarges those buds instead of filling a rectangular backing sheet.

Short, overlapping prisms form branching clumps behind the visible buds. Staggered edge pillars connect those clumps to the neighboring blocks, including mixed growth stages, without a continuous backing plane.

Shared-edge buds overlap along the full edge, including its corners, with varied heights and depths. This closes repeated block-seam slots without a single broad backing face.

Mixing includes adjacent growth on different support faces and growth wrapped around convex stone corners. Shared-edge buds are rendered for actual neighboring crystals, rather than forming a rim solely because two solid supports meet.

At floor/wall and convex-corner contacts, one endpoint owns small prisms that overlap the two cells by one pixel. Each new shared prism gets a stable 50/50 choice between the adjacent tier colors; shell and core agree. Geometry on unsupported open edges is not filled with a rim.

Body presets use explicit model variants; cached edge meshes are composed directly, avoiding the repeated loading of large multipart selectors for each saved preset state. Edge-bud heights remain staggered at maturity rather than all hitting the same height cap.

Shared-corner overlap cleanup also compares neighboring block meshes. A stable positional owner keeps each coplanar surface once, clipping duplicate regions from the other mesh. Temporary neighbor masks never render; color mixing and shared shapes are preserved without depth bias.

Larger prisms (at least six pixels tall) use a separate shell texture with a gentle transparency increase over the upper 30% of their length. Side UVs span each prism so the fade follows its tip, including tilted and wall-mounted crystals; top facets sample the clearer tip and bases retain the original density. Small buds keep their smoky texture.

## Large mature formations

A 2×2 patch can visually combine only when all four crystals are fully grown, share the same tier and primary attachment face. Eligible patches have a stable 35% formation roll; overlapping candidates compete by deterministic priority so a block belongs to at most one large formation. Six larger, blunt intergrown silhouettes are available on floors, walls and ceilings. Their shell and glowing core are split continuously across the four cells without internal cut faces, retaining the clearer tips. The outside edge buds and mixing remain. Mining a member dissolves the formation; the four blocks retain their normal drops, collision, light and mana behavior. Nearby changes refresh the affected render sections, including across borders, with no extra block entities or group ticks.

Large formations use slender dominant columns and small staggered branching growths extending back to the four original clumps and their perimeter seams. Wide panel-like daughter prisms are avoided; the connecting growths remain individual columns.

Each large formation also has two substantial companion crystals leaning at 22.5 degrees along different axes. Their locations vary with the six presets. Each rotated shell and core stays inside its cell, avoiding artificial cuts and new overlaps at the formation seams.

Large models export exterior shell and core surfaces separately, trimming buried prism faces and giving coincident outside faces one owner. This reduces translucent self-occlusion as the camera moves without deleting exposed faces or changing the silhouettes. The generator surface checks can be run with `python -m unittest discover -s tools -p test_crystal_surfaces.py`; Java geometry checks remain in `./gradlew test`.
