# Mob stamina

Goblin warriors, archers, and shamans opt into the shared `stamina` package. Players continue using Epic Fight's own stamina. Undead (`minecraft:undead` entity tag) are explicitly exempt, even if a future undead entity implements `StaminaUser`. Other mobs are unchanged until opted in.

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

Attacks pay once when launched, not once per collider contact or victim. Pursuit and unsuccessful reach predictions cost nothing. Sprint drain applies only while actually moving. Stamina does not regenerate during ongoing sprinting, burrowing, grappling, or committed Epic Fight actions. Outside those actions, normal regeneration resumes after the spending delay.

High confidence (the entity's existing configurable bold threshold) multiplies regeneration by **1.15**. Fleeing/retreating multiplies it by **1.25**. Fleeing takes precedence if both apply. These are regeneration bonuses, not extra capacity or free actions.

An unaffordable action is rejected and starts **panting**. The goblin stops navigation, lowers its shield, stops sprinting, and cannot launch attacks or skills. Regeneration immediately becomes **2× base (+100%)**, overriding the normal delay and adrenaline bonuses. It stays exposed until the pool is completely full. Damage and hit stun remain effective; panting grants no invulnerability. A completed attack can spend the final stamina points without being cut off mid-swing; the next unaffordable action triggers panting.

Panting uses a looping Epic Fight biped animation with a bent torso, lowered arms, and breathing shoulders. It uses a living motion rather than a knockdown, so normal incoming-hit reactions can interrupt its visual pose.

## Inspecting and testing

`/riftfall behavior debug` shows a stamina bar, exact current/maximum points, regeneration multiplier, last expense, and remaining delay in the existing hologram. `/riftfall behavior inspect <entity>` includes the detailed stamina record. Raw runtime values are under `NeoForgeData.RiftfallStamina`.

For a controlled operator test, summon a goblin with low stamina:

```mcfunction
/summon riftfall:goblin ~ ~ ~ {RiftfallStamina:{Current:1.0d,Panting:0b,DelayTicks:0}}
```

Let it attempt an attack or sprint. It should stop to pant, lower its shield, refill at eight points per second, and resume when full. To view the pose immediately, summon with `Current:0.0d,Panting:1b`. Saving/loading preserves the resource, delay, and exhausted state; it does not refill the mob.

## Reusing the component

Implement `StaminaUser`, own a `StaminaState`, provide confidence/fleeing flags, and sync its panting flag for rendering. Call `MobStamina.beforeTick` before AI and `afterTick` after AI, and `save/load` from entity serialization. Before launching a paid action call `MobStamina.spend`; stop that action when it returns false. Gate sprint requests with `sprintAllowed`. Give the entity's behavior engine a panting action that outranks voluntary combat and flight but yields to hit reactions. `GoblinEntity` and `GoblinBehavior` demonstrate the wiring.

`MobActiveSkill.staminaCost(owner)` supplies the NPC skill's resource cost; `MobSkillGoal` pays once before starting execution. Backstab overrides this with its configured cost. An adapted skill declaring Epic Fight’s STAMINA resource defaults to its declared consumption; other skills default to six points. Adapters can override the method for other abilities. The undead exemption is enforced centrally.

`python tools/generate_goblin_pant.py <epic-fight.jar>` regenerates the panting clip (requires NumPy).
