package org.Azkerash.riftfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Local, positional goblin voices shared by all three variants. */
public final class GoblinSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,Riftfall.MODID);
    static final DeferredHolder<SoundEvent,SoundEvent> LAUGH=register("goblin_laugh");
    public static final DeferredHolder<SoundEvent,SoundEvent> SCREAM=register("goblin_scream");
    static final DeferredHolder<SoundEvent,SoundEvent> DEATH=register("goblin_death");
    private static DeferredHolder<SoundEvent,SoundEvent> register(String name) {
        return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,name)));
    }
    static void register(IEventBus bus){SOUNDS.register(bus);}
    private GoblinSounds() {}
}
