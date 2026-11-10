# Active skills for players and mobs

Mobs have five active slots matching this version of Epic Fight: `dodge`, `guard`, `weapon_innate`, `mover`, and `identity`. These are reusable across mob types, not goblin-only. Assigned skill IDs and per-slot cooldowns persist through saving and loading.

## Backstab

`/give @s riftfall:backstab_skill_book` gives a real Epic Fight skill book. Learn it into the Identity slot. In Epic Fight combat mode, attack a nearby humanoid from behind with a melee weapon to use Backstab instead of an ordinary swipe. It has a ten-second cooldown. The weapon must actually reach the victim; the thrust uses Epic Fight's weapon collider. Weapons registered with Epic Fight's `GREATSWORD` category use a dedicated two-handed impale/lift/tip/release animation with the greatsword model's grip transform. Other weapons keep the existing one-handed thrust. Prediction, playback, and landed-hit recognition all use the same selected animation; compatible add-on greatswords do not require an item-ID whitelist.

Shielded goblin warriors learn this skill automatically. Unshielded warriors retain their existing back-climbing grapple. A successful Backstab deals six damage before mitigation. Ordinary backstabs play the existing roughly 4.6-second knees-to-face fall and get-up. Greatswords keep the blade level through a longer plunge, lift the victim about 0.045 model units (roughly an inch), then tip down to let them slide off. Their paired victim clip lasts five seconds, including the impale and recovery. The victim cannot act and is protected from ordinary damage throughout. Administrative damage that bypasses invulnerability still works. Greatsword targeting permits up to 2.2 units of clearance, subject to the actual equipped weapon collider; ordinary backstabs retain their 1.5-unit limit. Lethal backstabs leave the victim at up to one health until both the attacker animation and the victim’s faceplant finish. Death, its sound and rewards resolve afterward with the original damage source; the prone faceplant replaces the normal death pose for mobs and players. A Totem of Undying is checked at that point and, if it saves the victim, starts a protected get-up instead. If the attacker is interrupted before release, the victim drops into the shorter fall/recovery; a pending lethal hit still resolves after that fall. Player attackers stay anchored in position and facing throughout their attack while their camera remains free to orbit. Backstab automatically releases Epic Fight lock-on and does not reacquire it afterward.

The sequence requires grounded humanoids and clear, supported floor space ahead of the victim. This is an authored Epic Fight knockdown, not a physics ragdoll.

## Assigning skills to mobs

Operator commands:

```mcfunction
/riftfall mobskill equip @e[type=minecraft:zombie,sort=nearest,limit=1] identity riftfall:backstab
/riftfall mobskill inspect @e[type=minecraft:zombie,sort=nearest,limit=1]
/riftfall mobskill clear @e[type=minecraft:zombie,sort=nearest,limit=1] identity
```

Backstab's NPC implementation requires a shield and a melee weapon. Assigning it does not give the mob equipment or a new target-selection policy.

## Extension points

- `MobSkillLoadout.equip(mob, slot, skill)` validates the category and assigns the registered Epic Fight skill.
- `MobSkillLoadout.equipFromBook(mob, slot, stack)` reads an actual Epic Fight skill book. The caller controls item consumption.
- `MobSkillLoadout.equipped`, `ready`, and `clear` inspect or change the loadout.
- Implement `MobActiveSkill` on a skill, or register an adapter with `MobSkillLoadout.registerAdapter(skillId, adapter)` for a third-party skill. The adapter supplies eligibility, execution, cooldown, and NPC stamina cost. An execution supplies `running`, `tick`, `stop`, and whether it is committed.
- Ordinary mobs receive a shared `MobSkillGoal` only for assigned slots. It runs in their vanilla goal selector. No global per-tick mob scan is added.
- An entity whose behavior engine schedules skills implements `BehaviorSkillUser` and adds one `MobSkillGoal` per active slot to its engine. Use `MobSkillGoal.behaviorId(slot)` as the policy ID and `goal::committed` for commitment. `GoblinBehavior` demonstrates this integration; `GoblinBehaviorProfile` supplies species-specific priorities.

Stock player skills are not automatically safe to execute on NPCs: many require `ServerPlayerPatch`, stamina, or player input. They need an NPC adapter. Unsupported assignments fail with a clear error rather than crashing. Backstab is the first supported implementation; five available slots does not mean five free abilities are granted to every mob.

All non-undead mobs now automatically have stamina and pay the adapter's `staminaCost(owner)` before skill execution. See [mob-stamina.md](mob-stamina.md) for exhaustion and adrenaline rules. Undead are exempt.

## Animation authoring

`tools/generate_backstab_animations.py <epic-fight.jar>` regenerates the six authored clips from the biped rig. It requires Python and NumPy. The game loads the generated animation JSON files; the script is only an authoring tool.
