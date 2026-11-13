# Vermillion Legacy tree

An ancient, mana-nurtured hardwood. The current development portion targets **Manaheart Grove, the center biome**. Its adult trunk spans **three blocks across**, excluding its root system; this is not the size of one inventory log, nor a fixed trunk width at every growth stage. The first Dynamic Trees prototype rises roughly 73–75 blocks, with a broad, irregular green crown and roots reaching 17 blocks below its planting point in the native-server sample.

Its exterior is rough, mineralized ash-grey bark, with tiny green mana veins in the fissures. Dense heartwood has deep burgundy/red-amber coloring, tight growth rings and dark gold grain. The future artisan-polished finish should be jewel-like while remaining recognizably wood. Its difficulty of working and desirability to kings and artisans are part of the material design.

## First living-tree prototype

- Dynamic Trees family/species: `riftfall:vermillion_legacy`; branch radius grows up to 24 pixels, giving the adult trunk its three-block width.
- Three original adult JoCodes have tall central leaders, tiered boughs, diagonal branches and asymmetric crowns. These are connected Dynamic Trees branch networks, not stacks of static logs.
- A separate growing root network penetrates dirt and ordinary Overworld stone. Native generation produced 136 root blocks extending 17 blocks down.
- Custom Legacy growth logic gives young trees a tall leader and long outward boughs. Growth, felling, branch thickness and leaf support use Dynamic Trees.
- Original lush-green leaves, seed and sapling assets are included. Manaheart Grove's foliage color is `#63B36B`; its grass remains the existing placeholder white. No other biome colors change.
- Living branches retain diamond-axe harvesting/stripping and failed-strike durability wear. Each face can be stripped independently, yielding one bark item the first time that face is stripped.

To inspect an adult, stand on clear ground with roughly 55×55 blocks of open space and 85 blocks of headroom, then run:

```mcfunction
/legacytree ~ ~-1 ~
```

The coordinates identify the **soil block**, not the first trunk block. This operator preview command creates an adult and underground roots immediately. For normal growth, obtain `/give @s riftfall:vermillion_legacy_seed`, plant it on soil and use Dynamic Trees' ordinary growth mechanics.

Legacy trees also generate naturally in **new Manaheart Grove chunks**, through Dynamic Trees' normal biome-decoration feature. One jittered anchor per 4×4-chunk grove cell keeps neighboring trunk centers at least 48 blocks apart. Natural crowns reserve a margin for leaf ageing inside Minecraft's 3×3-chunk feature-write area; manual previews keep the full original crown. Other rings select temporary Dynamic Trees jungle species. See [biome integration](dynamic-trees.md). Existing trees are not replaced, and no terrain elevation changes are included.

## Implemented materials

| ID | Purpose |
| --- | --- |
| `riftfall:vermillion_legacy_log` | Bark-covered log; the raw Legacy Heartwood harvest/building material. |
| `riftfall:stripped_vermillion_legacy_log` | Exposed burgundy/amber grain. Axe stripping preserves axis and natural provenance. |
| `riftfall:vermillion_legacy_bark` | One piece per actual bark-to-stripped conversion. Reserved for future alchemy and forge mastery. |
| `riftfall:glinting_sap_amber` | Rare crystallized mana-sap, reserved for high-level alchemy/jewelcrafting. Future gear-reserve recipes are not implemented. |

Logs appear in Natural Blocks; bark and Sap-Amber appear in Ingredients. `/give @s <ID>` provides the corresponding item for review. Ordinary placed logs are normal one-block pillars with X/Y/Z placement.

## Harvesting and material behavior

- Diamond-or-better **axes** harvest and strip the logs. The block uses the standard axe/diamond-tier tags plus a correct-tool check; a diamond pickaxe is not a valid chopping tool. Creative mode retains ordinary building access.
- Low-tier axe strikes deflect with a metallic ring and cost **4 durability per failed strike**. They neither remove the log nor strip its bark. This is durability wear, not a separate dullness status system.
- Hardness is **8.0**, and blast resistance **1200** protects against conventional TNT/explosions. Magical degradation has no dedicated mechanic yet.
- Both log items sink in water. This applies to dropped log stacks; it does not make placed blocks fall.
- Log blocks emit light level **3**. Crimson grain and tiny green fissures pulse through animated 32px textures. The light value is stable; Minecraft's ordinary block lighting is not colored crimson and does not trigger repeated relighting per texture frame.
- Standard wood fire behavior is retained.

Correctly harvested static logs drop their corresponding log. Static material marked `natural=true` also has a **15%** chance to drop one Glinting Sap-Amber. Stripping preserves that provenance; normal placement defaults to `natural=false`, and harvested item drops do not copy it. Player-placed/rebroken logs therefore cannot farm Sap-Amber. For development, `/setblock ~ ~ ~ riftfall:vermillion_legacy_log[natural=true]` creates a provenance-marked sample. The new living branch networks currently use Dynamic Trees' volume-based log/stick drops; their natural-only Sap-Amber integration remains a later portion.

Bark from static logs drops on the actual server-side log replacement, not when a tool previews a stripping result. An already stripped static log cannot yield another bark item by being stripped again.

Dynamic branches strip **only the face hit by the axe**, including interactions through the outside collision shell of a three-block trunk. Other sides retain their bark. Repeating the same face yields no bark or additional axe wear; a different intact face can be stripped separately. The rendered geometry and branch connections remain Dynamic Trees' own, with exposed-wood textures applied only to selected bark faces; existing end-grain textures are retained.

Partially stripped segments store a six-bit face mask in a small, synchronized block entity. Only wounded segments have one; intact trees do not gain block entities or a separate baked model for every possible face combination. Face data persists through chunk saves, client updates, resizing and the bottom-flare feature. Once all six faces are stripped, the segment becomes the ordinary fully stripped branch. Existing fully stripped bands from earlier builds remain fully stripped, since those saves contain no record of which face was originally hit. Static building logs retain their ordinary whole-log stripping behavior.

Growth cannot restore harvested bark or allow repeated harvesting. Trunks can still change thickness and maintain their surrounding collision shells. Bark healing and sap hardening remain a later portion.

## Living-tree behavior and future portions

A **living, root-connected tree** should seal a stripping wound with hardening sap while its bark heals. Harvested, dead or player-placed wood should not regrow bark. That behavior must be tied to the family's live branch/root state, not inferred from the primitive log's provenance marker. The family and branch-stripping integration are implemented; sap scars and bark regrowth are not yet implemented.

**World-Eye** is planned as a seasonal/rare, heavy emerald-skinned Insight Fruit with a glowing red core. Its **Cosmic Clarity** buff is intended to reveal secrets, cosmic-enemy weaknesses or crafting insights. Fruit, buffs, recipe unlocks, permanent mana-reserve gear recipes, planks and polished blocks remain future portions.

![Vermillion Legacy wood preview](previews/legacy-wood.png)

Regenerate the pixel textures and preview with `python3 tools/assets/generate-legacy-wood.py` (Pillow is needed only for regeneration).

Regenerate foliage/seed textures with `python3 tools/assets/generate-legacy-foliage.py`, and the original adult/root JoCodes with `python3 tools/trees/generate-legacy-shapes.py`.

## Verification

Face-stripping checks exercised all six hit directions, including interactions through thick-trunk shells. Seven distinct face strips yielded seven bark items; repeat attempts yielded none. Partial face data survived thin/thick resizing, 200 bottom-flare updates and a block-entity save/load round-trip. Fully stripped segments removed their wound data, and harvesting a wounded segment still yielded wood without leaving an orphan block entity. A native client check inspected 324 baked quads at radii 4, 8, 16 and 24: 36 selected bark quads switched to exposed wood, while unselected/end-grain quads and all vertex positions remained unchanged.

A native growth regression check stripped the lowest two trunk segments and an upper segment on an adult, resized a stripped segment between thin and thick radii, and stripped the bottom two segments of a growing sapling. Six initial strips yielded exactly six bark items. Stripped states survived hundreds of growth pulses, repeated stripping was rejected, and the young tree retained its live root connection. This includes the bottom-flare update that previously restored bark immediately.

A disposable NeoForge 21.1.265 server loaded the actual Dynamic Trees tree pack and generated an adult with 2,199 branch/root blocks, 8,500 leaves, a 49×47-block canopy, radius-24 trunk and 17-block root depth. A planted seedling transitioned to a living tree and grew to 109 branches/root blocks under accelerated growth pulses in ticking chunks. Iron axes were rejected on both the trunk center and its outer shell, with four durability lost per failed strike. Diamond stripping succeeded, and repeating the strip failed. Asset model/texture references resolve against Riftfall, Dynamic Trees and Minecraft resources. This verifies server behavior and asset references; the appearance still needs in-game review.

A disposable native-server check exercised actual axe actions for X/Y/Z logs: simulated stripping spawned no bark, successful stripping spawned exactly one, repeat stripping added none, and natural provenance survived stripping. Diamond/netherite axes were accepted while iron axes and diamond pickaxes were rejected; a failed iron strike canceled mining and applied four durability wear. Natural-log loot sampled 152 Sap-Amber in 1,000 harvests, with zero from 1,000 placed-log checks. The log survived a TNT-strength explosion, emitted level 3 light and sank more than one block during the water-physics check. The temporary verification class is excluded from release builds.
