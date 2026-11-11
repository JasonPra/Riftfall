# Ambient mana

Ordinary chunks have an independent, persistent `riftfall:ambient_mana` attachment. New reservoirs start empty, hold at most **2,000 mana**, and regenerate **0.50 mana per game tick** (10/second, 600/minute) while loaded. Ordinary unloaded chunks do not catch up.

On first load after migration, any saved independent balance inside a geyser transfers once into the capped shared pool and is cleared locally.

Chunks inside a mana geyser instead use one **shared geyser reservoir**. They have no independent regeneration or spendable local balance. Every chunk in the full footprint contributes 2,000 capacity and 0.50 mana/tick to the shared pool, whether discovered, loaded, or not. Once the geyser is discovered, its ledger continues virtual generation and refinement while its terrain is unloaded. Time means server game time; closing the server pauses simulation.

`WorldMana.inspect/add/extract/tryExtract` route automatically to the correct reservoir on the server thread. The inspect snapshot reports the shared capacity and collective regeneration for a geyser chunk. Rates in the snapshot and `/riftfall ambient inspect` are **per second**, explicitly converted from the per-tick simulation rate.

See [geyser-regions.md](geyser-regions.md) and [mana-crystals.md](mana-crystals.md). There are no ley lines, ore regeneration bonuses, or crystal regeneration bonuses.
