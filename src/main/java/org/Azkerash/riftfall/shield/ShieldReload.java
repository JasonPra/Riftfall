package org.Azkerash.riftfall.shield;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid=Riftfall.MODID)
public final class ShieldReload {
    @SubscribeEvent public static void reload(AddReloadListenerEvent event){event.addListener((ResourceManagerReloadListener)manager->ShieldModels.clear());}
    private ShieldReload(){}
}
