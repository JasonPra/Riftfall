# Weighted combat behavior engine

The engine is separate from goblins. Existing goblin confidence and tactics have not been migrated. `riftfall:behavior_tester` is the first independent consumer. It uses a vanilla humanoid/zombie-texture placeholder, but is a living diagnostic mob with no natural spawn rules, special loot, or Epic Fight patch. Its unarmed attack is a basic vanilla punch; this prototype is for decisions and action execution, not weapon animation or collider testing.

## Try it in game

Get a **Behavior Tester Spawn Egg** from the Spawn Eggs creative tab, or use:

```mcfunction
/summon riftfall:behavior_tester ~ ~ ~
```

Use survival mode for targeting. The tester ignores creative and spectator players. The overhead label shows the action, normalized confidence, and raw confidence points. Spawn testers a few blocks apart on open ground:

1. One healthy, unarmed tester has zero confidence points and retreats when approached within eight blocks. At a safe distance it watches instead of immediately charging back.
2. Add three nearby testers. Each available ally contributes eight points, so the observer gets 24 points (60% confidence). It approaches and punches within 1.8 blocks, with a 25-tick cooldown and base damage two.
3. Remove allies or move them outside its sensing area: confidence falls, changing its decision. Retreating testers contribute only 25% of their normal ally weight. This can cause a group to lose its nerve together.
4. Wound a tester below 35% health: retreat gains enough utility to outweigh attacking, even with high confidence.
5. Break line of sight: it searches toward the last position it actually saw, then stops after five seconds without another sighting. It does not pursue the target's live hidden coordinates.

Equipment and target vulnerability use the existing confidence configuration. Testers recognize one another as allies by default; scoreboard teams can change that relationship. AI-disabled testers do not contribute. The demo defaults its own ally weight to eight; adding `"riftfall:behavior_tester=..."` to `combatConfidence.unitWeights` overrides it, including zero.

For all inputs and eligible action scores:

```mcfunction
/data get entity @e[type=riftfall:behavior_tester,sort=nearest,limit=1] NeoForgeData.RiftfallBehavior
```

`Action` is the chosen behavior. `Scores` contains eligible candidates, `Signals` contains situational inputs, and `Contributions` explains the confidence calculation. Debug state is server-produced, not used as saved AI state. The nameplate refreshes every five ticks.

## Integration for a new mob

Create one controller and one goal per entity. Do not share the controller or mutable action implementations between entities. A rule profile/engine is immutable and can be shared.

```java
var controller = new CombatBehaviorController(this, ConfidenceConfig::settings);
var policy = new BehaviorEngine(List.of(
    new BehaviorRule("wait", 0, Map.of(), c -> true),
    new BehaviorRule("pursue", 10, Map.of("confidence", 20D),
        c -> c.active("target.visible"))
), 2);

goalSelector.addGoal(1, new WeightedBehaviorGoal(controller, policy, Map.of(
    "wait", situation -> getNavigation().stop(),
    "pursue", situation -> {
        var position = situation.lastSeenPosition();
        if (position != null)
            getNavigation().moveTo(position.x, position.y, position.z, 1);
    }
)));
```

This is the initialization point. Add the new species' entity ID to `combatConfidence.unitWeights` (or supply its own confidence profile) to give nearby members a support weight; unknown entity IDs contribute zero. Your mob still defines factions through `isAlliedTo`, selects potential enemies with ordinary target goals, and supplies its own movement/attack implementations. The controller rejects dead, allied, creative, spectator, and cross-dimension targets. This does not automatically give other mobs goblin behavior.

For custom sensing, construct `CombatConfidenceController` with an ally predicate, an availability multiplier function, an extra-confidence-factors callback, sensing radius, and scan interval. Pass it to the extended `CombatBehaviorController` constructor to set memory duration and an extra-situational-signals callback. This supports species-specific fears, leaders, equipment, and conditions without editing the engine. Add matching weights to the confidence profile when introducing new confidence factors.

## Responsibilities

```text
ConfidenceProfile / ConfidenceEngine
    Pure weighted confidence calculation (existing API)
         ↓
CombatConfidenceController
    Per-actor allied support scan/cache + equipment/health/status inputs
         ↓
CombatBehaviorController
    Visibility, short-term target memory, normalized situation signals
         ↓
BehaviorEngine + BehaviorRule
    Eligibility → weighted utility scores → selected action
         ↓
WeightedBehaviorGoal + BehaviorAction
    stop old action → start new action → tick selected action
```

The controller scans support in a 16-block horizontal / four-block vertical area every ten ticks (or on target changes). Allies busy fighting another enemy are excluded while an enemy is visible. Equipment and current visible-target conditions are evaluated each update. Target visibility and memory update every AI tick; visibility is limited by the mob's follow range.

The standard signal keys are `confidence`, `fear`, `confidence.points`, `bold`, `self.health`, `self.wounded`, `allies`, `target.present`, `target.visible`, `target.remembered`, `target.distance`, `target.health`, `target.poison`, and `target.wither`. Health values are fractions. Boolean signals use zero/one. Hidden-target distance is to its last seen position; hidden health/status signals are zero. Callbacks can add other named signals.

## Utility selection

A rule is eligible only when its predicate passes. Its score is:

```text
base + Σ(signal value × rule weight)
```

Highest eligible score wins. Profile order breaks initial ties. An eligible current action stays selected unless a challenger exceeds its score by the switch margin (two in the demo). If the current action becomes ineligible, the engine switches immediately. Negative scores are allowed; zero does not mean ineligible. If no rule is eligible, no action executes.

The demo policy is defined in `BehaviorTestProfile`, not hardcoded into the engine:

| Action | Requirement | Score |
|---|---|---|
| Idle | No visible or remembered target | 0 |
| Search | Remembered target without visibility | 10 + confidence × 5 |
| Watch | Visible target, wounded or confidence < .45, distance ≥ 8 | 5 + fear × 30 + wounded × 50 |
| Approach | Visible target farther than 1.8 blocks | 10 + confidence × 20 |
| Attack | Visible target within 1.8 blocks | 20 + confidence × 20 |
| Retreat | Visible target within 8 blocks, and wounded or confidence < .45 | 15 + fear × 30 + wounded × 50 |

Action callbacks own pathfinding and combat. Override `start`, `tick`, and `stop` for stateful actions; stop should clean up navigation/flags that the action owns. The goal claims MOVE, LOOK and JUMP, so higher-priority goals such as floating can interrupt it. Target acquisition remains a separate responsibility. Registered rule IDs must have matching action implementations; incomplete integrations fail at construction.

This is an initial combat behavior engine. Formation allocation, weapon colliders, spell selection, and goblin migration are separate extensions, not behavior implicitly supplied by the engine.

## Harmful effects on the actor

Self-inflicted vulnerabilities now reduce confidence immediately: poison costs eight points, wither ten points, and every other distinct effect classified as `HARMFUL` costs four points. This includes modded harmful effects. Beneficial and neutral effects do not contribute. Penalties stack with each other and low health; poison/wither are excluded from the generic count so they are not counted twice. Amplifier and remaining duration do not multiply the penalty. Clearing an effect removes its contribution on the next update.

Tune `self.poison`, `self.wither`, and `self.harmful_effect` in `combatConfidence.factorWeights`. Existing config files receive these defaults in memory when their older lists lack the entries; add an explicit `=0` entry to disable a penalty. Other configured weights are preserved. Independently supplied profiles must define their own desired weights.

To test, spawn four nearby testers and apply effects to the nearest one:

```mcfunction
/effect give @e[type=riftfall:behavior_tester,sort=nearest,limit=1] minecraft:poison 30 0
/effect give @e[type=riftfall:behavior_tester,sort=nearest,limit=1] minecraft:wither 30 0
/effect clear @e[type=riftfall:behavior_tester,sort=nearest,limit=1]
```

The nameplate and `Contributions` show the changes. Damage can separately trigger the existing low-health penalty. These inputs are shared by the test engine and the current goblin confidence adapter.
