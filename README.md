# Riftfall

Minecraft **1.21.1 / NeoForge**. This branch is a fresh gameplay foundation containing the goblins, their equipment and combat behaviors, and the Enderman rendering fixes. The previous rift, dungeon, soul essence, and crystal systems have been removed.

## Goblins

- **Warrior:** the Goblin Shortsword chains a regular cut, a reverse cut, then the one-handed four-cut figure-eight flurry when attacks continue. The flurry works with a shield too; the shield stance retains its overhead cut as a fourth combo attack. Without a shield, the regular 0.6-second swipes keep the empty arm relaxed. Warriors also retain high-ground plunges. Only warriors without a shield equipped can use rear grapples; equipping a shield also releases an ongoing grapple. Mash the attack button to escape a grapple. The flurry keeps a short walking shuffle throughout, transfers rotation through the hips and chest, and snaps out of each loaded pose into a fast cut with follow-through. A collision-aware native animation step carries it forward slightly. The one-handed flurry uses native Epic Fight short stuns and impact: accepted stuns on low-poise targets last at least 0.35 seconds, connecting consecutive landed cuts through the 1.4-second animation. Shield blocks, stun immunity, and native poise checks still apply. Each flurry cut deals 65% of normal weapon damage. These stances are also registered on the shortsword’s Epic Fight capability for players; equipping other weapons retains their original basic combo. New warriors carry the shortsword; existing default stone swords migrate on load. The shortsword has stone-sword damage and durability and is available in the Combat tab.
- **Archer:** bow attacks, panicked retreats when unsupported, cover for retreating warriors, formation positioning behind warriors, and reachable elevated firing positions.
- **Shaman:** Iron's Spells casting through the Bone Staff, two close-range staff attacks: a continuous 0.6-second wind-up into an accelerating sweep (3 hearts base damage, original knockback) or a dramatic two-handed overhead smash with a 0.8-second wind-up (4½ hearts base damage). The smash leans back before snapping forward into a full ground-reaching follow-through; physical ground contact produces an ender-purple dust cloud with no area damage. Staff hits also emit a cosmetic purple impact burst to convey the knockback. Shamans favor overhead attacks against nearby stunned targets or targets committed to long Epic Fight attacks, closing up to six blocks for those openings with the same wind-up timing. Shamans also heal allied goblins. Bone helmet, tooth necklace, and primitive clothing are retained.
- Weighted confidence controls boldness, panic, retreat, and trembling; nearby warriors contribute 5 points, archers 1, and shamans 10, alongside personal equipment and situational modifiers. Confident groups with at least two actual warriors can flank and surround shared enemies. See the [calculation tree and configuration](docs/combat-confidence.md). Warriors screen archers. Larger groups and weakened enemies make goblins bolder. Combat animations retain the head and limb tremor. Its strength scales from **2× at zero confidence** down to **0.35× at full confidence**, using the same server confidence that drives retreat and boldness. Client smoothing prevents abrupt changes when allies join or leave. The torso and root stay steady.
- Zombies and skeletons hunt goblins; goblins recognize them as enemies.
- Goblins retain their approximately 1.5-block height, models, textures, held-item alignment, Epic Fight attacks, and animations.

Goblins are available through spawn eggs and `/summon`. This reset adds no natural spawning or dungeon-based spawning.

## Equipment

- **Bone Staff:** the shaman's casting weapon and melee club.
- **Goblin Buckler:** a small octagonal wood-and-bone shield with 180 durability. Warriors have a one-in-three chance to spawn holding one, including when spawned from eggs. This uses normal spawn initialization, ready for future natural spawning; archers and shamans do not receive shields. The buckler attaches to the forearm with rear straps. Shield-bearing warriors raise it while approaching visible enemies, using an Epic Fight blocking pose alongside movement and flanking. They lower it for sword attacks, plunges, grapples, and stuns, then raise it again between attacks.

Both items are in the Combat creative tab. Goblin spawn eggs are in Spawn Eggs. Existing item and mob registry names are preserved.

```mcfunction
/summon riftfall:goblin
/summon riftfall:goblin_archer
/summon riftfall:goblin_shaman
/give @s riftfall:bone_staff
/give @s riftfall:goblin_buckler
/item replace entity @e[type=riftfall:goblin,limit=1,sort=nearest] weapon.offhand with riftfall:goblin_buckler
```

Shamans drop bones instead of the removed soul essence crystal item. Previously spell-trained living Pillagers retain their casting behavior; no system grants new rift-based caster upgrades.

## Weapon collision and debugging

Custom goblin sword combos, plunges, staff attacks, and grapple stabs use Epic Fight `AttackAnimation` phases and swept weapon colliders. Distance only starts an attack or drives navigation; it never awards a melee hit. The equipped weapon collider must intersect a target hurtbox during the contact phase. Existing Epic Fight enemy attacks retain their native collision systems. Arrows and spells continue using their projectile collisions.

- **F8** or `/riftfall debug helditems`: toggle held-item **model bounds**, cyan for shields and gold for other items. These follow the rendered hand/forearm transforms in first and third person, including held swords, staffs, and bucklers.
- **F3+B** or `/riftfall debug colliders`: toggle entity hurtboxes and Epic Fight's native attack-collider visualizer. Attack colliders appear during attacks; these are the actual damage-contact shapes.
- `/riftfall debug off`: disable both overlays.

Shamans and warriors predict contact using the equipped Epic Fight weapon collider across the planned attack phases before winding up. The public `WeaponReach.canContact` helper supports future mob variants and add-on weapons without a fixed reach value. A shaman under close pressure moves in until that query succeeds; longer properly configured weapons can begin attacks farther away. Staff attack damage scales with the equipped weapon (1.2× sweep, 1.8× smash), preserving fully blocked shield hits. They commit to the swing after the warning; a target that moves out of the weapon path can still dodge it.

The Bone Staff's collider is editable in `data/riftfall/capabilities/weapons/bone_staff.json`: `center` is the joint-local offset, `size` contains half-extents before entity scaling, and `number` controls sweep samples. Model bounds are visual alignment aids, not damage or shield-blocking shapes. Shield defense still uses Minecraft's raised-shield frontal blocking rules. Debug toggles are client-only and need no operator permission.

## Enderman fixes

Outside combat, block-carrying Endermen use the correct vanilla carrying pose. In combat, the carried block is hidden and Epic Fight animations take priority. Hiding a previously visible block produces one purple ender-particle puff, suppressed immediately after a teleport. The actual held block is retained and becomes visible again after combat.

## Removed systems

There are no portals, pocket dimensions, dungeon construction or blocks, biome dungeon rosters, dungeon bosses or breaks, rift banks or power levels, replenishment, spacing checks, essence attachments or cultivation, crystals or crystal world generation, or `/rift` commands. The old server configuration and rift saved data are no longer read. No chunk scans or rift/essence server-tick handlers remain.

Use a new world for the new gameplay baseline. Removed blocks, items, and the arena dimension are no longer registered; existing world files are not automatically edited or deleted.

## Dependencies and development

Required integrations are retained: Epic Fight **21.17.3.1** (CurseForge file **8175609**), Iron's Spells 'n Spellbooks **1.21.1-3.16.3** (file **8680204**), and NeoForge GeckoLib **4.9.3** (file **8893490**). Gradle resolves their supporting dependencies.

Java **21** is required.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

Tests cover goblin combat geometry, squad positioning and confidence, grapple escape, combat tremor, spell eligibility, and Enderman block-hiding transitions. Existing combat previews are in [docs/previews](docs/previews).

The independent [weighted behavior engine and test entity](docs/behavior-engine.md) demonstrate reusable confidence sensing, situational awareness, and utility-based action selection. Warriors, archers and shamans now use this engine while retaining their established movesets and tactical actions.

[Reusable facial animation](docs/facial-animation.md) adds blinking, target-tracking eyes, idle glances and expression previews to all goblin roles, with separate controller/profile and skin-layout APIs for future mobs.

Panic and burrow escape: [behavior, tuning, rift flag and integration](docs/panic-escape.md).
