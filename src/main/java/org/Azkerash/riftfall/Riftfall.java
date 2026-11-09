package org.Azkerash.riftfall;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Riftfall.MODID)
public class Riftfall {
    public static final String MODID = "riftfall";

    public Riftfall(IEventBus bus) {
        GoblinEntities.TYPES.register(bus);
        GoblinItems.register(bus);
        bus.addListener(GoblinEntities::attributes);
        bus.addListener(GrappleEscapePayload::register);
        bus.addListener(GoblinAnimations::register);
        yesman.epicfight.api.event.EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(GoblinEntities::patches);
        NeoForge.EVENT_BUS.register(MobSpellcasting.class);
        NeoForge.EVENT_BUS.register(GoblinTactics.class);
        NeoForge.EVENT_BUS.register(GoblinHostility.class);
        NeoForge.EVENT_BUS.register(GoblinGrapple.class);
    }
}
