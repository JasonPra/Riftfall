# Dynamic Trees dependency

Riftfall requires **Dynamic Trees 1.7.2 for NeoForge**, CurseForge project **252818**, file **8227322**. The release declares Minecraft `[1.21.1, 1.22)` and NeoForge `[21.1.80,)`, compatible with this branch's Minecraft 1.21.1 / NeoForge 21.1.265.

Gradle pins `curse.maven:dynamic-trees-252818:8227322`; the mod metadata requires `dynamictrees` 1.7.2 on client and server. The library is a separate runtime dependency, not bundled into the Riftfall jar. Development runs resolve it automatically.

Riftfall registers its first custom family and species, `riftfall:vermillion_legacy`, through a bundled tree pack and NeoForge's Dynamic Trees registration hooks. It includes thick living branches, underground roots, leaves, a seed, a sapling, custom growth logic and three adult shapes. See [Vermillion Legacy](legacy-tree.md) for preview commands and the remaining portions.

This portion targets **Manaheart Grove only**. Its foliage palette is now lush green. Other biomes retain their colors and temporary vegetation. Natural grove placement is pending shape review and spacing work; this prototype can be planted as a seed or generated with `/legacytree <soil position>`. Terrain height and crystal rules are unchanged.
