# Biome arrival titles

Entering a biome displays its localized name in a large, warm-white serif title, with a soft fade and thin tapered horizontal rules inspired by Dark Souls area names. The bundled font is **EB Garamond**, licensed under the SIL Open Font License (included beside the font). This is an open-source Garamond rather than a bundled copy of Adobe's proprietary font. Unsupported characters fall back to Minecraft's default font.

All registered biomes are supported, including the mana-geyser rings and other mods' biomes. Names use normal `biome.<namespace>.<path>` translations and can be changed by resource packs. Untranslated biome IDs receive readable names. The display has its own HUD layer, leaving story/command titles alone, and hides with F1.

A biome must remain current for 30 client ticks before being announced; at least 200 ticks separate announcements. Brief boundary crossings do not queue stale titles. Names fade in over 20 ticks, hold for 60, and fade out over 30. Disconnecting or changing dimension resets biome tracking; pausing singleplayer pauses the display. No dimension titles, sounds, or commands are added.

Disable locally with `enabled = false` in `config/riftfall-titles-client.toml`. No server setup or additional mod dependency is needed.
