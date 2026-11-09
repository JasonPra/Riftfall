package org.Azkerash.riftfall.facial;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID)
public final class FacialEvents {
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        var entity=event.getEntity();
        if (!entity.level().isClientSide && entity.isAlive() && entity instanceof FacialParticipant participant)
            participant.facialController().tick();
    }
    private FacialEvents() {}
}
