# Vermillion Legacy tree

An ancient, mana-nurtured hardwood. The current development portion targets **Manaheart Grove, the center biome**. Its adult trunk spans **three blocks across**, excluding its root system; this is not the size of one inventory log, nor a fixed trunk width at every growth stage. Naturally generated samples reach roughly 34 blocks high, with crowns 39–45 blocks across. The current shape revision replaces the tall tiered crown with earlier forks, arching limbs and uneven foliage pads inspired by an umbrella tree and bonsai. Tapered surface roots spread from the flared base, alongside a separate underground anchoring network.

Its exterior is rough, mineralized ash-grey bark, with tiny green mana veins in the fissures. Dense heartwood has deep burgundy/red-amber coloring, tight growth rings and dark gold grain. The future artisan-polished finish should be jewel-like while remaining recognizably wood. Its difficulty of working and desirability to kings and artisans are part of the material design.

## First living-tree prototype

- Dynamic Trees family/species: `riftfall:vermillion_legacy`; branch radius grows up to 24 pixels. Mature trees retain a radius-24 lower bole for their first five trunk segments, then taper into the scaffold. Seedlings and slender juveniles retain normal growth.
- Three adult JoCodes have low forks, sideways-sweeping bowl-shaped limbs and shallow, overlapping foliage pads. The leader ends inside the crown instead of forming a separate tall peak. These are connected Dynamic Trees branch networks, not stacks of static logs.
- A separate growing root network penetrates dirt and ordinary Overworld stone. The revised native samples retain roughly 93–104 underground root blocks per tree.
- Custom Legacy growth logic encourages earlier irregular forks and long outward boughs. Growth, felling, branch thickness and leaf support use Dynamic Trees.
- Surface roots use Dynamic Trees’ supported root geometry, step down with the ground and taper away from the trunk. Main roots reach their full surface-root thickness once the trunk reaches an eight-pixel radius, and intersecting paths cannot revisit and thin an inner root. Native growth checks retained 45–50 surface root blocks per sample, including substantial inner roots. They can resize as the tree grows; roots overlapping the thick trunk retain access to its face-specific axe interaction.
- Original lush-green leaves, seed and sapling assets are included. Manaheart Grove's foliage color is `#63B36B`; its grass remains the existing placeholder white. No other biome colors change.
- Living branches retain diamond-axe harvesting/stripping and failed-strike durability wear. Each broad surface patch can be stripped independently, yielding one bark item the first time that patch is stripped.

To inspect an adult, stand on clear ground with roughly 55×55 blocks of open space and 50 blocks of headroom, then run:

```mcfunction
/legacytree ~ ~-1 ~
```

The coordinates identify the **soil block**, not the first trunk block. This operator preview command creates an adult and underground roots immediately. For normal growth, obtain `/give @s riftfall:vermillion_legacy_seed`, plant it on soil and use Dynamic Trees' ordinary growth mechanics.

Legacy trees also generate naturally in **new Manaheart Grove chunks**, through Dynamic Trees' normal biome-decoration feature. One jittered anchor per 4×4-chunk grove cell keeps neighboring trunk centers at least 48 blocks apart. Natural crowns reserve a margin for leaf ageing inside Minecraft's 3×3-chunk feature-write area; manual previews keep the full crown. Other rings select temporary Dynamic Trees jungle species. See [biome integration](dynamic-trees.md). Existing crowns are not rewritten; existing Legacy trees can gain surface roots on growth. No terrain elevation changes are included.

Thick trunk end grain uses complete, directly authored 96px animation frames for ordinary and stripped log tops, explicitly registered in `assets/minecraft/atlases/blocks.json`. Dynamic Trees’ thick-ring expander treats a vertical animation strip as one image and produces blank enlarged frames; these assets bypass that conversion while retaining the required `_thick` sprite names. Every frame remains opaque and only the mana tint changes subtly.

The foreground tree below generated naturally in Manaheart Grove (seed `923475`, soil at `29400 79 -28904`). Its ground is still the biome’s existing placeholder surface.

![Naturally generated umbrella Legacy tree](previews/legacy-umbrella-natural.png)

[Root-base close-up](previews/legacy-umbrella-roots.png).

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

Dynamic branches strip **only the broad surface patch hit by the axe**, including interactions through the outside collision shell of a thick trunk. New wounds divide a thin branch into one section, an intermediate trunk into two equal sections and a full three-block trunk into three equal sections. This follows the visible surface instead of leaving narrow world-grid slivers as separate targets. Small horizontal bark shoulders follow the adjoining side patch. Repeating a patch yields no bark or additional axe wear; neighboring patches remain harvestable. Clipping preserves Dynamic Trees’ branch geometry, winding, UV mapping and original end grain.

Wounded segments store a synchronized patch mask and section count in a small block entity. The section count remains stable during later growth, avoiding extra harvest targets appearing as bark thickens. Intact trees do not gain block entities or separate baked models for patch combinations. Wounds persist through saves, client updates, resizing and bottom-flare growth. Existing world-grid wounds migrate on load into the matching broad sections without extra bark drops; partially exposed sections can expand during migration so harvested bark is not restored. Old per-face wounds and fully stripped branches retain their exposure. Static building logs retain whole-log stripping from a bark-covered side.

Cut end-grain faces reject stripping without bark drops or axe wear. Placed logs use their X/Y/Z axis; living branches use Dynamic Trees’ connection rules for ring caps, including horizontal branch ends. Bark-covered shoulders remain part of the adjoining side patch.

Growth cannot restore harvested bark or allow repeated harvesting. Trunks can still change thickness and maintain their surrounding collision shells. Bark healing and sap hardening remain a later portion.

## Living-tree behavior and future portions

A **living, root-connected tree** should seal a stripping wound with hardening sap while its bark heals. Harvested, dead or player-placed wood should not regrow bark. That behavior must be tied to the family's live branch/root state, not inferred from the primitive log's provenance marker. The family and branch-stripping integration are implemented; sap scars and bark regrowth are not yet implemented.

**World-Eye** is planned as a seasonal/rare, heavy emerald-skinned Insight Fruit with a glowing red core. Its **Cosmic Clarity** buff is intended to reveal secrets, cosmic-enemy weaknesses or crafting insights. Fruit, buffs, recipe unlocks, permanent mana-reserve gear recipes, planks and polished blocks remain future portions.

![Vermillion Legacy wood preview](previews/legacy-wood.png)

Regenerate the pixel textures and preview with `python3 tools/assets/generate-legacy-wood.py` (Pillow is needed only for regeneration).

Regenerate foliage/seed textures with `python3 tools/assets/generate-legacy-foliage.py`, and the adult/root JoCodes with `python3 tools/trees/generate-legacy-shapes.py`.

## Verification

Three naturally generated Manaheart trees were checked through the normal chunk-decoration path: each reached 34 blocks high, with 39–45-block-wide crowns and roughly 986–1,270 occupied foliage columns. Their lower trunks measured radius 24, or three blocks across. Separate adult previews retained their surface and underground roots after ticking. A seed-grown tree stayed live through 600 growth pulses, reaching 63 branches and 14 blocks high in that young sample.

Native client checks resolved both 96px thick-end textures and inspected every pixel in all four animation frames: no transparent or blank wood pixels remained. Geometry checks covered 16 historical world-grid cases, 20 new surface-section cases and a horizontal bark shoulder following its adjoining wall patch. Intermediate trunks use two equal sections, and full-width trunks use three.

Native server interactions checked adjacent thick-trunk shell hits, repeat-harvest prevention, resizing, growth, serialization and old-save migration. Old grid strips merge into broad sections without new drops. Cut trunk tops and horizontal branch ends reject stripping, while bark shoulders remain strippable. Placed logs passed 36 axis/face/simulation combinations, accepting bark sides and rejecting cut ends for X, Y and Z orientations.

Earlier material checks sampled 152 Sap-Amber drops in 1,000 natural-log harvests and zero in 1,000 placed-log harvests. Low-tier strikes canceled mining and cost four durability; diamond and netherite axes were accepted, while diamond pickaxes were rejected. Logs survived a TNT-strength explosion, emitted light level 3 and sank in water. Temporary native verification classes are excluded from release builds.
