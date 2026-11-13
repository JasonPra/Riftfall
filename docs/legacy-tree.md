# Vermillion Legacy tree: material foundation

An ancient, mana-nurtured hardwood intended for the middle ring and parts of the outer ring. The current adult trunk design spans **3×3 blocks**, excluding its root system; this is not the size of one inventory log, nor a fixed trunk width at every growth stage. The eventual tree should rise exceptionally high, sink roots deep into the ground, and spread an enormous canopy of unusually healthy, lush green leaves.

Its exterior is rough, mineralized ash-grey bark, with tiny green mana veins in the fissures. Dense heartwood has deep burgundy/red-amber coloring, tight growth rings and dark gold grain. The future artisan-polished finish should be jewel-like while remaining recognizably wood. Its difficulty of working and desirability to kings and artisans are part of the material design.

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

Correctly harvested wood drops its corresponding log. **Naturally grown** material also has a **15%** chance to drop one Glinting Sap-Amber. `natural=true` is a generation-only provenance marker for future grown-tree integration. Stripping preserves it; normal placement defaults to `natural=false`, and harvested item drops do not copy it. Player-placed/rebroken logs therefore cannot farm Sap-Amber. For development, `/setblock ~ ~ ~ riftfall:vermillion_legacy_log[natural=true]` creates a provenance-marked sample.

Bark drops on the actual server-side log replacement, not when a tool previews a stripping result. An already stripped log cannot yield another bark item by being stripped again.

## Living-tree behavior and future portions

A **living, root-connected tree** should seal a stripping wound with hardening sap while its bark heals. Harvested, dead or player-placed wood should not regrow bark. That behavior must be tied to the Dynamic Trees family's live branch/root state, not inferred from the primitive log's provenance marker. The family, branch-stripping integration, sap scar and bark-regrowth lifecycle are the next tree stage; they are not implemented on these static log blocks.

**World-Eye** is planned as a seasonal/rare, heavy emerald-skinned Insight Fruit with a glowing red core. Its **Cosmic Clarity** buff is intended to reveal secrets, cosmic-enemy weaknesses or crafting insights. No fruit, buff, recipe unlock, permanent mana-reserve gear recipe, leaves, roots, planks or polished blocks are added by this first material step. No biome elevation or world-generation changes are made.

![Vermillion Legacy wood preview](previews/legacy-wood.png)

Regenerate the pixel textures and preview with `python3 tools/assets/generate-legacy-wood.py` (Pillow is needed only for regeneration).

## Verification

A disposable native-server check exercised actual axe actions for X/Y/Z logs: simulated stripping spawned no bark, successful stripping spawned exactly one, repeat stripping added none, and natural provenance survived stripping. Diamond/netherite axes were accepted while iron axes and diamond pickaxes were rejected; a failed iron strike canceled mining and applied four durability wear. Natural-log loot sampled 152 Sap-Amber in 1,000 harvests, with zero from 1,000 placed-log checks. The log survived a TNT-strength explosion, emitted level 3 light and sank more than one block during the water-physics check. The temporary verification class is excluded from release builds.
