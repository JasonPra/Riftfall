package org.Azkerash.riftfall.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.Azkerash.riftfall.*;
/** Mouse look is deliberately untouched; Epic Fight's model yaw is held separately. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class BackstabMovementInput {
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(BackstabActorLock.clientLocked(e.getEntity())){
        var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;e.getEntity().setSprinting(false);
    }}
    private BackstabMovementInput(){}
}
