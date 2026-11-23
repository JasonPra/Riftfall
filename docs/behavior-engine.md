# Goblin behavior engine

Warriors, archers and shamans share confidence sensing, awareness, weighted action selection and diagnostics. The independent demonstration mob has been removed; its sample policy exists only in unit tests.

Use `/summon riftfall:goblin`, `/summon riftfall:goblin_archer` or `/summon riftfall:goblin_shaman` to test actual goblins. Inspect the nearest goblin with `/riftfall behavior inspect @e[type=riftfall:goblin,sort=nearest,limit=1]` or `/data get entity @e[type=riftfall:goblin,sort=nearest,limit=1] NeoForgeData.RiftfallBehavior`.

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

The controller scans support in a 16-block horizontal / four-block vertical area every ten ticks (or on target changes). Allies busy fighting another enemy are excluded while an enemy is visible. Equipment and current visible-target conditions are evaluated each update. Target visibility and memory update on scheduled decisions and urgent state changes; visibility is limited by the mob's follow range.

The standard signal keys are `confidence`, `fear`, `confidence.points`, `bold`, `self.health`, `self.wounded`, `allies`, `target.present`, `target.visible`, `target.remembered`, `target.distance`, `target.health`, `target.poison`, and `target.wither`. Health values are fractions. Boolean signals use zero/one. Hidden-target distance is to its last seen position; hidden health/status signals are zero. Callbacks can add other named signals.

## Utility selection

A rule is eligible only when its predicate passes. Its score is:

```text
base + Σ(signal value × rule weight)
```

Highest eligible score wins. Profile order breaks initial ties. An eligible current action stays selected unless a challenger exceeds its score by the switch margin (configured by the profile). If the current action becomes ineligible, the engine switches immediately. Negative scores are allowed; zero does not mean ineligible. If no rule is eligible, no action executes.

A test-only sample policy in `BehaviorTestProfile` verifies the reusable engine. Production goblins use `GoblinBehaviorProfile`:

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

To test, spawn four nearby goblins and apply effects to the nearest one:

```mcfunction
/effect give @e[type=riftfall:goblin,sort=nearest,limit=1] minecraft:poison 30 0
/effect give @e[type=riftfall:goblin,sort=nearest,limit=1] minecraft:wither 30 0
/effect clear @e[type=riftfall:goblin,sort=nearest,limit=1]
```

The behavior inspection output and `Contributions` show the changes. Damage can separately trigger the existing low-health penalty. These inputs are consumed by the goblin confidence adapter.

## Witnessing an ally's death

An engine participant that sees a nearby ally killed by an enemy loses confidence temporarily. Defaults: **−8 points per witnessed death, 200 ticks (10 seconds), up to three active incidents, within 24 blocks**. The observer must be alive and AI-enabled, consider the victim an ally and the killer an enemy, and have direct line of sight to the victim at death. This is a line-of-sight check, not a forward-facing view cone. Projectile kills use the owning attacker. Environmental deaths, suicides, allied killers, and blocked/distant deaths do not add this penalty. Losing an ally's ordinary support is separate and can still happen without witnessing its death.

Each incident expires independently; a later death does not extend an older incident. At the cap, the newest incident replaces the oldest. Changing strength or cap affects current memories; changing duration/radius affects new incidents. Zero max stacks disables and clears the memory; duration zero prevents new incidents. Memories are temporary per-instance state, not persisted across unload/reload.

This event input is opt-in. All three goblin roles implement `ConfidenceParticipant`, expose their `CombatConfidenceController` through `confidenceController()`, and consume witnessed deaths.

## Per-species confidence tuning

The server config supports `entityOverrides` inside `[combatConfidence]`. The entity ID before `|` is the **observer's species**, not the victim's. For example:

```toml
entityOverrides = [
  "riftfall:goblin|factor.self.witnessed_ally_death=-6",
  "riftfall:goblin|witnessDurationTicks=100",
  "riftfall:goblin|witnessMaxStacks=2",
  "riftfall:goblin|witnessRadius=20",
  "riftfall:goblin|factor.self.poison=-12",
  "riftfall:goblin|factor.self.wither=-15",
  "riftfall:goblin|boldPoints=30"
]
```

Other supported keys: `normalizationPoints`, `woundedHealthFraction`, `vulnerableTargetHealthFraction`, `retreatingAllyMultiplier`, `factor.<any named factor>`, and `unit.<ally entity ID>`. For instance `unit.riftfall:goblin_shaman=15` changes how much shaman support this observer receives. Overrides preserve unmentioned values and never change another species' profile. Explicit zero weights disable the corresponding bonus/penalty. Invalid values are rejected by config validation. Config changes reload during play.

Global defaults remain `factorWeights`, `unitWeights`, and the normal threshold settings, plus `witnessRadius`, `witnessDurationTicks`, and `witnessMaxStacks`. Every `CombatConfidenceController` resolves the observer's overrides automatically, including when its owner supplies a custom base profile. Custom confidence users that bypass the controller must explicitly call `ConfidenceConfig.settingsFor(entityId)` to resolve these overrides.

## Detailed goblin inspection

```mcfunction
/riftfall behavior inspect @e[type=riftfall:goblin,sort=nearest,limit=1]
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

For a witnessed-kill test, use an actual enemy attack (for example, kill a goblin with your weapon). `/kill` has no attacking enemy and deliberately does not simulate this penalty. Check `self.witnessed_ally_death` and `WitnessedDeaths` to distinguish temporary fear from the separate loss of ordinary ally support.


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

Additional signals include `ready.<action>`, `committed.<action>`, `self.interrupted`, `cover.safe`, and `target.exposed`. The diagnostic candidate evaluations show which goblin action was eligible and how its utility was calculated. The reusable engine is also exercised independently by unit tests.

## Equipment threat and willingness to advance

Low confidence now prevents a warrior from voluntarily closing into melee. The old policy could select melee at any confidence as long as the warrior was healthy; a low score alone did not prevent approaching. Warriors now need `engagePoints` (default **12 raw points**) to advance. Below that threshold they watch from a distance or use the existing imperfect panic retreat when approached, and do not pursue remembered positions without sufficient confidence. If cornered at close range they can still defend themselves. Committed attacks finish before reassessing; wounds, stuns and invalid targets retain their interruption rules. Flanking/organized tactics still use the separate `boldPoints` threshold.

Visible opponents are compared using actual current attack-damage, armor, armor-toughness attributes and current health. These include equipped items' attribute modifiers, including Epic Fight weapons such as the iron greatsword. The generic confidence factors are:

| Factor | Input | Default weight | Maximum absolute contribution |
|---|---|---:|---:|
| `combat.damage_advantage` | (own damage − enemy damage) / 4, clamped to ±3 | +4 | 12 |
| `combat.armor_advantage` | (own armor − enemy armor) / 5, clamped to ±4 | +5 | 20 |
| `combat.toughness_advantage` | (own toughness − enemy toughness) / 4, clamped to ±3 | +3 | 9 |
| `combat.health_advantage` | (own health − enemy health) / 10, clamped to ±3 | +2 | 6 |

A stronger opponent produces negative contributions; better personal equipment produces positive ones. Ally support can outweigh the disadvantage. This is an observable-stat threat heuristic, not a prediction of exact damage, enchantments, DPS, or armor penetration. Hidden opponents do not supply live equipment or health inputs.

All four factor weights can be adjusted or disabled through global `factorWeights` or per-observer `entityOverrides`. `engagePoints` also supports a per-species override, for example `"riftfall:goblin|engagePoints=10"`. New factor defaults are supplied when older configuration lists lack them; explicit zero values are preserved. Independently supplied confidence profiles must define their desired comparison weights.

## Live overhead holograms

```mcfunction
/riftfall behavior debug on
/riftfall behavior debug off
```

`/riftfall behavior debug` toggles the display. These operator commands enable a personal world-space overlay over nearby engine entities, including Epic Fight-rendered goblins. They do not rename mobs, create display entities, or enable other players' overlays.

The display updates four times per second and shows current action, normalized confidence, raw points, advance threshold, self/opponent combat stats, every nonzero confidence contribution (green positive/red negative), and loss/search-memory timers. It covers the nearest 32 engine entities within 32 blocks. Session state clears on login/logout; received entries expire if updates stop and are matched by both entity ID and UUID. Debug text uses the vanilla see-through nameplate pass for readability, including while testing occlusion. Large custom profiles get an explicit overflow notice pointing to `inspect`, which remains the complete detailed view. Turning the overlay off clears it immediately.

## Retreating to an active healing circle

Wounded warriors, archers, and shamans now have a `seek_healing` recovery action. It uses the actual Iron's Spells `HealingAoe` entity from an allied goblin shaman's Healing Circle, including its current radius, owner, healing eligibility, delay and remaining lifetime. A visual target marker alone is not a healing destination. No new healing spell is introduced.

By default an actor starts seeking below 35% health, enters the reachable circle, and waits until 80% health or until the circle stops being usable. If it expires earlier, partial healing is still useful and normal decisions resume. A circle must have enough lifetime remaining for estimated travel plus a healing pulse. Recovery outranks ordinary combat and retreat but does not interrupt an already committed attack/cast or override stun.

Low-confidence actors avoid circles with a known enemy within five blocks of the center, and avoid routes that take them into enemy proximity. An actor already being chased can move away from that immediate danger toward healing; the safety test does not require its starting position to already be safe. Checks use actual navigation nodes and samples along each segment. Visible hostile players and the current visible enemy are considered; creative/spectator/allied players are excluded. This is local awareness, not knowledge of hidden enemies.

At 25 raw confidence points, actors may risk enemy proximity to obtain healing. Confidence does not override a blocked path, hazardous footing, incompatible ownership, expired magic or an unreachable zone. Paths are checked/replanned at most every ten ticks, with circle removal and recovery checked each action update. The wounded actor can switch to another usable allied circle. Retreating warriors continue requesting ranged covering fire while moving toward healing.

The reusable executor is `behavior/SeekHealingCircleGoal`. It takes a mob, settings supplier, raw-confidence supplier and eligible-caster predicate. Goblins supply their shaman/alliance predicate; the shared executor contains no goblin type checks. Its lifecycle is selected by the existing weighted engine through `GoalBehaviorAction`.

Global settings under `[combatConfidence]`, also supported in per-species `entityOverrides`:

| Key | Default |
|---|---:|
| `healingEntryHealthFraction` | 0.35 |
| `healingExitHealthFraction` | 0.8 |
| `healingSearchRadius` | 24 blocks |
| `healingDangerDistance` | 5 blocks |
| `healingRiskPoints` | 25 raw points |

For example, `"riftfall:goblin_archer|healingRiskPoints=30"` makes archers less willing to risk an enemy-camped circle. Entry zero disables seeking; the effective exit threshold is at least the entry threshold to avoid contradictory tuning.

The overhead overlay shows `seek_healing`, a route/rejection note, remaining circle seconds, and whether the actor is moving or healing inside. `inspect` includes `Notes` and `healing.*` situation signals, including the entry/exit health thresholds, danger radius, risk threshold and whether confidence is overriding proximity. `RiftfallHealingRetreats` counts started recoveries and `RiftfallSeekingHealing` marks the active action. All transient notes/flags are cleared on load.

## Goblin tactical sprinting

The reusable `RegroupGoal` accepts a faction-membership predicate and movement
settings. Goblins check once per second for a visible squadmate within 32 units.
If the closest visible squadmate is more than 10 units away, they can sprint back
until within 4 units. A visible enemy within 10 units prevents regrouping; routes
must avoid passing close to a visible target. Replanning runs every 10 ticks.
Regrouping yields to healing, retreats, committed attacks, and covering fire.

Wounded warriors prefer reachable ground behind a nearby ally before falling back
to their existing escape destination, and sprint during that withdrawal. Existing
healing, archer repositioning, and warrior screening sprint lifecycles remain in
place. Ordinary target approaches sprint only while moving toward a visible enemy
more than 10 units away; squared distances avoid raycasting or square roots just
to measure range. Archers retain their firing-position and engagement ranges.
Sprint state is cleared on arrival, attack, interruption, or behavior exit.
Panic already uses a configurable navigation multiplier (`panicFleeSpeed`, default
1.5); it does not need a second, stacking sprint bonus.

## Unwatched rear openings

`TargetAwareness.facingAway` measures horizontal attention with a dot product and
rejects ambiguous side-on positions. Goblin warriors publish `target.unaware` only
for a visible, living player within 16 units whose attention faces away from them.
This temporarily permits melee, grapple, equipped Backstab and plunge actions even below their normal
confidence engagement threshold; it does not change confidence points.

The `rear_approach` executor sprints toward a reachable point behind that player,
replanning every eight ticks. It yields to a ready grapple, shield Backstab or high-ground plunge,
or ordinary melee once close. If the player looks back, visibility is lost, or the
route fails, the uncommitted rush stops and normal decisions resume. Healing,
wounded-warrior retreat, and interruption retain their higher priorities.

An unwatched opening may temporarily override panic flight while preserving the
fear expression. Already-started burrowing is excluded. Committed grapples, leaps
and swings finish through their existing lifecycle and interruption rules.
Shielded warriors use `skill_identity` for their learned Epic Fight Backstab.
The five reusable active-skill executors and extension API are documented in
[mob-skills.md](mob-skills.md).
Shield-equipped warriors still cannot grapple; the existing grapple cooldown,
player restrictions, struggle mechanic and high-ground leap requirements remain.

## Scheduling and responsibility boundaries

`WeightedBehaviorGoal` schedules decisions separately from action execution. Its `BehaviorObserver` port provides cheap invalidation, full observations and diagnostic publication. The production `CombatBehaviorController` senses Minecraft state; the engine selects a policy action; `GoalActionRegistry` owns goal bindings and decision-local preparation; executors advance the selected movement/attack. Normal decisions use three ticks in combat and ten idle, with immediate interruption/target/damage checks. Actions continue ticking each game tick. See [performance and validation](behavior-performance.md).

Eligibility checks must not implement cooldowns by counting how often `canUse()` is called. Use game-time deadlines (`TickCooldown`) for inactive cooldowns; keep selected attack/animation counters in execution ticks.
