package org.Azkerash.riftfall.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.confidence.ConfidenceEngine;

/** Snapshot for one server tick. Pursuit actions should use lastSeenPosition when targetVisible is false. */
public record CombatSituation(BehaviorContext context,ConfidenceEngine.Result confidence,LivingEntity target,
        boolean targetVisible,Vec3 lastSeenPosition) {}
