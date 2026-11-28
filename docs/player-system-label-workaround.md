# Temporary Player System label workaround

Player System 0.0.11 emits `player_system.bonus.overflow` in the Bonuses tab but
does not include its English translation. Riftfall supplies only that missing
entry in `assets/player_system/lang/en_us.json`, displayed as **Overflow
absorption**. This describes the absorption supplied by the Overflow passive;
it does not change the bonus or its source label.

Minecraft merges language entries from resource packs, so the rest of Player
System's translations continue to come from the dependency.

Remove that language file and this note once the installed Player System version
provides its own translation for `player_system.bonus.overflow`.
