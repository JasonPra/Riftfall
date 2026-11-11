# Shared mob mana and equipment

Every `Mob` exposes `ManaUser`. The server allocates its mana state when casting or inspecting first requests it; ordinary non-casters do not run a second resource loop. The actual balance lives in Iron's `MagicData` attachment. Native `IMagicEntity` casters retain their own `getMagicData()` instead. This adds a resource API, not spell AI to animals or undead.

`MobMana.data(mob)` initializes/accesses the pool. `MobMana.canCast(mob, spell, level)` checks affordability; call `MobMana.spend(...)` once at release, after pre-cast conditions succeed. Offensive and healing spells use the same pool. Interrupted wind-ups cost no mana. Mobs that already implement Iron's native casting retain their AI; add-ons must call the shared spending API when integrating that AI. No second automatic charge is injected into other mods' spells.

Goblin shamans and opted-in pillager casting now use this API. Spell costs come from `AbstractSpell.getManaCost(level)`, including Iron's spell configuration. Casting and healing no longer spend stamina. Sprinting, physical attacks and shield use still do. Unaffordable spells cannot start or release, leaving the existing positioning and staff behaviors available. Reaching zero mana latches a casting lock until the pool refills to its current full capacity. An otherwise-valid, needed cast that cannot be afforded also starts recovery, covering fractional leftovers. Goals check targeting/healing need and cooldown before requesting mana, so unused candidate spells do not trigger it. A spell costing more than the maximum capacity is rejected without repeatedly locking other usable spells. Recovery state survives save/load, and capacity-changing equipment affects the full-refill threshold.

## Balance and equipment

`serverconfig/riftfall-mana-server.toml` in the world config controls defaults: 100 base mana, 3 points/second and 40 ticks of regeneration delay after a successful expense. The shaman profile is `riftfall:goblin_shaman=160,4`. Add `entity_id=capacity,regen_per_second` rows to tune another species. Profiles supply base capacity; existing custom attribute bases are preserved. Regeneration pauses during casting, and save/load preserves the balance and remaining delay. There is no regeneration catch-up while unloaded.

Iron's attributes are the source of equipment changes: `max_mana`, `mana_regen`, spell/school power and resistance, cooldown reduction, and cast-time reduction. The current mob spells use these attributes. Capacity increases add room without instantly refilling; decreases clamp the balance. Iron's mana-regeneration value is a multiplier: base 4/s with a 1.5 value regenerates 6/s.

All mob types receive missing Epic Fight attributes. Our stamina adapter reads `epicfight:stamina` and `epicfight:stamina_regen`, with the existing configured 40-point base and 4/s regeneration. For example, +10 stamina and +0.5 regeneration give 50 maximum and 6/s before confidence/panting bonuses. Explicit custom stamina bases are preserved. Undead still do not spend stamina.

Weapons and armor retain vanilla equip/unequip modifier and enchantment handling. Epic Fight's existing patched-entity weapon/armor capability logic remains responsible for its combat effects. Adding attributes does not install a custom Epic Fight animation patch on every mob or grant player-input skills automatically.

## Curios

Mobs inherit the player's configured Curios slot types/counts, including slots from other installed mods. Explicit per-entity Curios definitions take precedence for the same slot type. Players and armor stands retain their existing behavior. Inventories use Curios' own persistence, item validation, modifier processing, equip/unequip/tick callbacks, tracking synchronization and death-drop rules. Jewelry is not generated or assigned automatically.

`MobEquipment.equipCurio(mob, slot, index, stack)` equips through the shared inventory with slot/item validation; add-ons can also use `CuriosApi.getCuriosInventory(mob)` directly. Attribute bonuses and mob-compatible passive callbacks work normally. Adapters cover these Iron's player-restricted item events: Fireward protection for non-native casters, Lurker's invisible-hit bonus, Signet of the Betrayer's bonus against extra mana capacity, and Expulsion's defensive knockback. Concentration also protects long casts from hurt interruptions in the shared casting goals; Evasion/Abyssal Shroud reactions apply to ordinary mobs as well. Passive-ability cooldowns are per mob and persist in its data. Native Iron's/player handlers are not duplicated.

A third-party effect explicitly coded for `Player` (such as a key binding, player menu or skill activation) still requires a mob adapter. There is no safe universal translation of arbitrary player-only mod code into AI.

## In-game checks

Operator commands:

- `/riftfall behavior debug on`: live behavior, confidence, stamina and active mana diagnostics above nearby entities.
- `/riftfall mana inspect <entity>`: current/max mana, effective regeneration, delay, recharge-lock state, last expense and known spell costs. Inspecting a non-caster initializes its pool for testing.
- `/riftfall mana set <entity> <amount>`: set a test balance, capped at current capacity.
- `/riftfall equipment inspect <entity>`: list Curios slots and equipped stacks.
- `/riftfall equipment curio <entity> <slot> <index> <item>`: equip a Curio, or use `minecraft:air` to clear it. Index starts at zero. Item components are supported.

Use vanilla `/item replace entity ...` for hands and armor, and `/attribute <entity> <attribute> get` to inspect equipment bonuses. The behavior display lists each known spell cost and the most recent mana expense separately from stamina.

Verification includes a dedicated server with shamans, cows and zombies: native mana rings stack, armor resource modifiers apply/remove, Fireward and Poisonward protect non-casters, the Lurker/Betrayer bonuses and Expulsion impulse/cooldowns trigger, and actual attack/heal goals spend the same mana without draining stamina. Unit tests cover the resource budget and debug formatting.
