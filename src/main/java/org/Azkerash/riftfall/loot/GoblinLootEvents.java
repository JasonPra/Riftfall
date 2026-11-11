package org.Azkerash.riftfall.loot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.Azkerash.riftfall.*;
@EventBusSubscriber(modid=Riftfall.MODID)
public final class GoblinLootEvents {
    @SubscribeEvent public static void equipment(LivingEquipmentChangeEvent event){if(event.getEntity() instanceof GoblinEntity goblin)goblin.loot().changed();}
    private GoblinLootEvents(){}
}
