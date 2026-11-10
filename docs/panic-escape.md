# Panic, fleeing and burrow escape

Warriors and archers now have a separate panic lifecycle, driven by the shared weighted behavior engine. Shamans remain exempt. Ordinary low-confidence retreat, formations, regrouping, healing retreat and existing attacks are still available whenever panic escape is not selected.

## Default behavior

1. A combat threat and confidence at or below **-15 raw points for 20 ticks** trigger panic. This is distinct from the existing normalized `fear = 1 - confidence` scoring signal.
2. Panicked goblins automatically use the **scared facial expression**, with target tracking and blinking intact. Temporary `/riftfall face` preview overrides still take precedence. While afraid, each goblin repeats its entity-bound fear scream at a new random pitch from **1 / 1.5 (about 0.67) to 1.5** each time. Clips finish before repeating, with a short randomized pause. Fear ending, death, removal, silence or leaving the world stops the current fear clip. Completing a burrow instead stops new clips and holds the goblin underground until its current clip finishes. The separate death sound cuts off fear playback. This also works for frightened goblins inside a rift.
3. Outside a rift, panic escape wins over ordinary combat actions. Goblins turn and run at navigation speed **1.5**, plan reachable routes, and score destinations against the nearest of their known threats. Threats include players and non-player enemies/attackers, not just the player.
4. At least **12 blocks** from all known threats, they may kneel and dig for **50 ticks (2.5 seconds)** on supported ground in `riftfall:burrowable`. Epic Fight supplies a custom panic run and a hands-and-knees digging animation: planted knees, alternating free-hand scoops and downward knife scrapes. A dense rear cloud and backward-thrown clods use the actual block being dug, changing material as the goblin reaches the next soil layer. Digging sounds accompany the strokes. The model sinks during the final part of the animation.
5. Once digging starts, proximity alone does not interrupt it. Damaging/stunning an exposed goblin, losing safe ground, or blocking escape interrupts digging and resets its progress. Epic Fight lock-on and its focus/highlight release once the goblin is about halfway concealed (75% of the full animation), and cannot reacquire it while concealed. Normal attacks and Epic Fight hits cannot damage it once fully buried (the final 10% of the animation and the audio wait); it is also no longer pickable or pushable. Administrative damage such as `/kill` remains available. Successfully finishing the digging enters an underground wait: progress stays at 100%, the final animation pose is held, digging particles stop, and the current voice finishes before `discard()`. Approaching after the completed dig does not pull the goblin back above ground. Each listening client reports fear clip completion; bounded leases expire if a listener disconnects, and an in-flight hurt clip also delays removal. A short six-tick delivery grace covers the final synchronized state update. This is an escape, with no death, loot or XP event. Blocks are not excavated or destroyed.
6. Recovery needs confidence at or above **5 raw points for 60 ticks**, and at least **100 ticks** since panic began. Hysteresis prevents flickering between panic and normal actions. An unfinished interrupted dig must begin its entire animation again.

Terrain eligibility requires both the explicit `riftfall:burrowable` natural-soil allowlist and finite block hardness from **0 to 0.8 inclusive**. Dirt, grass blocks, coarse/rooted dirt, podzol, mycelium, sand/red sand, gravel, clay, mud, moss, snow blocks and soul sand/soil are included. Wood, stone/cobblestone, sandstone, netherrack, glass, wool and other crafted materials are excluded even if they are weak. The entry column must stay diggable through the goblin's sinking depth (at least two blocks), so a thin soil layer over stone cannot be used to escape. Ground support, fluids and interruptions are rechecked throughout the animation. A data pack can append additional soft natural soil to the tag, but the hardness ceiling still applies.

During digging, the item layers show a cosmetic knife in the working hand and keep the free hand empty. Bows and shields are visually stowed; their actual inventory stacks, durability, combat capabilities and loot are unchanged. Normal item rendering returns automatically on interruption. Goblins on unsuitable terrain continue trying to flee instead of silently disappearing.

## Future rift hook

No rift mechanics have been implemented. `GoblinEntity.isInRift()` and `setInRift(boolean)` read/write the persistent `RiftfallInRift` flag.

- **In a rift:** panic/fear still exists and the scared face still plays. The escape goal is ineligible, so existing retreat/regrouping/combat policies handle movement. No panic run, digging or escape despawn occurs.
- Setting the flag during digging aborts it immediately on the next behavior update. Escape permission is checked again on the completion tick.
- Clearing the flag permits escape again if the goblin is still panicking. Flags set before unload survive reload; transient panic/dig progress does not.

For a temporary test:

```mcfunction
/data merge entity @e[type=riftfall:goblin,sort=nearest,limit=1] {NeoForgeData:{RiftfallInRift:1b}}
```

Use `0b` to clear it. No dimension-name or nonexistent rift-system checks are assumed.

## Tuning and diagnostics

The existing server `riftfall-server.toml` `[combatConfidence]` section now includes:

| Key | Default |
|---|---:|
| `panicEntryPoints` | -15 |
| `panicRecoveryPoints` | 5 |
| `panicEntryTicks` | 20 |
| `panicRecoveryTicks` | 60 |
| `panicMinimumTicks` | 100 |
| `panicEscapeDistance` | 12 |
| `panicBurrowDangerDistance` | Legacy setting; proximity no longer cancels a started dig |
| `panicBurrowTicks` | 50 |
| `panicFleeSpeed` | 1.5 |

The same keys are supported in `entityOverrides`, e.g. `riftfall:goblin_archer|panicFleeSpeed=1.6`. Recovery points must exceed entry points; the danger distance must be smaller than the start distance. Animation playback follows the configured digging duration, synchronized to clients. The client also recovers the Epic Fight burrow action from synchronized entity state if the start message was missed or another animation replaced it; late recovery seeks to the current burrow progress instead of starting upright. The full-body clip has highest priority, and normal walking, blocking and bow-aim poses are suspended while burrowing. Cancellation returns to recovery/ordinary movement without suppressing hit-stun reactions.

`/riftfall behavior inspect <entity>` and the existing behavior holograms show panic, the escape restriction, selected action and burrow progress. Persistent diagnostic fields include `RiftfallPanicking`, `RiftfallBurrowing`, `RiftfallBurrowProgress`, `RiftfallPanicEscapeAllowed`, `RiftfallPanicSafety` and `RiftfallBurrowWaitingForSound`.

## Reuse

- `behavior/PanicState`: pure lifecycle/timer/hysteresis logic, independent of Minecraft entities.
- `behavior/PanicSettings`: immutable tuning, supplied per species.
- `behavior/PanicEscapeGoal`: generic `PathfinderMob` movement/safety/escape executor. Its owner supplies eligibility, an escape-permission predicate, interruption checks, ground eligibility and visual callbacks.
- `GoblinBehavior` and `GoblinBehaviorProfile`: engine integration and policy priority.
- `GoblinPanicVisuals` / `GoblinPatch`: goblin animation, particles and synchronized pose binding.
- `FacialController` accepts an optional automatic expression supplier, so other behaviors/species can select expressions without hardcoding goblins into the face system.

## Validation

The pure tests cover sustained entry, recovery hysteresis, full animation duration, last-tick interruption, environment restrictions that preserve fear, per-species tuning, policy priority and facial override precedence. Native tests cover natural player-driven panic/fleeing/escape, in-rift fallback with a scared face, mid-dig flag changes, approach without interruption, actual damage interruption and underground protection, unsupported terrain, archers, shaman exemption and a non-player hostile enemy. Terrain checks cover natural soft blocks, low-hardness crafted blocks, hard rock and thin dirt over stone. Pose tests verify planted knees, ground-level hand strokes and a downward-facing knife. A native fault-injection check suppresses the initial server animation message and verifies that both warriors and archers recover the actual client digging clip from synchronized state.

## Hurt and blocked-hit voices

A surviving goblin plays the high-pitched hurt scream only when its health actually decreases after damage reductions. Fully blocked, fully absorbed and otherwise zero-health-damage hits use a low grunt instead. Shield damage is blocked before health is changed; no damage-and-heal workaround is used. The grunt streams a random half-second PCM window from the existing scream recording at 0.55–0.75 pitch, with short edge fades. No additional recording or generated audio asset is needed. Lethal hits use the separate death sound.
