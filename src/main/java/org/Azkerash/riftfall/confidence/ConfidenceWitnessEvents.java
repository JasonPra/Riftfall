package org.Azkerash.riftfall.confidence;

import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.Azkerash.riftfall.Riftfall;

/** Event-driven witnessing: no entity registration map, polling for deaths, or references retained globally. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class ConfidenceWitnessEvents {
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void died(LivingDeathEvent event) {
        var victim=event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof LivingEntity killer) || killer==victim) return;
        for (var mob:victim.level().getEntitiesOfClass(Mob.class,victim.getBoundingBox().inflate(ConfidenceConfig.maxWitnessRadius()),
                observer->observer!=victim && observer.isAlive() && !observer.isNoAi() && observer instanceof ConfidenceParticipant))
            ((ConfidenceParticipant)mob).confidenceController().witnessDeath(victim,killer);
    }
    private ConfidenceWitnessEvents() {}
}
