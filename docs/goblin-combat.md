# Goblin combat and behavior priorities

This is the current gameplay reference for warriors, archers and shamans. Engine integration, confidence configuration and reusable APIs are documented separately in [behavior-engine.md](behavior-engine.md), [combat-confidence.md](combat-confidence.md), and [mob-skills.md](mob-skills.md).

## Goblin Shortsword

`riftfall:goblin_shortsword` is a real stone-tier `SwordItem`, with stone-sword damage/durability and the normal sword attack-speed modifier. It is available in the Combat creative tab and through:

```mcfunction
/give @s riftfall:goblin_shortsword
```

New warriors carry it. The legacy load migration converts an old default stone sword only on pre-inventory-system warriors; it preserves item components and does not overwrite the current equipment of an established loot carrier. Warriors can subsequently replace the sword with an allowed equipment upgrade.

| Equipment | Shortsword combo |
| --- | --- |
| No shield, warrior | Cut → reverse cut → four-cut figure-eight flurry |
| Empty offhand, player in Epic Fight combat mode | Cut → reverse cut → four-cut figure-eight flurry |
| Shield equipped | Regular cut → backswing → four-cut flurry → overhead cut |

The short unshielded cuts last about 0.6 seconds. The 1.4-second flurry has four contact phases, each at **65% normal weapon damage**, with a small collision-aware forward animation step and walking shuffle. Rotation passes through the hips and chest; the empty arm stays relaxed during normal unshielded cuts. The stance belongs to this item's Epic Fight capability, so players receive the authored combos too. Other equipped weapons keep the existing basic swipe/backswing/flurry selection and their own colliders.

Actual equipped-weapon sweeps decide damage; being within navigation distance does not award a hit. Warriors predict contact before committing, and close beyond their coarse formation destination if a short weapon still cannot reach. Accepted native `SHORT` stuns from the shortsword flurry last at least **0.35 seconds** when native stun-armor/impact checks allow them. This does not bypass shield blocks, stun immunity or poise checks. Physical stamina is charged once per attack: the default regular cut/overhead costs six, and a complete flurry costs twelve.

The item preset is registered in `GoblinSwordCombat`; `GoblinCombatRegistries` supplies modern Epic Fight movesets and offhand conditions. `GoblinTactics.Melee` selects NPC attacks; `GoblinFlurryStun` extends accepted stun durations. Animation assets remain under `assets/riftfall/animmodels/animations`.

## Roles and equipment

**Warriors** can approach, defend at close range, flank/surround, screen archers, withdraw behind allies and plunge from reachable high ground. A new warrior has a one-in-three chance of a 180-durability Goblin Buckler. Guarding rises during suitable approaches and lowers for attacks, plunges, grapples and stuns. Physical shield contact, stamina, durability and native blocked-hit reactions apply; guarding is not a blanket facing-cone immunity. See [shield-colliders.md](shield-colliders.md).

An unshielded warrior can climb onto a humanoid victim from behind and grapple; the victim can mash attack to escape. A shield in either hand prevents grappling. Shield-bearing warriors instead learn the Identity-slot Backstab when that slot is empty. Once learned, its NPC eligibility still requires appropriate equipment. Grapple and NPC Backstab share local same-species ambush recovery; interruption does not refund the cooldown. See [grapples-and-ambush-cooldowns.md](grapples-and-ambush-cooldowns.md) and [mob-skills.md](mob-skills.md).

**Archers** keep bow attacks, friendly-fire checks, positions behind warriors and reachable high ground. Retreating warriors advertise their pursuer so archers can provide covering fire. Safe covering fire has a substantial utility bonus, allowing it to outrank ordinary repositioning/regrouping.

**Shamans** cast offensive Iron's Spells and Healing Circle, preferring wounded retreating warriors when selecting a healing recipient. They also use the Bone Staff physically: an accelerating 0.6-second sweep (1.2× equipped weapon damage) or a two-handed 0.8-second-windup overhead smash (1.8×). The original staff's base values are three and 4½ hearts respectively, before mitigation. Staff contact and ground contact emit cosmetic purple particles; the ground cloud is not area damage. Exposed/stunned targets or long attack commitments favor an opportunistic overhead, with approach from up to six blocks when eligible. Spellcasting spends mana; staff attacks spend stamina. Shaman presentation retains the bone helmet, tooth necklace and primitive clothing. Shamans are exempt from panic burrow escape.

All three roles can loot, store items, upgrade permitted equipment and seek an allied active healing circle. Loot does not teach spells or skills. They can spawn from [Verdant Frontier camp spawners](goblin-camps.md), spawn eggs and `/summon`; no natural biome spawn pool contains goblins. See [loot and stash](goblin-loot-and-stash.md).

## How selection works

One `WeightedBehaviorGoal` owns movement/action selection. Vanilla floating remains a higher-priority survival goal. Target goals independently acquire enemies, react to attackers, assist allies and select threats for covering fire; they do not run separate competing attack goals.

For a ready action, utility is `base + weighted signals`. `confidence` is normalized confidence; `fear` is its complementary signal. Boolean signals are zero/one. Most rules also receive **+1,000 while committed**. A challenger must exceed an eligible current action by the switch margin of **2**; an ineligible current action is replaced immediately. Initial exact ties use profile order. Readiness includes role, equipment, range, navigation, resource, visibility and action-specific cooldown checks, so a high base does not make an unavailable action execute.

These are the current rules from `GoblinBehaviorProfile`, in profile order. The table omits the shared commitment bonus from each row:

| Action ID | Base | Additional utility | Purpose / main eligibility |
| --- | ---: | --- | --- |
| `interrupted` | 5,000 | — | Stun/interrupt owns the actor; stop navigation. |
| `pant` | 4,500 | — | Exhausted; stop voluntary movement, item use and attacks until recovered. |
| `panic_escape` | 2,000 | fear × 100 | Sustained panic, escape allowed, and suitable escape lifecycle. |
| `seek_healing` | 700 | fear × 50 + wounded × 50 | Reachable, safe, active allied Healing Circle. |
| `warrior_retreat` | 500 | fear × 50 + wounded × 40 | Wounded warrior near an enemy, with a withdrawal destination. |
| `cautious_retreat` | 300 | fear × 50 | Warrior below willingness-to-engage threshold, without a rear opening. |
| `watch` | 100 | fear × 20 | Visible target; melee role unwilling to advance and no unwatched opening. |
| `grapple` | 400 | confidence × 20 | Eligible unshielded rear grapple. |
| `skill_identity` | 410 | confidence × 20 | Ready assigned Identity skill, including shield-warrior Backstab. |
| `skill_dodge` | 600 | fear × 20 | Ready assigned NPC-compatible dodge skill. |
| `skill_guard` | 420 | fear × 20 | Ready assigned NPC-compatible guard skill. |
| `skill_weapon_innate` | 400 | confidence × 20 | Ready assigned NPC-compatible weapon skill. |
| `skill_mover` | 350 | confidence × 20 | Ready assigned NPC-compatible movement skill. |
| `plunge` | 350 | confidence × 20 | Valid high-ground leap/contact opportunity. |
| `rear_approach` | 320 | fear × 20 | Visible player within 16 blocks looking away; reachable rear approach. |
| `staff` | 500 | exposed target × 40 | Shaman's ready staff attack or opportunistic approach. |
| `heal` | 400 | fear × 10 | Shaman with a valid wounded ally, safe cast and sufficient mana. |
| `ranged_retreat` | 300 | fear × 50 | Ranged role under eligible close pressure. |
| `cast` | 250 | confidence × 20 | Ready offensive spell with valid target and sufficient mana. |
| `regroup` | 180 | fear × 20 | Separated visible squadmate, safe route and no close visible enemy. |
| `position` | 160 | fear × 10 | Ranged formation/high-ground positioning. |
| `bow` | 140 | confidence × 10 + safe cover × 200 | Archer with a bow and valid enemy. |
| `melee` | 150 | confidence × 20 | Warrior approach/contact attack, subject to willingness. |
| `search` | 50 | confidence × 5 | Remembered target; melee roles must be willing to engage. |
| `loot` | 30 | — | Available reachable loot with no target present. |
| `wander` | 10 | — | No target and a valid idle stroll. |
| `idle` | 0 | — | Fallback. |

`interrupted`, `watch`, `search` and `idle` do not use the common commitment bonus. The five skill entries are scheduling slots, not five automatically granted abilities; adapters and assigned skills are required. In addition to scores, the integration preserves a running committed executor and avoids preparing unrelated actions while it owns the slot.

## Situational priorities

- **Threat awareness:** ordinary pursuit requires visibility; hidden targets are searched for at their last seen position for up to five seconds. Hidden health/equipment are not live inputs. Default warrior willingness to advance is 12 raw confidence points. Close self-defense and committed attacks remain possible below that threshold.
- **Rear opportunity:** a visible player within 16 blocks looking away opens a rear rush even for a frightened warrior. It temporarily suppresses panic flight while preserving fear, and yields to a ready rear attack/plunge or close melee. Looking back, losing visibility or failing the route ends an uncommitted rush. A started burrow is excluded.
- **Recovery:** physical exhaustion prevents new attacks and requires full stamina recovery. Seeking healing normally starts below 35% health and ends at 80%, or when the actual allied circle expires/becomes unusable. Low-confidence actors avoid enemy-camped circles and unsafe routes; 25 raw confidence points permits proximity risk, not impossible paths.
- **Squad movement:** warriors try to retreat behind an ally; ranged roles cover withdrawal. Regrouping checks visible allies within 32 blocks, starts beyond ten and ends within four when safe. Ordinary visible-target approaches sprint beyond ten blocks and stop sprinting when attacking, arriving or interrupted.
- **Panic:** warriors/archers can flee and burrow through eligible natural soft ground after sustained fear. Burrowing, audio completion, damage interruption, concealment and the future in-rift restriction have their own lifecycle. See [panic-escape.md](panic-escape.md).

Confidence also reflects nearby role weights (warrior five, archer one, shaman ten by default), personal equipment, health/status, visible enemy strength and witnessed allied deaths. Boldness for organized tactics is distinct from willingness to approach. Configurable factors and entity overrides are in [combat-confidence.md](combat-confidence.md).

## Inspecting a decision

```mcfunction
/riftfall behavior debug on
/riftfall behavior inspect @e[type=riftfall:goblin,sort=nearest,limit=1]
/riftfall stamina inspect @e[type=riftfall:goblin,sort=nearest,limit=1]
/riftfall mana inspect @e[type=riftfall:goblin_shaman,sort=nearest,limit=1]
```

The inspector reports readiness/utility evaluations, confidence contributions, action notes, target memory and resources. Use the independent `riftfall:behavior_tester` to isolate shared-engine logic. For a reliable melee check, use survival mode and compare the native contact visualizer (`F3+B`) with the held-item alignment overlay (`F8`); item bounds are not damage colliders.
