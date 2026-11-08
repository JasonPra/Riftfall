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
        RiftEntities.TYPES.register(bus);
        RiftBlocks.register(bus);
        GoblinItems.register(bus);
        bus.addListener(RiftEntities::attributes);
        bus.addListener(GrappleEscapePayload::register);
        bus.addListener(RiftCastingAnimations::register);
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(RiftEntities::patches);
        bus.addListener(RiftMobs::attributes);
        CrystalWorldgen.FEATURES.register(bus);
        SoulEssenceEvents.TYPES.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.register(RiftEvents.class);
        NeoForge.EVENT_BUS.register(RiftSpellcasting.class);
        NeoForge.EVENT_BUS.register(GoblinTactics.class);
        NeoForge.EVENT_BUS.register(GoblinGrapple.class);
        NeoForge.EVENT_BUS.register(SoulEssenceEvents.class);
    }
}
