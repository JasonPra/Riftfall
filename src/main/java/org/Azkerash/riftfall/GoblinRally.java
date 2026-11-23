package org.Azkerash.riftfall;

import net.minecraft.world.entity.LivingEntity;

import org.Azkerash.riftfall.confidence.ConfidenceConfig;
import org.Azkerash.riftfall.spells.RiftfallSpells;

import java.util.*;

/** Goblin membership, low-morale eligibility and bounded Rally targeting. */
public final class GoblinRally {
    public static final int RANGE = 16;

    public static boolean active(LivingEntity entity) {
        return entity.hasEffect(RiftfallSpells.RALLIED);
    }

    public static boolean reachable(LivingEntity caster, GoblinEntity ally) {
        return ally != caster
                && ally.isAlive()
                && !ally.isNoAi()
                && !ally.isFullyBuried()
                && caster.isAlliedTo(ally)
                && caster.distanceToSqr(ally) <= RANGE * RANGE
                && caster.hasLineOfSight(ally);
    }

    public static boolean needsHelp(GoblinEntity ally) {
        var data = ally.getPersistentData();
        if (active(ally) || ally.isShaman() || !data.contains("RiftfallConfidencePoints")) {
            return false;
        }
        boolean threatened =
                ally.getTarget() != null && ally.getTarget().isAlive()
                        || ally.isPanicking()
                        || data.getBoolean("RiftfallRetreating");
        String id =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .getKey(ally.getType())
                        .toString();
        return threatened
                && RallyPolicy.atRisk(
                        data.getDouble("RiftfallConfidencePoints"),
                        ConfidenceConfig.panicSettingsFor(id).entryPoints());
    }

    public static List<GoblinEntity> allies(LivingEntity caster) {
        return caster.level()
                .getEntitiesOfClass(
                        GoblinEntity.class,
                        caster.getBoundingBox().inflate(RANGE),
                        ally -> reachable(caster, ally) && !active(ally));
    }

    public static List<GoblinEntity> needingHelp(LivingEntity caster) {
        return allies(caster).stream()
                .filter(GoblinRally::needsHelp)
                .sorted(
                        Comparator.<GoblinEntity>comparingDouble(
                                        g ->
                                                g.getPersistentData()
                                                        .getDouble("RiftfallConfidencePoints"))
                                .thenComparing(GoblinEntity::getUUID))
                .toList();
    }

    public static void refresh(GoblinEntity ally) {
        ally.behavior.awareness.invalidate();
    }

    private GoblinRally() {}
}
