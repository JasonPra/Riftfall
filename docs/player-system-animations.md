# Player System animation pass

The six supported humanoids now have their own Epic Fight idle, walk and close-range attack clips. Orcs use Epic Fight’s standard biped one-handed tool attacks, death knights a controlled sword cut, gargoyles a low claw rake, rotting zombies a slower reaching swipe, and skeletons/kobolds quick close-range jabs. Walk cadence and torso posture vary by species. Walking uses Epic Fight's movement-animation speed handling; attack roots stay planted rather than sliding the entity through terrain.

Regular melee uses the equipped weapon collider and existing damage attributes. Its damage window follows the fast contact stroke, with a visible wind-up and recovery. Bow handling and other specialized weapon moves remain delegated to the existing Epic Fight patch. The custom close-range attack is selected for empty hands, swords and axes only. The original species data, spells and effects are not replaced.

## Orc warrior rig overhaul

The `riftfall:orc_warrior` is a standalone Monster entity with no GeckoLib animation
controllers. Player System's original orc body is replaced before spawn setup or
saved NBT loads. Rifts, dungeons and tower floors therefore retain their existing
slots, species statistics, rewards and identity. Already loaded orcs convert when
their chunks reload; saved UUIDs and encounter metadata are retained.

The authored mesh uses Epic Fight's exact standard 20-joint biped bind pose.
Upper arms, forearms, thighs and shins are jointed pieces. The equipped iron axe
uses the normal Tool_R item attachment and Epic Fight's HUMANOID_ONEHAND_TOOLS
moveset. There is no baked-in duplicate axe. Species equipment application restores
an empty axe slot while respecting explicitly equipped alternative items.
The installed Player System skin is retained.

Authored idle, walk/chase, airborne, hit, knockdown, spell wind-up/slam and death
clips use that same biped rig. The other five humanoids retain their adapter.

Player System still owns the spell's telegraph, target checks, damage multiplier,
knockback, cooldown, cancellation and callback. Its native start/release/cancel
hooks play Epic Fight presentation clips without adding an attack phase or another
source of spell damage. Wind-up playback scales to the native wind-up duration.
Tracking notifications carry remaining time so late clients can join the current
pose. No renderer swap occurs during the cast.

Death uses a finite, non-looping collapse clip. Death takes priority over cast
presentation, and late cast starts/releases or cancellations cannot restart an idle
loop on a dead orc. The normal entity death/removal path runs; no deathTime reset or
corpse-retention override is introduced.

Regenerate the orc with `tools/generate_orc_warrior.py EPIC_FIGHT_JAR PLAYER_SYSTEM_JAR`
(Python and NumPy). The script authors its own cuboids and reads the installed
model only for UV rectangles. It writes the mesh with the standard biped bind pose and the authored clips.
`tools/generate_player_system_animations.py` handles the other humanoids. Separate
wing, tail and cape articulation is still outside their shared biped rig.

## Encounter confidence

Goblins with `RiftfallInRift=true` receive the confidence factor `self.in_rift`, worth +20 points by default. This helps small groups engage without forcing the engage flag. Low health, status effects and witnessed ally deaths still apply, so retreat remains possible. Ordinary unmarked goblins do not receive it. Existing confidence configurations inherit the new default unless they explicitly set the factor, including an explicit zero. Rally remains a separate +40-point buff.

## Mixed replacement groups

Player System encounter goblins share composition across their dungeon instance, so separate rooms on the same tower floor do not each start with another shaman. Other spawn sites use a local group within 32 horizontal blocks and four vertical blocks. Pending bodies count before they enter the world index, including the NoAI mobs Player System creates while a floor waits to activate. The first slots fill warrior, archer, then shaman roles; a one- or two-slot pack stays combat-focused and the first warrior gets a buckler. Groups of three or more receive one shaman, with a second allowed after twelve warriors/archers, and never more than two living shamans per instance. Other slots split evenly between warriors and archers, with roughly half the additional warriors getting bucklers. A lost shaman can be replaced by a later spawn. Existing saved populations are not rerolled.

No additional spawn slots are created. A one-slot group contains a warrior; two slots add an archer; all three roles require at least three slots. The selection runs before Player System applies its original species statistics and encounter/reward tracking.

Player System clears equipment when applying species statistics. The compatibility hook restores empty role slots after that step: staff for shamans, bow for archers, shortsword for warriors, and the selected buckler. Explicit nonempty equipment supplied by a datapack is preserved. The selected shield policy is stored with each replacement so a later stats application does not reroll it. Normal spawn eggs keep the ordinary goblin equipment rules.

Tower population reserves one three-goblin warrior pack when the floor’s original
spawn table permits `goblin_scavenger` at its current rank. The three slots become
a warrior, archer and shaman. This replaces one existing pack selection inside the
normal mob cap; it does not add a second population pass, alter the remaining
weighted rolls, expand rank eligibility or substitute hobgoblin entries.
