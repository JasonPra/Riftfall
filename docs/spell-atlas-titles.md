# Temporary Spell Atlas / Player System title bridge

Player System 0.0.11 and Spell Atlas keep separate title systems. The compatibility
mixins expose earned Spell Atlas titles in Player System's existing title cycle,
including previously earned titles and the Nemesis Hunter / Slayer / Bane series.
The bridge uses Spell Atlas's earned-title list and name components, including
school and bestiary titles. It refreshes Player System when Atlas sends its title
updates; an ordinary Player System view refresh also reads current Atlas progress.

Atlas titles are presentation entries, not grants to Player System's title state.
Selecting one calls Spell Atlas's own validated selection handler. Spell Atlas
alone applies its configured title bonus. The bridge never grants AP, currency,
XP, progression, or additional attribute modifiers. Repeated selection therefore
does not repeat an unlock reward. Unearned and malformed selection IDs are refused.

Selecting a Player System title clears the worn Atlas title and its bonus. Selecting
an Atlas title in either UI clears the selected Player System title. Clearing the
title in Player System also clears Atlas's selection. Earned titles remain owned
by their original mod. Atlas's titles toggle is respected, as is Player System's
awakening requirement. Player System's existing 64-title / 128-character view
limits still apply.

The three mixins are `PlayerSystemAtlasTitlesMixin`, `AtlasTitleSyncMixin`, and
`AtlasTitleNameMixin`; their helper is `AtlasTitleBridge`. Names are resolved using
Atlas's localized components at the point Player System requests a display label.
No upstream title definitions or reward tables are overridden. Remove these hooks
and their helper/tests once the installed mods supply equivalent integration.
