# Weighted combat behavior engine

The engine is separate from goblins. Warriors, archers, and shamans now use its shared confidence, awareness, action selection, and diagnostics. `riftfall:behavior_tester` is the first independent consumer. It uses a vanilla humanoid/zombie-texture placeholder, but is a living diagnostic mob with no natural spawn rules, special loot, or Epic Fight patch. Its unarmed attack is a basic vanilla punch; this prototype is for decisions and action execution, not weapon animation or collider testing.

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

This is an initial combat behavior engine. Formation allocation, weapon colliders, and spell execution remain species/action implementations, not behavior implicitly supplied by the engine.

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

## Witnessing an ally's death

An engine participant that sees a nearby ally killed by an enemy loses confidence temporarily. Defaults: **−8 points per witnessed death, 200 ticks (10 seconds), up to three active incidents, within 24 blocks**. The observer must be alive and AI-enabled, consider the victim an ally and the killer an enemy, and have direct line of sight to the victim at death. This is a line-of-sight check, not a forward-facing view cone. Projectile kills use the owning attacker. Environmental deaths, suicides, allied killers, and blocked/distant deaths do not add this penalty. Losing an ally's ordinary support is separate and can still happen without witnessing its death.

Each incident expires independently; a later death does not extend an older incident. At the cap, the newest incident replaces the oldest. Changing strength or cap affects current memories; changing duration/radius affects new incidents. Zero max stacks disables and clears the memory; duration zero prevents new incidents. Memories are temporary per-instance state, not persisted across unload/reload.

This event input is opt-in. The tester implements `ConfidenceParticipant` and returns its `CombatConfidenceController` from `confidenceController()`. Future entities using the controller can implement the same interface. All three goblin roles also implement this hook and consume witnessed deaths.

## Per-species confidence tuning

The server config supports `entityOverrides` inside `[combatConfidence]`. The entity ID before `|` is the **observer's species**, not the victim's. For example:

```toml
entityOverrides = [
  "riftfall:behavior_tester|factor.self.witnessed_ally_death=-6",
  "riftfall:behavior_tester|witnessDurationTicks=100",
  "riftfall:behavior_tester|witnessMaxStacks=2",
  "riftfall:behavior_tester|witnessRadius=20",
  "riftfall:behavior_tester|factor.self.poison=-12",
  "riftfall:behavior_tester|factor.self.wither=-15",
  "riftfall:behavior_tester|boldPoints=30"
]
```

Other supported keys: `normalizationPoints`, `woundedHealthFraction`, `vulnerableTargetHealthFraction`, `retreatingAllyMultiplier`, `factor.<any named factor>`, and `unit.<ally entity ID>`. For instance `unit.riftfall:goblin_shaman=15` changes how much shaman support this observer receives. Overrides preserve unmentioned values and never change another species' profile. Explicit zero weights disable the corresponding bonus/penalty. Invalid values are rejected by config validation. Config changes reload during play.

Global defaults remain `factorWeights`, `unitWeights`, and the normal threshold settings, plus `witnessRadius`, `witnessDurationTicks`, and `witnessMaxStacks`. Every `CombatConfidenceController` resolves the observer's overrides automatically, including when its owner supplies a custom base profile. Custom confidence users that bypass the controller must explicitly call `ConfidenceConfig.settingsFor(entityId)` to resolve these overrides.

## Detailed tester inspection

```mcfunction
/riftfall behavior inspect @e[type=riftfall:behavior_tester,sort=nearest,limit=1]
```

This read-only command requires operator permissions and prints only to the requester. It reports:

- Chosen action, previous action, confidence, raw points, bold threshold and normalization scale.
- Actual health/max health, held item IDs, and active effect IDs/categories/amplifiers/remaining ticks.
- Each active confidence factor's input, configured weight, and point contribution; all configured factor/ally weights are also included, so zero or inactive weights can be checked.
- Individual allies by UUID/type, cached distance, availability multiplier, weight and resulting support; nearby rejected candidates include reasons such as AI disabled, not allied, dead, or fighting another enemy.
- Witnessed-death victim/killer identifiers, remaining ticks per incident, and effective radius/duration/stack limit.
- Every situational signal; all candidate behaviors, eligibility, base score, weights, weighted terms and total score—even ineligible candidates. Only eligible candidates can win.
- Switching margin, target-memory duration/remaining ticks, last seen coordinates and the snapshot tick.

Structured equivalents live under `NeoForgeData.RiftfallBehavior`; `ConfidenceDetails`, `BehaviorEvaluations`, `Signals`, and `Contributions` can be queried separately with `/data get`. Detailed snapshots update every five ticks and on action changes; support candidates retain their ten-tick scan cache, with last/next scan ticks recorded. The compact overhead label remains a summary. Behavior eligibility predicates and species-specific action implementations remain defined by their behavior profile/code rather than these confidence overrides.

For a witnessed-kill test, use an actual enemy attack (for example, kill a tester with your weapon). `/kill` has no attacking enemy and deliberately does not simulate this penalty. Check `self.witnessed_ally_death` and `WitnessedDeaths` to distinguish temporary fear from the separate loss of ordinary ally support.


## Goblin migration

All goblin roles register one `WeightedBehaviorGoal` for action selection, alongside the higher-priority vanilla floating goal. The previous parallel combat-goal priority scheduler and the confidence tally in `GoblinSquadTactics` are removed. Ordinary target acquisition, ally assistance, and selecting an enemy threatening a retreating warrior remain target goals; they do not schedule movement or attacks.

`GoblinBehavior` supplies per-entity integration and situational readiness signals. `GoblinBehaviorProfile` defines utility rules. Existing movement/attack executors are wrapped by reusable `GoalBehaviorAction`; they are **not** independently registered in a goal selector. The weighted engine starts, ticks, and stops the selected executor. Swing phases, spell charge/release timing, grapple escape handling, and cooldowns remain local execution details so the established movesets are preserved.

Warriors retain swipes/backswings/flurries, shield restrictions on grappling, high-ground plunges, surrounding/screening, low-health retreat and buckler behavior. Archers retain imperfect panic retreat, high-ground/rear positioning, friendly-fire avoidance and covering withdrawing warriors. Shamans retain both staff moves, healing allies, and offensive spellcasting. Committed attack/casting phases finish unless interrupted or invalidated, rather than switching actions mid-animation. While committed, the integration skips preparing unrelated pathfinding-heavy actions.

`GoblinSquadTactics` now only allocates physical formation positions and implements associated movement helpers; it does not calculate confidence. Its confidence accessors read the shared controller's current snapshot. Formation-aligned warriors close past coarse block-centered path arrival until their actual equipped Epic Fight collider can contact, avoiding the previous short-weapon stall. No animation assets, weapon colliders, damage definitions or grapple escape rules are replaced by the migration.

Hidden enemies cannot start new attacks or drive live-coordinate pursuit: goblins use the controller's last-seen-position search and five-second memory. Already committed attacks may complete their current motion. The shared confidence controls existing fear trembling, retreat thresholds, and organized tactics, including self-status penalties, visible-target vulnerability, witnessed losses and per-species config overrides.

Inspect each role with:

```mcfunction
/riftfall behavior inspect @e[type=riftfall:goblin,sort=nearest,limit=1]
/riftfall behavior inspect @e[type=riftfall:goblin_archer,sort=nearest,limit=1]
/riftfall behavior inspect @e[type=riftfall:goblin_shaman,sort=nearest,limit=1]
```

Additional signals include `ready.<action>`, `committed.<action>`, `self.interrupted`, `cover.safe`, and `target.exposed`. The diagnostic candidate evaluations show which goblin action was eligible and how its utility was calculated. The tester remains available as an independent consumer for isolating engine changes from goblin-specific execution.
