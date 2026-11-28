# Player System animation pass

The six supported humanoids now have their own Epic Fight idle, walk and close-range attack clips. Orcs use a weighted overhead axe stroke, death knights a controlled sword cut, gargoyles a low claw rake, rotting zombies a slower reaching swipe, and skeletons/kobolds quick close-range jabs. Walk cadence and torso posture vary by species. Walking uses Epic Fight's movement-animation speed handling; attack roots stay planted rather than sliding the entity through terrain.

Regular melee uses the equipped weapon collider and existing damage attributes. Its damage window follows the fast contact stroke, with a visible wind-up and recovery. Bow handling and other specialized weapon moves remain delegated to the existing Epic Fight patch. The custom close-range attack is selected for empty hands, swords and axes only. The original species data, spells and effects are not replaced.

## Orc spell preservation

Player System still owns the orc's telegraphed special attack: its wind-up, cancellation, reach, damage multiplier, knockback, cooldown, sounds, particles and callback are unchanged. During the native telegraph and its release, the client temporarily delegates rendering to Player System's original GeckoLib renderer and animation controllers. In particular, `animation.orc_warrior.attack` is loaded unchanged from Player System's installed assets. This avoids hiding its spell animation behind an Epic Fight melee animation.

A presentation-only entity notification keeps the handoff synchronized, including players who start tracking a caster mid-cast. It expires automatically and clears on native cancellation. The Epic Fight melee selector waits while the native cast/release is active. Starting a native telegraph also stops any in-progress Epic Fight melee clip; it does not add an Epic Fight damage phase to the spell. Only Player System invokes the spell's damage callback.

During that handoff, the compatibility mixin skips Epic Fight's GeckoLib pre-render interception for the casting orc. The original GeckoLib render event remains available to other listeners. This prevents the humanoid patched renderer from receiving a GeckoLib renderer of an incompatible type.

The animation generator is `tools/generate_player_system_animations.py`, invoked with the pinned Epic Fight jar path. It requires Python and NumPy. It writes only `assets/riftfall/animmodels/animations/player_system/`; it does not overwrite the original GeckoLib assets. Separate wing, tail and cape articulation is still outside the shared biped rig.

## Encounter confidence

Goblins with `RiftfallInRift=true` receive the confidence factor `self.in_rift`, worth +20 points by default. This helps small groups engage without forcing the engage flag. Low health, status effects and witnessed ally deaths still apply, so retreat remains possible. Ordinary unmarked goblins do not receive it. Existing confidence configurations inherit the new default unless they explicitly set the factor, including an explicit zero. Rally remains a separate +40-point buff.

## Mixed replacement groups

Encounter goblins are selected against nearby living encounter goblins within 32 horizontal blocks and four vertical blocks. The first three available slots fill shaman, warrior and archer roles; the first warrior gets a buckler. Later slots favor warriors over archers 3:1, with roughly half the additional warriors getting bucklers. A lost shaman is replaced when another goblin slot spawns. Separate floors outside that vertical band form separate groups.

No additional spawn slots are created. A one-slot group can only contain the shaman; two slots add a warrior; all three roles require at least three slots. The selection runs before Player System applies its original species statistics and encounter/reward tracking.

Player System clears equipment when applying species statistics. The compatibility hook restores empty role slots after that step: staff for shamans, bow for archers, shortsword for warriors, and the selected buckler. Explicit nonempty equipment supplied by a datapack is preserved. The selected shield policy is stored with each replacement so a later stats application does not reroll it. Normal spawn eggs keep the ordinary goblin equipment rules.
