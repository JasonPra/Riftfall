package org.Azkerash.riftfall.client.titles;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid = Riftfall.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ArrivalTitleLayer {
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID, "arrival_titles"), ArrivalTitles::render);
    }
    private ArrivalTitleLayer() {}
}
