# Weighted combat confidence

`confidence/ConfidenceEngine` is independent of Minecraft entities and goblin AI. A `ConfidenceProfile` supplies weights for arbitrary entity IDs and factor names. Callers supply effective nearby unit counts and active combat factors. The result contains raw points, normalized confidence, and a contribution breakdown. Goblin combat tactics are the first consumer; adding another combatant does not require editing this engine.

The initial profile is configurable in `config/riftfall-server.toml`, under `[combatConfidence]`. A world can override it with its own `serverconfig/riftfall-server.toml`. Changes reload while the server runs. Other behavior adapters can supply independent profiles rather than using this default.

```text
Each combatant's confidence points
├─ Base                                                    +5
├─ Nearby available ALLIES (observer excluded)
│  ├─ Goblin warrior                           +5 per ally
│  ├─ Goblin archer                            +1 per ally
│  ├─ Goblin shaman                           +10 per ally
│  └─ Retreating ally                         ×0.25 of its unit weight
├─ Personal equipment: select ONE loadout
│  ├─ Sword                                    +3
│  ├─ Sword + shield                           +6
│  ├─ Bow / crossbow                           +2
│  ├─ Bow / crossbow + shield                   +1
│  ├─ Staff                                    +4
│  ├─ Staff + shield                           +6
│  ├─ Other weapon / tool                      +2
│  ├─ Other weapon / tool + shield             +4
│  ├─ Shield without a weapon                  −3
│  └─ Unarmed / holding a nonweapon            −5
├─ Personal health below 35%                   −9
├─ Personally poisoned                        −8
├─ Personally withered                       −10
├─ Other harmful effects                      −4 per distinct effect
├─ Target health at or below 30%              +10
├─ Target poisoned                             +8
└─ Target withered                             +8
   └─ Situational factors stack independently

Raw points → confidence = clamp(points / 40, 0, 1)
├─ 25+ raw points: bold, organized combat tactics
├─ Below 25: panicked retreat when threatened at close range
├─ Higher confidence: smaller flee distance and lower retreat-health threshold
└─ Lower confidence: stronger existing head/limb trembling
```

Equipment changes affect the wielder's personal score. An ally's unit contribution comes from its entity type, with the retreat multiplier applied when appropriate. A torch or food is not a weapon. Shields in either hand are recognized; the weapon in the main hand determines its category unless that hand is empty or holding the shield. Equipment values do not stack: sword + shield is +6, not +3 plus +6.

Support is scanned in the existing local area (16 blocks horizontally, four vertically), with a half-second cache. Allies must be alive, allied, and AI-enabled. During combat, allies occupied with a different enemy do not contribute. Dead, unloaded, distant, and disabled units do not contribute. A wounded ally retains its unit weight unless it is retreating. Each observer has its own local tally and adds its own equipment and conditions.

Formation geometry still requires actual units: at least two melee goblins for a surrounding ring, and actual warriors for an archer screen. Weighted confidence determines willingness; shamans do not become imaginary shield-bearing warriors. Guard and ring-slot allocation still uses the actual formation size. The old four-warrior confidence gate and old headcount caps are removed.

Examples (healthy observer and target with equal combat attributes, so relative-strength contributions are zero):

| Observer | Nearby allies | Raw points | Confidence | Bold? |
|---|---|---:|---:|---|
| Sword warrior | None | 5 + 3 = 8 | 0.20 | No |
| Sword + shield warrior | None | 5 + 6 = 11 | 0.275 | No |
| Bow archer | Four warriors | 5 + 2 + 20 = 27 | 0.675 | Yes |
| Bow archer | Two warriors and a shaman | 5 + 2 + 10 + 10 = 27 | 0.675 | Yes |
| Shield-only warrior | None | 5 − 3 = 2 | 0.05 | No |
| Wounded, unarmed warrior | None | 5 − 5 − 9 = −9 | 0 | No |

Configuration keys:

- `unitWeights`: strings such as `"riftfall:goblin_shaman=10"`. Add other allied entity IDs here to let them contribute to the shared profile; IDs missing from the profile contribute zero.
- `factorWeights`: strings such as `"equipment.sword_shield=6"` or `"self.low_health=-9"`. Future behavior adapters can provide additional named factors without changing the engine.
- `normalizationPoints`, `boldPoints`: tune saturation and organized behavior independently.
- `woundedHealthFraction`, `vulnerableTargetHealthFraction`, `retreatingAllyMultiplier`: tune the condition thresholds and retreating-unit effectiveness.
- Item tags `riftfall:confidence/staves` and `riftfall:confidence/weapons`: allow add-on items to use staff or improvised-weapon equipment values. The default staff tag includes the Bone Staff. Vanilla swords, bows, crossbows, tools, tridents and maces are recognized directly.

To inspect an individual goblin:

```mcfunction
/data get entity @e[type=riftfall:goblin,sort=nearest,limit=1] NeoForgeData
```

`RiftfallConfidencePoints` is the raw score; `RiftfallConfidence` is the normalized value; `RiftfallConfident` records the organized-behavior threshold. `RiftfallConfidenceBreakdown` lists every contribution. `RiftfallWarriorSupport` remains a formation diagnostic and does not control morale. Diagnostics and allied-unit counts refresh within half a second; the existing client interpolation smooths visual trembling.

## Reusing the engine

```java
var profile = new ConfidenceProfile(
    Map.of("example:captain", 12D),
    Map.of("base", 5D, "equipment.spear", 4D, "self.afraid_of_fire", -8D),
    40);
var result = ConfidenceEngine.evaluate(profile,
    Map.of("example:captain", 1D),
    Map.of("base", 1D, "equipment.spear", 1D));
// 21 points, 0.525 confidence. The caller chooses what behavior that produces.
```

`CombatConfidence.factors` provides reusable Minecraft loadout, health, and target-effect inputs for any `LivingEntity`. Custom combatants can extend those inputs or provide their own. Adding a unit weight does not automatically attach goblin tactics to that mob.

Visible-opponent equipment/health comparisons and the separate melee advance threshold are documented in [the behavior engine guide](behavior-engine.md#equipment-threat-and-willingness-to-advance). A low confidence score alone previously did not prevent warrior pursuit; the engagement threshold now controls that decision.
