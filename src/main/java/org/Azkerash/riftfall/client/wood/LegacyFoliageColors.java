package org.Azkerash.riftfall.client.wood;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.Azkerash.riftfall.wood.LegacyWood;

/** Primitive building leaves use vanilla biome tinting; DT handles the living leaves. */
@EventBusSubscriber(modid="riftfall",value=Dist.CLIENT)
public final class LegacyFoliageColors {
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){
        event.register((state,world,pos,tint)->world!=null&&pos!=null
            ?BiomeColors.getAverageFoliageColor(world,pos):FoliageColor.getDefaultColor(),LegacyWood.LEAVES.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->FoliageColor.getDefaultColor(),LegacyWood.LEAVES.get());
    }
    private LegacyFoliageColors(){}
}
