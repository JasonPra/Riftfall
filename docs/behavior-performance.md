# Behavior performance checks

Measured on a dedicated NeoForge 1.21.1 server with Epic Fight and Iron’s Spells, Java 21, using Java Flight Recorder profile settings. These are server tick timings, not client FPS or a guaranteed population limit.

## Load experiment

An isolated flat world contained 120 goblins (3 warriors : 1 archer : 1 shaman; one-third of warriors shielded), deterministic UUIDs and random seeds, in a 20-column grid with 2-by-3 spacing. Combat targeted a stationary invulnerable iron golem. Natural mob spawning was disabled. Each scenario warmed up for 300 ticks, then measured 600 ticks between server tick Pre/Post events. Idle and combat each ran twice per JVM, in alternating order. The temporary benchmark command was removed from the release.

| Scenario/run | Original mean ms | Optimized mean ms | Original p95 ms | Optimized p95 ms |
| --- | ---: | ---: | ---: | ---: |
| Idle 1 | 9.571 | 9.338 | 24.239 | 24.627 |
| Combat 1 | 8.903 | 4.511 | 16.957 | 9.064 |
| Idle 2 | 7.277 | 4.744 | 22.949 | 17.124 |
| Combat 2 | 7.211 | 4.706 | 14.196 | 10.248 |

Across the two final samples, mean tick time fell about 16% idle and 43% in combat. Idle measurements were more sensitive to JVM warm-up: a preceding optimized run averaged 6.016 ms idle and 4.681 ms combat. Treat these as fixture-specific measurements, not a universal speedup. The first final idle p95 did not improve.

JFR identified repeated diagnostic score maps, stream allocations, boxed signal defaults, tuning record reconstruction, and repeated path planning as avoidable costs.

## Changes and responsiveness

- Weighted selection runs every tick, with the same tie order and switch margin. Detailed explanations are generated on diagnostic refreshes rather than on every decision.
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
