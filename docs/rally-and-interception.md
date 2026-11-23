# Rally and ranged-ally protection

Goblin shamans can cast `riftfall:rally`, a registered Iron's Spells support spell. It uses the existing shaman charge/release/recovery animations and shared mana accounting. It is not added to random scroll loot or crafting.

## Rally defaults

- A visible allied warrior or archer within 16 blocks must have a live threat, be retreating, or already be panicking. Its last observed raw confidence must be within 15 points above its species' panic-entry threshold or lower (zero points with the default −15 threshold).
- One needy ally selects level one, costing 80 mana. Two or more select level two if 140 mana is available; otherwise the shaman can rally the weakest individual for 80 mana. Group Rally affects nearby visible allied goblins, including healthy allies. The caster is excluded.
- Targets already under Rally are excluded. Dead, buried, AI-disabled, out-of-range or obscured allies cannot be selected. Targets and mana are checked again at release. Interrupted or invalid casts spend no mana and retry after four seconds.
- The buff lasts 400 ticks (20 seconds), adds 40 raw confidence points through `self.rallied`, blocks panic and retreat actions, and enables advancing. It does not remove damage, stun, exhaustion, collision or pathfinding restrictions. It does not change the in-rift flag.
- The full cooldown is 600 ticks (30 seconds), modified by the caster's normal cooldown reduction. The shaman needs a safe casting distance or existing cover. Nearby allies are searched at most once every ten ticks.

Iron's spell configuration controls enabled status, school, levels, mana multiplier and cooldown. The server confidence configuration can tune `self.rallied`, including per-species overrides. Existing configuration files receive its default without replacing explicit values. The status effect is saved normally, displays a Rally banner icon, and emits ordinary beneficial-effect particles; release adds a brief green particle burst and chime.

Diagnostics retain the usual confidence contribution breakdown and add `self.rallied` to behavior signals. Shaman persistent diagnostics include `RiftfallRalliesCast`, `RiftfallLastRallyLevel`, and `RiftfallNextRally`.

## Warrior interception

Warriors protect both archers and shamans. Up to two warriors per nearby ranged ally take screening assignments; surplus warriors retain flanking behavior. Interception predicts a short portion of the enemy's horizontal movement, uses separate lateral lanes, and plans a reachable blocking position across the approach. It takes priority over ordinary pursuit and uncommitted attacks, while committed attacks and interruptions retain their existing priorities. At melee contact it yields to ordinary attacks.

Formation membership remains cached for ten ticks. Interception plans refresh at most every five ticks, and navigation reuses the selected path between refreshes. Interception does not introduce a leader, subordinate roster, or command hierarchy.

Warriors receive Epic Fight's Step in an empty Dodge slot once, preserving an existing assigned skill. Its NPC adapter can dash toward an interception waypoint 2.5–8 blocks away on approximately level terrain. A short swept-body and floor check rejects blocked or unsupported moves. It uses Epic Fight's forward Step animation and movement, pays the normal skill stamina cost (six by default), and has an 80-tick cooldown. It does not teleport or apply a dash attack's damage collider. A raised shield is lowered before Step takes control and can be raised again after the action ends.

Archers receive an additional 10% movement multiplier while sprinting. Walking speed and shaman speed are unchanged. Sprinting and Step still use the existing stamina restrictions.

`RiftfallIntercepting`, `RiftfallProtectedAlly`, `RiftfallInterceptX/Z` and `RiftfallInterceptDashes` help inspect formation behavior. Transient interception state is cleared when old goblins load.

## Automatic return to shamans

Warriors and archers more than 12 blocks from the nearest visible, reachable allied shaman can return toward it, stopping within 6 blocks. The search is bounded to 48 blocks; it runs through the existing regroup executor with a 20-tick search cooldown and 10-tick path refresh. Shamans do not receive this action.

The `return_to_shaman` behavior outranks ordinary pursuit and bow attacks. A visible current enemy within 4 blocks prevents or interrupts the return; panic, healing retreats, defensive abilities, and committed attacks retain priority. Unreachable routes and routes crossing too close to the visible enemy are rejected. The separate start and arrival distances prevent repeated starts at the tether boundary. This movement does not apply the Rally spell buff.
