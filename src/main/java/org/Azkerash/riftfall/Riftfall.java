package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Riftfall.MODID)
public class Riftfall {
    public static final String MODID = "riftfall";

    public Riftfall(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.register(RiftEvents.class);
    }
}
