# Ambient chunk mana

Every loaded full chunk has an independent, persistent `riftfall:ambient_mana` attachment. New reservoirs start empty, hold at most 2,000 mana and regenerate 0.50 mana per second of loaded time. Unloaded chunks do not regenerate and there is no offline catch-up.

Server-thread APIs in `WorldMana` provide inspection, capped addition, bounded extraction and all-or-nothing extraction. Operators can inspect their current loaded chunk with `/riftfall ambient inspect`.

The geyser, ley-line and crystal prototypes have been removed. Legacy attachment data preserves the saved mana amount while ignoring former crystal bonuses. Old ley-line saved-data files are unused; this change does not delete world files.
