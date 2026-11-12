# Verdant Frontier and goblin camps

**Verdant Frontier** is the display name of the monogeyser grassland fringe. Its saved biome ID stays `riftfall:geyser_red_outer`, preserving existing worlds and references. Grass is plains-like green `#8FBA62`, with slightly blue-tinged foliage `#78A83A`. Minecraft's normal biome-color sampling blends its boundary with neighboring biomes. Other rings retain their diagnostic colors.

Goblins are **not** added to any natural biome spawn pool. Camps are generation-time features in the Frontier, not a runtime terrain edit or a vanilla village replacement.

About one-third of new monogeysers are eligible for camps, determined from the world seed and field center. Eligible fields have sparse candidate sites: at most one per 8×8-chunk cell, with half of those cells selected and a minimum five-chunk separation across cell boundaries. Sites must lie in the outer ring and pass terrain checks; eligibility does not guarantee a camp. Steep slopes, water, trees, occupied ground and footprints crossing biome boundaries reject placement. Camps fit inside one chunk and level at most one block, without excavating hillsides or bridging rivers.

The first camp layout has two small canvas tents, a central fire, log seating, a barrel/hay stack, a crafting table and a goblin spawner inside a tent. No initial goblins are created by the feature: inhabitants come from that spawner when a non-spectator player approaches.

The spawner chooses warriors, archers and shamans with weights **6:3:1**. It activates within 16 blocks, attempts one goblin per cycle, waits 20–40 seconds between cycles after its initial five-second delay, and uses a six-nearby-goblin cap in its local detection box. It can operate in daylight and respects ordinary spawner collision, difficulty and player-proximity rules. Goblins retain their existing combat, teamwork, equipment, hostility and despawning behavior. Breaking the spawner stops that camp's supply; there is no special rebuilding or camp-respawn manager.

The feature runs only during new chunk decoration. Existing explored terrain is not retrofitted, and preserved legacy geyser fields do not receive camps. The new biome display name and colors also apply to already-saved Frontier biomes. Camps are features, so `/locate structure` does not locate them. Use the geyser development teleport command and inspect the outer fringe of new fields.
