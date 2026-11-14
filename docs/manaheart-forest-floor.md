# Manaheart forest floor

This visual iteration increases Heartwood placement opportunities from one per 4×4-chunk cell to one per 3×3-chunk cell, about 1.78 times as many. Jittered anchors stay at chunk centers to preserve the existing feature-write bounds; neighboring trunks can now be 32 blocks apart. Tree growth and captured wild-tree shapes are unchanged.

Only Manaheart Grove's land biome gains the new decorations. They use ordinary configured and placed biome features: vanilla forest-rock blobs for boulders, vegetation patches for moss and podzol, and random plant patches for underbrush. No tree placement is moved outside Dynamic Trees' normal decoration path.

| Name | Block ID | Appearance |
| --- | --- | --- |
| Starweave Moss | `riftfall:starweave_moss` | Deep green moss with fine, softly emissive silver-teal threads. |
| Starweave Moss Carpet | `riftfall:starweave_moss_carpet` | A low layer of the same moss. |
| Starweave Boulderstone | `riftfall:starweave_boulderstone` | Dark, moss-covered stone used for small native boulder clusters. |
| Whisperfrond Fern | `riftfall:whisperfrond_fern` | Pale, finely divided fronds, tinted by the biome's grass color. |
| Dawnstar Blossom | `riftfall:dawnstar_blossom` | An ivory-and-gold star flower with warm emissive details. |
| Moonveil Bloom | `riftfall:moonveil_bloom` | Small silver-blue flowers with cool emissive details. |

Podzol remains Minecraft's existing soil. Moss and flowers use original 32px art, flat plant sprites and separate emissive texture layers; no blinking animation or new terrain carving is involved. The moss and its carpet use restrained luminous markings, while flowers also emit a stable light level of one.

Starweave Moss is registered with Dynamic Trees as dirt-like planting soil, including a matching root-bearing soil model. Bone meal uses its own native vegetation-patch configuration, preserving magical moss and flora instead of spawning vanilla moss. Ferns drop their item with shears; other flora and moss use ordinary self-drops. Boulderstone is mined with a pickaxe. Items appear in Natural Blocks.

Regenerate art and models with `python3 tools/assets/generate-manaheart-flora.py`. In-game verification confirmed all new decorations in fresh Manaheart chunks, Heartwood soil acceptance and bone meal producing magical moss without vanilla moss. Existing generated terrain is not repainted.

![Forest floor](previews/manaheart-forest-floor.png)

![Nighttime emissive details](previews/manaheart-forest-night.png)

![Denser canopy](previews/manaheart-forest-canopy.png)
