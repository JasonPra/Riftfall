package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Composition root; each registration concern has one explicit owner. */
@Mod(Riftfall.MODID)
public final class Riftfall {
    public static final String MODID = "riftfall";

    public Riftfall(IEventBus bus, ModContainer container) {
        RiftfallConfiguration.register(bus, container);
        RiftfallContent.register(bus);
        RiftfallNetworking.register(bus);
        RiftfallCombat.register(bus);
    }
}
