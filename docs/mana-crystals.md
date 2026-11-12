# Mana crystals

Tiers, in order: **red → orange → yellow → green → cyan → blue → violet → white**. All emit light level 9. Models use translucent smoky mineral textures and intergrown blunt columns with uneven heights, tilted stems and smaller daughter growths. Two deterministic clump variations break up repetition. There are no pointed tips or full-square foundation slabs.

The structure is inspired by photographs of [Naica’s Cave of the Crystals](https://en.wikipedia.org/wiki/Cave_of_the_Crystals), adapted to the one-block growth limit. NeoForge model data composes growth against **every sturdy solid face** around the cluster. The primary placement face carries the taller stems; other solid contacts receive smaller intergrown stems and roots. These secondary surfaces do not create more crystal items or change refinement costs.

Connections follow each supported surface independently, regardless of the original placement face. A cluster against both a floor and wall joins neighbors along either surface. Convex corners wrap around the shared stone block; inside and three-surface corners work in all four horizontal directions, including wall-to-ceiling transitions. Roots cover shared junction corners without floating end caps. Crystal blocks themselves are not treated as solid supports.

Visual topology is computed from the client’s existing chunk render data, with no extra block entities, packets, or chunk loads. Server collision shapes follow the same surfaces. Placement requires natural stone or an ore block; removing the original support reanchors the cluster to another valid stone or ore face if one remains. Legacy connection properties are retained for saved-world compatibility, but the obsolete cap flag is always cleared. Connection/index repair waits until chunks are fully ready, avoiding reads during an unfinished load.

Neighboring tiers intermingle at supported shared edges and corners. Only root/seed-bed colors blend smoothly; boundary prisms choose a whole neighboring color with a stable 50% chance. Centers retain their own tier color. Root and coplanar cleanup operate during chunk meshing, preserving exposed surfaces at unequal growth stages. Visual mixing does not change tiers, drops, mana costs or refinement.

## Items and in-game checks

Each tier has a `raw_<tier>_mana_crystal` planting item, `cut_<tier>_mana_crystal` ingredient and `<tier>_mana_crystal_cluster` block, under the `riftfall` namespace. Raw items appear in Natural Blocks; cut items appear in Ingredients.

```mcfunction
/give @s riftfall:raw_red_mana_crystal
/give @s riftfall:raw_white_mana_crystal
/give @s riftfall:cut_blue_mana_crystal
/riftfall ambient inspect
/riftfall geyser inspect
/riftfall geyser teleport
```

Place raw items normally; stacking items on an existing cluster adds layers up to eight. An immature white seed can be planted as an investment, but cannot assist refinement until mature. To observe natural deposits and automatic tier refinement, use a geyser field; automatic refinement indexing is field-scoped. Outside fields, planted clusters still grow/spread using ordinary chunk mana, but do not enter the automatic geyser refinement ledger.

## Placement and growth

Raw crystals can be placed on natural stone or ore floors, walls and ceilings. Their thickness stacks from 1 to 8 layers, like snow. Eight layers is mature, occupying at most one block. Crystals cannot support towers of other crystals. Support is controlled by `riftfall:mana_crystal_supports`, including the shared `c:ores` tag for vanilla and tagged modded ores.

A successful branching attempt consumes 600 mana, with a 10% chance per random tick (not per game tick). Failed rolls or blocked locations cost nothing. Existing height growth requires 600 available but does not spend it. Immature clusters only grow their own height; branching is reserved for fully mature mother clusters. Higher-tier parents seed lower tiers: 70% one tier down, 25% two down, 5% three down, clamped at red. White therefore seeds violet, blue or cyan, never new white.

Geyser chunks draw these costs from their shared pool. Ordinary chunks use their own reservoir. Growth checks use already-loaded terrain and never force neighboring chunks to load.

## Ancient deposits and recovery

Each eligible cave chunk receives one initial connected deposit, about three quarters mature. New ecosystem cores use up to 48 clusters (`coreInitialDepositSize`): mature clusters are mostly white, with violet/blue mixed in, surrounded by lower-tier juveniles. Other zones use up to 24 (`initialDepositSize`): outer red/orange/yellow, middle yellow/green/cyan, inner cyan/blue/violet. White starting deposits are confined to the core. These are ancient starting conditions; new spontaneous seeds still begin red, and normal offspring remain lower-tier. Preserved legacy geysers retain their original deposit rules and excluded center.

A persistent survey flag prevents mining or reloading from granting a fresh free deposit. Completely mined-out eligible chunks need at least 2,000 mana available to nucleate a single young **red** seed, costing 600. Replacement attempts are spaced about a minute apart and require a suitable underground stone surface. No suitable cave means no terrain replacement or artificial cave creation.

## Refinement ledger

Automatic tier refinement runs for indexed mature clusters inside geyser fields. Only mature, uncut clusters refine. Default red-to-orange pace is **215 minutes**, costing **600 mana per tier upgrade**. Each higher step takes ×1.25 longer. Server config `riftfall-crystals-server.toml` exposes `refinementMinutes`, `refinementManaCost` and `refinementTimeMultiplierPerTier`, as well as deposit/spread settings.

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

Corner color contacts follow the same convex-wrap checks as surface connections, including clusters diagonally around a shared support block. A direct neighbor takes precedence over a corner neighbor. Root strips retain a continuous narrow shared seam but have staggered outer edges rather than a broad straight shelf; adult packing only widens crystal prisms, leaving the seed bed unchanged.

Seed-bed cleanup runs once during chunk mesh rebuilding. Coplanar faces pointing in the same direction are partitioned so each overlapping area has one owner, including multipart roots and multiple solid contacts. It preserves the union of the bed surfaces and their texture coordinates without depth offsets, tier render priority, or removing distinct prism faces.

## Sparkles

Crystals emit occasional stationary, fullbright star-shaped flashes tinted toward their tier color. Flashes last 8–14 ticks, quickly brighten and fade, and can appear on floors, walls, ceilings and waterlogged clusters. Spawning uses nearby client animation ticks and respects particle settings; it adds no server ticking or network packets.

Only fully mature mother clusters can produce new seedlings. Immature clusters use growth ticks only to increase their own height, regardless of the legacy `branch_next` saved property. The current implementation uses eight layer steps; only layer eight is mature. The spreading entry point also rejects immature sources before mana checks or random draws.

## Mature-neighbor refinement assistance

Only fully mature mothers provide or receive refinement assistance. The highest-tier mother touching one of the six block faces supplies the bonus; extra neighbors do not stack, and diagonal crystals do not count. A planted high-tier seed must reach maturity first. Removing the catalyst stops future assistance without erasing earned progress. Mana cost and center-out chunk rounds remain unchanged.

Default refinement **speed** bonuses from a white mother:

| Recipient | Speed bonus | Speed multiplier |
| --- | ---: | ---: |
| Red | +200% | 3.00× |
| Orange | +150% | 2.50× |
| Yellow | +100% | 2.00× |
| Green | +60% | 1.60× |
| Cyan | +35% | 1.35× |
| Blue | +15% | 1.15× |
| Violet | +5% | 1.05× |

The full recipient/donor table is configurable under `motherRefinementBonusPercent`. A speed bonus divides the required time; +200% means one third of the normal time, not instant refinement. The existing 215-minute base and 1.25-per-tier refinement time multiplier still apply, so late tiers remain slow.

Mother positions and adjacent mature-tier snapshots are saved for unloaded simulation without loading terrain. Indexed virtual upgrades update nearby assistance as events occur. Placing or removing a catalyst settles previously earned progress before its rate changes.

## Time to maturity by color

`growthTimeMultiplierPerTier` defaults to 1.10: each higher tier takes 10% longer on average to gain growth layers (white is about 1.95× red). The 20% underwater height-growth bonus remains multiplicative. This changes seed-to-mother growth only: mature spreading probability, spreading cost, and refinement settings are independent.

## Stable seed-bed rendering

Seed-bed/root faces render in the solid chunk pass with opaque smoky textures. Their RGB detail and smooth edge colors remain the same; the crystal prisms retain the existing translucent textures and pass. These disjoint passes prevent camera-dependent alpha sorting from changing which bed faces are visible. Coplanar bed cleanup still runs during chunk rebuilding.

## Coplanar prism overlap

Same-facing prism faces that occupy the same plane are partitioned once during chunk mesh construction so each surface area has one stable owner. This includes tilted faces. Uncovered surface portions retain their original positions, texture coordinates and opacity. Distinct planes and opposite-facing surfaces remain unchanged. Crystal dimensions, placement, density rules, model assets and textures are unchanged.

## Floor-to-wall corner seam

Where a floor cluster connects diagonally down around a shared support block to a wall cluster, only the floor bed’s connected outer edge extends to the wall bed’s outer plane. The extension matches that wall surface’s actual root thickness (0.6–1.2 model pixels). This works north, east, south and west. Missing or unrelated wall crystals cause no extension; the receiving wall surface, unconnected edges, ceiling surfaces, prism coordinates, textures and UVs stay unchanged. This is a cached mesh adjustment, not a growth or collision-rule change.

Vertical convex wall corners use the same thickness-matched extension. North/south-facing beds extend only along connected east/west edges; the east/west wall remains unchanged. This fixed ownership covers all four vertical corner orientations without double extensions. Only bed edge vertices move; wall prisms, UVs and opacity stay unchanged.

## Default balance reference

Values below are defaults from `CrystalRules`; world `serverconfig/riftfall-crystals-server.toml` can override them. Random-tick probabilities are not per-game-tick guarantees.

| Setting | Default | Applies to |
| --- | ---: | --- |
| `nucleationThreshold` | 2,000 mana | Replacement after complete mining |
| `spreadCost` | 600 mana | Successful offspring/replacement; also required available for height growth |
| `spreadChance` | 0.10 | Mature branching per random tick |
| `initialDepositSize` | 24 | Maximum starting clusters in non-core eligible chunks |
| `coreInitialDepositSize` | 48 | Maximum starting clusters in new white cores |
| `naturalWhiteCrystals` | true | Allow initial core white mothers |
| `refinementMinutes` | 215 minutes | Red-to-orange base time |
| `refinementManaCost` | 600 mana | Each funded tier upgrade |
| `refinementTimeMultiplierPerTier` | 1.25 | Slower successive refinement steps |
| `growthTimeMultiplierPerTier` | 1.10 | Slower higher-tier layer growth; spreading timing unchanged |
| `motherRefinementBonusPercent` | Recipient/donor table | Highest face-adjacent mature donor only |

Red height growth has a base 1/6 chance per random tick when enough mana is available. Divide this by `1.10^tierOrdinal` for higher tiers; water multiplies the chance by 1.2. Mature branching uses its independent 10% default (12% underwater), and only successful placements spend mana. White is the terminal tier.

## Generation and current scope

[geyser-regions.md](geyser-regions.md) defines the actual ring biomes, initial tier distributions, forest/grassland profiles, sediment rivers and layered cave generation. Crystal deposits survey eligible existing cave surfaces after chunk loading; they do not reshape terrain. Terrain shaping and carving occur earlier in Minecraft's generation pipeline, before vegetation.

The current visuals use two deterministic clump variants, eight layer states and connected surface composition. Whole-piece boundary colors and same-tier adult widening reuse existing prisms. The six-preset and separate four-block giant-crystal experiments are not part of this current model set. Sparkles are client particles; block light remains level nine. Cutting creates ingredients; metal processing, special harvesting hazards and custom ecosystem flora/fauna remain future systems.
