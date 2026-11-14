package org.Azkerash.riftfall.client.flora;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.Azkerash.riftfall.flora.ManaheartFlora;
@EventBusSubscriber(modid="riftfall",value=Dist.CLIENT)
public final class ManaheartFloraColors {
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){event.register((state,world,pos,tint)->world!=null&&pos!=null?net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(world,pos):net.minecraft.world.level.GrassColor.getDefaultColor(),ManaheartFlora.FERN.get(),ManaheartFlora.SHRUB.get());}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){event.register((stack,tint)->net.minecraft.world.level.GrassColor.getDefaultColor(),ManaheartFlora.FERN.get().asItem(),ManaheartFlora.SHRUB.get().asItem());}
 private ManaheartFloraColors(){}
}
