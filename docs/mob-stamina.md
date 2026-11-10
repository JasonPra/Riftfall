# Mob stamina

All vanilla and modded `Mob` subclasses automatically inherit the shared stamina pool, including animals, villagers, monsters, and bosses. Players continue using Epic Fight's own stamina. Undead (`minecraft:undead` entity tag), including correctly tagged modded undead, are explicitly exempt. No entity whitelist or per-species registration is required.

Epic Fight's `MobPatch` does not contain the player stamina resource, so mob execution uses a separate server-owned pool. Epic Fight still handles weapon collisions, attacks, hit reactions, and animation playback. This does not fabricate a player skill container for mobs.

## Defaults

Balance settings live in the world's `serverconfig/riftfall-stamina-server.toml`.

| Setting/action | Stamina |
| --- | ---: |
| Maximum | 40 |
| Passive regeneration | 4 per second |
| Regeneration delay after spending | 20 ticks |
| Moving while sprinting | 4 per second |
| Normal swipe / reverse / shield overhead | 6 |
| Flurry | 12 for the complete attack |
| Plunge | 10 |
| Grapple | 14 for the complete grapple |
| Shaman sweep / smash | 8 / 10 |
| Offensive spell / healing circle | 8 / 10 |
| Bow shot | 5 |
| Backstab | 12 |
| Other adapted active skill | Epic Fight stamina consumption, or 6 by default |
| Shield block | 2 + 0.5 × incoming damage |

Attacks pay once when launched, not once per collider contact or victim. Pursuit and unsuccessful reach predictions cost nothing. Sprint drain applies only while actually moving. For other mobs, fast navigation (speed modifier above 1.1) also counts as running, so vanilla animal panic and modded navigation cannot bypass stamina just because they never set the sprint flag. Stamina does not regenerate during ongoing sprinting, burrowing, grappling, item use, or committed Epic Fight actions. Outside those actions, normal regeneration resumes after the spending delay.

High confidence (the entity's existing configurable bold threshold) multiplies regeneration by **1.15**. Fleeing/retreating multiplies it by **1.25**. Vanilla panic/avoidance goals also qualify as fleeing. Mobs without a confidence engine do not receive an invented confidence score. Fleeing takes precedence if both apply. These are regeneration bonuses, not extra capacity or free actions.

An unaffordable action is rejected and starts **panting**. The mob stops navigation, lowers its shield, stops sprinting, and cannot launch attacks or skills. Regeneration immediately becomes **2× base (+100%)**, overriding the normal delay and adrenaline bonuses. It stays exposed until the pool is completely full. Damage and hit stun remain effective; panting grants no invulnerability. A completed attack can spend the final stamina points without being cut off mid-swing; the next unaffordable action triggers panting.

Goblins retain their looping Epic Fight biped animation with a bent torso, lowered arms, and breathing shoulders. It uses a living motion rather than a knockdown, so normal incoming-hit reactions can interrupt its visual pose. Other species stop their voluntary movement/attacks to recover; their original models and animations are preserved rather than forcing a goblin biped clip onto animals or custom rigs.

## Inspecting and testing

`/riftfall stamina inspect <entity>` reports the pool for any mob or explains its undead exemption. `/riftfall behavior debug` shows a stamina bar, exact current/maximum points, regeneration multiplier, last expense, and remaining delay in the existing hologram, including ordinary mobs without our behavior engine. `/riftfall behavior inspect <entity>` includes the detailed stamina record. Raw runtime values are under `NeoForgeData.RiftfallStamina`.

For a controlled operator test, summon a goblin with low stamina:

```mcfunction
/summon riftfall:goblin ~ ~ ~ {RiftfallStamina:{Current:1.0d,Panting:0b,DelayTicks:0}}
```

Let it attempt an attack or sprint. It should stop to pant, lower its shield, refill at eight points per second, and resume when full. To view the pose immediately, summon with `Current:0.0d,Panting:1b`. Saving/loading preserves the resource, delay, and exhausted state; it does not refill the mob.

## Reusing the component

The base `Mob` integration supplies `StaminaUser`, storage, saving/loading, sprint gating, ticking, and a high-priority recovery goal automatically. Do **not** call the resource tick or persistence methods again in subclasses. Override `staminaConfident`, `staminaFleeing`, or `syncPanting` for custom behavior/visual integration.

Common melee attempts (`Mob.doHurtTarget`), Epic Fight attack animations, and mob-owned projectile launches spend stamina automatically. Damage callbacks cover custom attack overrides that bypass the base melee method; an indirect damaging entity is charged once rather than on every lingering tick. Already-fired projectiles keep working when their owner later exhausts. Attack animations and explicitly paid supported skills reserve their payment so hits/phases/projectiles do not double-charge. Each new normal attack animation still pays, even when it interrupts a preceding swing.

For custom actions that bypass these conventions (for example a non-damaging spell, a custom movement engine, or an attack that bypasses Minecraft damage events), call `MobStamina.spend` at launch and stop the action if it returns false. Custom future skills still need an NPC execution adapter; a universal stamina pool does not create skill implementations. Species with an entire manually priced moveset/behavior engine may override `managesStaminaActions()` and own their recovery action. Goblins use this override to retain their existing explicit costs and custom panting pose; they still use the same shared pool and lifecycle.

`MobActiveSkill.staminaCost(owner)` supplies the NPC skill's resource cost; `MobSkillGoal` pays once before starting execution. Backstab overrides this with its configured cost. An adapted skill declaring Epic Fight’s STAMINA resource defaults to its declared consumption; other skills default to six points. Adapters can override the method for other abilities. The undead exemption is enforced centrally.

`python tools/generate_goblin_pant.py <epic-fight.jar>` regenerates the panting clip (requires NumPy).
