# Ry and the Magic Beasts faction

Ry is a territorial magical-beast NPC, registered as a creature with Minecraft's NPC marker rather than a monster. Its Epic Fight faction is **Magic Beasts**, with the persistent ID `riftfall:magic_beasts`. Other magical beasts can join the `riftfall:magic_beasts` entity-type tag and use `MagicBeastFaction.MAGIC_BEASTS` in their own Epic Fight patch. Ry will recognize them as allies.

Wild adults patrol their saved territory, growl at visible survival-mode players within six blocks, and attack if the intrusion continues for four seconds. Leaving warning range resets the warning. They proactively fight nearby zombies, skeletons, illagers and goblins, protect recently attacked packmates, and do not hunt ordinary forest animals. Creative and spectator players are excluded. Existing tamed Rys retain owner following, sitting and owner defence.

The same utility engine and local confidence/target-memory system used by goblins chooses Ry's actions. Ry has stronger baseline confidence, no penalty for natural weapons, and a smaller morale penalty for witnessed pack deaths. Healthy adults hold their ground. Adults retreat below 22% health, or below 45% when seriously outmatched; pups retreat from threats. Retreat prefers safer pack support and otherwise uses normal pathfinding away from the last observed threat. No hidden-target tracking or teleporting is used. Rys search the last seen position for up to five seconds, regroup, guard pups and wounded companions, and rest to recover slowly after combat. Selected actions, scores and reasons are available in `NeoForgeData.RiftfallBehavior` via `/data get entity`.

Epic Fight supplies an independent ten-joint quadruped armature, synchronized bite, casting charge/release and hit-reaction clips. The normal Ry model renders those joint poses over its native walking/sitting rig, preserving the detailed mane, tail, pups and ordinary wolf render layers. This is a direct pose bridge rather than substituting a humanoid or ravager model. Clip interpolation uses the render partial tick; each frame resets bind poses before native and Epic Fight animation are applied.

Gust uses Iron's actual spell API, mana costs and cooldown modifiers. A windup raises the head and braces the forelegs, opens the jaw into a howl, then releases the spell with a forward motion. Interruption stops the action; costs are charged only at release. Bites have a seven-tick telegraph before contact and a cooldown, with live reach, line-of-sight and allegiance checks at impact.

Regenerate the original armature and clips with `python3 tools/fauna/generate-ry-animations.py`. The approved implementation passed a clean release build and all 317 automated tests.

Focused policy tests passed (four tests). In-game checks confirmed the Magic Beasts patch and ten-joint rig, rejection of allied targets, interrupted casting without a release, a timed bite, retreat after critical injury, and recovery once safe. A client pose check confirmed nonzero head/jaw rotations from the synchronized animation. The clean release build subsequently passed all 317 automated tests.

[Cast/interruption preview](previews/ry-cast-interruption.png) — the red tint is the ordinary damage flash from the interruption check.
