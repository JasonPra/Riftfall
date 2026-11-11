# Grapples and shared ambush recovery

Grapple and backstab attempts share `AmbushCooldown` for NPCs. When an NPC commits to either attack, it stamps itself and living mobs of the exact same entity type within a spherical 16-block radius with a recovery deadline. Both attack entry points check that deadline, including a second check when execution starts. A previously longer deadline is preserved.

Defaults are the attack animation duration plus **200 ticks (10 seconds)** of recovery. For the four-second goblin grapple, another nearby warrior cannot start either ambush for 14 seconds from commitment. Interrupted or escaped attempts retain the cooldown. Ordinary attacks, leaps and player backstab skills retain their existing rules. Cooldowns are saved in entity persistent data as `RiftfallAmbushReady`; they apply to mobs present and loaded when the attempt begins. This uses one local scan per committed attempt and constant-time readiness checks, without a world-wide tick scan or retained entity references.

## Reusing a grapple

`GrappleGoal` works with `Mob` actors and living victims. It owns rear-position checks, restraint, climbing, interruption, release, server-authoritative escape presses and cleanup. The actor requires an Epic Fight living-entity patch and an animation compatible with its armature. Species eligibility and optional synchronized visuals are passed in:

```java
var grapple = new GrappleGoal(
    entity,
    MyAnimations.GRAPPLE,             // AnimationAccessor<? extends AttackAnimation>
    MyAnimations.GRAPPLE_RELEASE,     // optional ActionAnimation accessor; may be null
    new GrappleGoal.Settings(1.5, .45, .55, 8, 200, 16),
    actor -> canGrappleWithEquipment(actor),
    new GrappleGoal.Visuals() {
        // Optionally synchronize target ID / escape progress for your renderer.
        @Override public void state(Mob actor, LivingEntity victim, int progress) {}
        @Override public void released(Mob actor, LivingEntity victim, boolean escaped) {}
    }
);
```

Settings are reach, rear offset, climb height, climb ticks, recovery ticks and shared-cooldown radius. Add the goal to your weighted engine through `GoalBehaviorAction`, or to the entity's normal goal selector; do not schedule the same instance through both. The weighted goblin engine still owns goblin scheduling. `GoblinGrapple` now only supplies equipment policy and goblin visuals.

The animation's total duration sets the hold duration. Its Epic Fight `AttackAnimation.Phase` definitions set active damage windows, damage, stun and the weapon joint/collider. A phase still requires actual swept collider contact; the grapple does not apply range-based damage. Goblin grapple retains four phases with one damage point each and no hit stun. There is no separate hardcoded `stabsAt(tick)` damage schedule.

Player victims can escape with six distinct attack presses (packet bursts cannot count as multiple presses). Other living victims release at the end or on interruption. Death, removal, dimension changes, disconnect, stun, equipment-policy changes and server shutdown release the restraint and restore actor gravity/physics. Goblins continue to use grapples only without a shield; shielded warriors use their backstab skill.

## Validation

The clean automated suite checks cooldown arithmetic (including long world times and preserving later deadlines), settings validation, and existing combat/animation behavior. A temporary native-server harness checked local type/radius boundaries, committed grapple continuation, interruption cleanup and alternate animation selection on a zombie actor. A live client test verified one point of phase-driven damage, restraint, six-button escape and restored movement for that non-goblin actor. The temporary harness is excluded from the release. Visual backstab choreography was unchanged and was not revalidated by these checks.
