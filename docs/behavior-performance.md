# Behavior performance checks

Measured on a dedicated NeoForge 1.21.1 server with Epic Fight and Iron’s Spells, Java 21, using Java Flight Recorder profile settings. These are server tick timings, not client FPS or a guaranteed population limit.

## Historical load experiment (before the multi-rate refactor)

An isolated flat world contained 120 goblins (3 warriors : 1 archer : 1 shaman; one-third of warriors shielded), deterministic UUIDs and random seeds, in a 20-column grid with 2-by-3 spacing. Combat targeted a stationary invulnerable iron golem. Natural mob spawning was disabled. Each scenario warmed up for 300 ticks, then measured 600 ticks between server tick Pre/Post events. Idle and combat each ran twice per JVM, in alternating order. The temporary benchmark command was removed from the release.

| Scenario/run | Original mean ms | Optimized mean ms | Original p95 ms | Optimized p95 ms |
| --- | ---: | ---: | ---: | ---: |
| Idle 1 | 9.571 | 9.338 | 24.239 | 24.627 |
| Combat 1 | 8.903 | 4.511 | 16.957 | 9.064 |
| Idle 2 | 7.277 | 4.744 | 22.949 | 17.124 |
| Combat 2 | 7.211 | 4.706 | 14.196 | 10.248 |

Across the two final samples, mean tick time fell about 16% idle and 43% in combat. Idle measurements were more sensitive to JVM warm-up: a preceding optimized run averaged 6.016 ms idle and 4.681 ms combat. Treat these as fixture-specific measurements, not a universal speedup. The first final idle p95 did not improve.

JFR identified repeated diagnostic score maps, stream allocations, boxed signal defaults, tuning record reconstruction, and repeated path planning as avoidable costs.

## Earlier baseline changes

- In the measured baseline, weighted selection ran every tick, with the same tie order and switch margin. Detailed explanations are generated on diagnostic refreshes rather than on every decision.
- Diagnostic snapshots retain every candidate and contribution; periodic work is staggered by entity ID.
- Immutable behavior rules are shared. Effective tuning is cached and invalidated when base settings or override maps change.
- Support/formation sensing runs at most every 10 ticks, with target changes and witnessed deaths still invalidating confidence support immediately.
- Cover and healing searches use five-tick intervals. Rear ambush retries use eight ticks. Melee closing paths refresh at most every five ticks; reach, attack execution and safety checks still run each tick.
- Loot scanning checks at most four candidates per scan, rotating across candidates so unreachable items cannot permanently starve reachable ones. The initial successful path is reused.
- Stamina maximum reads avoid rewriting unchanged persistent baseline values.

Periodic acquisition can consequently take a few ticks to notice a new opportunity. Existing movesets, confidence weights and weapon colliders were not changed. Debug snapshots still have a cost, as do vanilla navigation, entity collisions and third-party combat code; this is not a claim that all possible bottlenecks are eliminated.

## Regression validation

`JAVA_HOME=/path/to/jdk21 bash gradlew build`

The new efficiency tests compare fast and detailed selection over 10,000 randomized contexts, check compensated score arithmetic against the previous stream implementation over 10,000 inputs, verify phased scheduling, context immutability, and configuration cache invalidation. Existing behavior, mana, inventory and combat tests remain enabled.

Native server checks confirmed that a looter reaches a valid fifth item after four unreachable candidates, and that live diagnostic snapshots retain weighted candidate explanations and ally contributions. Visual animation and client rendering performance were not measured by this server fixture.

## Current multi-rate decision loop

The fresh `prototyping` branch starts from the goblin-only `main` baseline (`5644270`). The historical timing table above predates this refactor; it is not a measurement of the new scheduler.

- Normal combat sensing/selection runs every three game ticks (about 150 ms at 20 TPS); idle sensing/selection runs every ten (about 500 ms). Phase alignment spreads updates by entity ID. First use and resume evaluate immediately.
- New damage, target changes, target validity changes, and entering/leaving interruption or panting trigger immediate sensing. A lightweight continuation guard checks only the active executor between decisions.
- Selected action execution, animation counters, contacts and navigation continue each tick. Other existing support/formation and pathfinding budgets remain in force.
- `GoalActionRegistry` owns each action's executor, commitment predicate and cached signal keys in one binding. It preserves registration priority and caches successful **and failed** preparation for the current decision; no temporary preparation map or ambush stream is created for each update.
- Shaman casting cooldowns use absolute game time through `TickCooldown`, so fewer eligibility polls do not multiply the cooldown.
- `BehaviorObserver` separates the scheduler from Minecraft sensing and makes the actual execution/decision loop testable without a simulated world. `CombatBehaviorController` implements that port; policy and executors remain separate.
- Debug snapshots include `DecisionUpdates` and the active/idle interval defaults. `observationCount()` permits direct profiling without exposing mutable controller state.

Deterministic tests run 30 execution ticks with 10 combat decisions or 3 idle decisions, distribute scheduled decisions across 300 observer IDs, check immediate interruption and action lifecycle, verify clock-based cooldowns under different polling frequencies, and compare registry preparation/results against the previous implementation over 2,000 randomized situations. These are workload and correctness checks, not an FPS or server tick-time speedup claim.

A fresh dedicated-server smoke check over 100 measured ticks observed 11 idle decisions and 35 combat decisions (including additional safety/action-completion rechecks), one completed shaman spell, and a next-tick decision after damage. The temporary fixture was removed afterward. The full regression suite passed 221 tests. A subsequent load experiment is recorded below.


## Follow-up load experiment on the recreated branch

Compared `5644270` with the refactored branch on the same machine using a temporary dedicated-server fixture: 120 goblins, 3 warriors : 1 archer : 1 shaman, one-third of warriors shielded, fixed UUIDs/RNG seeds, a 12-by-10 grid at three-block spacing, and 25 forced chunks. A stationary iron golem was healed and reacquired every tick to sustain combat. Natural spawning was disabled. Each alternating idle/combat phase warmed up for 150 ticks and measured 300 ticks. JFR used profile settings. The fixture is not included in the release.

| Phase | Baseline mean ms | Refactored mean ms | Baseline p95 ms | Refactored p95 ms |
| --- | ---: | ---: | ---: | ---: |
| Idle 1 | 9.366 | 5.875 | 23.753 | 8.751 |
| Combat 1 | 42.316 | 57.877 | 68.684 | 135.586 |
| Idle 2 | 9.209 | 5.687 | 27.694 | 9.645 |
| Combat 2 | 29.937 | 25.228 | 42.965 | 38.148 |

All 120 actors survived every phase. Combat ended with 119–120 active actors and 72 casts per phase, plus 100–106 arrows. Idle timings improved in this run, but combat results were mixed, with substantial first-phase outliers. One JVM run per version and brief tooling activity during the refactored run limit the comparison; these numbers do not establish a reliable combat speedup.

The nearest Riftfall frame appeared in 767 of 2,563 sampled server-thread stacks in the baseline and 384 of 1,848 in the refactored run. Remaining sampled work includes confidence diagnostics, sensing, collider contact and combat tremor. Sampling counts are diagnostic evidence, not directly comparable CPU durations or proof of a particular percentage improvement.
