package org.Azkerash.riftfall.spells;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.Azkerash.riftfall.Riftfall;

public final class RiftfallSpells {
    private static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, Riftfall.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Riftfall.MODID);
    public static final DeferredHolder<AbstractSpell, RallySpell> RALLY =
            SPELLS.register("rally", RallySpell::new);
    public static final DeferredHolder<MobEffect, MobEffect> RALLIED =
            EFFECTS.register(
                    "rallied", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE9C85D) {});

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
        EFFECTS.register(bus);
    }

    private RiftfallSpells() {}
}
