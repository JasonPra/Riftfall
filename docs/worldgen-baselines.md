# Repeatable native world-generation baseline

See [integration contract](worldgen-integration-contract.md) and [issue #5](https://github.com/JasonPra/Riftfall/issues/5).

Captured November 12, 2026 from the native dedicated-server runtime: Minecraft 1.21.1 / NeoForge 21.1.265, Java 21, baseline terrain code `872a8f1`, seed **923475**, Overworld, normal preset (`minecraft:normal`, noise settings `minecraft:overworld`), empty generator-settings. No datapack worldgen overrides. The pending four river resource assets were present, but not wired into biome selection. They do not affect these original-source queries.

The complete 15 sampled values are in [baseline-923475.json](../tools/worldgen/baseline-923475.json). Heights are the generator's `getBaseHeight` return values (first free position), **not** the Y of the top solid block. Biomes are original native source samples at sea-level quart Y, not postprocessed geyser biome assignments.

| Case | Anchor X, Z | Native biome | WORLD_SURFACE_WG | OCEAN_FLOOR_WG |
| --- | --- | --- | --- | --- |
| Coast | -3648, -4096 | beach | 64 | 64 |
| Inland | -2752, -4096 | plains | 93 | 93 |
| River | -2624, -4096 | river | 63 | 62 |

Each anchor is aligned to a chunk boundary. Samples at X offsets -1, 0, 1, 15 and 16 cover neighboring chunks and negative-coordinate rounding. Z is also a chunk boundary. These values deliberately preserve a shallow native river and coast for comparison with the old custom deep channel. They are not evidence of final generated blocks, aquifer continuity, decoration correctness or visual quality. Additional positive-coordinate and generation-order coverage is required for the mapping implementation.

## Reproduce

`tools/worldgen/WorldgenBaselineProbe.java` is an opt-in server-start probe, outside the main source set. It searches the native biome source in a fixed order, emits these samples and stops the test server. It does not request generation at the sample locations, discover geysers or modify saved descriptors. Normal server startup still generates its spawn area.

1. Use a disposable dedicated-server directory with EULA accepted, seed `923475`, `level-type=minecraft:normal`, `generator-settings={}` and a fresh level-name. Never point a probe at a valued save.
2. Temporarily copy the probe into `src/main/java/org/Azkerash/riftfall/worldmana/geysers/`, then run `gradlew classes` with Java 21 and the project's normal dependencies.
3. Launch the configured NeoForge development server (`gradlew runServer`, with its working directory configured to the disposable directory). Capture stdout containing `BASELINE_SEED` and `BASELINE_SAMPLE`.
4. Compare all 15 records with the JSON. Keep the same game/dependency versions and record the tested commit. A changed search result is a regression signal to inspect, not a reason to silently refresh expectations.
5. Remove the temporary main-source copy and run a **clean** release build, ensuring the probe class is absent from the jar. Never ship the server-start probe.

In the managed workspace this capture used the cached development server launcher and `/workspace/work/worldgen-baseline-run`, with log `/workspace/work/worldgen-baseline-native.log`. Those paths are conveniences, not dependencies of the checked-in recipe. An offline authentication public-key fetch error occurred; the server continued and all samples were emitted.

## Future comparisons

For #7, retain this unwarped source reference and add output-space cases for the chosen temperate reservation: center, every ring boundary, exterior handoff, native water connections and influence-radius boundary. Freeze the descriptor and mapping version alongside coordinates. No reservation is selected by this baseline.

For #8–#10/#25, generate fresh chunks around those cases in both neighbor orders and compare complete columns, predicted versus actual heights, water/air below the river surface, lake exits, surface materials and structure forecasts. Include positive and negative coordinates and save/reload. The same seed in an already explored world does not rerun generation; use fresh disposable saves or ungenerated regions. Existing local coastal/cave screenshots and older fixture logs remain qualitative defect examples, not accepted outputs for the new architecture.
