package org.Azkerash.riftfall;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Shared, read-only reach prediction for Epic Fight-equipped attackers and attacks. */
public final class WeaponReach {
    /** Checks the equipped collider along the planned swing without playing it or dealing damage. */
    public static boolean canContact(Mob attacker,LivingEntity target,AttackAnimation animation) {
        return canContact((LivingEntity)attacker,target,animation);
    }
    public static boolean canContact(LivingEntity attacker,LivingEntity target,AttackAnimation animation) {
        if (target==null || !target.isAlive() || attacker.level()!=target.level()) return false;
        var patch=EpicFightCapabilities.getEntityPatch(attacker,LivingEntityPatch.class);
        if (patch==null) return false;
        for (var phase:animation.phases) {
            // Sample ordinary tick intervals as well as each collider's own sweep samples.
            // This also handles add-ons supplying a single OBB instead of a multi-collider.
            for (float from=phase.preDelay;from<phase.contact;) {
                float to=Math.min(phase.contact,from+.05F);
                if (phase.getCollidingEntities(patch,animation,from,to,1F).contains(target)) return true;
                from=to;
            }
        }
        return false;
    }
    private WeaponReach() {}
}
