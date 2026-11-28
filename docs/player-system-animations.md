# Player System animation pass

The six supported humanoids now have their own Epic Fight idle, walk and close-range attack clips. Orcs use a weighted overhead axe stroke, death knights a controlled sword cut, gargoyles a low claw rake, rotting zombies a slower reaching swipe, and skeletons/kobolds quick close-range jabs. Walk cadence and torso posture vary by species. Walking uses Epic Fight's movement-animation speed handling; attack roots stay planted rather than sliding the entity through terrain.

Regular melee uses the equipped weapon collider and existing damage attributes. Its damage window follows the fast contact stroke, with a visible wind-up and recovery. Bow handling and other specialized weapon moves remain delegated to the existing Epic Fight patch. The custom close-range attack is selected for empty hands, swords and axes only. The original species data, spells and effects are not replaced.

## Orc warrior rig overhaul

The orc now has an authored Epic Fight mesh and its own 20-joint humanoid armature,
with rest frames sized for its broad chest, longer limbs, heavy shoulders and head.
Upper arms, forearms, thighs and shins are separate jointed pieces. Its default axe
has a dedicated Tool_R mesh part; explicitly equipping an item hides that built-in
axe and uses Epic Fight's normal held-item layer. The installed Player System skin
is retained. No GeckoLib renderer or geometry conversion is used for the orc.

Nine clips use this rig: idle, walk/chase, airborne pose, melee, hit reaction,
knockdown/recovery, spell wind-up, spell slam and death. The other five humanoids
retain their existing geometry adapter and animation sets.

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
model only for UV rectangles. It writes the dedicated mesh/armature and nine clips.
`tools/generate_player_system_animations.py` handles the other humanoids. Separate
wing, tail and cape articulation is still outside their shared biped rig.

## Encounter confidence

Goblins with `RiftfallInRift=true` receive the confidence factor `self.in_rift`, worth +20 points by default. This helps small groups engage without forcing the engage flag. Low health, status effects and witnessed ally deaths still apply, so retreat remains possible. Ordinary unmarked goblins do not receive it. Existing confidence configurations inherit the new default unless they explicitly set the factor, including an explicit zero. Rally remains a separate +40-point buff.

## Mixed replacement groups

Player System encounter goblins share composition across their dungeon instance, so separate rooms on the same tower floor do not each start with another shaman. Other spawn sites use a local group within 32 horizontal blocks and four vertical blocks. Pending bodies count before they enter the world index. The first three slots fill shaman, warrior and archer roles; the first warrior gets a buckler. Groups receive one shaman, with a second allowed after twelve warriors/archers, and never more than two living shamans per instance. Other slots split evenly between warriors and archers, with roughly half the additional warriors getting bucklers. A lost shaman can be replaced by a later spawn. Existing saved populations are not rerolled.

No additional spawn slots are created. A one-slot group can only contain the shaman; two slots add a warrior; all three roles require at least three slots. The selection runs before Player System applies its original species statistics and encounter/reward tracking.

Player System clears equipment when applying species statistics. The compatibility hook restores empty role slots after that step: staff for shamans, bow for archers, shortsword for warriors, and the selected buckler. Explicit nonempty equipment supplied by a datapack is preserved. The selected shield policy is stored with each replacement so a later stats application does not reroll it. Normal spawn eggs keep the ordinary goblin equipment rules.
