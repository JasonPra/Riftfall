# Dynamic Trees dependency

Riftfall requires **Dynamic Trees 1.7.2 for NeoForge**, CurseForge project **252818**, file **8227322**. The release declares Minecraft `[1.21.1, 1.22)` and NeoForge `[21.1.80,)`, compatible with this branch's Minecraft 1.21.1 / NeoForge 21.1.265.

Gradle pins `curse.maven:dynamic-trees-252818:8227322`; the mod metadata requires `dynamictrees` 1.7.2 on client and server. The library is a separate runtime dependency, not bundled into the Riftfall jar. Development runs resolve it automatically.

This adds the framework for the next custom tree. No Riftfall tree family, species, model or treepack is registered yet. Other rings retain their existing temporary vegetation. Manaheart Grove's jungle surface vegetation is cleared separately in preparation for future content; its height and crystal rules are unchanged.
